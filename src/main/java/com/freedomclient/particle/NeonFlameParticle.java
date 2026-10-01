package com.freedomclient.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Llama de energía de Neon (como su muro de la referencia): una lengua que sube despacio ondulando, crece un poco y
 * se apaga. Abajo es amarillo verdoso y al subir se vuelve cian y azul. Brilla siempre, también de noche.
 */
public class NeonFlameParticle extends SingleQuadParticle {
	/** Del suelo hacia arriba: amarillo lima, verde agua, cian y azul. */
	private static final int[] RAMP = {0xF2FF8A, 0xC6F25A, 0x7CF5D0, 0x3FD7FF, 0x2F9BFF, 0x2F6BFF};
	private static final RandomSource RANDOM = RandomSource.create();

	private final float baseSize;
	private final float startHeat;
	private final float swayPhase;

	/** {@code heat} (0..1): 0 = base amarilla del muro, 1 = parte de arriba azul. */
	public NeonFlameParticle(ClientLevel level, double x, double y, double z, float size, float heat, double rise, int lifetime) {
		super(level, x, y, z, PixelParticles.sprite("neon_flame_" + RANDOM.nextInt(3)));
		this.baseSize = size;
		this.quadSize = size * 0.6F;
		this.startHeat = heat;
		this.lifetime = lifetime;
		this.gravity = 0.0F;
		this.friction = 0.96F;
		this.hasPhysics = false;
		this.xd = 0.0;
		this.zd = 0.0;
		this.yd = rise;
		this.swayPhase = random.nextFloat() * Mth.TWO_PI;
		applyColor(heat);
	}

	/** Color del degradado del muro en {@code t} (0..1). */
	public static int ramp(float t) {
		float position = Mth.clamp(t, 0.0F, 1.0F) * (RAMP.length - 1);
		int index = Math.min(RAMP.length - 2, (int) position);
		float mix = position - index;
		int a = RAMP[index];
		int b = RAMP[index + 1];
		int r = (int) Mth.lerp(mix, a >> 16 & 0xFF, b >> 16 & 0xFF);
		int g = (int) Mth.lerp(mix, a >> 8 & 0xFF, b >> 8 & 0xFF);
		int bl = (int) Mth.lerp(mix, a & 0xFF, b & 0xFF);
		return r << 16 | g << 8 | bl;
	}

	private void applyColor(float heat) {
		int rgb = ramp(heat);
		setColor((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
	}

	@Override
	public void tick() {
		super.tick();
		float life = age / (float) Math.max(1, lifetime);
		// Ondula de lado a lado mientras sube, como las lenguas de energía.
		xd += Mth.sin(age * 0.45F + swayPhase) * 0.0035;
		zd += Mth.cos(age * 0.38F + swayPhase) * 0.0035;
		// Crece rápido al nacer y se encoge al final.
		float grow = life < 0.25F ? 0.6F + life / 0.25F * 0.5F : 1.1F - (life - 0.25F) * 0.5F;
		quadSize = baseSize * grow;
		// Al subir se enfría hacia el azul.
		applyColor(Math.min(1.0F, startHeat + life * 0.45F));
		setAlpha(life < 0.5F ? 0.62F : Math.max(0.0F, (1.0F - life) / 0.5F * 0.62F));
	}

	@Override
	protected int getLightColor(float partialTick) {
		return LightTexture.FULL_BRIGHT;
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	/** Añade una llama al mundo (si hay mundo). */
	public static void spawn(double x, double y, double z, float size, float heat, double rise, int lifetime) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) return;
		Minecraft.getInstance().particleEngine.add(new NeonFlameParticle(level, x, y, z, size, heat, rise, lifetime));
	}
}
