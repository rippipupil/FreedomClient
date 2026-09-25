package com.freedomclient.module.visual;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.NumberSetting;

/** Campo de visión fuera del rango de vanilla (hasta 150) y opción de quitar los cambios de FOV al correr. */
public class FovChangerModule extends Module {
	private final NumberSetting fov = add(new NumberSetting("FOV", "Field of view (vanilla allows 30-110).", 90, 30, 150, 1));
	private final BooleanSetting dynamic = add(new BooleanSetting("Dynamic FOV", "Let sprinting, speed and bows change your FOV.", true));

	public FovChangerModule() {
		super("FOV Changer", "Set any field of view and turn off FOV changes from sprinting and effects.", Category.VISUAL, false);
	}

	/** @param current FOV calculado por vanilla; @param optionFov FOV elegido en las opciones de vanilla. */
	public float apply(float current, int optionFov) {
		if (!dynamic.get()) return fov.getFloat();
		return current * fov.getFloat() / optionFov;
	}
}
