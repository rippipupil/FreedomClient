package com.freedomclient.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

/** Pluma pixel que sale despedida al golpear y cae meciéndose de lado a lado. */
public class FeatherParticle extends SingleQuadParticle {
	private final float spin;
	private final float swayPhase;

	public FeatherParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite) {
		super(level, x, y, z, sprite);
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.gravity = 0.04F;
		this.friction = 0.9F;
		this.hasPhysics = true;
		this.lifetime = 30 + random.nextInt(20);
		this.quadSize = 0.13F + random.nextFloat() * 0.06F;
		this.roll = random.nextFloat() * Mth.TWO_PI;
		this.oRoll = roll;
		this.spin = (random.nextFloat() - 0.5F) * 0.25F;
		this.swayPhase = random.nextFloat() * Mth.TWO_PI;
	}

	@Override
	public void tick() {
		super.tick();
		oRoll = roll;
		// Al caer se mece de lado a lado y gira un poco, como una pluma de verdad.
		if (yd < 0.0) {
			yd *= 0.6;
			xd += Mth.sin(age * 0.35F + swayPhase) * 0.006;
			zd += Mth.cos(age * 0.35F + swayPhase) * 0.006;
			roll += spin;
		}
		// Se desvanece en los últimos ticks.
		int fadeTicks = 10;
		if (lifetime - age < fadeTicks) setAlpha(Math.max(0.0F, (lifetime - age) / (float) fadeTicks));
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}
}
