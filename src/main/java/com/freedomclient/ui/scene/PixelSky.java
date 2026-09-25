package com.freedomclient.ui.scene;

import com.freedomclient.module.visual.CustomScreensModule;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Cielo en pixel art, animado (nubes que se mueven, estrellas que parpadean y el sol o la luna), en tres estilos:
 * atardecer rojizo, noche estrellada y día. Con paralaje, cada capa se desplaza distinto al mover el ratón.
 * Se dibuja solo con rectángulos, sin texturas, para poder usarlo en la pantalla de carga.
 */
public final class PixelSky {
	public enum Style {
		SUNSET(SUNSET_BANDS, 40, 0xF5F1E8, 0xE89A7A, 0xB8606A, 0xFFC9A0, 0xD9776A, 0x3A0F1A, 0x5C1A2A),
		NIGHT(NIGHT_BANDS, 110, 0xF5F1E8, 0x3A3F6B, 0x2A2E55, 0x4A5080, 0x323766, 0x0B0C1E, 0x1C1E3E),
		DAY(DAY_BANDS, 0, 0xFFFFFF, 0xF4F8FC, 0xD2E2EE, 0xFFFFFF, 0xD6E6F2, 0x3E7A34, 0x5FA048);

		final int[] bands;
		final int stars;
		final int starColor;
		final int farCloud;
		final int farCloudShade;
		final int nearCloud;
		final int nearCloudShade;
		final int hills;
		final int hillsTop;

		Style(int[] bands, int stars, int starColor, int farCloud, int farCloudShade, int nearCloud, int nearCloudShade, int hills, int hillsTop) {
			this.bands = bands;
			this.stars = stars;
			this.starColor = starColor;
			this.farCloud = farCloud;
			this.farCloudShade = farCloudShade;
			this.nearCloud = nearCloud;
			this.nearCloudShade = nearCloudShade;
			this.hills = hills;
			this.hillsTop = hillsTop;
		}
	}

