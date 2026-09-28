package com.freedomclient.module.performance;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.performance.PerformanceSettings;
import com.freedomclient.setting.ActionSetting;
import net.minecraft.client.Minecraft;

/** Tarjeta con el botón que aplica los ajustes de vídeo más rápidos. */
public class GameOptimizerModule extends Module {
	public GameOptimizerModule() {
		super("FPS Optimizer", "Applies the fastest video settings without lowering your render distance.", Category.PERFORMANCE, true);
		add(new ActionSetting("Optimize video settings",
				"Turns off VSync, clouds, entity shadows, biome blend, vignette, chunk fade-in, menu blur and texture filtering, "
						+ "uses fast leaves and threaded chunk updates, uncaps the frame rate, lowers it while AFK and applies the fastest Sodium options.",
				"Apply", () -> PerformanceSettings.apply(Minecraft.getInstance())));
	}

	@Override
	public boolean canToggle() {
		return false;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
