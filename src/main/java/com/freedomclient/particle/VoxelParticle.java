package com.freedomclient.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

/**
 * Partícula "pixel 3D": un objeto de vóxeles (pluma, copo o calabaza) dibujado en 8 posiciones de giro
 * (tools/make_voxel_particles.py) que va pasando de una a otra, así parece que da vueltas en el aire.
 */
public class VoxelParticle extends SingleQuadParticle {
	private final TextureAtlasSprite[] frames;
	/** Fotogramas por tick (con signo: gira hacia un lado o hacia el otro). */
	private final float spinSpeed;
	private float frame;
	private final float swayPhase;
	private final boolean flutter;

	public VoxelParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
			TextureAtlasSprite[] frames, float size, int lifetime, boolean flutter) {
		super(level, x, y, z, frames[0]);
		this.frames = frames;
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.gravity = flutter ? 0.03F : 0.06F;
		this.friction = 0.9F;
		this.hasPhysics = true;
		this.lifetime = lifetime;
		this.quadSize = size;
		this.frame = random.nextInt(frames.length);
		this.spinSpeed = (0.25F + random.nextFloat() * 0.25F) * (random.nextBoolean() ? 1 : -1);
		this.swayPhase = random.nextFloat() * Mth.TWO_PI;
		this.flutter = flutter;
		setSprite(frames[(int) frame]);
	}

	@Override
	public void tick() {
		super.tick();
		frame = (frame + spinSpeed + frames.length) % frames.length;
		setSprite(frames[(int) frame]);
		// Las plumas y los copos caen meciéndose; las calabazas caen rectas.
		if (flutter && yd < 0.0) {
			yd *= 0.6;
			xd += Mth.sin(age * 0.35F + swayPhase) * 0.005;
			zd += Mth.cos(age * 0.35F + swayPhase) * 0.005;
		}
		int fadeTicks = 6;
		if (lifetime - age < fadeTicks) setAlpha(Math.max(0.0F, (lifetime - age) / (float) fadeTicks));
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}
}
