package com.freedomclient.util;

public final class ColorUtil {
	private ColorUtil() {
	}

	/** Color arcoíris ARGB que cambia con el tiempo; {@code offsetMs} desfasa el tono. */
	public static int rainbow(long offsetMs) {
		float hue = ((System.currentTimeMillis() + offsetMs) % 4000L) / 4000.0F;
		return 0xFF000000 | hsvToRgb(hue, 0.6F, 1.0F);
	}

	/** Convierte HSV (0..1) a RGB empaquetado en un entero 0xRRGGBB. */
	public static int hsvToRgb(float hue, float saturation, float value) {
		float h = (hue - (float) Math.floor(hue)) * 6.0F;
		int sector = (int) h;
		float f = h - sector;
		float p = value * (1.0F - saturation);
		float q = value * (1.0F - saturation * f);
		float t = value * (1.0F - saturation * (1.0F - f));

		float r, g, b;
		switch (sector) {
			case 0 -> { r = value; g = t; b = p; }
			case 1 -> { r = q; g = value; b = p; }
			case 2 -> { r = p; g = value; b = t; }
			case 3 -> { r = p; g = q; b = value; }
			case 4 -> { r = t; g = p; b = value; }
			default -> { r = value; g = p; b = q; }
		}

		return ((int) (r * 255.0F) << 16) | ((int) (g * 255.0F) << 8) | (int) (b * 255.0F);
	}
}
