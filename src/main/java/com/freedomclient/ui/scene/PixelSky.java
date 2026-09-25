package com.freedomclient.ui.scene;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Cielo de atardecer rojizo en pixel art, animado (nubes con paralaje, estrellas que parpadean y el sol).
 * Se dibuja solo con rectángulos, sin texturas, para poder usarlo en la pantalla de carga.
 */
public final class PixelSky {
	/** Franjas del degradado de arriba (noche) a abajo (horizonte dorado). */
	private static final int[] BANDS = {
			0x1A0B2E, 0x241035, 0x2E1339, 0x3B1639, 0x4A1838, 0x5C1A35, 0x701C31, 0x86202E,
			0x9C262C, 0xB22F2A, 0xC63D29, 0xD74F2A, 0xE4642E, 0xEE7A34, 0xF4913C, 0xF7A845,
			0xF6BD52, 0xF2C94C,
	};

	private static final String[] CLOUD_BIG = {
			"......####..........",
			"....########...###..",
			"..##############.##.",
			".##################.",
			"####################",
			".##################.",
	};

	private static final String[] CLOUD_SMALL = {
			"...###....",
			".#######..",
			"##########",
			".########.",
	};

	/** Logo "FC": F y C de trazo doble con la C redondeada (16x10). */
	private static final String[] FC = {
			"#######...######",
			"#######..#######",
			"##.......##.....",
			"##.......##.....",
			"######...##.....",
			"######...##.....",
			"##.......##.....",
			"##.......##.....",
			"##.......#######",
			"##........######",
	};
	/** Color de cada fila del logo: blanco arriba, crema y dorado abajo, con la última fila roja como sombra. */
	private static final int[] FC_ROWS = {0xFFFFFF, 0xF5F1E8, 0xF5F1E8, 0xF5F1E8, 0xF7E6C8, 0xF7E6C8, 0xF5D98A, 0xF2C94C, 0xE8A93A, 0xD7263D};

	private PixelSky() {
	}

	/** Tamaño de un "píxel" del cielo en unidades de interfaz, según la altura de la pantalla. */
	public static int pixelSize(int height) {
		return Math.max(2, height / 100);
	}

	/** Dibuja el cielo completo. {@code alpha} (0..1) permite hacer fundidos. */
	public static void render(GuiGraphics g, int width, int height, float alpha) {
		int p = pixelSize(height);
		long time = System.currentTimeMillis();

		// Degradado por franjas con una fila de "tramado" entre franjas para que parezca pixel art.
		int horizon = height * 3 / 4;
		int bandHeight = Math.max(p, horizon / BANDS.length / p * p);
		for (int i = 0; i < BANDS.length; i++) {
			int y1 = i * bandHeight;
			int y2 = i == BANDS.length - 1 ? horizon : y1 + bandHeight;
			g.fill(0, y1, width, y2, color(BANDS[i], alpha));
			if (i + 1 < BANDS.length) {
				for (int x = (i % 2) * p; x < width; x += p * 2) {
					g.fill(x, y2 - p, x + p, y2, color(BANDS[i + 1], alpha));
				}
			}
		}

		// Estrellas en la parte alta que parpadean.
		for (int i = 0; i < 40; i++) {
			int sx = (int) ((i * 7919L) % Math.max(1, width / p)) * p;
			int sy = (int) ((i * 104729L) % Math.max(1, (horizon / 3) / p)) * p;
			boolean on = ((time / 400) + i * 13) % 7 != 0;
			if (on) g.fill(sx, sy, sx + p, sy + p, color(0xF5F1E8, alpha * (i % 3 == 0 ? 0.9F : 0.5F)));
		}

		// Sol dorado cerca del horizonte con un halo.
		int sunR = p * 9;
		int sunX = width * 2 / 3;
		int sunY = horizon - p * 4;
		disc(g, sunX, sunY, sunR + p * 3, p, color(0xFFD27A, alpha * 0.35F));
		disc(g, sunX, sunY, sunR, p, color(0xFFE08A, alpha));
		disc(g, sunX, sunY, sunR - p * 3, p, color(0xFFF1C2, alpha));

		// Nubes en dos capas que se mueven a distinta velocidad.
		clouds(g, width, horizon, p, time, alpha);

		// Colinas oscuras en el horizonte.
		for (int x = 0; x < width; x += p) {
			double wave = Math.sin(x / (double) (p * 23)) * 3 + Math.sin(x / (double) (p * 9)) * 1.5;
			int top = horizon - (int) Math.round(wave + 3) * p;
			g.fill(x, top, x + p, height, color(0x3A0F1A, alpha));
			g.fill(x, top, x + p, top + p, color(0x5C1A2A, alpha));
		}
	}

