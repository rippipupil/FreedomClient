package com.freedomclient.cosmetic;

import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.NumberSetting;

public class HaloCosmetic extends CosmeticModule {
	public final NumberSetting height = add(new NumberSetting("Height", "How high the halo floats above your head.", 3, 1, 6, 0.5, "px"));
	public final BooleanSetting spin = add(new BooleanSetting("Spin", "The halo slowly spins and floats up and down.", true));

	public HaloCosmetic() {
		super("Halo", "A golden halo floating over your head.");
	}
}