	private static final int[] NIGHT_BANDS = {
			0x04051A, 0x060822, 0x090B2A, 0x0C0F33, 0x10143C, 0x141945, 0x19204F, 0x1F2759,
			0x252E63, 0x2C366C, 0x333E74, 0x3B467B, 0x434E80, 0x4C5684,
	};
	private static final int[] DAY_BANDS = {
			0x3F8FD8, 0x4799DD, 0x50A2E1, 0x5AABE5, 0x64B4E9, 0x6FBDEC, 0x7AC5EF, 0x86CDF2,
			0x93D5F5, 0xA0DCF7, 0xAEE2F9, 0xBDE8FA,
	};
	/** Franjas del atardecer de arriba (noche) a abajo (horizonte dorado). */
	private static final int[] SUNSET_BANDS = {
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

	/** Dibuja el cielo completo con el estilo elegido en Client Screens. {@code alpha} (0..1) permite hacer fundidos. */
	public static void render(GuiGraphics g, int width, int height, float alpha) {
		render(g, width, height, alpha, 0.0F, 0.0F);
	}

	/** Igual, con paralaje: {@code parallaxX}/{@code parallaxY} van de -1 a 1 según dónde esté el ratón. */
	public static void render(GuiGraphics g, int width, int height, float alpha, float parallaxX, float parallaxY) {
		Style style = CustomScreensModule.skyStyle();
		int p = pixelSize(height);
		long time = System.currentTimeMillis();
		int[] bands = style.bands;

		// Degradado por franjas con una fila de "tramado" entre franjas para que parezca pixel art.
		int horizon = height * 3 / 4;
		int bandHeight = Math.max(p, horizon / bands.length / p * p);
		for (int i = 0; i < bands.length; i++) {
			int y1 = i * bandHeight;
			int y2 = i == bands.length - 1 ? horizon + p * 20 : y1 + bandHeight;
			g.fill(0, y1, width, y2, color(bands[i], alpha));
			if (i + 1 < bands.length) {
				for (int x = (i % 2) * p; x < width; x += p * 2) {
					g.fill(x, y2 - p, x + p, y2, color(bands[i + 1], alpha));
				}
			}
		}

		// Estrellas en la parte alta que parpadean (la capa más lejana: casi no se mueve).
		int starsX = layerShift(parallaxX, 2, p);
		int starsY = layerShift(parallaxY, 1, p);
		int starArea = style == Style.NIGHT ? horizon * 2 / 3 : horizon / 3;
		for (int i = 0; i < style.stars; i++) {
			int sx = (int) ((i * 7919L) % Math.max(1, width / p)) * p + starsX;
			int sy = (int) ((i * 104729L) % Math.max(1, starArea / p)) * p + starsY;
			boolean on = ((time / 400) + i * 13) % 7 != 0;
			if (on) g.fill(sx, sy, sx + p, sy + p, color(style.starColor, alpha * (i % 3 == 0 ? 0.9F : 0.5F)));
		}

		// Sol (atardecer y día) o luna (noche).
		int sunX = width * 2 / 3 + layerShift(parallaxX, 4, p);
		int shiftY = layerShift(parallaxY, 2, p);
		if (style == Style.NIGHT) {
			int moonR = p * 7;
			int moonY = horizon / 4 + shiftY;
			disc(g, sunX, moonY, moonR + p * 4, p, color(0xB8C4F0, alpha * 0.18F));
			disc(g, sunX, moonY, moonR, p, color(0xF2F0E6, alpha));
			disc(g, sunX - p * 2, moonY - p * 2, p * 2, p, color(0xD6D2C4, alpha));
			disc(g, sunX + p * 3, moonY + p * 2, p * 1, p, color(0xD6D2C4, alpha));
		} else {
			int sunR = p * 9;
			int sunY = (style == Style.DAY ? horizon / 4 : horizon - p * 4) + shiftY;
			disc(g, sunX, sunY, sunR + p * 3, p, color(style == Style.DAY ? 0xFFF4B0 : 0xFFD27A, alpha * 0.35F));
			disc(g, sunX, sunY, sunR, p, color(style == Style.DAY ? 0xFFEE88 : 0xFFE08A, alpha));
			disc(g, sunX, sunY, sunR - p * 3, p, color(0xFFF1C2, alpha));
		}

		// Nubes en dos capas que se mueven a distinta velocidad y con distinto paralaje.
		clouds(g, width, horizon, p, time, alpha, style, parallaxX, parallaxY);

		// Colinas en el horizonte: la capa más cercana, la que más se mueve.
		int hillsX = layerShift(parallaxX, 10, p);
		int hillsY = layerShift(parallaxY, 3, p);
		for (int x = -p * 12; x < width + p * 12; x += p) {
			int wx = x - hillsX;
			double wave = Math.sin(wx / (double) (p * 23)) * 3 + Math.sin(wx / (double) (p * 9)) * 1.5;
			int top = horizon - (int) Math.round(wave + 3) * p + hillsY;
			g.fill(x, top, x + p, height, color(style.hills, alpha));
			g.fill(x, top, x + p, top + p, color(style.hillsTop, alpha));
		}
	}

	/** Cuánto se desplaza una capa: {@code depth} píxeles de cielo como máximo, en la dirección contraria al ratón. */
	private static int layerShift(float parallax, int depth, int p) {
		return Math.round(-parallax * depth) * p;
	}

	private static void clouds(GuiGraphics g, int width, int horizon, int p, long time, float alpha, Style style,
			float parallaxX, float parallaxY) {
		int far = color(style.farCloud, alpha * 0.8F);
		int farShade = color(style.farCloudShade, alpha * 0.8F);
		int near = color(style.nearCloud, alpha);
		int nearShade = color(style.nearCloudShade, alpha);
		int farX = layerShift(parallaxX, 3, p);
		int nearX = layerShift(parallaxX, 6, p);
		int nearY = layerShift(parallaxY, 2, p);

		for (int i = 0; i < 4; i++) {
			int span = width + 40 * p;
			int x = (int) ((i * span / 4 + time / 90) % span) - 20 * p;
			int y = horizon / 4 + (i % 2) * p * 10;
			shape(g, CLOUD_SMALL, x / p * p + farX, y / p * p, p, far, farShade);
		}
		for (int i = 0; i < 3; i++) {
			int span = width + 60 * p;
			int x = (int) ((i * span / 3 + time / 45) % span) - 30 * p;
			int y = horizon / 2 + (i % 2) * p * 12;
			shape(g, CLOUD_BIG, x / p * p + nearX, y / p * p + nearY, p, near, nearShade);
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
