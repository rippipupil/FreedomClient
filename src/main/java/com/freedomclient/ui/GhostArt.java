package com.freedomclient.ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * El fantasmita de FreedomPlayer en 2D para el menú y los cielos de los temas Pink Ghost y Neon Alley: sprites de 30x29
 * a medio píxel (con contorno), uno por cara, sacados del mismo dibujo que el modelo 3D (scripts/textures/ghost).
 * Rosa para lo bueno; blanco para sorpresa, dormido y triste, como en FreedomPlayer.
 */
public final class GhostArt {
	public static final String[] FACES = {"feliz", "contento", "owo", "uwu", "guino", "sorpresa", "dormido", "triste"};
	public static final int FELIZ = 0, CONTENTO = 1, OWO = 2, UWU = 3, GUINO = 4, SORPRESA = 5, DORMIDO = 6, TRISTE = 7;
	public static final int WIDTH = 30, HEIGHT = 29;

	/** Colores por papel (cuerpo, faldón, brillo, tinta, mofletes, lengua): rosa y blanco. */
	private static final int[] PINK = {0xF4A6CB, 0xE58DB8, 0xFFE6F2, 0xC2326F, 0xFF6FAE, 0xFF6FAE};
	private static final int[] WHITE = {0xF3EEF4, 0xDCD2E4, 0xFFFFFF, 0x1A1420, 0xF4A6CB, 0xFF6FAE};

	private GhostArt() {
	}

	/** Si la cara va en blanco cuando el color es automático (sorpresa, dormido y triste). */
	public static boolean whiteFace(int face) {
		return face == SORPRESA || face == DORMIDO || face == TRISTE;
	}

	public static int face(String name) {
		for (int i = 0; i < FACES.length; i++) if (FACES[i].equals(name)) return i;
		return CONTENTO;
	}

	/**
	 * Dibuja el fantasma con la esquina de arriba a la izquierda en (x, y) de interfaz; {@code scale} es lo que mide un
	 * píxel del sprite (0,5: 15x14,5 de interfaz). Los tramos seguidos del mismo color van en un solo rectángulo.
	 */
	public static void draw(GuiGraphics g, int face, boolean white, float x, float y, float scale, float alpha, int outline) {
		int[] pal = white ? WHITE : PINK;
		String[] rows = SPRITES[face];
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		for (int r = 0; r < rows.length; r++) {
			String row = rows[r];
			int c = 0;
			while (c < row.length()) {
				char ch = row.charAt(c);
				int e = c + 1;
				while (e < row.length() && row.charAt(e) == ch) e++;
				if (ch != '.') {
					int rgb = switch (ch) {
						case 'o' -> outline;
						case 's' -> pal[1];
						case 'w' -> pal[2];
						case 'k' -> pal[3];
						case 'c' -> pal[4];
						case 't' -> pal[5];
						default -> pal[0];
					};
					g.fill(c, r, e, r + 1, color(rgb, alpha));
				}
				c = e;
			}
		}
		g.pose().popMatrix();
	}

	/** Solo el contorno y la tinta de la cara, en un color (el letrero de neón del callejón). */
	public static void outline(GuiGraphics g, int face, float x, float y, float scale, int lineRgb, int inkRgb, float alpha) {
		String[] rows = SPRITES[face];
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		for (int r = 0; r < rows.length; r++) {
			String row = rows[r];
			for (int c = 0; c < row.length(); c++) {
				char ch = row.charAt(c);
				if (ch == 'o' || ch == 'k') g.fill(c, r, c + 1, r + 1, color(ch == 'k' ? inkRgb : lineRgb, alpha));
			}
		}
		g.pose().popMatrix();
	}

	private static int color(int rgb, float alpha) {
		int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
		return a << 24 | (rgb & 0xFFFFFF);
	}

