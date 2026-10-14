package com.freedomclient.ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Los dibujos de los temas Pink Ghost y Neon Alley, copiados del diseño (scripts/textures/ghost/ghost.html): los
 * fantasmitas y destellos del cielo rosa, el callejón de noche (ciudad, edificios con ventanas, letrero de neón, cables
 * con bombillas, farola, charcos y lluvia) y los adornos de la ventana del menú. Todo con rectángulos.
 * <p>
 * Se dibuja en medias unidades (el diseño usa medios píxeles): quien llama escala la pose a la mitad (por {@code u})
 * y aquí todas las coordenadas se doblan.
 */
public final class ThemeArt {
	/** Pantalla del diseño: 480x270 de interfaz; el cielo del juego se escala a esto con {@code u = alto / 270}. */
	public static final double W = 480, H = 270;

	private static final String[] CLOUD_BIG = {"......####..........", "....########...###..", "..##############.##.", ".##################.", "####################", ".##################."};
	private static final String[] SPARK = {"..#..", "..#..", "##.##", "..#..", "..#.."};
	private static final String[] Z = {"###", "..#", ".#.", "#..", "###"};
	private static final String[] BOO = {"##..###..###", "#.#.#.#..#.#", "##..#.#..#.#", "#.#.#.#..#.#", "##..###..###"};
	/** Edificios del callejón (de FreedomPlayer, Fondo PC: 1280x260) y sus ventanas: x, y, ancho, alto, color, opacidad. */
	private static final int[][] BUILDINGS = {{0, 80, 120, 180, 0x1A1328}, {110, 130, 90, 130, 0x160F22}, {196, 50, 130, 210, 0x1C1430},
			{320, 110, 100, 150, 0x150F21}, {414, 70, 120, 190, 0x1A1328}, {530, 140, 110, 120, 0x160F22}, {636, 40, 140, 220, 0x1C1430},
			{770, 120, 96, 140, 0x150F21}, {860, 80, 124, 180, 0x1A1328}, {980, 130, 110, 130, 0x160F22}, {1086, 60, 120, 200, 0x1C1430},
			{1200, 110, 80, 150, 0x150F21}};
	private static final double[][] WINDOWS = {{20, 104, 0xFF3FA4, .5}, {44, 104, 0x5B57D9, .5}, {216, 74, 0xFF3FA4, .55}, {240, 102, 0x2F9C82, .45},
			{290, 74, 0xF4A6CB, .4}, {436, 96, 0x5B57D9, .55}, {476, 124, 0xFF3FA4, .35}, {660, 64, 0xFF3FA4, .6}, {684, 64, 0xFF3FA4, .25},
			{720, 92, 0x2F9C82, .5}, {880, 104, 0xF4A6CB, .45}, {930, 132, 0x5B57D9, .5}, {1106, 84, 0xFF3FA4, .5}, {1150, 112, 0x2F9C82, .45},
			{70, 150, 0xF4A6CB, .35}, {560, 170, 0x5B57D9, .4}, {1010, 160, 0xFF3FA4, .4}, {1230, 140, 0xF4A6CB, .45}};
	private static final int[] NEON = {0xFF3FA4, 0x5B57D9, 0x8FE3CB, 0xF4A6CB};
	private static final int[] NEON_WINDOWS = {0xFF3FA4, 0x5B57D9, 0x2F9C82, 0xF4A6CB};
	private static final int[] SKY_FACES = {GhostArt.CONTENTO, GhostArt.UWU, GhostArt.FELIZ, GhostArt.OWO, GhostArt.GUINO};

	private ThemeArt() {
	}

	/** Número pseudoaleatorio fijo (0..1), el mismo que el diseño. */
	public static double hash(double n) {
		double v = Math.sin(n * 127.1 + 311.7) * 43758.5453;
		return v - Math.floor(v);
	}

