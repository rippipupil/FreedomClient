package com.freedomclient.ui.scene;

import net.minecraft.client.gui.GuiGraphics;

/** Pantalla de carga de FreedomClient: cielo pixel rojizo, iniciales "FC" con halo y barra de progreso. */
public final class LoadingScreenRenderer {
	private static float shownProgress;

	private LoadingScreenRenderer() {
	}

	/**
	 * @param progress progreso real de la carga (0..1)
	 * @param alpha opacidad de toda la pantalla (para el fundido final)
	 * @param barAlpha opacidad de la barra (desaparece antes que el resto)
	 */
	public static void render(GuiGraphics g, float progress, float alpha, float barAlpha) {
		int width = g.guiWidth();
		int height = g.guiHeight();
		PixelSky.render(g, width, height, alpha);

		int p = Math.max(3, height / 55);
		int centerX = width / 2;
		int logoY = height / 2 - p * 3;
		PixelSky.halo(g, centerX, logoY - p * 9, p, alpha);
		PixelSky.logo(g, centerX, logoY, p, alpha);

		// La barra avanza suave aunque la carga vaya a saltos.
		shownProgress += (progress - shownProgress) * 0.1F;
		if (progress < shownProgress) shownProgress = progress;
		if (barAlpha > 0) renderBar(g, centerX, logoY + p * 10, Math.min(width / 3, p * 60), p, barAlpha * alpha);
	}

	private static void renderBar(GuiGraphics g, int centerX, int y, int barWidth, int p, float alpha) {
		int x = centerX - barWidth / 2;
		int h = p * 2;
		int outline = PixelSky.color(0x1A0508, alpha);
		// Marco con esquinas recortadas.
		g.fill(x, y - p, x + barWidth, y, outline);
		g.fill(x, y + h, x + barWidth, y + h + p, outline);
		g.fill(x - p, y, x, y + h, outline);
		g.fill(x + barWidth, y, x + barWidth + p, y + h, outline);
		g.fill(x, y, x + barWidth, y + h, PixelSky.color(0x3A0F1A, alpha));

		int filled = Math.round(barWidth * Math.min(1.0F, shownProgress)) / p * p;
		g.fill(x, y, x + filled, y + h, PixelSky.color(0xF2C94C, alpha));
		g.fill(x, y + h - p, x + filled, y + h, PixelSky.color(0xC98F1E, alpha));

		// Brillo que recorre la parte llena.
		if (filled > p * 3) {
			int shine = (int) ((System.currentTimeMillis() / 8) % Math.max(1, filled)) / p * p;
			g.fill(x + shine, y, x + Math.min(filled, shine + p * 2), y + p, PixelSky.color(0xFFF1C2, alpha));
		}
	}
}
