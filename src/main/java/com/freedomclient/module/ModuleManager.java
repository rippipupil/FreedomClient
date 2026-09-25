package com.freedomclient.module;

import com.freedomclient.cosmetic.CapeCosmetic;
import com.freedomclient.cosmetic.HaloCosmetic;
import com.freedomclient.cosmetic.PetCosmetic;
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
import com.freedomclient.module.hud.NowPlayingHud;
import com.freedomclient.module.hud.PingHud;
import com.freedomclient.module.hud.PotionEffectsHud;
import com.freedomclient.module.hud.ReachHud;
import com.freedomclient.module.hud.WatermarkHud;
import com.freedomclient.module.performance.BundledModModule;
import com.freedomclient.module.performance.GameOptimizerModule;
import com.freedomclient.module.performance.CullLeavesModule;
import com.freedomclient.module.performance.EntityCullingModule;
import com.freedomclient.module.visual.BetterGrassModule;
import com.freedomclient.module.visual.CapesModule;
import com.freedomclient.module.visual.HitParticlesModule;
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
import com.freedomclient.module.pvp.LowHealthWarningModule;
import com.freedomclient.module.pvp.ShieldFixModule;
import com.freedomclient.module.pvp.ToggleSprintModule;
import com.freedomclient.module.pvp.ViewModelModule;
import com.freedomclient.module.utility.AnnouncementsModule;
import com.freedomclient.module.utility.ChatFilterModule;
import com.freedomclient.module.utility.CrashGuardModule;
import com.freedomclient.module.utility.DiscordPresenceModule;
import com.freedomclient.module.utility.FastWorldJoinModule;
import com.freedomclient.module.utility.LogCleanerModule;
import com.freedomclient.module.utility.QuickPackModule;
import com.freedomclient.module.utility.SoundTweaksModule;
import com.freedomclient.module.visual.BlockOutlineModule;
import com.freedomclient.module.visual.CustomHitboxesModule;
import com.freedomclient.module.visual.FcNametagModule;
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

		// HUD
		add(new WatermarkHud());
		add(new FpsHud());
		add(new PingHud());
		add(new CoordinatesHud());
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
		add(new BetterGrassModule());
		add(new WavyCapesModule());
		add(new CapesModule());
		add(new HitParticlesModule());
		add(new VisualsModule());

		// Utility
		add(new WaypointsModule());
		add(new ChatFilterModule());
		add(new AnnouncementsModule());
		add(new SoundTweaksModule());
		add(new QuickPackModule());
		add(new FastWorldJoinModule());
		add(new LogCleanerModule());
		add(new CrashGuardModule());
		add(new DiscordPresenceModule());

		// Mods originales incluidos (siempre activos).
		add(new BundledModModule("Continuity", "continuity", "Connected textures for glass and resource packs that use them.", Category.VISUAL));
		add(new BundledModModule("Mouse Tweaks", "mousetweaks", "Better inventory controls: drag to move items, scroll to move stacks.", Category.UTILITY));
		add(new BundledModModule("Debugify", "debugify", "Fixes many vanilla Minecraft bugs.", Category.UTILITY));
		add(new BundledModModule("Fast IP Ping", "fastipping", "Makes the server list ping servers much faster.", Category.UTILITY));

		// Cosméticos
		add(new WingsCosmetic());
		add(new HaloCosmetic());
		add(new CapeCosmetic());
		add(new PetCosmetic());

		// Performance
		add(new GameOptimizerModule());
		add(new EntityCullingModule());
		add(new CullLeavesModule());
		add(new BundledModModule("Sodium", "sodium", "Modern rendering engine. The biggest FPS boost."));
		add(new BundledModModule("Lithium", "lithium", "Optimizes physics, mob AI and game logic."));
		add(new BundledModModule("FerriteCore", "ferritecore", "Greatly reduces memory usage."));
		add(new BundledModModule("ImmediatelyFast", "immediatelyfast", "Speeds up HUD, text and entity rendering."));
		add(new BundledModModule("Dynamic FPS", "dynamic_fps", "Lowers FPS while the game is minimized or in the background."));
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

	public <T extends Module> T get(Class<T> type) {
		for (Module module : modules) {
			if (type.isInstance(module)) {
				return type.cast(module);
			}
		}
		throw new IllegalArgumentException("Module not registered: " + type.getSimpleName());
	}
}