	/** Rectángulo en unidades del diseño (con medias unidades), en la pose escalada a la mitad. */
	static void fill(GuiGraphics g, double x1, double y1, double x2, double y2, int rgb, double alpha) {
		if (x2 <= x1 || y2 <= y1 || alpha <= 0) return;
		int a = (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255);
		g.fill((int) Math.round(x1 * 2), (int) Math.round(y1 * 2), (int) Math.round(x2 * 2), (int) Math.round(y2 * 2), a << 24 | (rgb & 0xFFFFFF));
	}

	static void shape(GuiGraphics g, String[] rows, double x, double y, double p, int color, int shade, double alpha) {
		for (int r = 0; r < rows.length; r++) {
			for (int c = 0; c < rows[r].length(); c++) {
				if (rows[r].charAt(c) == '#') fill(g, x + c * p, y + r * p, x + (c + 1) * p, y + (r + 1) * p, r == rows.length - 1 ? shade : color, alpha);
			}
		}
	}

	static void disc(GuiGraphics g, double cx, double cy, double r, double p, int rgb, double alpha) {
		for (double y = -r; y <= r; y += p) {
			double half = Math.floor(Math.sqrt(Math.max(0, r * r - y * y)) / p) * p;
			fill(g, cx - half, cy + y, cx + half, cy + y + p, rgb, alpha);
		}
	}

	static void spark(GuiGraphics g, double x, double y, int rgb, double alpha) {
		shape(g, SPARK, x - 2, y - 2, 1, rgb, rgb, alpha);
	}

	static void ghost(GuiGraphics g, int face, boolean white, double x, double y, double scale, double alpha, int outline) {
		GhostArt.draw(g, face, white, (float) (x * 2), (float) (y * 2), (float) (scale * 2), (float) alpha, outline);
	}

	// ------------------------------------------------------------------------------------------------
	// Cielo (PixelSky): en unidades del diseño, con la pose ya escalada a u / 2.
	// ------------------------------------------------------------------------------------------------

	/** Fantasmitas que pasean despacio entre las nubes (rosas y blancos), meciéndose; cambian de cara cada 5 s. */
	public static void skyGhosts(GuiGraphics g, double w, double horizon, double t) {
		for (int i = 0; i < 4; i++) {
			double span = w + 60, x = (i * span / 4 + t * (6 + 2 * (i % 2))) % span - 30;
			double y = horizon * (0.2 + 0.17 * i) + Math.sin(t * 1.6 + i * 2) * 3;
			int face = SKY_FACES[(int) ((i + Math.floor(t / 5)) % SKY_FACES.length)];
			ghost(g, face, i % 2 == 1, Math.round(x), Math.round(y), i == 1 ? 1 : 0.75, 1, i % 2 == 1 ? 0xA48FB3 : 0xC2326F);
		}
	}

	/** Destellos del cielo rosa que titilan. */
	public static void skySparkles(GuiGraphics g, double w, double horizon, double t) {
		for (int i = 0; i < 14; i++) {
			double tw = 0.5 + 0.5 * Math.sin(t * (1.3 + hash(i + 9) * 2) + i * 2);
			if (tw > 0.35) spark(g, Math.floor(hash(i) * w), Math.floor(8 + hash(i + 40) * horizon * 0.7), i % 3 != 0 ? 0xFFFFFF : 0xFF8CC6, tw);
		}
	}

	/** Halo rosa de la ciudad arriba a la derecha (con discos de la talla del píxel del cielo {@code p}). */
	public static void alleyHalo(GuiGraphics g, double w, double p) {
		for (int k = 6; k >= 1; k--) disc(g, w - 70, 10, p * 9 * k, p, 0xFF3FA4, 0.05);
	}

