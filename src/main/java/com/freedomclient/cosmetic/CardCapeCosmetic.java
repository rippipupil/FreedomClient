package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.core.ClientAsset;

/**
 * Card Cape: capa negra de carta de póker con palos grises de fondo, marco blanco y un palo en cada esquina. El palo
 * grande del centro va cambiando (tréboles, diamantes, corazones y picas) fundiéndose de uno a otro.
 */
public class CardCapeCosmetic extends ClientCapeCosmetic {
	/** Por palo: el fotograma quieto y tres de fundido hacia el siguiente. */
	private static final int FRAMES_PER_SUIT = 4;
	private static final int SUITS = 4;
	private static final ClientAsset.Texture[] FRAMES = new ClientAsset.Texture[FRAMES_PER_SUIT * SUITS];

	static {
		for (int i = 0; i < FRAMES.length; i++) {
			FRAMES[i] = new ClientAsset.ResourceTexture(FreedomClient.id("cosmetic/cape_card_" + i),
					FreedomClient.id("textures/cosmetic/cape_card_" + i + ".png"));
		}
	}

	public final NumberSetting hold = add(new NumberSetting("Suit time", "Seconds each suit stays before changing to the next one.", 2.0, 0.5, 6.0, 0.5, "s"));

	public CardCapeCosmetic() {
		super("Card Cape", "A black playing card cape with a white frame. The big suit in the middle keeps changing: clubs, diamonds, hearts and spades.",
				false);
	}

	@Override
	public ClientAsset.Texture texture() {
		long holdMs = (long) (hold.get() * 1000.0);
		long fadeMs = 360L;
		long cycle = (holdMs + fadeMs) * SUITS;
		long time = System.currentTimeMillis() % cycle;
		int suit = (int) (time / (holdMs + fadeMs));
		long inSuit = time % (holdMs + fadeMs);
		int step = inSuit < holdMs ? 0 : 1 + (int) Math.min(FRAMES_PER_SUIT - 2, (inSuit - holdMs) * (FRAMES_PER_SUIT - 1) / fadeMs);
		return FRAMES[suit * FRAMES_PER_SUIT + step];
	}
}
