package com.freedomclient.cosmetic;

import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;

/** Soul Scythe: una guadaña pixel colgada en diagonal a la espalda. */
public class ScytheCosmetic extends CosmeticModule {
	public final NumberSetting size = add(new NumberSetting("Size", "How big the scythe is.", 0.85, 0.4, 1.2, 0.05, "x"));
	public final ModeSetting side = add(new ModeSetting("Blade side", "Which shoulder the blade sticks out over.", "Left", "Left", "Right"));

	public ScytheCosmetic() {
		super("Soul Scythe", "A dark pixel scythe with a silver blade, hanging across your back.", CosmeticSlot.BACK, false);
	}
}