	/** El callejón (ver la clase). */
	public static void alley(GuiGraphics g, double w, double h, double horizon, double t) {
		// Ciudad lejana, recortada contra el halo.
		int i = 0;
		for (double x = 0; x < w; i++) {
			double bw = 18 + Math.floor(hash(i + 300) * 30), bh = 20 + Math.floor(hash(i + 400) * 44);
			fill(g, x, horizon - bh, x + bw, horizon + 4, 0x24173A, 1);
			x += bw;
		}
		// Edificios cercanos (1280 -> ancho de pantalla, 260 -> 110 de alto).
		double k = w / 1280, top = h - 110 - 18, sy = 110.0 / 260;
		for (int[] b : BUILDINGS) fill(g, Math.round(b[0] * k), Math.round(top + b[1] * sy), Math.round((b[0] + b[2]) * k) + 1, h, b[4], 1);
		for (int n = 0; n < WINDOWS.length; n++) {
			double[] wd = WINDOWS[n];
			boolean on = hash(n * 3 + Math.floor(t / (2 + hash(n) * 4))) > 0.15;
			if (on) fill(g, Math.round(wd[0] * k), Math.round(top + wd[1] * sy), Math.round(wd[0] * k) + 3, Math.round(top + wd[1] * sy) + 4, (int) wd[2], wd[3] + 0.25);
		}
		// Letrero de neón con un fantasma (el contorno del sprite) y "BOO".
		boolean on = signOn(t);
		double sx = 12, syy = Math.round(top + 80 * sy) - 26;
		fill(g, sx - 3, syy - 3, sx + 19, syy + 18, 0x0D0915, 0.9);
		GhostArt.outline(g, GhostArt.CONTENTO, (float) (sx * 2), (float) (syy * 2), 1.0F, 0xFF3FA4, 0xFFF6FA, on ? 1.0F : 0.18F);
		if (on) for (int r = 1; r <= 3; r++) fill(g, sx - 3 - r, syy - 3 - r, sx + 19 + r, syy + 18 + r, 0xFF3FA4, 0.05);
		shape(g, BOO, sx + 8 - 6 * 0.5, syy + 14, 0.5, 0x8FE3CB, 0x8FE3CB, on ? 0.9 : 0.25);
		// Cables entre edificios con bombillas que titilan.
		for (int c = 0; c < 2; c++) {
			double x1 = c == 1 ? 196 * k : 0, x2 = c == 1 ? w : 230 * k + 40, y = top + (c == 1 ? 30 : 46);
			for (double x = x1; x < x2; x += 2) {
				double u = (x - x1) / (x2 - x1), sag = Math.sin(u * Math.PI) * 10;
				fill(g, x, y + sag, x + 2, y + sag + 1, 0x0B0812, 1);
			}
			for (int b = 1; b < 8; b++) {
				double u = b / 8.0, x = x1 + (x2 - x1) * u, sag = Math.sin(u * Math.PI) * 10, tw = 0.55 + 0.45 * Math.sin(t * 2.3 + b * 1.7 + c);
				fill(g, x - 1, y + sag + 1, x + 1, y + sag + 3, NEON[(b + c) % 4], tw);
			}
		}
		// Fantasma durmiendo en un tejado, medio transparente, con su "z" subiendo.
		double gx = Math.round(414 * k) + 12, gy = Math.round(top + 70 * sy) - 13 + Math.sin(t * 0.8);
		ghost(g, GhostArt.DORMIDO, true, gx, gy, 0.5, 0.45, 0x1A1420);
		double zu = (t % 3) / 3;
		shape(g, Z, gx + 15 + zu * 4, gy - 4 - zu * 8, 0.75, 0xF4A6CB, 0xF4A6CB, 1 - zu);
		// Farola con su cono de luz y el parpadeo de FreedomPlayer.
		double glow = lampGlow(t), lx = w - 120, ly = 70;
		for (double y = ly + 4; y < h - 8; y += 2) {
			double u = (y - ly) / (h - ly), half = 3 + u * 34;
			fill(g, lx - half, y, lx + half, y + 2, 0xFF8CC6, 0.18 * (1 - u) * glow);
		}
		fill(g, lx - 1, ly + 2, lx + 1, h, 0x1B1428, 1);
		disc(g, lx, ly, 10, 1, 0xFF3FA4, 0.12 * glow);
		disc(g, lx, ly, 5, 1, 0xFFB4DC, 0.5 * glow);
		disc(g, lx, ly, 3, 1, 0xFFE3F1, 0.6 + 0.4 * glow);
		// Calle mojada y charcos que reflejan el neón.
		fill(g, 0, h - 10, w, h, 0x0D0915, 1);
		for (int n = 0; n < 6; n++) {
			double cx = 40 + n * 80 + hash(n) * 30, pw = 16 + hash(n + 7) * 20;
			for (int r = 0; r < 3; r++) {
				double wob = Math.sin(t * 2 + n + r) * 1.5;
				fill(g, cx - pw / 2 + wob, h - 8 + r * 2, cx + pw / 2 + wob, h - 7 + r * 2, r == 1 ? 0x5B57D9 : 0xFF3FA4, 0.35 - r * 0.08);
			}
		}
		rain(g, 0, 0, w, h - 10, t, 60, 0.35);
	}

