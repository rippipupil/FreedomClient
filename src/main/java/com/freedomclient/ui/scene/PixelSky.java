package com.freedomclient.ui.scene;

import com.freedomclient.module.visual.CustomScreensModule;
import com.freedomclient.ui.theme.ThemeManager;
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
		DAY(DAY_BANDS, 0, 0xFFFFFF, 0xF4F8FC, 0xD2E2EE, 0xFFFFFF, 0xD6E6F2, 0x3E7A34, 0x5FA048),
		/** Tema Neon: noche azul con nubes de tormenta y rayos suaves que mezclan amarillo, verde lima, cian y azul. */
		STORM(STORM_BANDS, 60, 0xBFEFFF, 0x1C2458, 0x121840, 0x26307A, 0x1A2160, 0x060920, 0x2F6BFF),
		/** Tema morado: anochecer violeta que acaba en rosa en el horizonte, con luna y nubes lavanda. */
		PURPLE(PURPLE_BANDS, 90, 0xF3E6FF, 0x7A4A9E, 0x5E3680, 0x9A63B8, 0x7A4A9E, 0x1E0B33, 0x4A2270),
		/** Tema menta: mañana verde agua con nubes blancas y colinas verdes. */
		MINT(MINT_BANDS, 0, 0xFFFFFF, 0xF2FFFB, 0xCDEFE6, 0xFFFFFF, 0xD4F2EA, 0x2F7A5E, 0x5FBF8C);

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
	private static final int[] STORM_BANDS = {
			0x04061A, 0x060A24, 0x080E2E, 0x0B1338, 0x0E1842, 0x121D4D, 0x162358, 0x1A2963,
			0x1F306F, 0x25387B, 0x2B4086, 0x31488F, 0x2F5AA0, 0x2D6FB0, 0x2C86BF,
	};
	private static final int[] PURPLE_BANDS = {
			0x120726, 0x1A0A33, 0x230D40, 0x2D114D, 0x38155A, 0x441A66, 0x511F72, 0x5F257D,
			0x6E2C87, 0x7E3490, 0x8F3E98, 0xA04A9E, 0xB158A3, 0xC168A6, 0xD07BA8,
	};
	private static final int[] MINT_BANDS = {
			0x2E8C8A, 0x359692, 0x3DA09A, 0x46AAA2, 0x50B4AA, 0x5BBDB2, 0x67C6BA, 0x74CEC2,
			0x82D6CA, 0x91DDD1, 0xA1E4D9, 0xB2EAE0, 0xC4F0E8,
	};
	/** Solo para las pruebas: fija el momento del primer rayo (ms desde que cae) para que salga en las capturas. */
	public static long forcedLightning = -1;
	/** Colores de los rayos de arriba abajo: se mezclan como la energía de Neon. */
	private static final int[] BOLT = {0xFFE14A, 0xE6F055, 0xC6F25A, 0x8EF07A, 0x5EF0C8, 0x3FD7FF, 0x3FA8FF, 0x3A7BFF, 0x6B5BFF};
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

	/**
	 * Emblema de FreedomClient sin letras (tools/fc_logo.py EMBLEM): tres barras, una diagonal y dos nodos.
	 * '#' = trazo, 'o' = borde de un nodo, 'O' = centro de un nodo.
	 */
	private static final String[] FC = {
			"..oo..................",
			".oOOo#################",
			".oOOo################.",
			"..oo...........####...",
			"..............####....",
			"....#########.####....",
			"...#########.####.....",
			"............####......",
			"...........####.......",
			"..........####....oo..",
			"..#############..oOOo.",
			"..############...oOOo.",
			"..................oo..",
	};
	/** Degradado de los trazos por fila, nodos (borde, centro), halo (claro, oscuro) y contorno, en Angel Devil y en Neon. */
	private static final int[] FC_ROWS = {0xFFFFFF, 0xFAF6EC, 0xF5F1E8, 0xF7EAD0, 0xF7E2BE, 0xF6D696, 0xF5CD78, 0xF2C45A, 0xECB646, 0xE8A93A, 0xDE783C, 0xD7263D, 0xD7263D};
	private static final int[] FC_ROWS_NEON = {0xEAFAFF, 0xC8F4FF, 0xA0EAFF, 0x6EDCFF, 0x3FD7FF, 0x3CBEFF, 0x3AA0FF, 0x3482FF, 0x2E64FF, 0x2A50FF, 0x4646F0, 0x6B5BFF, 0x6B5BFF};
	private static final int[] FC_EXTRA = {0xD7263D, 0xFF7878, 0xF2C94C, 0xC98F1E, 0x1A0508};
	private static final int[] FC_EXTRA_NEON = {0xFFD84A, 0xFFF5AA, 0xA8F05A, 0x3FD7FF, 0x06081E};
	private static final int[] FC_ROWS_PURPLE = {0xFFFFFF, 0xF6EEFF, 0xEEDFFF, 0xE3CCFF, 0xD7B8FF, 0xC9A3FF, 0xBA8CFF, 0xAB76FF, 0x9B5CFF, 0x8C4BF0, 0xA040D8, 0xC03AB8, 0xC03AB8};
	private static final int[] FC_EXTRA_PURPLE = {0xC03AB8, 0xFF9AE8, 0xE6B8FF, 0x9B5CFF, 0x120726};
	private static final int[] FC_ROWS_MINT = {0xFFFFFF, 0xF0FFFA, 0xE0FFF4, 0xCCFBEA, 0xB8F5E0, 0xA2EED5, 0x8AE6C9, 0x72DDBC, 0x5AD3AF, 0x44C8A2, 0x33B894, 0x2A9E86, 0x2A9E86};
	private static final int[] FC_EXTRA_MINT = {0x2A9E86, 0xB8FFE4, 0xE8FFF6, 0x3FC9A0, 0x08201C};

	/** Degradado de los trazos del logo según el tema. */
	private static int[] logoRows() {
		if (ThemeManager.isNeon()) return FC_ROWS_NEON;
		if (ThemeManager.isPurple()) return FC_ROWS_PURPLE;
		if (ThemeManager.isMint()) return FC_ROWS_MINT;
		return FC_ROWS;
	}

	/** Nodos, halo y contorno del logo según el tema. */
	private static int[] logoExtra() {
		if (ThemeManager.isNeon()) return FC_EXTRA_NEON;
		if (ThemeManager.isPurple()) return FC_EXTRA_PURPLE;
		if (ThemeManager.isMint()) return FC_EXTRA_MINT;
		return FC_EXTRA;
	}

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
		int starArea = style == Style.NIGHT || style == Style.PURPLE ? horizon * 2 / 3 : horizon / 3;
		for (int i = 0; i < style.stars; i++) {
			int sx = (int) ((i * 7919L) % Math.max(1, width / p)) * p + starsX;
			int sy = (int) ((i * 104729L) % Math.max(1, starArea / p)) * p + starsY;
			boolean on = ((time / 400) + i * 13) % 7 != 0;
			if (on) g.fill(sx, sy, sx + p, sy + p, color(style.starColor, alpha * (i % 3 == 0 ? 0.9F : 0.5F)));
		}

		// Sol (atardecer y día) o luna (noche).
		int sunX = width * 2 / 3 + layerShift(parallaxX, 4, p);
		int shiftY = layerShift(parallaxY, 2, p);
		if (style == Style.STORM) {
			// Sin sol ni luna: la luz la ponen los rayos.
		} else if (style == Style.NIGHT || style == Style.PURPLE) {
			int moonR = p * 7;
			int moonY = horizon / 4 + shiftY;
			disc(g, sunX, moonY, moonR + p * 4, p, color(0xB8C4F0, alpha * 0.18F));
			disc(g, sunX, moonY, moonR, p, color(0xF2F0E6, alpha));
			disc(g, sunX - p * 2, moonY - p * 2, p * 2, p, color(0xD6D2C4, alpha));
			disc(g, sunX + p * 3, moonY + p * 2, p * 1, p, color(0xD6D2C4, alpha));
		} else {
			int sunR = p * 9;
			boolean high = style == Style.DAY || style == Style.MINT;
			int sunY = (high ? horizon / 4 : horizon - p * 4) + shiftY;
			disc(g, sunX, sunY, sunR + p * 3, p, color(high ? 0xFFF4B0 : 0xFFD27A, alpha * 0.35F));
			disc(g, sunX, sunY, sunR, p, color(high ? 0xFFEE88 : 0xFFE08A, alpha));
			disc(g, sunX, sunY, sunR - p * 3, p, color(0xFFF1C2, alpha));
		}

		// Nubes en dos capas que se mueven a distinta velocidad y con distinto paralaje.
		clouds(g, width, horizon, p, time, alpha, style, parallaxX, parallaxY);
		if (style == Style.STORM) {
			lightning(g, width, horizon, p, time, alpha, parallaxX);
		}

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

	/**
	 * Rayos de la tormenta del tema Neon: cada pocos segundos cae uno (a veces con una rama), aparece de golpe y se
	 * desvanece suave, con un destello muy leve en el cielo. Todo sale del tiempo, así que no guarda estado.
	 */
	private static void lightning(GuiGraphics g, int width, int horizon, int p, long time, float alpha, float parallaxX) {
		for (int lane = 0; lane < 2; lane++) {
			long period = 3400 + lane * 1500L;
			long t = time + lane * 1900L;
			long index = t / period;
			long phase = lane == 0 && forcedLightning >= 0 ? forcedLightning : t % period;
			long duration = 700;
			if (phase > duration) continue;
			float life = phase / (float) duration;
			float strength = life < 0.08F ? life / 0.08F : (1.0F - life) * (1.0F - life);
			java.util.Random random = new java.util.Random(index * 7919L + lane * 31L);
			g.fill(0, 0, width, horizon, color(0x3FD7FF, alpha * strength * 0.06F));
			int x = (int) (width * (0.12F + random.nextFloat() * 0.76F)) / p * p + layerShift(parallaxX, 5, p);
			int top = horizon / 5 + random.nextInt(Math.max(1, horizon / 8)) / p * p;
			int bottom = horizon - p * 2 - random.nextInt(Math.max(1, horizon / 3)) / p * p;
			bolt(g, x, top, bottom, p, random, alpha * strength, true);
		}
	}

	/** Un rayo en zigzag de (x, top) a la altura {@code bottom}, con el color degradado y un brillo a los lados. */
	private static void bolt(GuiGraphics g, int x, int top, int bottom, int p, java.util.Random random, float alpha, boolean branches) {
		int y = top;
		while (y < bottom) {
			int direction = random.nextBoolean() ? 1 : -1;
			int segment = 2 + random.nextInt(4);
			for (int k = 0; k < segment && y < bottom; k++) {
				float t = (y - top) / (float) Math.max(1, bottom - top);
				int rgb = BOLT[Math.min(BOLT.length - 1, (int) (t * BOLT.length))];
				g.fill(x - p, y, x, y + p, color(rgb, alpha * 0.25F));
				g.fill(x + p, y, x + p * 2, y + p, color(rgb, alpha * 0.25F));
				g.fill(x, y, x + p, y + p, color(rgb, alpha));
				y += p;
				if (k % 2 == 0) x += direction * p;
			}
			if (branches && random.nextFloat() < 0.18F) {
				int branchEnd = Math.min(bottom, y + (bottom - top) / 4);
				bolt(g, x + direction * p, y, branchEnd, p, random, alpha * 0.6F, false);
			}
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

	/** Cubre cada píxel del logo (trazos y nodos) con un color: para la sombra y el contorno. */
	private static void logoMask(GuiGraphics g, int x, int y, int p, int color) {
		for (int row = 0; row < FC.length; row++) {
			for (int column = 0; column < FC[row].length(); column++) {
				if (FC[row].charAt(column) != '.') {
					g.fill(x + column * p, y + row * p, x + (column + 1) * p, y + (row + 1) * p, color);
				}
			}
		}
	}

	/** El emblema de FreedomClient con contorno, sombra y degradado, centrado en (cx, cy). Con el tema Neon cambia de colores. */
	public static void logo(GuiGraphics g, int cx, int cy, int p, float alpha) {
		int[] rows = logoRows();
		int[] extra = logoExtra();
		int w = FC[0].length() * p;
		int h = FC.length * p;
		int x = cx - w / 2;
		int y = cy - h / 2;
		int outline = color(extra[4], alpha);
		// Sombra abajo a la derecha y contorno oscuro alrededor.
		logoMask(g, x + p * 2, y + p * 2, p, color(extra[4], alpha * 0.45F));
		logoMask(g, x - p, y, p, outline);
		logoMask(g, x + p, y, p, outline);
		logoMask(g, x, y - p, p, outline);
		logoMask(g, x, y + p, p, outline);
		for (int row = 0; row < FC.length; row++) {
			for (int column = 0; column < FC[row].length(); column++) {
				char c = FC[row].charAt(column);
				if (c == '.') continue;
				int rgb = c == 'o' ? extra[0] : c == 'O' ? extra[1] : rows[Math.min(row, rows.length - 1)];
				g.fill(x + column * p, y + row * p, x + (column + 1) * p, y + (row + 1) * p, color(rgb, alpha));
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

	/** Halo (anillo pixelado) que flota arriba y abajo: dorado, o cian y verde con el tema Neon. */
	public static void halo(GuiGraphics g, int cx, int cy, int p, float alpha) {
		halo(g, cx, cy, p, 9, alpha);
	}

	/** Halo con un radio horizontal de {@code radius} píxeles de logo. */
	public static void halo(GuiGraphics g, int cx, int cy, int p, int radius, float alpha) {
		int bob = (int) Math.round(Math.sin(System.currentTimeMillis() / 400.0) * 1.5) * p;
		int rx = p * radius;
		int ry = p * 2;
		int[] extra = logoExtra();
		int gold = color(extra[2], alpha);
		int dark = color(extra[3], alpha);
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