	private static void clouds(GuiGraphics g, int width, int horizon, int p, long time, float alpha) {
		int far = color(0xE89A7A, alpha * 0.8F);
		int farShade = color(0xB8606A, alpha * 0.8F);
		int near = color(0xFFC9A0, alpha);
		int nearShade = color(0xD9776A, alpha);

		for (int i = 0; i < 4; i++) {
			int span = width + 40 * p;
			int x = (int) ((i * span / 4 + time / 90) % span) - 20 * p;
			int y = horizon / 4 + (i % 2) * p * 10;
			shape(g, CLOUD_SMALL, x / p * p, y / p * p, p, far, farShade);
		}
		for (int i = 0; i < 3; i++) {
			int span = width + 60 * p;
			int x = (int) ((i * span / 3 + time / 45) % span) - 30 * p;
			int y = horizon / 2 + (i % 2) * p * 12;
			shape(g, CLOUD_BIG, x / p * p, y / p * p, p, near, nearShade);
		}
	}

	/** Dibuja una forma de '#' con la última fila en el color de sombra. */
	public static void shape(GuiGraphics g, String[] rows, int x, int y, int p, int color, int shadeColor) {
		for (int row = 0; row < rows.length; row++) {
			int c = row == rows.length - 1 ? shadeColor : color;
			for (int column = 0; column < rows[row].length(); column++) {
				if (rows[row].charAt(column) == '#') {
					g.fill(x + column * p, y + row * p, x + (column + 1) * p, y + (row + 1) * p, c);
				}
			}
		}
	}

	/** Las iniciales "FC" en pixel art con contorno, sombra y degradado, centradas en (cx, cy). */
	public static void logo(GuiGraphics g, int cx, int cy, int p, float alpha) {
		int w = FC[0].length() * p;
		int h = FC.length * p;
		int x = cx - w / 2;
		int y = cy - h / 2;
		int outline = color(0x1A0508, alpha);
		// Sombra abajo a la derecha y contorno oscuro alrededor.
		shape(g, FC, x + p * 2, y + p * 2, p, color(0x1A0508, alpha * 0.45F), color(0x1A0508, alpha * 0.45F));
		shape(g, FC, x - p, y, p, outline, outline);
		shape(g, FC, x + p, y, p, outline, outline);
		shape(g, FC, x, y - p, p, outline, outline);
		shape(g, FC, x, y + p, p, outline, outline);
		for (int row = 0; row < FC.length; row++) {
			int c = color(FC_ROWS[row], alpha);
			for (int column = 0; column < FC[row].length(); column++) {
				if (FC[row].charAt(column) == '#') {
					g.fill(x + column * p, y + row * p, x + (column + 1) * p, y + (row + 1) * p, c);
				}
			}
		}
	}

	/** Ancho y alto del logo en píxeles de logo (sin contorno). */
	public static int logoWidth() {
		return FC[0].length();
	}

	public static int logoHeight() {
		return FC.length;
	}

	/** Halo dorado (anillo pixelado) que flota arriba y abajo. */
	public static void halo(GuiGraphics g, int cx, int cy, int p, float alpha) {
		halo(g, cx, cy, p, 9, alpha);
	}

	/** Halo con un radio horizontal de {@code radius} píxeles de logo. */
	public static void halo(GuiGraphics g, int cx, int cy, int p, int radius, float alpha) {
		int bob = (int) Math.round(Math.sin(System.currentTimeMillis() / 400.0) * 1.5) * p;
		int rx = p * radius;
		int ry = p * 2;
		int gold = color(0xF2C94C, alpha);
		int dark = color(0xC98F1E, alpha);
		for (int x = -rx; x <= rx; x += p) {
			double t = x / (double) rx;
			int dy = (int) Math.round(Math.sqrt(Math.max(0, 1 - t * t)) * ry / p) * p;
			g.fill(cx + x, cy + bob - dy - p, cx + x + p, cy + bob - dy, gold);
			g.fill(cx + x, cy + bob + dy, cx + x + p, cy + bob + dy + p, dark);
		}
	}

	private static void disc(GuiGraphics g, int cx, int cy, int r, int p, int color) {
		for (int y = -r; y <= r; y += p) {
			int half = (int) Math.sqrt(Math.max(0, r * r - y * y)) / p * p;
			g.fill(cx - half, cy + y, cx + half, cy + y + p, color);
		}
	}

	public static int color(int rgb, float alpha) {
		int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
		return a << 24 | (rgb & 0xFFFFFF);
	}
}
