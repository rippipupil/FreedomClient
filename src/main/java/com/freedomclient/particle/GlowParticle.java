package com.freedomclient.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Partícula que crece y se desvanece en su sitio (el brillo del halo de TotemPop) o sale despedida (chispas). */
public class GlowParticle extends SingleQuadParticle {
	private final float startSize;
	private final float endSize;

	public GlowParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite,
			float startSize, float endSize, int lifetime) {
		super(level, x, y, z, sprite);
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.startSize = startSize;
		this.endSize = endSize;
		this.quadSize = startSize;
		this.lifetime = lifetime;
		this.gravity = 0.0F;
		this.friction = 0.88F;
		this.hasPhysics = false;
	}

	@Override
	public void tick() {
		super.tick();
		float life = Math.min(1.0F, age / (float) lifetime);
		quadSize = startSize + (endSize - startSize) * (float) Math.sqrt(life);
		setAlpha(life < 0.6F ? 1.0F : Math.max(0.0F, (1.0F - life) / 0.4F));
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}
}