	// --- generado: sprites (scripts/textures/ghost/export_game.py) ---
	private static final String[][] SPRITES = {
			{ // feliz
					"............oooooo............",
					"..........oobbbbbboo..........",
					"........oobbbbbbbbbboo........",
					".......obbbbbbbbbbbbbbo.......",
					"......obwwwwwbbbbbbbbbbo......",
					"......owwwwwwwbbbbbbbbbo......",
					".....obwwwwwwwbbbbbbbbbbo.....",
					".....obbwwwwwbbbbbbbbbbbo.....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbkkbbbbbbbbbbkkbbbo....",
					"....obbbbkkkbbbbbbkkkbbbbo....",
					"....obbbbbkkkbbbbkkkbbbbbo....",
					"....obbbbkkkbbbbbbkkkbbbbo....",
					"....obbbkkbbbbbbbbbbkkbbbo....",
					"...oobbbbbbbkkbbkkbbbbbbboo...",
					"..obbbbbbbbbbkkkkbbbbbbbbbbo..",
					".obbbbbbbbbbbbbbbbbbbbbbbbbbo.",
					"obbbobbbbbbbbbbbbbbbbbbbbobbbo",
					".oooobbbbbbbbbbbbbbbbbbbboooo.",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....osssssssssssssssssssso....",
					"....osssssssssssssssssssso....",
					".....osssossssssssssossso.....",
					"......ooo.osssoossso.ooo......",
					"...........ooo..ooo...........",
					"..............................",
					"..............................",
			},
			{ // contento
					"............oooooo............",
					"..........oobbbbbboo..........",
					"........oobbbbbbbbbboo........",
					".......obbbbbbbbbbbbbbo.......",
					"......obwwwwwbbbbbbbbbbo......",
					"......owwwwwwwbbbbbbbbbo......",
					".....obwwwwwwwbbbbbbbbbbo.....",
					".....obbwwwwwbbbbbbbbbbbo.....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbkkkbbbbbbkkkbbbbo....",
					"....obbbkkkkkbbbbkkkkkbbbo....",
					"....obbbkkbkkbbbbkkbkkbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"...oobbbbbbbkkbbkkbbbbbbboo...",
					"..obbbbbbbbbbkkkkbbbbbbbbbbo..",
					".obbbbbbbbbbbbbbbbbbbbbbbbbbo.",
					"obbbobbbbbbbbbbbbbbbbbbbbobbbo",
					".oooobbbbbbbbbbbbbbbbbbbboooo.",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....osssssssssssssssssssso....",
					"....osssssssssssssssssssso....",
					".....osssossssssssssossso.....",
					"......ooo.osssoossso.ooo......",
					"...........ooo..ooo...........",
					"..............................",
					"..............................",
			},
			{ // owo
					"............oooooo............",
					"..........oobbbbbboo..........",
					"........oobbbbbbbbbboo........",
					".......obbbbbbbbbbbbbbo.......",
					"......obwwwwwbbbbbbbbbbo......",
					"......owwwwwwwbbbbbbbbbo......",
					".....obwwwwwwwbbbbbbbbbbo.....",
					".....obbwwwwwbbbbbbbbbbbo.....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbkkkbbbbbbkkkbbbbo....",
					"....obbbkbbbkbbbbkbbbkbbbo....",
					"....obbbkbbbkbbbbkbbbkbbbo....",
					"....obbbkbbbkbbbbkbbbkbbbo....",
					"....obbbbkkkbbbbbbkkkbbbbo....",
					"...oobcccbbbkbkkbkbbbcccboo...",
					"..obbbbbbbbbkkkkkkbbbbbbbbbo..",
					".obbbbbbbbbbbbbbbbbbbbbbbbbbo.",
					"obbbobbbbbbbbbbbbbbbbbbbbobbbo",
					".oooobbbbbbbbbbbbbbbbbbbboooo.",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....osssssssssssssssssssso....",
					"....osssssssssssssssssssso....",
					".....osssossssssssssossso.....",
					"......ooo.osssoossso.ooo......",
					"...........ooo..ooo...........",
					"..............................",
					"..............................",
			},
			{ // uwu
					"............oooooo............",
					"..........oobbbbbboo..........",
					"........oobbbbbbbbbboo........",
					".......obbbbbbbbbbbbbbo.......",
					"......obwwwwwbbbbbbbbbbo......",
					"......owwwwwwwbbbbbbbbbo......",
					".....obwwwwwwwbbbbbbbbbbo.....",
					".....obbwwwwwbbbbbbbbbbbo.....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbkkbkkbbbbkkbkkbbbo....",
					"....obbbbkkkbbbbbbkkkbbbbo....",
					"....obbbbbkbbbbbbbbkbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"...oobcccbbbkbkkbkbbbcccboo...",
					"..obbbbbbbbbkkkkkkbbbbbbbbbo..",
					".obbbbbbbbbbbbbbbbbbbbbbbbbbo.",
					"obbbobbbbbbbbbbbbbbbbbbbbobbbo",
					".oooobbbbbbbbbbbbbbbbbbbboooo.",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....osssssssssssssssssssso....",
					"....osssssssssssssssssssso....",
					".....osssossssssssssossso.....",
					"......ooo.osssoossso.ooo......",
					"...........ooo..ooo...........",
					"..............................",
					"..............................",
			},
			{ // guino
					"............oooooo............",
					"..........oobbbbbboo..........",
					"........oobbbbbbbbbboo........",
					".......obbbbbbbbbbbbbbo.......",
					"......obwwwwwbbbbbbbbbbo......",
					"......owwwwwwwbbbbbbbbbo......",
					".....obwwwwwwwbbbbbbbbbbo.....",
					".....obbwwwwwbbbbbbbbbbbo.....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbkkkbbbbbbkkbbbbbo....",
					"....obbbkkkkkbbbbkkkkkbbbo....",
					"....obbbkkbkkbbbbbbkkbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"...oobcccbbbkkbbkkbbbcccboo...",
					"..obbbbbbbbbbkkkkbbbbbbbbbbo..",
					".obbbbbbbbbbbbbbbbbbbbbbbbbbo.",
					"obbbobbbbbbbbbbbbbbbbbbbbobbbo",
					".oooobbbbbbbbbbbbbbbbbbbboooo.",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....osssssssssssssssssssso....",
					"....osssssssssssssssssssso....",
					".....osssossssssssssossso.....",
					"......ooo.osssoossso.ooo......",
					"...........ooo..ooo...........",
					"..............................",
					"..............................",
			},
			{ // sorpresa
					"............oooooo............",
					"..........oobbbbbboo..........",
					"........oobbbbbbbbbboo........",
					".......obbbbbbbbbbbbbbo.......",
					"......obwwwwwbbbbbbbbbbo......",
					"......owwwwwwwbbbbbbbbbo......",
					".....obwwwwwwwbbbbbbbbbbo.....",
					".....obbwwwwwbbbbbbbbbbbo.....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbkkkbbbbbbkkkbbbbo....",
					"....obbbbkkkbbbbbbkkkbbbbo....",
					"....obbbbbkkbbbbbbkkbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"...oobbbbbbbbbkkbbbbbbbbboo...",
					"..obbbbbbbbbbkkkkbbbbbbbbbbo..",
					".obbbbbbbbbbbkkkkbbbbbbbbbbbo.",
					"obbbobbbbbbbbbkkbbbbbbbbbobbbo",
					".oooobbbbbbbbbbbbbbbbbbbboooo.",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....osssssssssssssssssssso....",
					"....osssssssssssssssssssso....",
					".....osssossssssssssossso.....",
					"......ooo.osssoossso.ooo......",
					"...........ooo..ooo...........",
					"..............................",
					"..............................",
			},
			{ // dormido
					"............oooooo............",
					"..........oobbbbbboo..........",
					"........oobbbbbbbbbboo........",
					".......obbbbbbbbbbbbbbo.......",
					"......obwwwwwbbbbbbbbbbo......",
					"......owwwwwwwbbbbbbbbbo......",
					".....obwwwwwwwbbbbbbbbbbo.....",
					".....obbwwwwwbbbbbbbbbbbo.....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbkkkkkbbbbkkkkkbbbo....",
					"....obbbbkkkbbbbbbkkkbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"...oobbbbbbbbbbbbbbbbbbbboo...",
					"..obbbbbbbbbbbkkbbbbbbbbbbbo..",
					".obbbbbbbbbbbbbbbbbbbbbbbbbbo.",
					"obbbobbbbbbbbbbbbbbbbbbbbobbbo",
					".oooobbbbbbbbbbbbbbbbbbbboooo.",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....osssssssssssssssssssso....",
					"....osssssssssssssssssssso....",
					".....osssossssssssssossso.....",
					"......ooo.osssoossso.ooo......",
					"...........ooo..ooo...........",
					"..............................",
					"..............................",
			},
			{ // triste
					"............oooooo............",
					"..........oobbbbbboo..........",
					"........oobbbbbbbbbboo........",
					".......obbbbbbbbbbbbbbo.......",
					"......obwwwwwbbbbbbbbbbo......",
					"......owwwwwwwbbbbbbbbbo......",
					".....obwwwwwwwbbbbbbbbbbo.....",
					".....obbwwwwwbbbbbbbbbbbo.....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbkkkbbbbkkkbbbbbo....",
					"....obbbbkkkkbbbbkkkkbbbbo....",
					"....obbbbkkkbbbbbbkkkbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"...oobbbbbbbbbbbbbbbbbbbboo...",
					"..obbbbbbbbbbbkkbbbbbbbbbbbo..",
					".obbbbbbbbbbbbkkbbbbbbbbbbbbo.",
					"obbbobbbbbbbbbkkbbbbbbbbbobbbo",
					".oooobbbbbbbbbbbbbbbbbbbboooo.",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....obbbbbbbbbbbbbbbbbbbbo....",
					"....osssssssssssssssssssso....",
					"....osssssssssssssssssssso....",
					".....osssossssssssssossso.....",
					"......ooo.osssoossso.ooo......",
					"...........ooo..ooo...........",
					"..............................",
					"..............................",
			},
	};
	// --- fin de lo generado ---
}
