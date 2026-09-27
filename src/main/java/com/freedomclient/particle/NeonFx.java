package com.freedomclient.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

/** Efectos eléctricos de Neon: descargas, chispas y puntos brillantes con los colores de su energía mezclados. */
public final class NeonFx {
	/** Amarillo, verde lima, verde agua, cian, azul y violeta: se recorren en orden para que los colores se mezclen. */
	public static final int[] PALETTE = {0xFFE14A, 0xC6F25A, 0x5EF0C8, 0x3FD7FF, 0x3A7BFF, 0x6B5BFF};
	private static final RandomSource RANDOM = RandomSource.create();

	private NeonFx() {
	}

	/** Color del degradado de Neon en {@code t} (0..1, se repite), interpolado entre colores vecinos. */
	public static int gradient(double t) {
		double wrapped = t - Math.floor(t);
		double position = wrapped * PALETTE.length;
		int index = (int) position;
		float mix = (float) (position - index);
		int a = PALETTE[index % PALETTE.length];
		int b = PALETTE[(index + 1) % PALETTE.length];
		int r = (int) ((a >> 16 & 0xFF) * (1 - mix) + (b >> 16 & 0xFF) * mix);
		int g = (int) ((a >> 8 & 0xFF) * (1 - mix) + (b >> 8 & 0xFF) * mix);
		int bl = (int) ((a & 0xFF) * (1 - mix) + (b & 0xFF) * mix);
		return r << 16 | g << 8 | bl;
	}

	private static TextureAtlasSprite[] bolts() {
		return new TextureAtlasSprite[] {PixelParticles.sprite("neon_bolt_a"), PixelParticles.sprite("neon_bolt_b"), PixelParticles.sprite("neon_bolt_c")};
	}

	/** Una descarga: un rayito que parpadea y se apaga en unos ticks. */
	public static void discharge(double x, double y, double z, float size, int lifetime) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) return;
		Minecraft.getInstance().particleEngine.add(new ElectricParticle(level, x, y, z, 0, 0, 0, bolts(), size, lifetime, true));
	}

	/** Chispa en cruz que sale despedida un poco. */
	public static void spark(double x, double y, double z, double speed, float size, int lifetime) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) return;
		double xd = (RANDOM.nextDouble() - 0.5) * speed;
		double yd = (RANDOM.nextDouble() - 0.3) * speed;
		double zd = (RANDOM.nextDouble() - 0.5) * speed;
		Minecraft.getInstance().particleEngine.add(new ElectricParticle(level, x, y, z, xd, yd, zd,
				new TextureAtlasSprite[] {PixelParticles.sprite("neon_spark")}, size, lifetime, false));
	}

	/** Punto brillante del color dado (para las estelas y los pasos); {@code flicker} lo hace titilar. */
	public static void dot(double x, double y, double z, int rgb, float size, int lifetime, boolean flicker) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) return;
		ElectricParticle particle = new ElectricParticle(level, x, y, z, 0, 0, 0,
				new TextureAtlasSprite[] {PixelParticles.sprite("neon_dot")}, size, lifetime, false);
		particle.setColor((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
		Minecraft.getInstance().particleEngine.add(particle);
		if (flicker && RANDOM.nextFloat() < 0.02F) {
			// De vez en cuando, una chispita en la línea.
			spark(x, y, z, 0.02, size * 2.2F, 4);
		}
	}
}
