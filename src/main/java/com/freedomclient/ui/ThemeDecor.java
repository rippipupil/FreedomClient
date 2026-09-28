package com.freedomclient.ui;

import com.freedomclient.ui.theme.ThemeManager;
import com.freedomclient.ui.theme.ThemePreset;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Detalles propios de cada tema en la ventana del menú, en pixel art y animados con calma: sol poniéndose y pájaros
 * en Red Sunset, nubes que pasan en Day Sky, estrellas que titilan, luna y estrellas fugaces en Starry Night, y
 * llamas con brasas que suben en Red Devil (Neon ya tiene su energía). Son pocos rectángulos por fotograma.
 */
public final class ThemeDecor {
	private ThemeDecor() {
	}

	private static long now() {
		return System.currentTimeMillis();
	}

	/** Número pseudoaleatorio fijo (0..1) para colocar estrellas y brasas siempre en el mismo sitio. */
	private static double hash(int n) {
		double v = Math.sin(n * 127.1 + 311.7) * 43758.5453;
		return v - Math.floor(v);
	}

	private static int alpha(int rgb, float alpha) {
		return ThemeManager.withAlpha(0xFF000000 | rgb, alpha);
	}

	/** Detrás del contenido, sobre el fondo de la ventana. */
	public static void background(GuiGraphics g, int x, int y, int w, int h, int header) {
		ThemePreset preset = ThemeManager.getPreset();
		switch (preset) {
			case RED_SUNSET -> g.fillGradient(x + 1, y + h - 40, x + w - 1, y + h - 1, 0x00FF8C42, 0x38FF8C42);
			case DAY_SKY -> {
				g.fillGradient(x + 1, y + h - 36, x + w - 1, y + h - 1, 0x005DADE2, 0x305DADE2);
				clouds(g, x, y + h - 22, w, 0.5F, 1, 90_000L);
			}
			case STARRY_NIGHT -> stars(g, x + 2, y + header + 2, w - 4, h - header - 4, 26, 0.55F);
			case RED_DEVIL -> g.fillGradient(x + 1, y + h - 44, x + w - 1, y + h - 1, 0x00D7263D, 0x40D7263D);
			default -> {
			}
		}
	}

	/** Por encima del contenido (medio transparente): las llamas y las brasas de Red Devil suben sobre las tarjetas. */
	public static void foreground(GuiGraphics g, int x, int y, int w, int h, int header) {
		if (ThemeManager.getPreset() == ThemePreset.RED_DEVIL) {
			flames(g, x + 1, y + h - 1, w - 2);
			embers(g, x + 1, y + header, w - 2, h - header - 2, 16);
		}
	}

	/** En la cabecera, después de su fondo y antes del logo y las pestañas. */
	public static void header(GuiGraphics g, int x, int y, int w, int h) {
		ThemePreset preset = ThemeManager.getPreset();
		switch (preset) {
			case RED_SUNSET -> sunset(g, x, y, w, h);
			case DAY_SKY -> clouds(g, x, y + 4, w, 0.8F, 2, 60_000L);
			case STARRY_NIGHT -> {
				stars(g, x + 2, y + 2, w - 4, h - 4, 18, 1.0F);
				moon(g, x + (int) (w * 0.40), y + 8);
				shootingStar(g, x, y, w, h);
			}
			case RED_DEVIL -> {
				g.fillGradient(x + 1, y + 1, x + w - 1, y + h, 0x00000000, 0x30FF4B4B);
				embers(g, x + 1, y + 1, w - 2, h - 1, 6);
			}
			default -> {
			}
		}
	}

