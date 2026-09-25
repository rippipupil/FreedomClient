package com.freedomclient.module.pvp;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;

/** Centra la mira de vanilla al píxel exacto (vanilla la desplaza medio píxel en algunas resoluciones). */
public class CenteredCrosshairModule extends Module {
	public CenteredCrosshairModule() {
		super("Centered Crosshair", "Puts the vanilla crosshair exactly in the center of the screen.", Category.PVP, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
