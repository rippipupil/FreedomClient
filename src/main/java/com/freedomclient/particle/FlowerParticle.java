package com.freedomclient.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

/**
 * Flor "pixel 3D" que brota en el suelo donde pisas: crece, se mece despacio (4 fotogramas de la flor girando un
 * poco a cada lado) y al final se encoge y se desvanece. Se queda con la base apoyada en el suelo.
 */
public class FlowerParticle extends SingleQuadParticle {
	private final TextureAtlasSprite[] frames;
	private final double groundY;
	private final float fullSize;
	private final float swayPhase;

	public FlowerParticle(ClientLevel level, double x, double groundY, double z, String flower, float size, int lifetime) {
		super(level, x, groundY, z, PixelParticles.sprite("flower_" + flower + "_0"));
		frames = new TextureAtlasSprite[4];
		for (int i = 0; i < frames.length; i++) frames[i] = PixelParticles.sprite("flower_" + flower + "_" + i);
		this.groundY = groundY;
		this.fullSize = size;
		this.lifetime = lifetime;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.xd = 0.0;
		this.yd = 0.0;
		this.zd = 0.0;
		this.quadSize = 0.0F;
		this.swayPhase = random.nextFloat() * Mth.TWO_PI;
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		if (age++ >= lifetime) {
			remove();
			return;
		}
		float grow = Math.min(1.0F, age / 7.0F);
		float shrink = Math.min(1.0F, (lifetime - age) / 10.0F);
		quadSize = fullSize * grow * shrink;
		if (lifetime - age < 10) setAlpha(shrink);
		// Balanceo suave: pasa por los fotogramas de un lado al otro.
		float sway = (Mth.sin(age * 0.12F + swayPhase) + 1.0F) / 2.0F;
		setSprite(frames[Math.min(frames.length - 1, (int) (sway * frames.length))]);
		// El sprite se dibuja centrado: se sube la mitad para que el tallo toque el suelo.
		setPos(x, groundY + quadSize * 0.95, z);
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}
}