	/** Sol medio escondido con franjas (estilo retro) y dos pájaros que se mecen. */
	private static void sunset(GuiGraphics g, int x, int y, int w, int h) {
		int cx = x + (int) (w * 0.42);
		int bottom = y + h;
		int r = 10;
		for (int dy = 0; dy < r; dy++) {
			// Franjas: se saltan algunas filas de la parte de abajo del sol.
			if (dy < 5 && dy % 2 == 1) continue;
			int half = (int) Math.round(Math.sqrt(r * r - (dy + 0.5) * (dy + 0.5)));
			int color = dy > 6 ? 0xFFFFE08A : dy > 3 ? 0xFFFFB04A : 0xFFFF7A3A;
			g.fill(cx - half, bottom - dy - 1, cx + half, bottom - dy, ThemeManager.withAlpha(color, 0.75F));
		}
		long t = now();
		for (int i = 0; i < 2; i++) {
			int bx = cx - 26 + i * 11;
			int by = y + 6 + i * 3 + (int) Math.round(Math.sin(t / 700.0 + i * 2.0));
			boolean up = (t / 350 + i) % 2 == 0;
			int bird = 0xB02A0A0A;
			g.fill(bx - 2, by + (up ? 0 : 1), bx - 1, by + (up ? 1 : 2), bird);
			g.fill(bx - 1, by + 1, bx, by + 2, bird);
			g.fill(bx, by + 1, bx + 1, by + 2, bird);
			g.fill(bx + 1, by + (up ? 0 : 1), bx + 2, by + (up ? 1 : 2), bird);
		}
	}

	/** Nubes pixel que cruzan despacio de izquierda a derecha ({@code periodMs} tarda una en dar la vuelta). */
	private static void clouds(GuiGraphics g, int x, int y, int w, float alpha, int count, long periodMs) {
		int white = ThemeManager.withAlpha(0xFFFFFFFF, 0.55F * alpha);
		int shade = ThemeManager.withAlpha(0xFFC8DCF0, 0.45F * alpha);
		for (int i = 0; i < count + 1; i++) {
			double phase = ((now() % periodMs) / (double) periodMs + i / (double) (count + 1)) % 1.0;
			int cx = x - 30 + (int) Math.round((w + 60) * phase);
			int cy = y + (i % 2) * 5;
			int left = Math.max(x + 1, cx - 14);
			int right = Math.min(x + w - 1, cx + 16);
			if (right <= left) continue;
			// Tres bultos y la base plana.
			fillClipped(g, cx - 14, cy + 6, cx + 16, cy + 10, x, w, shade);
			fillClipped(g, cx - 10, cy + 3, cx + 2, cy + 8, x, w, white);
			fillClipped(g, cx - 2, cy, cx + 10, cy + 8, x, w, white);
			fillClipped(g, cx + 8, cy + 4, cx + 14, cy + 8, x, w, white);
		}
	}

	private static void fillClipped(GuiGraphics g, int x1, int y1, int x2, int y2, int x, int w, int color) {
		int left = Math.max(x + 1, x1);
		int right = Math.min(x + w - 1, x2);
		if (right > left) g.fill(left, y1, right, y2, color);
	}

	/** Estrellas fijas que titilan cada una a su ritmo; algunas son crucecitas más brillantes. */
	private static void stars(GuiGraphics g, int x, int y, int w, int h, int count, float alpha) {
		long t = now();
		for (int i = 0; i < count; i++) {
			int sx = x + (int) (hash(i) * w);
			int sy = y + (int) (hash(i + 100) * h);
			float twinkle = (float) (0.35 + 0.65 * (0.5 + 0.5 * Math.sin(t / (500.0 + hash(i + 200) * 900.0) + i)));
			int color = ThemeManager.withAlpha(i % 5 == 0 ? 0xFFFFE9A8 : 0xFFE8ECFF, twinkle * alpha * 0.8F);
			g.fill(sx, sy, sx + 1, sy + 1, color);
			if (i % 6 == 0 && twinkle > 0.7F) {
				int glow = ThemeManager.withAlpha(0xFFE8ECFF, (twinkle - 0.7F) * alpha);
				g.fill(sx - 1, sy, sx, sy + 1, glow);
				g.fill(sx + 1, sy, sx + 2, sy + 1, glow);
				g.fill(sx, sy - 1, sx + 1, sy, glow);
				g.fill(sx, sy + 1, sx + 1, sy + 2, glow);
			}
		}
	}

