package com.freedomclient.test;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.visual.ZoomModule;
import com.freedomclient.ui.menu.FreedomMenuScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Abre un mundo y el menú de FreedomClient en cada pestaña, y guarda capturas para revisar el diseño. */
@SuppressWarnings("UnstableApiUsage")
public class MenuScreenshotTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1920, 1080);

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getClientWorld().waitForChunksRender();
			context.waitTicks(20);
			context.takeScreenshot("hud");

			for (FreedomMenuScreen.Tab tab : FreedomMenuScreen.Tab.values()) {
				context.setScreen(() -> {
					FreedomMenuScreen screen = new FreedomMenuScreen();
					screen.setTab(tab);
					return screen;
				});
				context.waitTicks(10);
				context.takeScreenshot("menu_" + tab.name().toLowerCase(java.util.Locale.ROOT));
			}

			context.setScreen(() -> {
				FreedomMenuScreen screen = new FreedomMenuScreen();
				screen.setTab(FreedomMenuScreen.Tab.MODS);
				screen.openModule(FreedomClient.getModuleManager().get(ZoomModule.class));
				return screen;
			});
			context.waitTicks(10);
			context.takeScreenshot("menu_settings_zoom");

			// Misma vista con la escala de interfaz 2, para ver la ventana compacta en pantallas grandes.
			context.runOnClient(client -> {
				client.options.guiScale().set(2);
				client.resizeDisplay();
			});
			context.setScreen(() -> {
				FreedomMenuScreen screen = new FreedomMenuScreen();
				screen.setTab(FreedomMenuScreen.Tab.MODS);
				return screen;
			});
			context.waitTicks(10);
			context.takeScreenshot("menu_mods_guiscale2");

			context.setScreen(() -> null);
		}
	}
}
