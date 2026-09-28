package com.freedomclient.cosmetic.vox;

/** Diamond Mask (Bee Swarm): máscara azul hielo con corona blanca, ojos de estrella y sonrisa blanca. */
public class DiamondMaskCosmetic extends MaskCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("diamond_mask",
			'b', 0xFF7ED3F0, 'B', 0xFF5BBCE0, 'd', 0xFF8AD8F2, 'D', 0xFF6CC6E8,
			'f', 0x887ED3F0, 'w', 0xFFFFFFFF, 's', 0xFFD9EEF6);

	public DiamondMaskCosmetic() {
		super("Diamond Mask", "Bee Swarm: ice-blue mask with a white crown, star eyes and a smile. Covers your head; the face is see-through.");
	}

	@Override
	protected Vox.Palette palette() {
		return PALETTE;
	}

	@Override
	protected void decorate(Vox.Shape s) {
		// Cúpula redonda sobre el ala.
		float[] radii = {5.2F, 4.9F, 4.4F, 3.7F, 2.8F};
		for (int i = 0; i < radii.length; i++) s.disc(i % 2 == 0 ? 'd' : 'D', 0.0F, -7.4F - i, 0.0F, radii[i], 1.0F);
		// Corona: arcos blancos alrededor de la cúpula y una flor de lis en el centro.
		for (int i = 0; i < 8; i++) {
			double angle = i * Math.PI / 4;
			float x = (float) Math.cos(angle) * 3.3F;
			float z = (float) Math.sin(angle) * 3.3F;
			s.box('w', x - 0.5F, -12.6F, z - 0.5F, 1.0F, 3.0F, 1.0F);
			s.box('s', x - 0.4F, -13.6F, z - 0.4F, 0.8F, 1.0F, 0.8F);
			double mid = angle + Math.PI / 8;
			s.box('w', (float) Math.cos(mid) * 3.5F - 0.4F, -11.2F, (float) Math.sin(mid) * 3.5F - 0.4F, 0.8F, 0.8F, 0.8F);
		}
		s.box('w', -0.5F, -15.8F, -0.5F, 1.0F, 4.0F, 1.0F);
		s.box('w', -1.6F, -14.6F, -0.4F, 1.0F, 1.0F, 0.8F);
		s.box('w', 0.6F, -14.6F, -0.4F, 1.0F, 1.0F, 0.8F);
		s.box('s', -0.4F, -16.8F, -0.4F, 0.8F, 1.0F, 0.8F);
		features(s, new String[] {
				".w.....w.",
				"www...www",
				".w.....w.",
				".........",
				".w.....w.",
				"..wwwww..",
		});
	}
}