	/** Lluvia fina: rayitas en diagonal que caen (posición fija por gota, sin estado). */
	public static void rain(GuiGraphics g, double x, double y, double w, double h, double t, int count, double alpha) {
		for (int i = 0; i < count; i++) {
			double speed = 160 + hash(i + 50) * 80, u = (t * speed / h + hash(i + 90)) % 1;
			int len = 4 + (int) Math.floor(hash(i + 70) * 4);
			double rx = x + (hash(i) * (w + 30) - u * 30), ry = y + u * h;
			for (int k = 0; k < len; k++) fill(g, rx - k * 0.5, ry - k, rx - k * 0.5 + 1, ry - k + 1, 0xC9B8E8, alpha * (1 - k / (double) len));
		}
	}

	/** Parpadeo de la farola de FreedomPlayer (7 s): normal, un apagón corto a los 3,3 s y un bajón a los 5,8 s. */
	public static double lampGlow(double t) {
		double[][] k = {{0, 1}, {3.22, 1}, {3.36, 0.55}, {3.5, 1}, {5.81, 0.85}, {7, 1}};
		double c = t % 7;
		for (int i = 1; i < k.length; i++) {
			if (c <= k[i][0]) {
				double u = (c - k[i - 1][0]) / (k[i][0] - k[i - 1][0]);
				return k[i - 1][1] + (k[i][1] - k[i - 1][1]) * u;
			}
		}
		return 1;
	}

	/** El letrero de neón: casi siempre encendido; cada 9 s tiembla y se apaga dos veces. */
	public static boolean signOn(double t) {
		double c = t % 9;
		return !(c > 6.1 && c < 6.25 || c > 6.4 && c < 6.5 || c > 6.6 && c < 6.9);
	}

	// ------------------------------------------------------------------------------------------------
	// Ventana del menú (ThemeDecor), en unidades de interfaz con la pose escalada a la mitad.
	// ------------------------------------------------------------------------------------------------

	public static void pinkBackground(GuiGraphics g, double x, double y, double w, double h, double t) {
		gradient(g, x + 1, y + h - 40, x + w - 1, y + h - 1, 0xFF8CC6, 0, 0xFF8CC6, 0.22);
		// Nubes bajas y dos fantasmas que asoman por abajo meciéndose.
		for (int i = 0; i < 4; i++) shape(g, CLOUD_BIG, x + 20 + i * 100 + Math.sin(t * 0.3 + i) * 6, y + h - 13, 1.5, 0xFFFFFF, 0xFFDCEC, 0.85);
		for (int i = 0; i < 2; i++) {
			double px = x + 70 + i * 230, peek = 8 + Math.sin(t * 1.2 + i * 2.5) * 3;
			ghost(g, i == 1 ? GhostArt.UWU : GhostArt.OWO, i == 1, px, y + h - 1 - peek, 0.5, 1, 0xE58DB8);
		}
	}

	public static void alleyBackground(GuiGraphics g, double x, double y, double w, double h, double t) {
		gradient(g, x + 1, y + h - 50, x + w - 1, y + h - 1, 0x5C2153, 0, 0x5C2153, 0.45);
		// Ciudad abajo con ventanitas que se encienden y apagan.
		int i = 0;
		for (double bx = x + 1; bx < x + w - 1; i++) {
			double bw = 16 + Math.floor(hash(i + 11) * 22), bh = 10 + Math.floor(hash(i + 31) * 26);
			fill(g, bx, y + h - 1 - bh, Math.min(x + w - 1, bx + bw), y + h - 1, new int[] {0x1A1328, 0x160F22, 0x1C1430}[i % 3], 1);
			for (int k = 0; k < 3; k++) {
				if (hash(i * 7 + k + Math.floor(t / 3)) > 0.45) fill(g, bx + 3 + k * 5, y + h - bh + 3, bx + 5 + k * 5, y + h - bh + 6, NEON_WINDOWS[(i + k) % 4], 0.6);
			}
			bx += bw;
		}
	}

