package com.freedomclient.test;

import com.freedomclient.FreedomClient;
import com.freedomclient.hud.HudEditorScreen;
import com.freedomclient.hud.HudModule;
import com.freedomclient.module.Module;
import com.freedomclient.module.visual.ZoomModule;
import com.freedomclient.ui.menu.FreedomMenuScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.util.Locale;

/** Abre un mundo, prepara el jugador y guarda capturas del HUD, su editor y el menú en cada pestaña. */
@SuppressWarnings("UnstableApiUsage")
public class MenuScreenshotTest implements FabricClientGameTest {
	private static final String[] SETUP_COMMANDS = {
			"gamemode survival @a",
			"time set noon",
			"weather clear",
			"item replace entity @a armor.head with diamond_helmet",
			"item replace entity @a armor.chest with diamond_chestplate[damage=500]",
			"item replace entity @a armor.legs with iron_leggings",
			"item replace entity @a armor.feet with golden_boots",
			"item replace entity @a weapon.mainhand with diamond_sword",
			"item replace entity @a weapon.offhand with cooked_beef 16",
			"give @a ender_pearl 16",
			"give @a golden_apple 12",
			"give @a totem_of_undying 2",
			"give @a splash_potion 5",
			"give @a arrow 64",
			"give @a cobblestone 64",
			"effect give @a speed 120 1",
			"effect give @a strength 8 0",
			"effect give @a fire_resistance 300 0",
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1920, 1080);

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getClientWorld().waitForChunksRender();
			for (String command : SETUP_COMMANDS) {
				singleplayer.getServer().runCommand(command);
			}
			// Baja el hambre para ver la vista previa de AppleSkin.
			singleplayer.getServer().runOnServer(server -> server.getPlayerList().getPlayers()
					.forEach(player -> player.getFoodData().setFoodLevel(12)));

			context.runOnClient(client -> {
				for (Module module : FreedomClient.getModuleManager().getModules()) {
					if (module instanceof HudModule) module.setEnabled(true);
				}
			});
			context.waitTicks(40);
			// Quita las notificaciones de logros para que no tapen las capturas.
			context.runOnClient(client -> client.getToastManager().clear());
			context.waitTicks(2);
			context.takeScreenshot("hud");

			context.setScreen(() -> new HudEditorScreen(null));
			context.waitTicks(10);
			context.takeScreenshot("hud_editor");

			for (FreedomMenuScreen.Tab tab : FreedomMenuScreen.Tab.values()) {
				context.setScreen(() -> {
					FreedomMenuScreen screen = new FreedomMenuScreen();
					screen.setTab(tab);
					return screen;
				});
				context.waitTicks(10);
				context.takeScreenshot("menu_" + tab.name().toLowerCase(Locale.ROOT));
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
