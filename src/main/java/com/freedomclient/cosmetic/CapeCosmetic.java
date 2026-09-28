package com.freedomclient.cosmetic;

import com.freedomclient.setting.ModeSetting;

/** Capa de FreedomClient con el emblema sin letras: estilo Angel Devil (atardecer) o Neon (energía eléctrica). */
public class CapeCosmetic extends CosmeticModule {
	public final ModeSetting style = add(new ModeSetting("Style", "Angel Devil: sunset cape with the golden emblem. Neon: night-blue cape with the electric emblem.",
			"Angel Devil", "Angel Devil", "Neon"));

	public CapeCosmetic() {
		super("Cape", "FreedomClient cape with the client emblem, in the Angel Devil or Neon style.", CosmeticSlot.CAPE);
	}
}
