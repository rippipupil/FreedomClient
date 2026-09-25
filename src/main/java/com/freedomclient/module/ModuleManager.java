package com.freedomclient.module;

import com.freedomclient.module.hud.ArmorStatusModule;
import com.freedomclient.module.hud.CoordinatesModule;
import com.freedomclient.module.hud.FpsModule;
import com.freedomclient.module.hud.KeystrokesModule;
import com.freedomclient.module.hud.ModuleListModule;
import com.freedomclient.module.hud.PingModule;
import com.freedomclient.module.hud.WatermarkModule;
import com.freedomclient.module.movement.ToggleSprintModule;
import com.freedomclient.module.render.FullbrightModule;
import com.freedomclient.module.render.ZoomModule;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {
	private final List<Module> modules = new ArrayList<>();

	public ModuleManager() {
		// HUD
		add(new WatermarkModule());
		add(new FpsModule());
		add(new PingModule());
		add(new CoordinatesModule());
		add(new KeystrokesModule());
		add(new ArmorStatusModule());
		add(new ModuleListModule());

		// Render
		add(new FullbrightModule());
		add(new ZoomModule());

		// Movimiento
		add(new ToggleSprintModule());
	}

	private void add(Module module) {
		modules.add(module);
	}

	public void onTick(Minecraft client) {
		for (Module module : modules) {
			KeyMapping key = module.getToggleKey();
			if (key != null) {
				while (key.consumeClick()) {
					module.toggle();
				}
			}
		}

		for (Module module : modules) {
			if (module.isEnabled()) {
				module.onTick(client);
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
		throw new IllegalArgumentException("Módulo no registrado: " + type.getSimpleName());
	}
}
