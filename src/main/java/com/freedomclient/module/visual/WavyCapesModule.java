package com.freedomclient.module.visual;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.NumberSetting;

/** Wavy Capes: las capas se dibujan en tiras que se curvan y ondean con el movimiento (ver WavyCapeRenderer). */
public class WavyCapesModule extends Module {
	private static WavyCapesModule instance;

	public final NumberSetting wind = add(new NumberSetting("Wind", "How much the cape waves.", 1, 0, 2, 0.1, "x"));
	public final NumberSetting speed = add(new NumberSetting("Speed", "How fast the waves move.", 1, 0.5, 2, 0.1, "x"));

	public WavyCapesModule() {
		super("Wavy Capes", "Capes bend and wave smoothly with your movement instead of being a stiff board.", Category.VISUAL, true);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** El módulo activo, o null si está desactivado. */
	public static WavyCapesModule active() {
		return instance != null && instance.isEnabled() ? instance : null;
	}
}
