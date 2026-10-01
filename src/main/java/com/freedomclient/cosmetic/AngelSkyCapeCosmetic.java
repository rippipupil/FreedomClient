package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import com.freedomclient.setting.ModeSetting;
import net.minecraft.core.ClientAsset;

/**
 * Starry Sky Cape: Angel Devil con su halo mirando al cielo estrellado bajo el kanji 天使 ("ángel"). Las alas baten
 * despacio, las estrellas parpadean y caen un par de plumas. En blanco y negro o en color con el cielo morado.
 */
public class AngelSkyCapeCosmetic extends ClientCapeCosmetic {
	private static final int FRAMES = 16;
	private static final long FRAME_MS = 120L;
	private static final ClientAsset.Texture[] COLOR = frames("color");
	private static final ClientAsset.Texture[] MONO = frames("mono");

	public final ModeSetting style = add(new ModeSetting("Style", "Purple sky: in colour under a purple night sky. Black & White: monochrome, like a manga page.",
			"Purple sky", "Purple sky", "Black & White"));

	public AngelSkyCapeCosmetic() {
		super("Starry Sky Cape", "Animated cape: Angel Devil looking up at the starry sky with flapping wings, twinkling stars and falling "
				+ "feathers. Purple sky or black & white.", false);
	}

	private static ClientAsset.Texture[] frames(String style) {
		ClientAsset.Texture[] frames = new ClientAsset.Texture[FRAMES];
		for (int i = 0; i < FRAMES; i++) {
			String name = "cape_angel_sky_" + style + "_" + i;
			frames[i] = new ClientAsset.ResourceTexture(FreedomClient.id("cosmetic/" + name), FreedomClient.id("textures/cosmetic/" + name + ".png"));
		}
		return frames;
	}

	@Override
	public ClientAsset.Texture texture() {
		int frame = (int) (System.currentTimeMillis() % (FRAMES * FRAME_MS) / FRAME_MS);
		return (style.is("Black & White") ? MONO : COLOR)[frame];
	}
}
