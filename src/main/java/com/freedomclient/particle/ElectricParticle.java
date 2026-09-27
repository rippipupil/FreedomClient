package com.freedomclient.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

/**
 * Partícula eléctrica de Neon: brilla siempre (aunque sea de noche), puede parpadear cambiando de forma y se apaga
 * enseguida. Sirve para descargas, chispas y los puntos de las estelas.
 */
public class ElectricParticle extends SingleQuadParticle {
	private final TextureAtlasSprite[] frames;
	private final boolean flicker;

	public ElectricParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
			TextureAtlasSprite[] frames, float size, int lifetime, boolean flicker) {
		super(level, x, y, z, frames[0]);
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.frames = frames;
		this.flicker = flicker;
		this.quadSize = size;
		this.lifetime = lifetime;
		this.gravity = 0.0F;
		this.friction = 0.82F;
		this.hasPhysics = false;
		this.roll = random.nextFloat() * Mth.TWO_PI;
		this.oRoll = roll;
		if (frames.length > 1) setSprite(frames[random.nextInt(frames.length)]);
	}

	@Override
	public void tick() {
		super.tick();
		oRoll = roll;
		float life = age / (float) Math.max(1, lifetime);
		float fade = life < 0.7F ? 1.0F : Math.max(0.0F, (1.0F - life) / 0.3F);
		if (flicker) {
			// Las descargas cambian de forma y de giro y titilan, como un chispazo.
			if (age % 2 == 0 && frames.length > 1) setSprite(frames[random.nextInt(frames.length)]);
			if (random.nextFloat() < 0.4F) roll = random.nextFloat() * Mth.TWO_PI;
			fade *= 0.65F + random.nextFloat() * 0.35F;
		}
		setAlpha(fade);
	}

	@Override
	protected int getLightColor(float partialTick) {
		return LightTexture.FULL_BRIGHT;
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}
}