	/** Luna creciente de 12 px: disco claro con un mordisco del color de la cabecera. */
	private static void moon(GuiGraphics g, int x, int y) {
		String[] rows = {
				"...####.....",
				".######.....",
				".#####......",
				"#####.......",
				"####........",
				"####........",
				"####........",
				"#####.......",
				".#####......",
				".######.....",
				"...####.....",
		};
		int light = 0xE0F4F0D8;
		int dim = 0xC0C8C4B0;
		for (int row = 0; row < rows.length; row++) {
			String line = rows[row];
			int start = line.indexOf('#');
			int end = line.lastIndexOf('#') + 1;
			if (start < 0) continue;
			g.fill(x + start, y + row, x + end, y + row + 1, light);
			g.fill(x + start, y + row, x + start + 1, y + row + 1, dim);
		}
	}

	/** De vez en cuando, una estrella fugaz cruza la cabecera en diagonal. */
	private static void shootingStar(GuiGraphics g, int x, int y, int w, int h) {
		long period = 7000;
		long time = now() % period;
		long travel = 700;
		if (time > travel) return;
		float p = time / (float) travel;
		int hx = x + (int) (w * (0.15 + 0.5 * p));
		int hy = y + 3 + (int) ((h - 8) * p);
		for (int i = 0; i < 10; i++) {
			int color = ThemeManager.withAlpha(0xFFFFF7D6, (1.0F - i / 10.0F) * 0.9F);
			g.fill(hx - i * 2, hy - i, hx - i * 2 + 2, hy - i + 1, color);
		}
	}

	/** Llamas que suben del borde de abajo, cada columna con su altura moviéndose (rojo, naranja y amarillo). */
	private static void flames(GuiGraphics g, int x, int bottom, int w) {
		double t = now() / 1000.0;
		int step = 4;
		for (int i = 0; i * step < w; i++) {
			double wave = Math.sin(t * 3.1 + i * 0.9) + Math.sin(t * 5.3 + i * 1.7) * 0.6 + Math.sin(t * 1.3 + i * 0.35) * 0.8;
			int height = 8 + (int) Math.round((wave + 2.4) * 3.2);
			int left = x + i * step;
			int right = Math.min(x + w, left + step);
			g.fillGradient(left, bottom - height, right, bottom, 0x00D7263D, 0xA0E0303D);
			int core = height * 3 / 5;
			g.fillGradient(left, bottom - core, right, bottom, 0x00FF7A2A, 0x90FF8C3A);
			int tip = height / 3;
			g.fillGradient(left + 1, bottom - tip, right - 1, bottom, 0x00FFD060, 0x90FFE08A);
		}
	}

	/** Brasas: puntitos naranjas y amarillos que suben despacio, se mecen y se apagan arriba. */
	private static void embers(GuiGraphics g, int x, int y, int w, int h, int count) {
		long t = now();
		for (int i = 0; i < count; i++) {
			double speed = 5000 + hash(i + 7) * 4000;
			double p = ((t + hash(i + 3) * speed) % speed) / speed;
			int ex = x + (int) (hash(i) * w + Math.sin(t / 600.0 + i) * 3);
			int ey = y + h - (int) (p * h);
			float fade = (float) Math.min(1.0, (1.0 - p) * 2.0);
			int color = i % 3 == 0 ? 0xFFFFE08A : i % 3 == 1 ? 0xFFFF8C42 : 0xFFFF4B4B;
			int size = i % 4 == 0 ? 2 : 1;
			g.fill(ex, ey, ex + size, ey + size, alpha(color & 0xFFFFFF, fade * 0.9F));
		}
	}
}
