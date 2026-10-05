package com.freedomclient.ui;

import com.freedomclient.ui.theme.ThemeManager;
import com.freedomclient.util.ModIcons;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Primitivas de dibujo con estilo pixel: esquinas recortadas, bordes de 1 px y relieve. */
public final class Draw {
	private Draw() {
	}

	/** Rectángulo con borde de 1 px y las esquinas recortadas (aspecto pixel art). */
	public static void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
		g.fill(x + 1, y, x + w - 1, y + 1, border);
		g.fill(x + 1, y + h - 1, x + w - 1, y + h, border);
		g.fill(x, y + 1, x + 1, y + h - 1, border);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
	}

	/** Panel con relieve: línea clara arriba y oscura abajo por dentro del borde. */
	public static void bevelPanel(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
		panel(g, x, y, w, h, fill, border);
		g.fill(x + 1, y + 1, x + w - 1, y + 2, ThemeManager.mix(fill, 0xFFFFFFFF, 0.14F));
		g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, ThemeManager.mix(fill, 0xFF000000, 0.3F));
	}

	/** Interruptor pixel de 20x10; {@code progress} va de 0 (apagado) a 1 (encendido) para animarlo. */
	public static void toggle(GuiGraphics g, int x, int y, float progress, boolean hovered) {
		int track = ThemeManager.mix(ThemeManager.shade(), ThemeManager.accent(), progress);
		int border = hovered ? ThemeManager.highlight() : ThemeManager.mix(ThemeManager.border(), 0xFF000000, 0.35F);
		panel(g, x, y, 20, 10, track, border);
		if (NeonStyle.on() && progress > 0.0F) {
			// Encendido: la pista pasa de lima a amarillo, como la energía cargada.
			NeonStyle.hLine(g, x + 1, x + 19, y + 1, 8, 0.12, 0.0, progress);
		}

		int knobX = x + 2 + Math.round(progress * 10);
		g.fill(knobX, y + 2, knobX + 6, y + 8, ThemeManager.text());
		g.fill(knobX, y + 7, knobX + 6, y + 8, ThemeManager.mix(ThemeManager.text(), 0xFF000000, 0.3F));
	}

	/** Barra de un deslizador con su relleno y el tirador. */
	public static void slider(GuiGraphics g, int x, int y, int w, double progress, boolean hovered) {
		int filled = (int) Math.round(progress * (w - 2));
		panel(g, x, y, w, 6, ThemeManager.shade(), ThemeManager.mix(ThemeManager.border(), 0xFF000000, 0.35F));
		if (NeonStyle.on()) {
			NeonStyle.hLine(g, x + 1, x + 1 + filled, y + 1, 4, 0.35, 0.35 - 0.35 * progress, 1.0F);
		} else {
			g.fill(x + 1, y + 1, x + 1 + filled, y + 5, ThemeManager.accent());
		}

		int knobX = x + filled - 1;
		panel(g, knobX, y - 2, 4, 10, hovered ? ThemeManager.highlight() : ThemeManager.text(), 0xFF000000);
	}

	/**
	 * Rectángulo con degradado de izquierda ({@code left}) a derecha ({@code right}). El juego solo hace degradados
	 * de arriba abajo, así que se dibuja girado 90 grados.
	 */
	public static void hGradient(GuiGraphics g, int x1, int y1, int x2, int y2, int left, int right) {
		if (x2 <= x1 || y2 <= y1) return;
		g.pose().pushMatrix();
		g.pose().translate(x1, y2);
		g.pose().rotate((float) (-Math.PI / 2.0));
		g.fillGradient(0, 0, y2 - y1, x2 - x1, left, right);
		g.pose().popMatrix();
	}

	/** Tramos horizontales de cada dibujo pixel ({x, y, largo}), calculados una vez por dibujo. */
	private static final java.util.Map<String[], int[][]> ART_RUNS = new java.util.IdentityHashMap<>();

	/**
	 * Dibujo pixel hecho con '#' en {@code (x, y)}: cada tramo seguido de una fila es un solo rectángulo, en vez de
	 * un rectángulo por píxel (la rueda de ajustes pasa de 40 a 14).
	 */
	public static void art(GuiGraphics g, String[] rows, int x, int y, int color) {
		int[][] runs = ART_RUNS.computeIfAbsent(rows, Draw::runs);
		for (int[] run : runs) g.fill(x + run[0], y + run[1], x + run[0] + run[2], y + run[1] + 1, color);
	}

	private static int[][] runs(String[] rows) {
		java.util.List<int[]> runs = new java.util.ArrayList<>();
		for (int row = 0; row < rows.length; row++) {
			String line = rows[row];
			int col = 0;
			while (col < line.length()) {
				if (line.charAt(col) != '#') {
					col++;
					continue;
				}
				int start = col;
				while (col < line.length() && line.charAt(col) == '#') col++;
				runs.add(new int[] {start, row, col - start});
			}
		}
		return runs.toArray(new int[0][]);
	}

	/** Dibuja un icono (propio de 16x16 o el original de un mod) escalado a {@code size} px. */
	public static void icon(GuiGraphics g, Identifier icon, int x, int y, int size) {
		int textureSize = ModIcons.textureSize(icon);
		g.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0.0F, 0.0F, size, size, textureSize, textureSize, textureSize, textureSize);
	}

	/** Recuadro con el icono dentro, como en el boceto de las tarjetas. */
	public static void iconBox(GuiGraphics g, Identifier icon, int x, int y, int boxSize) {
		panel(g, x, y, boxSize, boxSize, ThemeManager.shade(), ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.3F));
		int offset = (boxSize - 16) / 2;
		icon(g, icon, x + offset, y + offset, 16);
	}

	/** Caja del icono teñida con el color de la categoría del mod, con una franja de ese color abajo. */
	public static void iconBox(GuiGraphics g, Identifier icon, int x, int y, int boxSize, int accent) {
		panel(g, x, y, boxSize, boxSize, ThemeManager.mix(ThemeManager.shade(), accent, 0.16F), ThemeManager.mix(accent, ThemeManager.border(), 0.35F));
		g.fill(x + 2, y + boxSize - 3, x + boxSize - 2, y + boxSize - 2, ThemeManager.mix(accent, ThemeManager.shade(), 0.2F));
		int offset = (boxSize - 16) / 2;
		icon(g, icon, x + offset, y + offset - 1, 16);
	}
}
