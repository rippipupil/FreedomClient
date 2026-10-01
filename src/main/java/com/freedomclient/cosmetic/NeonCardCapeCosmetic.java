package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import net.minecraft.core.ClientAsset;

/**
 * Neon Card Cape: tarjeta de agente animada de Neon. Su puño cargado suelta rayos cian y amarillos que cambian en
 * cada fotograma, con chispas, el resplandor latiendo y un brillo que recorre los chevrones dorados de abajo.
 */
public class NeonCardCapeCosmetic extends ClientCapeCosmetic {
	private static final int FRAMES = 12;
	private static final long FRAME_MS = 90L;
	private static final ClientAsset.Texture[] TEXTURES = new ClientAsset.Texture[FRAMES];

	static {
		for (int i = 0; i < FRAMES; i++) {
			String name = "cape_neon_card_" + i;
			TEXTURES[i] = new ClientAsset.ResourceTexture(FreedomClient.id("cosmetic/" + name), FreedomClient.id("textures/cosmetic/" + name + ".png"));
		}
	}

	public NeonCardCapeCosmetic() {
		super("Neon Card Cape", "Animated agent card cape: Neon's charged fist firing cyan and yellow lightning, with sparks and golden chevrons.",
				false);
	}

	@Override
	public ClientAsset.Texture texture() {
		return TEXTURES[(int) (System.currentTimeMillis() % (FRAMES * FRAME_MS) / FRAME_MS)];
	}
}