	public static void pinkHeader(GuiGraphics g, double x, double y, double w, double h, double t) {
		gradient(g, x + 1, y + 1, x + w - 1, y + h, 0xFFFFFF, 0, 0xFF8CC6, 0.18);
		for (int i = 0; i < 9; i++) {
			double tw = 0.5 + 0.5 * Math.sin(t * (1.1 + hash(i) * 1.6) + i);
			if (tw > 0.4) spark(g, x + 110 + Math.floor(hash(i + 3) * 110), y + 5 + Math.floor(hash(i + 5) * 18), i % 2 == 1 ? 0xFF8CC6 : 0xFFFFFF, tw);
		}
		// Cada 8 s un fantasmita cruza la cabecera dejando destellos.
		double c = t % 8;
		if (c < 3) {
			double u = c / 3, gx = x - 16 + (w + 32) * u, gy = y + 6 + Math.sin(u * Math.PI * 4) * 3;
			for (int k = 1; k < 6; k++) spark(g, gx - k * 9, gy + 7 + Math.sin((u * 3 - k * 0.1) * Math.PI * 4) * 3, 0xFF8CC6, 0.8 - k * 0.14);
			ghost(g, GhostArt.FELIZ, false, gx, gy, 0.5, 1, 0xE58DB8);
		}
	}

	public static void alleyHeader(GuiGraphics g, double x, double y, double w, double h, double t) {
		gradient(g, x + 1, y + 1, x + w - 1, y + h, 0x000000, 0, 0xFF3FA4, 0.12);
		// Cable con bombillas colgando de la cabecera.
		for (double px = x + 100; px < x + w - 2; px += 2) {
			double u = (px - x - 100) / (w - 102), sag = Math.sin(u * Math.PI * 2) * 3 + 3;
			fill(g, px, y + 2 + sag, px + 2, y + 3 + sag, 0x05030A, 1);
		}
		for (int i = 0; i < 12; i++) {
			double u = (i + 0.5) / 12, px = x + 100 + (w - 102) * u, sag = Math.sin(u * Math.PI * 2) * 3 + 3, tw = 0.5 + 0.5 * Math.sin(t * 2.6 + i * 1.9);
			fill(g, px - 1, y + 3 + sag, px + 1, y + 5 + sag, NEON[i % 4], 0.4 + 0.6 * tw);
		}
	}

	public static void pinkForeground(GuiGraphics g, double x, double y, double w, double h, double head, double t) {
		for (int i = 0; i < 8; i++) {
			double u = (t * 0.08 + hash(i)) % 1, px = x + 10 + hash(i + 20) * (w - 20) + Math.sin(t + i) * 4, py = y + h - 10 - u * (h - head - 20);
			spark(g, px, py, i % 2 == 1 ? 0xFF8CC6 : 0xFFFFFF, (1 - u) * 0.7);
		}
	}

	public static void alleyForeground(GuiGraphics g, double x, double y, double w, double h, double head, double t) {
		rain(g, x + 1, y + head + 1, w - 2, h - head - 2, t, 26, 0.18);
	}

	/** Degradado vertical (como fillGradient del diseño), en medias unidades. */
	static void gradient(GuiGraphics g, double x1, double y1, double x2, double y2, int top, double topA, int bottom, double bottomA) {
		int a = (int) Math.round(topA * 255), b = (int) Math.round(bottomA * 255);
		g.fillGradient((int) Math.round(x1 * 2), (int) Math.round(y1 * 2), (int) Math.round(x2 * 2), (int) Math.round(y2 * 2), a << 24 | top, b << 24 | bottom);
	}
}
