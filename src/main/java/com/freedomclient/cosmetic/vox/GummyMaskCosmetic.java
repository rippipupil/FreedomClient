package com.freedomclient.cosmetic.vox;

/** Gummy Mask (Bee Swarm): máscara de goma rosa/morada con tapón verde menta, burbuja y cara verde. */
public class GummyMaskCosmetic extends MaskCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("gummy_mask",
			'b', 0xFFD37CDE, 'B', 0xFFC56AD2, 'p', 0xFFD884E2, 'P', 0xFFE9AEF2,
			'm', 0xFF4FD9A2, 'M', 0xFF3DF2AC, 'u', 0xFFD6C8F6, 'f', 0x90D884E2, 'g', 0xFF3ED89A);

	public GummyMaskCosmetic() {
		super("Gummy Mask", "Bee Swarm: glossy pink gummy mask with a mint cap, a bubble and a green goofy face. The face is see-through.");
	}

	@Override
	protected Vox.Palette palette() {
		return PALETTE;
	}

	@Override
	protected void decorate(Vox.Shape s) {
		// Banda verde brillante sobre el ala y cúpula alta de goma con un brillo.
		s.disc('M', 0.0F, -7.4F, 0.0F, 5.5F, 1.0F);
		float[] radii = {5.1F, 5.0F, 4.8F, 4.5F, 4.0F, 3.3F};
		for (int i = 0; i < radii.length; i++) s.disc('p', 0.0F, -8.4F - i, 0.0F, radii[i], 1.0F);
		s.box('P', -3.4F, -12.2F, -4.3F, 1.2F, 3.0F, 0.6F);
		// Tapón verde menta arriba y la burbuja pegada a un lado.
		s.disc('m', 0.0F, -16.4F, 0.0F, 2.5F, 3.0F);
		s.sphere('u', 2.3F, -15.2F, -1.0F, 0.9F);
		features(s, new String[] {
				".gg...gg.",
				".g.g.g.g.",
				".gg...gg.",
				".........",
				"g.g...g.g",
				".g.ggg.g.",
		});
	}
}
