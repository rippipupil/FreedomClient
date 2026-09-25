package com.freedomclient.cosmetic;

import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.NumberSetting;

public class WingsCosmetic extends CosmeticModule {
	public final NumberSetting size = add(new NumberSetting("Size", "Size of the wings.", 1, 0.5, 1.5, 0.05, "x"));
	public final BooleanSetting flap = add(new BooleanSetting("Flap", "Wings move gently, faster while sprinting or flying.", true));

	public WingsCosmetic() {
		super("Wings", "White angel wings on your back (visible in third person).");
	}
}
