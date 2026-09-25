package com.freedomclient.module;

import com.freedomclient.module.hud.AppleSkinModule;
import com.freedomclient.module.hud.ArmorAlertHud;
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
import com.freedomclient.module.hud.PingHud;
import com.freedomclient.module.hud.PotionEffectsHud;
import com.freedomclient.module.hud.ReachHud;
import com.freedomclient.module.hud.WatermarkHud;
import com.freedomclient.module.performance.BundledModModule;
import com.freedomclient.module.performance.GameOptimizerModule;
import com.freedomclient.module.hud.TargetHud;
import com.freedomclient.module.pvp.AutoTextModule;
import com.freedomclient.module.pvp.BetterCrosshairModule;
import com.freedomclient.module.pvp.BetterHurtCamModule;
import com.freedomclient.module.pvp.CenteredCrosshairModule;
import com.freedomclient.module.pvp.FreelookModule;
import com.freedomclient.module.pvp.HealthIndicatorsModule;
import com.freedomclient.module.pvp.ShieldFixModule;
import com.freedomclient.module.pvp.ToggleSprintModule;
import com.freedomclient.module.pvp.ViewModelModule;
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
		add(new CenteredCrosshairModule());
		add(new HitColorModule());
		add(new BetterHurtCamModule());
		add(new HealthIndicatorsModule());
		add(new FreelookModule());
		add(new ViewModelModule());
		add(new ShieldFixModule());
		add(new AutoTextModule());

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
		add(new ArmorAlertHud());
		add(new InventoryHud());
		add(new PotionEffectsHud());
		add(new ItemCounterHud());
		add(new ModuleListHud());
		add(new TargetHud());
		add(new AppleSkinModule());

		// Visual
		add(new FullbrightModule());
		add(new ZoomModule());
		add(new FovChangerModule());

		// Performance
		add(new GameOptimizerModule());
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
