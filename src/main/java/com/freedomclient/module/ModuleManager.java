package com.freedomclient.module;

import com.freedomclient.module.hud.ArmorStatusModule;
import com.freedomclient.module.hud.CoordinatesModule;
import com.freedomclient.module.hud.FpsModule;
import com.freedomclient.module.hud.KeystrokesModule;
import com.freedomclient.module.hud.ModuleListModule;
import com.freedomclient.module.hud.PingModule;
import com.freedomclient.module.hud.WatermarkModule;
import com.freedomclient.module.performance.BundledModModule;
import com.freedomclient.module.performance.GameOptimizerModule;
import com.freedomclient.module.pvp.ToggleSprintModule;
import com.freedomclient.module.visual.FullbrightModule;
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

		// HUD
		add(new WatermarkModule());
		add(new FpsModule());
		add(new PingModule());
		add(new CoordinatesModule());
		add(new KeystrokesModule());
		add(new ArmorStatusModule());
		add(new ModuleListModule());

		// Visual
		add(new FullbrightModule());
		add(new ZoomModule());

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
