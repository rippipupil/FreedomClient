package com.freedomclient.module.visual;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.ui.menu.FreedomMenuScreen;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.List;
import java.util.Locale;

/**
 * Visuals: cambios visuales solo para ti.
 * - Hora y clima propios (el servidor no se entera; solo cambia lo que ves).
 * - Cielo de atardecer FC (la hora fija en el atardecer).
 * - Física de objetos: los objetos tirados quedan tumbados en el suelo sin girar ni flotar.
 * - Color del brillo de encantamiento, con paquetes de recursos integrados.
 */
public class VisualsModule extends Module {
	private static final List<String> GLINTS = List.of("Red", "Gold", "Sky", "Pink", "White", "Purple", "Green");
	private static VisualsModule instance;

	private final ModeSetting time = add(new ModeSetting("Time", "Time of day you see. FC Sunset is the client's red sunset sky.", "Server",
			"Server", "Day", "FC Sunset", "Night", "Custom"));
	private final NumberSetting customTime = add(new NumberSetting("Custom time", "0 = sunrise, 6000 = noon, 12000 = sunset, 18000 = midnight.",
			6000, 0, 23999, 250));
	private final ModeSetting weather = add(new ModeSetting("Weather", "Weather you see.", "Server", "Server", "Clear", "Rain", "Thunder"));
	private final BooleanSetting itemPhysics = add(new BooleanSetting("Item physics", "Dropped items lie flat on the ground instead of floating and spinning.", true));
	private final ModeSetting glint = add(new ModeSetting("Glint color", "Color of the enchantment glint. Applied when you close the menu.", "Vanilla",
			"Vanilla", "Red", "Gold", "Sky", "Pink", "White", "Purple", "Green"));

	public VisualsModule() {
		super("Visuals", "Your own time and weather, FC sunset sky, item physics and enchantment glint color.", Category.VISUAL, true);
		instance = this;
		customTime.visibleWhen(() -> time.is("Custom"));
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	private static VisualsModule active() {
		return instance != null && instance.isEnabled() ? instance : null;
	}

	/** Hora que se ve, o la del servidor si no se cambia. */
	public static long dayTime(long server) {
		VisualsModule module = active();
		if (module == null) return server;
		long day = Math.floorDiv(server, 24000L) * 24000L;
		return switch (module.time.get()) {
			case "Day" -> day + 6000L;
			case "FC Sunset" -> day + 12600L;
			case "Night" -> day + 18000L;
			case "Custom" -> day + module.customTime.getInt();
			default -> server;
		};
	}

	/** Nivel de lluvia que se ve (0..1), o -1 para usar el del servidor. */
	public static float rainLevel() {
		VisualsModule module = active();
		if (module == null) return -1.0F;
		return switch (module.weather.get()) {
			case "Clear" -> 0.0F;
			case "Rain", "Thunder" -> 1.0F;
			default -> -1.0F;
		};
	}

	/** Nivel de tormenta que se ve (0..1), o -1 para usar el del servidor. */
	public static float thunderLevel() {
		VisualsModule module = active();
		if (module == null) return -1.0F;
		return switch (module.weather.get()) {
			case "Clear", "Rain" -> 0.0F;
			case "Thunder" -> 1.0F;
			default -> -1.0F;
		};
	}

	public static boolean itemPhysics() {
		VisualsModule module = active();
		return module != null && module.itemPhysics.get();
	}

	// --- Color del brillo de encantamiento: un paquete de recursos integrado por color ---

	public static void registerPacks() {
		FabricLoader.getInstance().getModContainer(FreedomClient.MOD_ID).ifPresent(container -> {
			for (String color : GLINTS) {
				String name = color.toLowerCase(Locale.ROOT);
				ResourceLoader.registerBuiltinPack(FreedomClient.id("glint_" + name), container,
						Component.literal("FreedomClient " + color + " Glint"), PackActivationType.NORMAL);
			}
		});
	}

	private static String packId(PackRepository repository, String color) {
		String suffix = "glint_" + color.toLowerCase(Locale.ROOT);
		for (String id : repository.getAvailableIds()) {
			if (id.endsWith(suffix)) return id;
		}
		return null;
	}

	/** Deja la opción igual que los paquetes activos (por si se cambiaron desde la pantalla de paquetes). */
	public void syncWithPacks(Minecraft client) {
		PackRepository repository = client.getResourcePackRepository();
		String selected = "Vanilla";
		for (String color : GLINTS) {
			String id = packId(repository, color);
			if (id != null && repository.getSelectedIds().contains(id)) selected = color;
		}
		if (isEnabled()) glint.set(selected);
	}

	@Override
	public void onTick(Minecraft client) {
		// El cambio de color recarga los recursos, así que se aplica al cerrar el menú.
		if (!(client.screen instanceof FreedomMenuScreen)) applyGlint(client, glint.get());
	}

	@Override
	protected void onDisable(Minecraft client) {
		applyGlint(client, "Vanilla");
	}

	private static void applyGlint(Minecraft client, String wanted) {
		PackRepository repository = client.getResourcePackRepository();
		boolean changed = false;
		for (String color : GLINTS) {
			String id = packId(repository, color);
			if (id == null) continue;
			boolean shouldBeOn = color.equals(wanted);
			boolean isOn = repository.getSelectedIds().contains(id);
			if (shouldBeOn && !isOn) changed |= repository.addPack(id);
			if (!shouldBeOn && isOn) changed |= repository.removePack(id);
		}
		if (changed) {
			client.options.updateResourcePacks(repository);
		}
	}
}
