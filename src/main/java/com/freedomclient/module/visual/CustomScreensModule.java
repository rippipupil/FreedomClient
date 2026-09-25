package com.freedomclient.module.visual;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.ui.scene.PixelSky;

import java.time.LocalTime;

/** Activa la pantalla de carga y el menú principal propios de FreedomClient. */
public class CustomScreensModule extends Module {
	private final BooleanSetting loadingScreen = add(new BooleanSetting("Loading screen", "Animated FreedomClient loading screen.", true));
	private final BooleanSetting mainMenu = add(new BooleanSetting("Main menu", "FreedomClient main menu instead of Minecraft's.", true));
	private final ModeSetting sky = add(new ModeSetting("Menu sky", "Background of the main menu and loading screen. Real time follows your clock.",
			"Sunset", "Sunset", "Starry night", "Day", "Real time"));
	private final BooleanSetting parallax = add(new BooleanSetting("Parallax", "The sky layers move with your mouse in the main menu.", true));

	public CustomScreensModule() {
		super("Client Screens", "FreedomClient loading screen and main menu with a pixel sky: sunset, starry night, day or real time.", Category.VISUAL, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	private static CustomScreensModule instance() {
		ModuleManager manager = FreedomClient.getModuleManager();
		return manager == null ? null : manager.get(CustomScreensModule.class);
	}

	public static boolean loadingScreenEnabled() {
		CustomScreensModule module = instance();
		return module != null && module.isEnabled() && module.loadingScreen.get();
	}

	public static PixelSky.Style skyStyle() {
		CustomScreensModule module = instance();
		String mode = module == null ? "Sunset" : module.sky.get();
		if (mode.equals("Real time")) {
			// Día de 8 a 18, atardecer de 18 a 21 y de 6 a 8, y noche el resto.
			int hour = LocalTime.now().getHour();
			if (hour >= 8 && hour < 18) return PixelSky.Style.DAY;
			if (hour >= 18 && hour < 21 || hour >= 6 && hour < 8) return PixelSky.Style.SUNSET;
			return PixelSky.Style.NIGHT;
		}
		return switch (mode) {
			case "Starry night" -> PixelSky.Style.NIGHT;
			case "Day" -> PixelSky.Style.DAY;
			default -> PixelSky.Style.SUNSET;
		};
	}

	public static boolean parallaxEnabled() {
		CustomScreensModule module = instance();
		return module == null || module.parallax.get();
	}

	public static boolean mainMenuEnabled() {
		CustomScreensModule module = instance();
		return module != null && module.isEnabled() && module.mainMenu.get();
	}
}
