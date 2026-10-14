package com.freedomclient.particle;

import com.freedomclient.cosmetic.vox.GhostMotion;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Flor de la estela Ghost Bloom (blanca, rosa o rosa fuerte): brota con un saltito, se mece y al final se cierra y se
 * encoge, con la vida de {@link GhostMotion#flower} (la del diseño). Los sprites salen de la flor de vóxeles del diseño
 * (scripts/textures/ghost/bloom_sprites.py): 4 de vaivén y 3 cerrándose. Se queda con la base apoyada en el suelo.
 */
public class BloomParticle extends SingleQuadParticle {
	/** Inclinación de cada sprite (radianes), como FLOWER_TILTS en bloom_sprites.py. */
	private static final double[] TILTS = {-0.12, -0.04, 0.04, 0.12, 0.25, 0.38, 0.5};
	/** Lo que mide el sprite entero, en píxeles del diseño (FLOWER_SPAN). */
	private static final double SPAN = 4.0;

	private final TextureAtlasSprite[] frames = new TextureAtlasSprite[TILTS.length];
	private final double groundY;
	private final double seed;
	private final double life;
	private final double[] state = new double[3];

	public BloomParticle(ClientLevel level, double x, double groundY, double z, String kind, double seed, double lifeSeconds) {
		super(level, x, groundY, z, PixelParticles.sprite("bloom_" + kind + "_0"));
		for (int i = 0; i < frames.length; i++) frames[i] = PixelParticles.sprite("bloom_" + kind + "_" + i);
		this.groundY = groundY;
		this.seed = seed;
		this.life = lifeSeconds;
		this.lifetime = (int) Math.ceil(lifeSeconds * 20) + 1;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.xd = 0.0;
		this.yd = 0.0;
		this.zd = 0.0;
		this.quadSize = 0.0F;
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		if (age++ >= lifetime || !GhostMotion.flower(age / 20.0, seed, life, state)) {
			remove();
			return;
		}
		// El sprite más parecido a lo inclinada que está: el vaivén y, al final, cerrándose.
		double tilt = state[1] + state[2] * 0.5;
		int best = 0;
		for (int i = 1; i < TILTS.length; i++) if (Math.abs(TILTS[i] - tilt) < Math.abs(TILTS[best] - tilt)) best = i;
		setSprite(frames[best]);
		// El sprite entero mide SPAN píxeles a tamaño FLOWER_SIZE; quadSize es la mitad, en bloques.
		quadSize = (float) (SPAN / 16.0 / 2.0 * state[0] / GhostMotion.FLOWER_SIZE);
		setPos(x, groundY + quadSize, z);
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	/** Pétalo suelto que sale de la flor al brotar: sube dando vueltas y se encoge ({@link GhostMotion#petal}). */
	public static class Petal extends SingleQuadParticle {
		private static final double SPAN = 1.5;
		private final TextureAtlasSprite[] frames = new TextureAtlasSprite[8];
		private final double baseX, baseY, baseZ, dx, dz, spin;
		private final double[] state = new double[5];

		public Petal(ClientLevel level, double x, double y, double z, String kind, double dx, double dz, double spin) {
			super(level, x, y, z, PixelParticles.sprite("bloom_petal_" + kind + "_0"));
			for (int i = 0; i < frames.length; i++) frames[i] = PixelParticles.sprite("bloom_petal_" + kind + "_" + i);
			baseX = x;
			baseY = y;
			baseZ = z;
			this.dx = dx;
			this.dz = dz;
			this.spin = spin;
			this.lifetime = 25;
			this.gravity = 0.0F;
			this.hasPhysics = false;
			this.xd = 0.0;
			this.yd = 0.0;
			this.zd = 0.0;
			this.quadSize = 0.0F;
		}

		@Override
		public void tick() {
			xo = x;
			yo = y;
			zo = z;
			if (age++ >= lifetime || !GhostMotion.petal(age / 20.0, dx, dz, spin, state)) {
				remove();
				return;
			}
			setPos(baseX + state[0] / 16.0, baseY + state[1] / 16.0, baseZ + state[2] / 16.0);
			setSprite(frames[Math.floorMod((int) Math.floor(state[3] / (Math.PI * 2) * frames.length), frames.length)]);
			quadSize = (float) (SPAN / 16.0 / 2.0 * state[4]);
		}

		@Override
		protected Layer getLayer() {
			return Layer.TRANSLUCENT;
		}
	}
}
