package com.freedomclient.cosmetic;

import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;

public class PetCosmetic extends CosmeticModule {
	public final ModeSetting side = add(new ModeSetting("Side", "Which shoulder the pet floats next to.", "Right", "Right", "Left"));
	public final NumberSetting size = add(new NumberSetting("Size", "Size of the pet.", 0.45, 0.3, 0.7, 0.05, "x"));

	public PetCosmetic() {
		super("Angel Devil Pet", "A tiny pixel Angel Devil that floats next to you and flaps its wings.");
	}
}
