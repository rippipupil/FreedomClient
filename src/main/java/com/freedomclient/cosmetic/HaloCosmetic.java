package com.freedomclient.cosmetic;

import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;

public class HaloCosmetic extends CosmeticModule {
	public final ModeSetting style = add(new ModeSetting("Style", "Ring halo, cracked broken halo, spinning crown or devil horns.", "Ring",
			"Ring", "Broken", "Crown", "Horns"));
	public final NumberSetting height = add(new NumberSetting("Height", "How high the halo floats above your head.", 3, 1, 6, 0.5, "px"));
	public final BooleanSetting spin = add(new BooleanSetting("Spin", "The halo slowly spins and floats up and down.", true));

	public HaloCosmetic() {
		super("Halo", "A golden halo over your head: ring, broken, crown or devil horns.");
		height.visibleWhen(() -> !style.is("Horns"));
		spin.visibleWhen(() -> !style.is("Horns"));
	}
}
