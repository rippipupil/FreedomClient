package com.freedomclient.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Brasa "pixel 3D": un cubito que gira (8 fotogramas de hit_ember), sube despacio, se encoge y se apaga.
 * Brilla en la oscuridad y desaparece al pasar a primera persona.
 */
public class EmberParticle extends SingleQuadParticle {
	private final TextureAtlasSprite[] frames;
	private final float startSize;
	private float frame;

	public EmberParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, int rgb, float size, int lifetime) {
		super(level, x, y, z, PixelParticles.sprite("hit_ember_0"));
		frames = new TextureAtlasSprite[8];
		for (int i = 0; i < frames.length; i++) frames[i] = PixelParticles.sprite("hit_ember_" + i);
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.gravity = -0.01F;
		this.friction = 0.92F;
		this.hasPhysics = false;
		this.lifetime = lifetime;
		this.startSize = size;
		this.quadSize = size;
		this.frame = random.nextInt(frames.length);
		setColor((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
	}

	@Override
	public void tick() {
		if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
			remove();
			return;
		}
		super.tick();
		frame = (frame + 0.35F) % frames.length;
		setSprite(frames[(int) frame]);
		float life = age / (float) lifetime;
		quadSize = startSize * (1.0F - life * 0.7F);
		if (life > 0.6F) setAlpha((1.0F - life) / 0.4F);
	}

	@Override
	public int getLightColor(float partialTick) {
		return LightTexture.FULL_BRIGHT;
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}
}
