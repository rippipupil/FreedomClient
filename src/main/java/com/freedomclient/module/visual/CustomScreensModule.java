package com.freedomclient.module.visual;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.BooleanSetting;

/** Activa la pantalla de carga y el menú principal propios de FreedomClient. */
public class CustomScreensModule extends Module {
	private final BooleanSetting loadingScreen = add(new BooleanSetting("Loading screen", "Animated FreedomClient loading screen.", true));
	private final BooleanSetting mainMenu = add(new BooleanSetting("Main menu", "FreedomClient main menu instead of Minecraft's.", true));

	public CustomScreensModule() {
		super("Client Screens", "FreedomClient loading screen and main menu with the pixel sky.", Category.VISUAL, true);
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

	public static boolean mainMenuEnabled() {
		CustomScreensModule module = instance();
		return module != null && module.isEnabled() && module.mainMenu.get();
	}
}
