package com.freedomclient.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Partícula del aura: gira alrededor de una entidad (siguiéndola si se mueve), sube o baja poco a poco,
 * puede parpadear y se desvanece al final.
 */
public class AuraParticle extends SingleQuadParticle {
	private final Entity center;
	private final double radius;
	private final float angularSpeed;
	private final double rise;
	private final boolean twinkle;
	private float angle;
	private double height;

	public AuraParticle(ClientLevel level, Entity center, TextureAtlasSprite sprite, double radius, double height, float angularSpeed,
			double rise, boolean twinkle, float size, int lifetime) {
		super(level, center.getX(), center.getY() + height, center.getZ(), sprite);
		this.center = center;
		this.radius = radius;
		this.height = height;
		this.angularSpeed = angularSpeed;
		this.rise = rise;
		this.twinkle = twinkle;
		this.angle = random.nextFloat() * Mth.TWO_PI;
		this.quadSize = size;
		this.lifetime = lifetime;
		this.hasPhysics = false;
		this.gravity = 0.0F;
		place();
		this.xo = x;
		this.yo = y;
		this.zo = z;
	}

	private void place() {
		setPos(center.getX() + Mth.cos(angle) * radius, center.getY() + height, center.getZ() + Mth.sin(angle) * radius);
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		if (age++ >= lifetime || !center.isAlive()) {
			remove();
			return;
		}
		angle += angularSpeed;
		height += rise;
		place();
		float life = age / (float) lifetime;
		float fade = life < 0.15F ? life / 0.15F : life > 0.7F ? (1.0F - life) / 0.3F : 1.0F;
		if (twinkle) fade *= 0.55F + 0.45F * Mth.sin(age * 0.7F + angle * 3.0F);
		setAlpha(Math.max(0.0F, Math.min(1.0F, fade)));
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}
}
