package com.freedomclient.module.utility;

import com.freedomclient.FreedomClient;
import com.freedomclient.discord.DiscordIpc;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.StringSetting;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Discord Rich Presence: tu perfil de Discord muestra que juegas con FreedomClient y dónde.
 * Todo el trabajo con Discord va en un hilo aparte para no bajar los FPS.
 */
public class DiscordPresenceModule extends Module {
	/** Aplicación de Discord "FreedomClient" (su nombre y su logo son los que se ven en el perfil). */
	private static final String DEFAULT_APPLICATION_ID = "";

	private final StringSetting applicationId = add(new StringSetting("Application ID",
			"ID of the Discord application (from discord.com/developers).", DEFAULT_APPLICATION_ID, 32));
	private final BooleanSetting showServer = add(new BooleanSetting("Show server", "Show the server address you are playing on.", true));

	private ScheduledExecutorService executor;
	private DiscordIpc ipc;
	private String connectedId;
	private volatile String details = "In the menus";
	private volatile String state = "";
	private String sent;
	private final long startedAt = System.currentTimeMillis() / 1000;
	private long retryAt;

	public DiscordPresenceModule() {
		super("Discord Presence", "Shows FreedomClient and where you are playing on your Discord profile.", Category.UTILITY, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	@Override
	public void onTick(Minecraft client) {
		if (executor == null) {
			executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
				Thread thread = new Thread(runnable, "FreedomClient Discord");
				thread.setDaemon(true);
				return thread;
			});
			executor.scheduleWithFixedDelay(this::update, 1, 5, TimeUnit.SECONDS);
		}

		if (client.level == null) {
			details = "In the menus";
			state = "";
		} else if (client.isLocalServer()) {
			details = "Playing Singleplayer";
			state = "";
		} else {
			ServerData server = client.getCurrentServer();
			details = "Playing Multiplayer";
			state = showServer.get() && server != null ? server.ip : "";
		}
	}

	@Override
	protected void onDisable(Minecraft client) {
		if (executor != null) {
			ScheduledExecutorService old = executor;
			executor = null;
			old.execute(this::disconnect);
			old.shutdown();
		}
	}

	@Override
	public void onShutdown(Minecraft client) {
		onDisable(client);
	}

	/** En el hilo de Discord: conecta si hace falta y manda la actividad si ha cambiado. */
	private void update() {
		String id = applicationId.get().trim();
		if (id.isEmpty() || !id.chars().allMatch(Character::isDigit)) return;
		try {
			if (ipc == null || !id.equals(connectedId)) {
				disconnect();
				if (System.currentTimeMillis() < retryAt) return;
				ipc = DiscordIpc.connect(id);
				connectedId = id;
				sent = null;
			}
			String current = details + "|" + state;
			if (Objects.equals(current, sent)) return;
			ipc.setActivity(activity());
			sent = current;
		} catch (Exception e) {
			// Discord cerrado o sin permiso: se vuelve a intentar dentro de un rato, sin llenar el log.
			disconnect();
			retryAt = System.currentTimeMillis() + 15_000;
		}
	}

	private JsonObject activity() {
		JsonObject activity = new JsonObject();
		activity.addProperty("details", details);
		if (!state.isEmpty()) activity.addProperty("state", state);
		JsonObject timestamps = new JsonObject();
		timestamps.addProperty("start", startedAt);
		activity.add("timestamps", timestamps);
		JsonObject assets = new JsonObject();
		assets.addProperty("large_image", "logo");
		assets.addProperty("large_text", FreedomClient.NAME + " for Minecraft 1.21.11");
		activity.add("assets", assets);
		return activity;
	}

	private void disconnect() {
		if (ipc != null) {
			try {
				ipc.setActivity(null);
			} catch (Exception ignored) {
			}
			ipc.close();
			ipc = null;
		}
	}
}
