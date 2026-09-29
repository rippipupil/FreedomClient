package com.freedomclient.module;

import com.freedomclient.cosmetic.AuraCosmetic;
import com.freedomclient.cosmetic.CapeCosmetic;
import com.freedomclient.cosmetic.CloudPetCosmetic;
import com.freedomclient.cosmetic.HaloCosmetic;
import com.freedomclient.cosmetic.PetCosmetic;
import com.freedomclient.cosmetic.LightningTrailCosmetic;
import com.freedomclient.cosmetic.NeonPackCosmetic;
import com.freedomclient.cosmetic.NeonPetCosmetic;
import com.freedomclient.cosmetic.NeonStepsCosmetic;
import com.freedomclient.cosmetic.ScytheCosmetic;
import com.freedomclient.cosmetic.WingsCosmetic;
import com.freedomclient.module.hud.AppleSkinModule;
import com.freedomclient.module.hud.ArmorHud;
import com.freedomclient.module.hud.ClockHud;
import com.freedomclient.module.hud.ComboHud;
import com.freedomclient.module.hud.CoordinatesHud;
import com.freedomclient.module.hud.CpsHud;
import com.freedomclient.module.hud.FpsHud;
import com.freedomclient.module.hud.InventoryHud;
import com.freedomclient.module.hud.ItemCounterHud;
import com.freedomclient.module.hud.KeystrokesHud;
import com.freedomclient.module.hud.ModuleListHud;
import com.freedomclient.module.hud.NetherCoordsHud;
import com.freedomclient.module.hud.MusicPlayerHud;
import com.freedomclient.module.hud.NowPlayingHud;
import com.freedomclient.module.hud.PingHud;
import com.freedomclient.module.hud.ScoreboardHud;
import com.freedomclient.module.hud.PotionEffectsHud;
import com.freedomclient.module.hud.ReachHud;
import com.freedomclient.module.hud.WatermarkHud;
import com.freedomclient.module.performance.BundledModModule;
import com.freedomclient.module.performance.GameOptimizerModule;
import com.freedomclient.module.performance.CullLeavesModule;
import com.freedomclient.module.performance.ParticleLimiterModule;
import com.freedomclient.module.performance.ProcessPriorityModule;
import com.freedomclient.module.performance.EntityCullingModule;
import com.freedomclient.module.visual.BetterGrassModule;
import com.freedomclient.module.visual.CapesModule;
import com.freedomclient.module.visual.HitParticlesModule;
import com.freedomclient.module.visual.TotemPopModule;
import com.freedomclient.module.visual.VisualsModule;
import com.freedomclient.module.visual.WavyCapesModule;
import com.freedomclient.module.hud.TargetHud;
import com.freedomclient.module.pvp.AttackIndicatorModule;
import com.freedomclient.module.pvp.AutoTextModule;
import com.freedomclient.module.pvp.BetterCrosshairModule;
import com.freedomclient.module.pvp.BetterHurtCamModule;
import com.freedomclient.module.pvp.CenteredCrosshairModule;
import com.freedomclient.module.pvp.FreelookModule;
import com.freedomclient.module.pvp.HealthIndicatorsModule;
import com.freedomclient.module.pvp.HitSoundsModule;
import com.freedomclient.module.pvp.LowHealthWarningModule;
import com.freedomclient.module.pvp.ShieldFixModule;
import com.freedomclient.module.pvp.ToggleSprintModule;
import com.freedomclient.module.pvp.ViewModelModule;
import com.freedomclient.module.utility.AnnouncementsModule;
import com.freedomclient.module.utility.ChatFilterModule;
import com.freedomclient.module.utility.ChatHeadsModule;
import com.freedomclient.module.utility.CrashGuardModule;
import com.freedomclient.module.utility.DiscordPresenceModule;
import com.freedomclient.module.utility.UpdatesModule;
import com.freedomclient.module.utility.FastWorldJoinModule;
import com.freedomclient.module.utility.LogCleanerModule;
import com.freedomclient.module.utility.QuickPackModule;
import com.freedomclient.module.utility.SoundTweaksModule;
import com.freedomclient.module.visual.BlockOutlineModule;
import com.freedomclient.module.visual.CustomHitboxesModule;
import com.freedomclient.module.visual.FcNametagModule;
import com.freedomclient.module.visual.TagModule;
import com.freedomclient.module.visual.ShulkerPreviewModule;
import com.freedomclient.waypoint.WaypointsModule;
import com.freedomclient.module.visual.CustomScreensModule;
import com.freedomclient.module.visual.FovChangerModule;
import com.freedomclient.module.visual.FullbrightModule;
import com.freedomclient.module.visual.HitColorModule;
import com.freedomclient.module.visual.ZoomModule;
import com.freedomclient.setting.KeybindSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModuleManager {
	private final List<Module> modules = new ArrayList<>();
	private final java.util.Map<Class<?>, Module> byType = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.Map<Class<?>, List<?>> listsByType = new java.util.concurrent.ConcurrentHashMap<>();
	/** Teclas de módulos pulsadas en el tick anterior, para activar solo al pulsar (no al mantener). */
	private final Set<Integer> heldKeys = new HashSet<>();

	public ModuleManager() {
		// PvP
		add(new ToggleSprintModule());
		add(new BetterCrosshairModule());
		add(new AttackIndicatorModule());
		add(new CenteredCrosshairModule());
		add(new HitColorModule());
		add(new BetterHurtCamModule());
		add(new HealthIndicatorsModule());
		add(new FreelookModule());
		add(new ViewModelModule());
		add(new ShieldFixModule());
		add(new AutoTextModule());
		add(new LowHealthWarningModule());
		add(new HitSoundsModule());

		// HUD
		add(new WatermarkHud());
		add(new FpsHud());
		add(new PingHud());
		add(new CoordinatesHud());
		add(new NetherCoordsHud());
		add(new ClockHud());
		add(new CpsHud());
		add(new ComboHud());
		add(new ReachHud());
		add(new KeystrokesHud());
		add(new ArmorHud());
		add(new InventoryHud());
		add(new PotionEffectsHud());
		add(new ItemCounterHud());
		add(new ModuleListHud());
		add(new TargetHud());
		add(new NowPlayingHud());
		add(new MusicPlayerHud());
		add(new ScoreboardHud());
		add(new AppleSkinModule());

		// Visual
		add(new FullbrightModule());
		add(new ZoomModule());
		add(new FovChangerModule());
		add(new CustomHitboxesModule());
		add(new CustomScreensModule());
		add(new BlockOutlineModule());
		add(new ShulkerPreviewModule());
		add(new FcNametagModule());
		add(new TagModule());
		add(new com.freedomclient.module.pvp.GapCounterModule());
		add(new com.freedomclient.module.utility.CustomF3Module());
		add(new BetterGrassModule());
		add(new WavyCapesModule());
		add(new CapesModule());
		add(new HitParticlesModule());
		add(new TotemPopModule());
		add(new VisualsModule());
		add(new com.freedomclient.module.visual.InvModule());

		// Utility
		add(new WaypointsModule());
		add(new ChatFilterModule());
		add(new ChatHeadsModule());
		add(new AnnouncementsModule());
		add(new SoundTweaksModule());
		add(new QuickPackModule());
		add(new FastWorldJoinModule());
		add(new LogCleanerModule());
		add(new CrashGuardModule());
		add(new DiscordPresenceModule());
		add(new UpdatesModule());

		// Mods originales incluidos (siempre activos).
		add(new BundledModModule("Continuity", "continuity", "Connected textures for glass and resource packs that use them.", Category.VISUAL));
		add(new com.freedomclient.module.utility.MouseTweaksModule());
		add(new BundledModModule("Debugify", "debugify", "Fixes many vanilla Minecraft bugs.", Category.UTILITY));
		add(new BundledModModule("Fast IP Ping", "fastipping", "Makes the server list ping servers much faster.", Category.UTILITY));

		// Cosméticos
		add(new WingsCosmetic());
		add(new HaloCosmetic());
		add(new CapeCosmetic());
		add(new PetCosmetic());
		add(new CloudPetCosmetic());
		add(new AuraCosmetic());
		add(new ScytheCosmetic());
		add(new NeonPackCosmetic());
		add(new NeonPetCosmetic());
		add(new LightningTrailCosmetic());
		add(new NeonStepsCosmetic());
		// Bee Swarm
		add(new com.freedomclient.cosmetic.vox.DiamondMaskCosmetic());
		add(new com.freedomclient.cosmetic.vox.DemonMaskCosmetic());
		add(new com.freedomclient.cosmetic.vox.GummyMaskCosmetic());
		add(new com.freedomclient.cosmetic.vox.TidePopperCosmetic());
		add(new com.freedomclient.cosmetic.vox.DarkScytheCosmetic());
		add(new com.freedomclient.cosmetic.vox.GummyballerCosmetic());
		add(new com.freedomclient.cosmetic.vox.WindyBeeCosmetic());
		add(new com.freedomclient.cosmetic.vox.TabbyBeeCosmetic());
		// FNAF
		add(new com.freedomclient.cosmetic.vox.FreddyHatCosmetic());
		add(new com.freedomclient.cosmetic.vox.FreddyPetCosmetic());
		add(new com.freedomclient.cosmetic.vox.CupcakePetCosmetic());
		// OneShot
		add(new com.freedomclient.cosmetic.vox.NikoHatCosmetic());
		add(new com.freedomclient.cosmetic.vox.NikoScarfCosmetic());
		add(new com.freedomclient.cosmetic.vox.SunBackpackCosmetic());
		add(new com.freedomclient.cosmetic.vox.NikoPetCosmetic());
		// Halloween y rastros de flores
		add(new com.freedomclient.cosmetic.vox.JackOLanternCosmetic());
		add(new com.freedomclient.cosmetic.FlowerStepsCosmetic());
		add(new com.freedomclient.cosmetic.AbyssFlowersCosmetic());

		// Performance
		add(new GameOptimizerModule());
		add(new EntityCullingModule());
		add(new CullLeavesModule());
		add(new ParticleLimiterModule());
		add(new com.freedomclient.module.performance.ChunkPreloaderModule());
		add(new ProcessPriorityModule());
		add(new BundledModModule("Sodium", "sodium", "Modern rendering engine. The biggest FPS boost."));
		add(new BundledModModule("Lithium", "lithium", "Optimizes physics, mob AI and game logic."));
		add(new BundledModModule("FerriteCore", "ferritecore", "Greatly reduces memory usage."));
		add(new BundledModModule("ImmediatelyFast", "immediatelyfast", "Speeds up HUD, text and entity rendering."));
		add(new BundledModModule("More Culling", "moreculling", "Skips drawing block faces you cannot see, like the inside of leaves."));
		add(new BundledModModule("C2ME", "c2me", "Generates and loads chunks on all your CPU cores in singleplayer."));
		add(new BundledModModule("Krypton", "krypton", "Lighter, faster networking."));
		add(new BundledModModule("Sodium Extra", "sodium-extra", "More video options on top of Sodium (animations, particles, fog...)."));
	}

	private void add(Module module) {
		modules.add(module);
	}

	public void onTick(Minecraft client) {
		handleKeybinds(client);

		for (Module module : modules) {
			if (module.isEnabled()) {
				module.onTick(client);
			}
		}
	}

	private void handleKeybinds(Minecraft client) {
		if (client.screen != null) {
			heldKeys.clear();
			return;
		}

		for (Module module : modules) {
			KeybindSetting keybind = module.getKeybind();
			if (!module.canToggle() || !keybind.isBound()) continue;

			int key = keybind.get();
			boolean down = InputConstants.isKeyDown(client.getWindow(), key);
			if (down && heldKeys.add(key)) {
				module.toggle();
			} else if (!down) {
				heldKeys.remove(key);
			}
		}
	}

	public void onShutdown(Minecraft client) {
		for (Module module : modules) {
			module.onShutdown(client);
		}
	}

	public List<Module> getModules() {
		return Collections.unmodifiableList(modules);
	}

	public List<Module> getModules(Category category) {
		return modules.stream().filter(module -> module.getCategory() == category).toList();
	}

	/**
	 * El mod de ese tipo. Se busca una vez y se guarda: se llama muchas veces por fotograma (cosméticos, sonidos,
	 * HUD...) y la lista no cambia después de arrancar.
	 */
	public <T extends Module> T get(Class<T> type) {
		Module cached = byType.get(type);
		if (cached == null) {
			for (Module module : modules) {
				if (type.isInstance(module)) {
					cached = module;
					break;
				}
			}
			if (cached == null) throw new IllegalArgumentException("Module not registered: " + type.getSimpleName());
			byType.put(type, cached);
		}
		return type.cast(cached);
	}

	/** Todos los mods de ese tipo (p. ej. los elementos del HUD), calculado una vez. */
	@SuppressWarnings("unchecked")
	public <T> List<T> ofType(Class<T> type) {
		return (List<T>) listsByType.computeIfAbsent(type, key -> modules.stream().filter(key::isInstance).map(key::cast).toList());
	}
}
