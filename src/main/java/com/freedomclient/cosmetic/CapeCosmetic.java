package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import com.freedomclient.setting.ModeSetting;
import net.minecraft.core.ClientAsset;

/** Capa de FreedomClient con el emblema sin letras: estilo Angel Devil (atardecer) o Neon (energía eléctrica). */
public class CapeCosmetic extends CosmeticModule {
	public static final ClientAsset.Texture ANGEL_CAPE = new ClientAsset.ResourceTexture(
			FreedomClient.id("cosmetic/cape_angel"), FreedomClient.id("textures/cosmetic/cape_angel.png"));
	public static final ClientAsset.Texture NEON_CAPE = new ClientAsset.ResourceTexture(
			FreedomClient.id("cosmetic/cape_neon"), FreedomClient.id("textures/cosmetic/cape_neon.png"));

	public final ModeSetting style = add(new ModeSetting("Style", "Angel Devil: sunset cape with the golden emblem. Neon: night-blue cape with the electric emblem.",
			"Angel Devil", "Angel Devil", "Neon"));

	public CapeCosmetic() {
		super("Cape", "FreedomClient cape with the client emblem, in the Angel Devil or Neon style.", CosmeticSlot.CAPE);
	}

	/** Textura de la capa con el estilo elegido. */
	public ClientAsset.Texture texture() {
		return style.is("Neon") ? NEON_CAPE : ANGEL_CAPE;
	}
}
