package com.freedomclient.cosmetic.vox;

/** Demon Mask (Bee Swarm): bombín negro con banda roja, cuernos rojo oscuro y cara enfadada roja. */
public class DemonMaskCosmetic extends MaskCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("demon_mask",
			'b', 0xFF1C1C1E, 'B', 0xFF111113, 'k', 0xFF141416, 'K', 0xFF262629,
			'r', 0xFF9A1018, 'R', 0xFF6E0A10, 'g', 0xFF4A4A4E, 'f', 0x901E1E22, 'e', 0xFFC0141E);

	public DemonMaskCosmetic() {
		super("Demon Mask", "Bee Swarm: black bowler mask with a red band, big dark red horns and an angry red face. The face is see-through.");
	}

	@Override
	protected Vox.Palette palette() {
		return PALETTE;
	}

	@Override
	protected void decorate(Vox.Shape s) {
		// Parte de abajo más clara, como en la máscara.
		s.box('g', -4.7F, -0.4F, -4.7F, 9.4F, 1.0F, 0.6F);
		// Bombín: banda roja y cúpula negra.
		s.disc('r', 0.0F, -7.4F, 0.0F, 5.4F, 1.0F);
		float[] radii = {5.0F, 4.8F, 4.4F, 3.8F, 2.9F};
		for (int i = 0; i < radii.length; i++) s.disc(i % 2 == 0 ? 'k' : 'K', 0.0F, -8.4F - i, 0.0F, radii[i], 1.0F);
		// Cuernos: bloques que salen de los lados de la cúpula y suben curvándose hacia fuera.
		for (int side = -1; side <= 1; side += 2) {
			float[][] segments = {{4.2F, -10.0F, 2.2F}, {5.4F, -12.4F, 2.0F}, {6.5F, -14.8F, 1.7F}, {7.3F, -17.2F, 1.4F}, {7.8F, -19.4F, 1.0F}};
			for (float[] seg : segments) {
				float size = seg[2];
				float x = side > 0 ? seg[0] : -seg[0] - size;
				s.box(size > 1.5F ? 'R' : 'r', x, seg[1], -size / 2, size, 2.6F, size);
			}
		}
		features(s, new String[] {
				"ee.....ee",
				".ee...ee.",
				"..e...e..",
				".........",
				"e.e.e.e.e",
				".e.e.e.e.",
		});
	}
}
