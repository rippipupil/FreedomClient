package com.freedomclient.test;

import com.freedomclient.FreedomClient;
import com.freedomclient.hud.HudEditorScreen;
import com.freedomclient.hud.HudModule;
import com.freedomclient.module.Module;
import com.freedomclient.module.pvp.AttackIndicatorModule;
import com.freedomclient.module.pvp.BetterCrosshairModule;
import com.freedomclient.module.pvp.HitSoundsModule;
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
			"item replace entity @a weapon.mainhand with diamond_sword[damage=1500]",
			"item replace entity @a weapon.offhand with cooked_beef 16",
			"give @a ender_pearl 16",
			"give @a golden_apple 12",
			"give @a totem_of_undying 2",
			"give @a splash_potion 5",
			"give @a arrow 64",
			"give @a cobblestone 64",
			"scoreboard objectives add fc dummy \"FreedomClient\"",
			"scoreboard objectives setdisplay sidebar fc",
			"scoreboard players set Kills fc 12",
			"scoreboard players set Deaths fc 2",
			"scoreboard players set Streak fc 5",
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

	/** El módulo registrado de esa clase exacta. */
	private static Module cosmetic(Class<?> type) {
		for (Module module : FreedomClient.getModuleManager().getModules()) {
			if (module.getClass() == type) return module;
		}
		throw new IllegalArgumentException(type.getName());
	}

	/** Activa esos cosméticos, pone la cámara, espera un poco, hace la captura y los vuelve a apagar. */
	private static void shoot(ClientGameTestContext context, String name, net.minecraft.client.CameraType camera, Class<?>... types) {
		context.runOnClient(client -> {
			for (Class<?> type : types) cosmetic(type).setEnabled(true);
			client.options.setCameraType(camera);
		});
		context.waitTicks(12);
		context.takeScreenshot(name);
		context.runOnClient(client -> {
			for (Class<?> type : types) cosmetic(type).setEnabled(false);
		});
	}

	private static void setMode(Module module, String name, String value) {
		for (Setting<?> setting : module.getSettings()) {
			if (setting instanceof ModeSetting mode && mode.getName().equals(name)) mode.set(value);
		}
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1920, 1080);

		// Menú principal propio.
		// La primera vez que se abre una versión salen las novedades.
		context.waitFor(client -> client.screen instanceof com.freedomclient.ui.scene.WhatsNewScreen, 20 * 30);
		context.waitTicks(10);
		context.takeScreenshot("whats_new");
		context.runOnClient(client -> client.screen.onClose());
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

		// Tema Neon: noche de tormenta, logo en azul y amarillo, y el menú con la paleta de Neon.
		context.runOnClient(client -> {
			com.freedomclient.ui.theme.ThemeManager.setPreset(com.freedomclient.ui.theme.ThemePreset.NEON);
			com.freedomclient.ui.scene.PixelSky.forcedLightning = 90;
		});
		context.waitTicks(3);
		context.takeScreenshot("title_neon");
		context.runOnClient(client -> client.setScreen(new FreedomMenuScreen()));
		context.waitTicks(10);
		context.takeScreenshot("menu_neon");
		// Ajustes de un mod con el tema Neon (deslizador, interruptores y líneas de sección con el degradado).
		context.setScreen(() -> {
			FreedomMenuScreen screen = new FreedomMenuScreen();
			screen.openModule(FreedomClient.getModuleManager().get(com.freedomclient.module.performance.ParticleLimiterModule.class));
			return screen;
		});
		context.waitTicks(10);
		context.takeScreenshot("menu_neon_particles");
		context.runOnClient(client -> {
			client.screen.onClose();
			com.freedomclient.ui.theme.ThemeManager.setPreset(com.freedomclient.ui.theme.ThemePreset.RED_SUNSET);
			com.freedomclient.ui.scene.PixelSky.forcedLightning = -1;
		});
		context.waitTicks(5);

		// Abrir y cerrar Singleplayer y Multiplayer desde el menú principal (antes crasheaba al volver).
		// Con Client Screens → Other menus llevan el cielo del tema y los botones del cliente.
		context.runOnClient(client -> client.setScreen(new SelectWorldScreen(client.screen)));
		context.waitTicks(20);
		context.takeScreenshot("singleplayer_themed");
		context.runOnClient(client -> client.screen.onClose());
		context.waitTicks(20);
		context.runOnClient(client -> client.setScreen(new JoinMultiplayerScreen(client.screen)));
		context.waitTicks(20);
		context.takeScreenshot("multiplayer_themed");
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
			// Chunk Preloader en marcha durante toda la prueba (despacio), para comprobar que no rompe nada.
			context.runOnClient(client -> {
				var preloader = FreedomClient.getModuleManager().get(com.freedomclient.module.performance.ChunkPreloaderModule.class);
				setMode(preloader, "Speed", "Gentle");
				preloader.setEnabled(true);
			});
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

			// Soul Scythe a la espalda: sola (sin alas ni capa) y con las alas, de espaldas y de lado.
			context.runOnClient(client -> {
				var manager = FreedomClient.getModuleManager();
				manager.get(com.freedomclient.cosmetic.ScytheCosmetic.class).setEnabled(true);
				manager.get(com.freedomclient.cosmetic.WingsCosmetic.class).setEnabled(false);
				manager.get(com.freedomclient.cosmetic.CapeCosmetic.class).setEnabled(false);
				client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
			});
			context.waitTicks(10);
			context.takeScreenshot("scythe_back");
			context.runOnClient(client -> client.player.setYRot(client.player.getYRot() + 90.0F));
			context.waitTicks(10);
			context.takeScreenshot("scythe_side");
			context.runOnClient(client -> {
				client.player.setYRot(client.player.getYRot() - 90.0F);
				var manager = FreedomClient.getModuleManager();
				manager.get(com.freedomclient.cosmetic.WingsCosmetic.class).setEnabled(true);
				manager.get(com.freedomclient.cosmetic.CapeCosmetic.class).setEnabled(true);
			});
			context.waitTicks(10);
			context.takeScreenshot("scythe_with_wings");
			// Estilo Classic sin aura, para comparar con el 3D.
			context.runOnClient(client -> {
				var manager = FreedomClient.getModuleManager();
				var scythe = manager.get(com.freedomclient.cosmetic.ScytheCosmetic.class);
				setMode(scythe, "Style", "Classic");
				scythe.ghostAura.set(false);
				manager.get(com.freedomclient.cosmetic.WingsCosmetic.class).setEnabled(false);
				manager.get(com.freedomclient.cosmetic.CapeCosmetic.class).setEnabled(false);
			});
			context.waitTicks(10);
			context.takeScreenshot("scythe_classic");
			context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(com.freedomclient.cosmetic.ScytheCosmetic.class), "Style", "3D"));
			context.waitTicks(5);
			context.takeScreenshot("scythe_3d_no_aura");
			context.runOnClient(client -> {
				var manager = FreedomClient.getModuleManager();
				var scythe = manager.get(com.freedomclient.cosmetic.ScytheCosmetic.class);
				setMode(scythe, "Style", "3D");
				scythe.ghostAura.set(true);
				manager.get(com.freedomclient.cosmetic.WingsCosmetic.class).setEnabled(true);
				manager.get(com.freedomclient.cosmetic.CapeCosmetic.class).setEnabled(true);
			});
			// En primera persona el aura fantasmal no debe verse (ni partículas delante de la cámara).
			context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			context.waitTicks(40);
			context.takeScreenshot("scythe_first_person");
			context.runOnClient(client -> FreedomClient.getModuleManager().get(com.freedomclient.cosmetic.ScytheCosmetic.class).setEnabled(false));

			// Cosméticos Neon: mochila, mascota Funko, estela de rayo, pasos eléctricos y aura de descargas.
			context.runOnClient(client -> {
				var manager = FreedomClient.getModuleManager();
				manager.get(com.freedomclient.cosmetic.WingsCosmetic.class).setEnabled(false);
				manager.get(com.freedomclient.cosmetic.CapeCosmetic.class).setEnabled(false);
				manager.get(com.freedomclient.cosmetic.NeonPackCosmetic.class).setEnabled(true);
				manager.get(com.freedomclient.cosmetic.NeonPetCosmetic.class).setEnabled(true);
				manager.get(com.freedomclient.cosmetic.LightningTrailCosmetic.class).setEnabled(true);
				manager.get(com.freedomclient.cosmetic.NeonStepsCosmetic.class).setEnabled(true);
				setMode(manager.get(com.freedomclient.cosmetic.AuraCosmetic.class), "Style", "Neon discharges");
				PetBehavior.forceMood(PetBehavior.Mood.IDLE);
				client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
			});
			// Unos pasos hacia delante para que salgan la estela y las líneas de los pies.
			for (int i = 0; i < 14; i++) {
				context.runOnClient(client -> {
					var player = client.player;
					float yaw = player.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
					player.setPos(player.getX() - net.minecraft.util.Mth.sin(yaw) * 0.22, player.getY(), player.getZ() + net.minecraft.util.Mth.cos(yaw) * 0.22);
				});
				context.waitTicks(1);
			}
			context.takeScreenshot("neon_back");
			context.runOnClient(client -> client.player.setYRot(client.player.getYRot() + 90.0F));
			context.waitTicks(6);
			context.takeScreenshot("neon_side");
			context.runOnClient(client -> client.player.setYRot(client.player.getYRot() - 90.0F));
			context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(10);
			context.takeScreenshot("neon_front");
			// Golpe Neon: normal (una descarga) y crítico (varias), en el soporte de armadura.
			context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			context.runOnClient(client -> {
				for (var entity : client.level.entitiesForRendering()) {
					if (entity instanceof net.minecraft.world.entity.decoration.ArmorStand) {
						FreedomClient.getModuleManager().get(HitParticlesModule.class).neonHit(entity, false);
					}
				}
			});
			context.waitTicks(2);
			context.takeScreenshot("neon_hit");
			context.runOnClient(client -> {
				for (var entity : client.level.entitiesForRendering()) {
					if (entity instanceof net.minecraft.world.entity.decoration.ArmorStand) {
						FreedomClient.getModuleManager().get(HitParticlesModule.class).neonHit(entity, true);
					}
				}
			});
			context.waitTicks(2);
			context.takeScreenshot("neon_crit");
			context.runOnClient(client -> {
				var manager = FreedomClient.getModuleManager();
				manager.get(com.freedomclient.cosmetic.NeonPackCosmetic.class).setEnabled(false);
				manager.get(com.freedomclient.cosmetic.NeonPetCosmetic.class).setEnabled(false);
				manager.get(com.freedomclient.cosmetic.LightningTrailCosmetic.class).setEnabled(false);
				manager.get(com.freedomclient.cosmetic.NeonStepsCosmetic.class).setEnabled(false);
				setMode(manager.get(com.freedomclient.cosmetic.AuraCosmetic.class), "Style", "Angel light");
				manager.get(com.freedomclient.cosmetic.WingsCosmetic.class).setEnabled(true);
				manager.get(com.freedomclient.cosmetic.CapeCosmetic.class).setEnabled(true);
			});
			context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));

			// Cosméticos nuevos (Bee Swarm, FNAF, OneShot, Halloween, flores y capas), cada grupo por separado.
			Class<?>[] defaults = {com.freedomclient.cosmetic.WingsCosmetic.class, HaloCosmetic.class, com.freedomclient.cosmetic.CapeCosmetic.class,
					com.freedomclient.cosmetic.PetCosmetic.class, com.freedomclient.cosmetic.CloudPetCosmetic.class, com.freedomclient.cosmetic.AuraCosmetic.class};
			context.runOnClient(client -> {
				for (Class<?> type : defaults) cosmetic(type).setEnabled(false);
				PetBehavior.forceMood(PetBehavior.Mood.IDLE);
			});
			shoot(context, "mask_diamond", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, com.freedomclient.cosmetic.vox.DiamondMaskCosmetic.class);
			shoot(context, "mask_demon", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, com.freedomclient.cosmetic.vox.DemonMaskCosmetic.class);
			shoot(context, "mask_gummy", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, com.freedomclient.cosmetic.vox.GummyMaskCosmetic.class);
			shoot(context, "back_tide_popper", net.minecraft.client.CameraType.THIRD_PERSON_BACK, com.freedomclient.cosmetic.vox.TidePopperCosmetic.class);
			shoot(context, "back_dark_scythe", net.minecraft.client.CameraType.THIRD_PERSON_BACK, com.freedomclient.cosmetic.vox.DarkScytheCosmetic.class);
			shoot(context, "back_gummyballer", net.minecraft.client.CameraType.THIRD_PERSON_BACK, com.freedomclient.cosmetic.vox.GummyballerCosmetic.class);
			shoot(context, "bee_pets", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, com.freedomclient.cosmetic.vox.WindyBeeCosmetic.class,
					com.freedomclient.cosmetic.vox.TabbyBeeCosmetic.class);
			shoot(context, "fnaf_front", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, com.freedomclient.cosmetic.vox.FreddyHatCosmetic.class,
					com.freedomclient.cosmetic.vox.FreddyPetCosmetic.class);
			// Mr. Cupcake: parado a tu lado y a mitad de un salto después de alejarte un poco.
			shoot(context, "cupcake_front", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, com.freedomclient.cosmetic.vox.CupcakePetCosmetic.class);
			context.runOnClient(client -> {
				cosmetic(com.freedomclient.cosmetic.vox.CupcakePetCosmetic.class).setEnabled(true);
				client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
			});
			context.waitTicks(12);
			context.takeScreenshot("cupcake_back");
			singleplayer.getServer().runCommand("execute as @a at @s run tp @s ^ ^ ^3");
			context.waitTicks(9);
			context.takeScreenshot("cupcake_hop");
			context.waitTicks(40);
			context.runOnClient(client -> cosmetic(com.freedomclient.cosmetic.vox.CupcakePetCosmetic.class).setEnabled(false));
			// Bufanda de Niko de lado: las puntas van hacia atrás.
			context.runOnClient(client -> {
				cosmetic(com.freedomclient.cosmetic.vox.NikoScarfCosmetic.class).setEnabled(true);
				client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
				client.player.setYRot(client.player.getYRot() + 90.0F);
			});
			context.waitTicks(25);
			context.takeScreenshot("scarf_side");
			context.runOnClient(client -> {
				client.player.setYRot(client.player.getYRot() - 90.0F);
				cosmetic(com.freedomclient.cosmetic.vox.NikoScarfCosmetic.class).setEnabled(false);
			});
			context.waitTicks(25);
			// Hide Armor: sin casco ni pechera, pantalones ni botas (la armadura de las pruebas sigue puesta).
			context.runOnClient(client -> FreedomClient.getModuleManager().get(com.freedomclient.module.visual.HideArmorModule.class).setEnabled(true));
			shoot(context, "hide_armor", net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
			context.runOnClient(client -> FreedomClient.getModuleManager().get(com.freedomclient.module.visual.HideArmorModule.class).setEnabled(false));
			// Mascotas nuevas y sus emociones: corazoncitos, sonrojadas y volando con élitros.
			Class<?>[] pets = {com.freedomclient.cosmetic.vox.FreddyPetCosmetic.class, com.freedomclient.cosmetic.vox.MadelinePetCosmetic.class,
					com.freedomclient.cosmetic.vox.VerityPetCosmetic.class, com.freedomclient.cosmetic.vox.TabbyBeeCosmetic.class};
			context.runOnClient(client -> PetBehavior.forceEmotion(PetBehavior.Emotion.HEARTS));
			context.waitTicks(16);
			shoot(context, "pets_hearts", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, pets);
			context.runOnClient(client -> PetBehavior.forceEmotion(PetBehavior.Emotion.BLUSH));
			shoot(context, "pets_blush", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, pets);
			context.runOnClient(client -> PetBehavior.forceEmotion(PetBehavior.Emotion.NONE));
			context.runOnClient(client -> PetBehavior.forceFlying(true));
			shoot(context, "pets_flying", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, com.freedomclient.cosmetic.vox.FreddyPetCosmetic.class,
					com.freedomclient.cosmetic.vox.MadelinePetCosmetic.class, com.freedomclient.cosmetic.vox.VerityPetCosmetic.class,
					com.freedomclient.cosmetic.vox.CupcakePetCosmetic.class);
			shoot(context, "pets_flying_2", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, com.freedomclient.cosmetic.vox.NikoPetCosmetic.class,
					com.freedomclient.cosmetic.vox.TabbyBeeCosmetic.class, com.freedomclient.cosmetic.NeonPetCosmetic.class);
			context.runOnClient(client -> PetBehavior.forceFlying(false));
			shoot(context, "madeline_front", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, com.freedomclient.cosmetic.vox.MadelinePetCosmetic.class);
			shoot(context, "black_hole_back", net.minecraft.client.CameraType.THIRD_PERSON_BACK,
					com.freedomclient.cosmetic.vox.BlackHoleBackpackCosmetic.class);
			context.runOnClient(client -> setMode((Module) cosmetic(com.freedomclient.cosmetic.vox.BlackHoleBackpackCosmetic.class), "Style", "Void"));
			context.runOnClient(client -> client.player.setYRot(client.player.getYRot() + 60.0F));
			shoot(context, "black_hole_void_side", net.minecraft.client.CameraType.THIRD_PERSON_BACK,
					com.freedomclient.cosmetic.vox.BlackHoleBackpackCosmetic.class);
			context.runOnClient(client -> client.player.setYRot(client.player.getYRot() - 60.0F));
			shoot(context, "guitar_back", net.minecraft.client.CameraType.THIRD_PERSON_BACK, com.freedomclient.cosmetic.vox.GuitarCosmetic.class);
			shoot(context, "star_guitar_back", net.minecraft.client.CameraType.THIRD_PERSON_BACK, com.freedomclient.cosmetic.vox.StarGuitarCosmetic.class);
			context.runOnClient(client -> setMode((Module) cosmetic(HaloCosmetic.class), "Style", "Sun & Moon"));
			shoot(context, "halo_sun_moon", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, HaloCosmetic.class);
			context.runOnClient(client -> setMode((Module) cosmetic(HaloCosmetic.class), "Style", "Ring"));
			context.runOnClient(client -> setMode((Module) cosmetic(com.freedomclient.cosmetic.AuraCosmetic.class), "Style", "Clouds"));
			context.waitTicks(1);
			context.runOnClient(client -> cosmetic(com.freedomclient.cosmetic.AuraCosmetic.class).setEnabled(true));
			context.waitTicks(60);
			shoot(context, "aura_clouds", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, com.freedomclient.cosmetic.AuraCosmetic.class);
			context.runOnClient(client -> setMode((Module) cosmetic(com.freedomclient.cosmetic.AuraCosmetic.class), "Style", "Angel light"));
			// Muro de energía de Neon: el jugador avanza unos pasos con el rastro y los pasos puestos.
			context.runOnClient(client -> {
				cosmetic(com.freedomclient.cosmetic.LightningTrailCosmetic.class).setEnabled(true);
				cosmetic(com.freedomclient.cosmetic.NeonStepsCosmetic.class).setEnabled(true);
				client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
			});
			for (int step = 0; step < 14; step++) {
				singleplayer.getServer().runCommand("execute as @a at @s run tp @s ^ ^ ^-0.25");
				context.waitTicks(1);
			}
			context.waitTicks(2);
			context.takeScreenshot("neon_wall");
			context.runOnClient(client -> {
				cosmetic(com.freedomclient.cosmetic.LightningTrailCosmetic.class).setEnabled(false);
				cosmetic(com.freedomclient.cosmetic.NeonStepsCosmetic.class).setEnabled(false);
			});
			for (int step = 0; step < 14; step++) singleplayer.getServer().runCommand("execute as @a at @s run tp @s ^ ^ ^0.25");
			context.waitTicks(30);
			// Élitros con el diseño de la capa de FreedomClient.
			singleplayer.getServer().runCommand("item replace entity @a armor.chest with elytra");
			shoot(context, "elytra_cape", net.minecraft.client.CameraType.THIRD_PERSON_BACK, com.freedomclient.cosmetic.CapeCosmetic.class);
			singleplayer.getServer().runCommand("item replace entity @a armor.chest with air");
			shoot(context, "oneshot_front", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, com.freedomclient.cosmetic.vox.NikoHatCosmetic.class,
					com.freedomclient.cosmetic.vox.NikoScarfCosmetic.class, com.freedomclient.cosmetic.vox.NikoPetCosmetic.class);
			shoot(context, "oneshot_back", net.minecraft.client.CameraType.THIRD_PERSON_BACK, com.freedomclient.cosmetic.vox.NikoHatCosmetic.class,
					com.freedomclient.cosmetic.vox.NikoScarfCosmetic.class, com.freedomclient.cosmetic.vox.SunBackpackCosmetic.class);
			shoot(context, "pumpkin_happy", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, com.freedomclient.cosmetic.vox.JackOLanternCosmetic.class);
			context.runOnClient(client -> {
				((com.freedomclient.cosmetic.vox.JackOLanternCosmetic) cosmetic(com.freedomclient.cosmetic.vox.JackOLanternCosmetic.class)).forceEvil(true);
				cosmetic(com.freedomclient.cosmetic.vox.JackOLanternCosmetic.class).setEnabled(true);
				client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
			});
			context.waitTicks(3);
			context.takeScreenshot("pumpkin_evil");
			context.runOnClient(client -> cosmetic(com.freedomclient.cosmetic.vox.JackOLanternCosmetic.class).setEnabled(false));
			// Mascota Neon rehecha.
			shoot(context, "neon_pet_new", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, com.freedomclient.cosmetic.NeonPetCosmetic.class);
			// Con armadura: casco y máscara por fuera de él, pechera y mascota de hombro, élitros con alas y guadaña.
			singleplayer.getServer().runCommand("item replace entity @a armor.head with iron_helmet");
			singleplayer.getServer().runCommand("item replace entity @a armor.chest with diamond_chestplate");
			shoot(context, "armor_mask_front", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, com.freedomclient.cosmetic.vox.DemonMaskCosmetic.class,
					com.freedomclient.cosmetic.vox.FreddyPetCosmetic.class);
			singleplayer.getServer().runCommand("item replace entity @a armor.head with air");
			singleplayer.getServer().runCommand("item replace entity @a armor.chest with elytra");
			shoot(context, "elytra_back", net.minecraft.client.CameraType.THIRD_PERSON_BACK, com.freedomclient.cosmetic.WingsCosmetic.class,
					com.freedomclient.cosmetic.vox.DarkScytheCosmetic.class);
			singleplayer.getServer().runCommand("item replace entity @a armor.chest with air");
			// Angel Devil rehecha y la capa en sus dos estilos.
			shoot(context, "angel_devil_pet", net.minecraft.client.CameraType.THIRD_PERSON_FRONT, com.freedomclient.cosmetic.PetCosmetic.class,
					com.freedomclient.cosmetic.WingsCosmetic.class, HaloCosmetic.class);
			shoot(context, "cape_angel", net.minecraft.client.CameraType.THIRD_PERSON_BACK, com.freedomclient.cosmetic.CapeCosmetic.class);
			context.runOnClient(client -> setMode((Module) cosmetic(com.freedomclient.cosmetic.CapeCosmetic.class), "Style", "Neon"));
			shoot(context, "cape_neon", net.minecraft.client.CameraType.THIRD_PERSON_BACK, com.freedomclient.cosmetic.CapeCosmetic.class);
			context.runOnClient(client -> setMode((Module) cosmetic(com.freedomclient.cosmetic.CapeCosmetic.class), "Style", "Angel Devil"));
			// Flores al andar.
			for (Class<?> trail : new Class<?>[] {com.freedomclient.cosmetic.FlowerStepsCosmetic.class, com.freedomclient.cosmetic.AbyssFlowersCosmetic.class}) {
				context.runOnClient(client -> {
					for (var module : FreedomClient.getModuleManager().getModules()) {
						if (module.getClass() == trail) module.setEnabled(true);
					}
					client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
				});
				for (int i = 0; i < 16; i++) {
					context.runOnClient(client -> {
						var player = client.player;
						float yaw = player.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
						player.setPos(player.getX() - net.minecraft.util.Mth.sin(yaw) * 0.2, player.getY(), player.getZ() + net.minecraft.util.Mth.cos(yaw) * 0.2);
					});
					context.waitTicks(1);
				}
				context.waitTicks(4);
				context.takeScreenshot(trail == com.freedomclient.cosmetic.FlowerStepsCosmetic.class ? "flower_steps" : "abyss_flowers");
				context.runOnClient(client -> {
					for (var module : FreedomClient.getModuleManager().getModules()) {
						if (module.getClass() == trail) module.setEnabled(false);
					}
				});
			}
			context.runOnClient(client -> {
				for (Class<?> type : defaults) cosmetic(type).setEnabled(true);
				client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
			});

			// Visuals: cielo de atardecer FC.
			context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(VisualsModule.class), "Time", "FC Sunset"));
			context.waitTicks(10);
			context.takeScreenshot("visuals_sunset");
			context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(VisualsModule.class), "Time", "Server"));

			// Hit Particles: plumas al golpear el soporte de armadura.
			context.runOnClient(client -> {
				for (var entity : client.level.entitiesForRendering()) {
					if (entity instanceof net.minecraft.world.entity.decoration.ArmorStand) {
						// Crítico con plumas, copos y calabazas a la vez para ver los tres tipos.
						HitParticlesModule hitParticles = FreedomClient.getModuleManager().get(HitParticlesModule.class);
						for (Setting<?> setting : hitParticles.getSettings()) {
							if (setting.getName().startsWith("Crit: ") && !setting.getName().endsWith("Neon")) {
								((com.freedomclient.setting.BooleanSetting) setting).set(true);
							}
						}
						hitParticles.spawn(entity, true);
					}
				}
			});
			context.waitTicks(4);
			context.takeScreenshot("hit_particles");
			// Partículas pixel 3D de cerca: un crítico con plumas, copos y calabazas alrededor del jugador (F5).
			context.runOnClient(client -> {
				client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
				FreedomClient.getModuleManager().get(HitParticlesModule.class).spawn(client.player, true);
			});
			context.waitTicks(3);
			context.takeScreenshot("hit_particles_3d");
			context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));

			// Aviso de poca vida a 2 corazones.
			singleplayer.getServer().runOnServer(server -> server.getPlayerList().getPlayers().forEach(player -> player.setHealth(4.0F)));
			context.waitTicks(20);
			context.takeScreenshot("low_health");
			singleplayer.getServer().runOnServer(server -> server.getPlayerList().getPlayers().forEach(player -> player.setHealth(20.0F)));
			context.waitTicks(10);

			// Vida por encima de 10 corazones: los corazones del nombre se apilan (x2, x3…).
			singleplayer.getServer().runOnServer(server -> server.getPlayerList().getPlayers().forEach(player -> {
				player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(60.0);
				player.setHealth(47.0F);
			}));
			context.runOnClient(client -> {
				setMode(FreedomClient.getModuleManager().get(com.freedomclient.module.pvp.HealthIndicatorsModule.class), "Style", "Both");
				client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
			});
			context.waitTicks(10);
			context.takeScreenshot("health_stacked");
			singleplayer.getServer().runOnServer(server -> server.getPlayerList().getPlayers().forEach(player -> {
				player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20.0);
				player.setHealth(20.0F);
			}));
			context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			context.waitTicks(5);

			// Custom F3.
			context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_F3);
			context.waitTicks(10);
			context.takeScreenshot("custom_f3");
			context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_F3);
			context.waitTicks(5);

			// Menús de vanilla con el tema: pausa y opciones.
			context.setScreen(() -> new net.minecraft.client.gui.screens.PauseScreen(true));
			context.waitTicks(10);
			context.takeScreenshot("pause_themed");
			context.runOnClient(client -> client.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(client.screen, client.options)));
			context.waitTicks(10);
			context.takeScreenshot("options_themed");
			context.setScreen(() -> null);
			context.waitTicks(5);

			// Gap Counter: 12 manzanas de oro en la mano (primera persona y en la mano de otro jugador visto de frente).
			singleplayer.getServer().runCommand("item replace entity @a weapon.mainhand with golden_apple 12");
			context.waitTicks(10);
			context.takeScreenshot("gap_counter_hand");
			// Estilo calabaza (recarga los recursos) y el contador en la hotbar.
			context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(com.freedomclient.module.pvp.GapCounterModule.class), "Style", "Pumpkin"));
			context.waitTicks(3);
			context.waitFor(client -> client.getOverlay() == null, 20 * 60);
			context.waitTicks(10);
			context.takeScreenshot("gap_counter_pumpkin");
			context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(com.freedomclient.module.pvp.GapCounterModule.class), "Style", "Vanilla numbers"));
			context.waitTicks(3);
			context.waitFor(client -> client.getOverlay() == null, 20 * 60);
			context.waitTicks(5);
			context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(5);
			context.takeScreenshot("gap_counter_third_person");
			context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			context.setScreen(() -> {
				FreedomMenuScreen screen = new FreedomMenuScreen();
				screen.openModule(FreedomClient.getModuleManager().get(com.freedomclient.module.pvp.GapCounterModule.class));
				return screen;
			});
			context.waitTicks(10);
			context.takeScreenshot("menu_settings_gapcounter");
			context.setScreen(() -> null);
			singleplayer.getServer().runCommand("item replace entity @a weapon.mainhand with diamond_sword[damage=1500]");
			context.waitTicks(5);

			// TotemPop: un tótem en la mano izquierda y un golpe mortal.
			singleplayer.getServer().runCommand("item replace entity @a weapon.offhand with totem_of_undying");
			context.waitTicks(2);
			singleplayer.getServer().runCommand("damage @p 60 minecraft:generic");
			context.waitTicks(6);
			context.takeScreenshot("totem_pop");
			context.waitTicks(40);

			// Target HUD con la cara de un mob: un gólem de hierro quieto delante y después un slime.
			singleplayer.getServer().runCommand("execute at @p run summon iron_golem ~ ~ ~3 {NoAI:1b,Tags:[\"target\"]}");
			context.waitTicks(10);
			context.takeScreenshot("target_hud_golem");
			singleplayer.getServer().runCommand("kill @e[tag=target]");
			singleplayer.getServer().runCommand("execute at @p run summon slime ~ ~ ~3 {NoAI:1b,Size:2,Tags:[\"target\"]}");
			context.runOnClient(client -> client.player.setXRot(22.0F));
			context.waitTicks(10);
			context.takeScreenshot("target_hud_slime");
			// Al vacío en vez de matarlo: un slime muerto se divide en slimes pequeños que saldrían en las otras capturas.
			singleplayer.getServer().runCommand("tp @e[tag=target] ~ -300 ~");
			context.runOnClient(client -> client.player.setXRot(0.0F));
			context.waitTicks(5);

			// Freecam: la cámara se va 4 bloques atrás y 2 arriba y mira al jugador, que se queda quieto.
			context.runOnClient(client -> {
				client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
				float yaw = client.player.getYRot();
				double rad = Math.toRadians(yaw);
				net.minecraft.world.phys.Vec3 back = new net.minecraft.world.phys.Vec3(Math.sin(rad) * 4.0, 2.0, -Math.cos(rad) * 4.0);
				com.freedomclient.module.utility.FreecamModule.startForTest(client, back, yaw, 25.0F);
			});
			context.waitTicks(5);
			context.takeScreenshot("freecam");
			context.runOnClient(com.freedomclient.module.utility.FreecamModule::stopForTest);
			// Custom Sky: mirando al cielo con los temas morado, atardecer y menta.
			for (String sky : new String[] {"Purple", "Sunset", "Mint"}) {
				context.runOnClient(client -> {
					var module = FreedomClient.getModuleManager().get(com.freedomclient.module.visual.CustomSkyModule.class);
					setMode(module, "Sky", sky);
					module.setEnabled(true);
					client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
					client.player.setXRot(-25.0F);
				});
				context.waitTicks(5);
				context.takeScreenshot("custom_sky_" + sky.toLowerCase(Locale.ROOT));
			}
			context.runOnClient(client -> {
				FreedomClient.getModuleManager().get(com.freedomclient.module.visual.CustomSkyModule.class).setEnabled(false);
				client.player.setXRot(0.0F);
			});

			// INV: inventario con los colores del tema y después con una imagen de fondo (un degradado generado).
			context.runOnClient(client -> FreedomClient.getModuleManager().get(com.freedomclient.module.visual.InvModule.class).setEnabled(true));
			context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(5);
			context.takeScreenshot("inv_theme");
			context.setScreen(() -> null);
			context.runOnClient(client -> {
				try {
					java.nio.file.Path folder = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("freedomclient").resolve("inventory");
					java.nio.file.Files.createDirectories(folder);
					// Imagen apaisada (como una foto) con un círculo blanco y una cruz en el centro exacto y el borde rojo:
					// en la captura se ve si el centro de la imagen cae en el centro de la ventana.
					int iw = 320;
					int ih = 180;
					com.mojang.blaze3d.platform.NativeImage image = new com.mojang.blaze3d.platform.NativeImage(iw, ih, false);
					for (int y = 0; y < ih; y++) {
						for (int x = 0; x < iw; x++) {
							int color = 0xFF000000 | (40 + y / 2) << 16 | (20 + x / 3) << 8 | (120 + (x + y) / 8);
							double d = Math.hypot(x - iw / 2.0, y - ih / 2.0);
							if (Math.abs(d - 30) < 3 || Math.abs(x - iw / 2) < 2 || Math.abs(y - ih / 2) < 2) color = 0xFFFFFFFF;
							if (x < 6 || y < 6 || x >= iw - 6 || y >= ih - 6) color = 0xFFFF2020;
							image.setPixel(x, y, color);
						}
					}
					image.writeToFile(folder.resolve("background.png"));
					image.close();
				} catch (java.io.IOException e) {
					throw new RuntimeException(e);
				}
			});
			// Vuelve a leer la imagen recién escrita (ya se intentó cargar al abrir el inventario la primera vez).
			context.runOnClient(client -> {
				com.freedomclient.module.visual.InvModule inv = FreedomClient.getModuleManager().get(com.freedomclient.module.visual.InvModule.class);
				inv.reloadImage();
			});
			context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(5);
			context.takeScreenshot("inv_image");
			context.setScreen(() -> null);
			// Modo Fit: la imagen entera y el hueco que sobra relleno con la misma imagen desenfocada.
			context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(com.freedomclient.module.visual.InvModule.class), "Image fit", "Fit"));
			context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(5);
			context.takeScreenshot("inv_image_fit");
			context.setScreen(() -> null);
			context.runOnClient(client -> setMode(FreedomClient.getModuleManager().get(com.freedomclient.module.visual.InvModule.class), "Image fit", "Fill"));
			// Menú de paquetes de recursos: solo una entrada "FreedomClient".
			context.setScreen(() -> new net.minecraft.client.gui.screens.packs.PackSelectionScreen(
					net.minecraft.client.Minecraft.getInstance().getResourcePackRepository(), repository -> {
					}, net.minecraft.client.Minecraft.getInstance().getResourcePackDirectory(), net.minecraft.network.chat.Component.literal("Select Resource Packs")));
			context.waitTicks(10);
			context.takeScreenshot("resource_packs");
			context.setScreen(() -> null);

			// Ventanita para nombrar un waypoint nuevo.
			context.setScreen(() -> new com.freedomclient.waypoint.WaypointNameScreen(com.freedomclient.waypoint.WaypointStore.current().get(0)));
			context.waitTicks(5);
			context.takeScreenshot("waypoint_name");
			context.setScreen(() -> null);

			context.setScreen(() -> new HudEditorScreen(null));
			context.waitTicks(10);
			context.takeScreenshot("hud_editor");

			// Un mod favorito para ver la estrella (sale el primero de la lista).
			context.runOnClient(client -> FreedomClient.getModuleManager().get(ZoomModule.class).loadFavorite(true));
			// Carpetas de mods: una con tres mods para ver los chips de carpetas y el orden.
			context.runOnClient(client -> {
				com.freedomclient.ui.menu.ModFolders.create("PvP set");
				com.freedomclient.ui.menu.ModFolders.create("Visuals");
				for (String id : new String[] {"toggle_sprint", "zoom", "hitsounds"}) {
					com.freedomclient.ui.menu.ModFolders.toggle("PvP set", id);
				}
			});
			for (FreedomMenuScreen.Tab tab : FreedomMenuScreen.Tab.values()) {
				context.setScreen(() -> {
					FreedomMenuScreen screen = new FreedomMenuScreen();
					screen.setTab(tab);
					return screen;
				});
				context.waitTicks(10);
				context.takeScreenshot("menu_" + tab.name().toLowerCase(Locale.ROOT));
			}
			// Cada tema con sus detalles (sol, nubes, estrellas, fuego, energía).
			for (com.freedomclient.ui.theme.ThemePreset preset : com.freedomclient.ui.theme.ThemePreset.values()) {
				context.runOnClient(client -> com.freedomclient.ui.theme.ThemeManager.setPreset(preset));
				context.setScreen(() -> {
					FreedomMenuScreen screen = new FreedomMenuScreen();
					screen.setTab(FreedomMenuScreen.Tab.MODS);
					return screen;
				});
				context.waitTicks(10);
				context.takeScreenshot("theme_" + preset.name().toLowerCase(Locale.ROOT));
			}
			context.runOnClient(client -> com.freedomclient.ui.theme.ThemeManager.setPreset(com.freedomclient.ui.theme.ThemePreset.RED_SUNSET));
			// Bordes de la sección Borders de Cosmetics en las tarjetas de mods.
			Class<?>[] borders = {com.freedomclient.cosmetic.border.ElectricBorderCosmetic.class,
					com.freedomclient.cosmetic.border.RainbowBorderCosmetic.class, com.freedomclient.cosmetic.border.SparkleBorderCosmetic.class};
			for (Class<?> border : borders) {
				context.runOnClient(client -> cosmetic(border).setEnabled(true));
				context.setScreen(() -> {
					FreedomMenuScreen screen = new FreedomMenuScreen();
					screen.setTab(FreedomMenuScreen.Tab.MODS);
					return screen;
				});
				context.waitTicks(10);
				context.takeScreenshot("border_" + border.getSimpleName().replace("BorderCosmetic", "").toLowerCase(Locale.ROOT));
			}
			context.runOnClient(client -> cosmetic(com.freedomclient.cosmetic.border.SparkleBorderCosmetic.class).setEnabled(false));
			// Pestaña Cosmetics con las fichas de secciones compactas.
			context.setScreen(() -> {
				FreedomMenuScreen screen = new FreedomMenuScreen();
				screen.setTab(FreedomMenuScreen.Tab.COSMETICS);
				return screen;
			});
			context.waitTicks(10);
			context.takeScreenshot("menu_cosmetics_chips");

			context.setScreen(() -> {
				FreedomMenuScreen screen = new FreedomMenuScreen();
				screen.setTab(FreedomMenuScreen.Tab.MODS);
				screen.openModule(FreedomClient.getModuleManager().get(ZoomModule.class));
				return screen;
			});
			context.waitTicks(10);
			context.takeScreenshot("menu_settings_zoom");

			// Selector de opciones con todas a la vista (HitSounds).
			context.setScreen(() -> {
				FreedomMenuScreen screen = new FreedomMenuScreen();
				screen.openModule(FreedomClient.getModuleManager().get(HitSoundsModule.class));
				return screen;
			});
			context.waitTicks(10);
			context.takeScreenshot("menu_settings_hitsounds");

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
			context.runOnClient(client -> {
				setMode(FreedomClient.getModuleManager().get(AttackIndicatorModule.class), "Style", "Circle");
				client.player.resetAttackStrengthTicker();
			});
			context.waitTicks(4);
			context.takeScreenshot("attack_indicator_circle");

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

			// Sound Tweaks con la cuadrícula de mobs (gólem de hierro y slime silenciados) y la lista de waypoints.
			context.runOnClient(client -> {
				for (Setting<?> setting : FreedomClient.getModuleManager().get(com.freedomclient.module.utility.SoundTweaksModule.class).getSettings()) {
					if (setting instanceof com.freedomclient.setting.MobListSetting mobs) {
						mobs.toggle("iron_golem");
						mobs.toggle("slime");
					}
				}
			});
			context.setScreen(() -> {
				FreedomMenuScreen screen = new FreedomMenuScreen();
				screen.openModule(FreedomClient.getModuleManager().get(com.freedomclient.module.utility.SoundTweaksModule.class));
				return screen;
			});
			context.waitTicks(10);
			context.takeScreenshot("menu_settings_soundtweaks");
			context.runOnClient(client -> client.screen.mouseScrolled(client.getWindow().getGuiScaledWidth() / 2.0,
					client.getWindow().getGuiScaledHeight() / 2.0, 0.0, -30.0));
			context.waitTicks(10);
			context.takeScreenshot("menu_settings_soundtweaks_mobs");
			context.setScreen(() -> {
				FreedomMenuScreen screen = new FreedomMenuScreen();
				screen.openModule(FreedomClient.getModuleManager().get(com.freedomclient.waypoint.WaypointsModule.class));
				return screen;
			});
			context.waitTicks(10);
			context.takeScreenshot("menu_settings_waypoints");
			context.runOnClient(client -> client.screen.mouseScrolled(client.getWindow().getGuiScaledWidth() / 2.0,
					client.getWindow().getGuiScaledHeight() / 2.0, 0.0, -30.0));
			context.waitTicks(10);
			context.takeScreenshot("menu_settings_waypoints_list");

			context.setScreen(() -> null);
			// Botón "Optimize video settings" del FPS Optimizer (vanilla + Sodium): no debe fallar.
			context.runOnClient(client -> com.freedomclient.performance.PerformanceSettings.apply(client));
			context.waitTicks(5);
			context.takeScreenshot("after_optimize");
		}
	}
}
