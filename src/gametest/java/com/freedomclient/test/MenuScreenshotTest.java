package com.freedomclient.test;

import com.freedomclient.FreedomClient;
import com.freedomclient.hud.HudEditorScreen;
import com.freedomclient.hud.HudModule;
import com.freedomclient.module.Module;
import com.freedomclient.module.pvp.AttackIndicatorModule;
import com.freedomclient.module.pvp.BetterCrosshairModule;
import com.freedomclient.cosmetic.HaloCosmetic;
import com.freedomclient.cosmetic.PetBehavior;
import com.freedomclient.module.visual.CustomScreensModule;
import com.freedomclient.module.visual.HitParticlesModule;
import com.freedomclient.module.visual.VisualsModule;
import com.freedomclient.module.visual.ZoomModule;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.Setting;
import com.freedomclient.ui.menu.FreedomMenuScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import com.freedomclient.ui.scene.CrashGuardScreen;
import com.freedomclient.module.utility.CrashGuardModule;
import net.minecraft.CrashReport;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
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
			// Un montículo de hierba a un lado para ver Better Grass.
			"execute at @p run fill ~4 ~ ~5 ~7 ~1 ~8 grass_block",
			// Un cofre a la vista y otro detrás del montículo para Entity Culling.
			"execute at @p run setblock ~-3 ~ ~4 chest",
			"execute at @p run setblock ~6 ~ ~10 chest",
			// Objetos tirados (física de objetos de Visuals) y un soporte de armadura para las plumas de Hit Particles.
			"execute at @p run summon item ~-1 ~ ~3 {Item:{id:\"minecraft:diamond_sword\",count:1}}",
			"execute at @p run summon item ~1 ~ ~3 {Item:{id:\"minecraft:golden_apple\",count:1}}",
			"execute at @p run summon armor_stand ~2 ~ ~4 {NoGravity:1b}",
	};

	private static void setMode(Module module, String name, String value) {
		for (Setting<?> setting : module.getSettings()) {
			if (setting instanceof ModeSetting mode && mode.getName().equals(name)) mode.set(value);
		}
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1920, 1080);

		// Menú principal propio.
		context.waitFor(client -> client.screen instanceof com.freedomclient.ui.scene.FreedomTitleScreen);
		context.waitTicks(20);
		context.takeScreenshot("title_screen");

		// Fondos del menú principal: noche estrellada y día.
		context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(CustomScreensModule.class), "Menu sky", "Starry night"));
		context.waitTicks(3);
		context.takeScreenshot("title_night");
		context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(CustomScreensModule.class), "Menu sky", "Day"));
		context.waitTicks(3);
		context.takeScreenshot("title_day");
		context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(CustomScreensModule.class), "Menu sky", "Sunset"));

		// Abrir y cerrar Singleplayer y Multiplayer desde el menú principal (antes crasheaba al volver).
		context.runOnClient(client -> client.setScreen(new SelectWorldScreen(client.screen)));
		context.waitTicks(20);
		context.runOnClient(client -> client.screen.onClose());
		context.waitTicks(20);
		context.runOnClient(client -> client.setScreen(new JoinMultiplayerScreen(client.screen)));
		context.waitTicks(20);
		context.runOnClient(client -> client.screen.onClose());
		context.waitTicks(20);
		context.takeScreenshot("title_after_menus");

		// Crash Guard: se le pasa un informe de crasheo como el que genera el bucle del juego. (Lanzar la excepción
		// dentro de un tick rompe la sincronización tick a tick de las pruebas, así que se llama directamente.)
		context.runOnClient(client -> CrashGuardModule.tryRecover(client,
				new CrashReport("Ticking screen", new IllegalStateException("FreedomClient Crash Guard test"))));
		context.waitFor(client -> client.screen instanceof CrashGuardScreen, 20 * 10);
		context.waitTicks(10);
		context.takeScreenshot("crash_guard");
		context.setScreen(() -> null);
		context.waitFor(client -> client.screen instanceof com.freedomclient.ui.scene.FreedomTitleScreen);

		// Pantalla de carga: se fuerza una recarga de recursos para verla.
		context.runOnClient(client -> client.reloadResourcePacks());
		context.waitTicks(30);
		context.takeScreenshot("loading_screen");
		context.waitFor(client -> client.getOverlay() == null, 20 * 120);

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

			// Un waypoint delante del jugador.
			context.runOnClient(client -> {
				var player = client.player;
				com.freedomclient.waypoint.WaypointStore.add(new com.freedomclient.waypoint.Waypoint("Base",
						player.getBlockX() + 2, player.getBlockY(), player.getBlockZ() + 12,
						player.level().dimension().toString(), 0xFF5DADE2, false));
			});
			context.waitTicks(5);
			context.takeScreenshot("waypoint");

			// Cosméticos en tercera persona (de espaldas: alas y capa; de frente: halo).
			context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
			context.waitTicks(10);
			context.takeScreenshot("cosmetics_back");
			context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(10);
			context.takeScreenshot("cosmetics_front");

			// Estilos de halo y reacciones de las mascotas (de frente).
			context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(HaloCosmetic.class), "Style", "Crown"));
			context.waitTicks(5);
			context.takeScreenshot("halo_crown");
			context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(HaloCosmetic.class), "Style", "Horns"));
			context.waitTicks(5);
			context.takeScreenshot("halo_horns");
			context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(HaloCosmetic.class), "Style", "Broken"));
			context.runOnClient(client -> PetBehavior.forceMood(PetBehavior.Mood.WAVE));
			context.waitTicks(10);
			context.takeScreenshot("pets_wave");
			context.runOnClient(client -> PetBehavior.forceMood(PetBehavior.Mood.SLEEP));
			context.waitTicks(10);
			context.takeScreenshot("pets_sleep");
			context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(HaloCosmetic.class), "Style", "Ring"));
			context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));

			// Visuals: cielo de atardecer FC.
			context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(VisualsModule.class), "Time", "FC Sunset"));
			context.waitTicks(10);
			context.takeScreenshot("visuals_sunset");
			context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(VisualsModule.class), "Time", "Server"));

			// Hit Particles: plumas al golpear el soporte de armadura.
			context.runOnClient(client -> {
				for (var entity : client.level.entitiesForRendering()) {
					if (entity instanceof net.minecraft.world.entity.decoration.ArmorStand) {
						FreedomClient.getModuleManager().get(HitParticlesModule.class).onHit(entity);
					}
				}
			});
			context.waitTicks(4);
			context.takeScreenshot("hit_particles");

			// Aviso de poca vida a 2 corazones.
			singleplayer.getServer().runOnServer(server -> server.getPlayerList().getPlayers().forEach(player -> player.setHealth(4.0F)));
			context.waitTicks(20);
			context.takeScreenshot("low_health");
			singleplayer.getServer().runOnServer(server -> server.getPlayerList().getPlayers().forEach(player -> player.setHealth(20.0F)));
			context.waitTicks(10);

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

			// Mira personalizada con el editor pixel visible.
			context.runOnClient(client -> {
				for (Setting<?> setting : FreedomClient.getModuleManager().get(BetterCrosshairModule.class).getSettings()) {
					if (setting instanceof ModeSetting mode && mode.getName().equals("Style")) mode.set("Custom");
				}
			});
			context.setScreen(() -> {
				FreedomMenuScreen screen = new FreedomMenuScreen();
				screen.openModule(FreedomClient.getModuleManager().get(BetterCrosshairModule.class));
				return screen;
			});
			context.waitTicks(10);
			context.takeScreenshot("menu_settings_crosshair");
			context.setScreen(() -> null);
			context.waitTicks(5);
			context.takeScreenshot("crosshair_custom");

			// Custom Attack Indicator cargando justo después de atacar.
			context.runOnClient(client -> {
				FreedomClient.getModuleManager().get(AttackIndicatorModule.class).setEnabled(true);
				client.player.resetAttackStrengthTicker();
			});
			context.waitTicks(4);
			context.takeScreenshot("attack_indicator");

			// Block Outline: mirando al suelo, el bloque apuntado se tiñe del color elegido.
			context.runOnClient(client -> client.player.setXRot(55.0F));
			context.waitTicks(5);
			context.takeScreenshot("block_outline");
			context.runOnClient(client -> client.player.setXRot(0.0F));

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
