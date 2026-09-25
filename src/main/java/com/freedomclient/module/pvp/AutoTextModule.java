package com.freedomclient.module.pvp;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.KeybindSetting;
import com.freedomclient.setting.StringSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Envía un mensaje o comando al pulsar una tecla (por ejemplo "gg" o "/spawn"). */
public class AutoTextModule extends Module {
	private static final int SLOTS = 5;
	private static final long COOLDOWN_MS = 1000;

	private record Slot(KeybindSetting key, StringSetting text) {
	}

	private final List<Slot> slots = new ArrayList<>();
	private final Set<Integer> held = new HashSet<>();
	private long lastSentAt;

	public AutoTextModule() {
		super("AutoText", "Send a chat message or command with a key. Messages starting with / run as commands.", Category.PVP, true);
		String[] defaults = {"gg", "/spawn", "", "", ""};
		for (int i = 1; i <= SLOTS; i++) {
			KeybindSetting key = add(new KeybindSetting("Key " + i, "Key that sends message " + i + ".", KeybindSetting.NONE));
			StringSetting text = add(new StringSetting("Message " + i, "Text or /command to send.", defaults[i - 1], 256));
			slots.add(new Slot(key, text));
		}
	}

	@Override
	public void onTick(Minecraft client) {
		if (client.screen != null || client.player == null) {
			held.clear();
			return;
		}

		for (Slot slot : slots) {
			if (!slot.key().isBound() || slot.text().get().isBlank()) continue;
			int key = slot.key().get();
			boolean down = InputConstants.isKeyDown(client.getWindow(), key);
			if (down && held.add(key)) {
				send(client, slot.text().get().trim());
			} else if (!down) {
				held.remove(key);
			}
		}
	}

	private void send(Minecraft client, String message) {
		long now = System.currentTimeMillis();
		ClientPacketListener connection = client.getConnection();
		if (connection == null || now - lastSentAt < COOLDOWN_MS) return;
		lastSentAt = now;

		if (message.startsWith("/")) {
			connection.sendCommand(message.substring(1));
		} else {
			connection.sendChat(message);
		}
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
