package com.freedomclient.cosmetic.border;

import com.freedomclient.FreedomClient;
import com.freedomclient.cosmetic.CosmeticModule;
import com.freedomclient.cosmetic.CosmeticSlot;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Cosmético de borde: un efecto animado alrededor de las tarjetas del menú de mods (y de cosméticos y HUD).
 * Solo se puede llevar uno a la vez, como un sombrero: al ponerte uno se quitan los demás. Mientras hay uno puesto
 * sustituye al borde del mod Card Borders. Todo sale del reloj, así que no guarda estado por tarjeta.
 */
public abstract class BorderCosmetic extends CosmeticModule {
	private final ModeSetting cards = add(new ModeSetting("Cards", "Which cards get the effect.", "Enabled mods", "Enabled mods", "All cards"));

	protected BorderCosmetic(String name, String description) {
		super(name, description, CosmeticSlot.BORDERS, false);
	}

	@Override
	protected void onEnable(Minecraft client) {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null) return;
		for (BorderCosmetic other : manager.ofType(BorderCosmetic.class)) {
			if (other != this) other.setEnabled(false);
		}
	}

	/** El borde que llevas puesto, o null. */
	public static BorderCosmetic active() {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null) return null;
		for (BorderCosmetic border : manager.ofType(BorderCosmetic.class)) {
			if (border.isEnabled()) return border;
		}
		return null;
	}

	/**
	 * Dibuja el borde puesto en una tarjeta. {@code on} (0..1) es cuánto está activado el mod de la tarjeta.
	 * Devuelve false si no llevas ningún borde (entonces la tarjeta usa Card Borders).
	 */
	public static boolean drawActive(GuiGraphics g, int x, int y, int w, int h, int categoryColor, float on, float hover) {
		BorderCosmetic border = active();
		if (border == null) return false;
		float strength = border.cards.is("All cards") ? Math.max(on, 0.7F) : on;
		if (strength > 0.01F) border.draw(g, x, y, w, h, categoryColor, Math.min(1.0F, strength + hover * 0.15F), hover);
		return true;
	}

	/** Dibuja el efecto en el rectángulo; {@code strength} (0..1) es la opacidad general. */
	public abstract void draw(GuiGraphics g, int x, int y, int w, int h, int categoryColor, float strength, float hover);

	// ---------- Ayudas para dibujar el borde ----------
	// Todo se dibuja con tramos enteros (un rectángulo por trozo de línea), nunca píxel a píxel: con muchas tarjetas a
	// la vista eran cientos de rectángulos por tarjeta y el menú iba a tirones.

	/** Píxeles que tiene el contorno de un rectángulo de w x h. */
	protected static int perimeter(int w, int h) {
		return Math.max(4, 2 * (w - 1) + 2 * (h - 1));
	}

	/**
	 * Punto del contorno a {@code s} píxeles desde la esquina de arriba a la izquierda, en el sentido de las agujas
	 * del reloj: {x, y, nx, ny, lado} con la normal hacia fuera (lado: 0 arriba, 1 derecha, 2 abajo, 3 izquierda).
	 */
	protected static int[] point(int s, int x, int y, int w, int h) {
		int p = perimeter(w, h);
		s = Math.floorMod(s, p);
		int top = w - 1;
		int right = top + h - 1;
		int bottom = right + w - 1;
		if (s < top) return new int[] {x + s, y, 0, -1, 0};
		if (s < right) return new int[] {x + w - 1, y + s - top, 1, 0, 1};
		if (s < bottom) return new int[] {x + w - 1 - (s - right), y + h - 1, 0, 1, 2};
		return new int[] {x, y + h - 1 - (s - bottom), -1, 0, 3};
	}

	/**
	 * Marco de 1 px con las esquinas recortadas, como los paneles del cliente ({@code inset} px hacia dentro).
	 */
	protected static void frame(GuiGraphics g, int x, int y, int w, int h, int inset, int color) {
		x += inset;
		y += inset;
		w -= inset * 2;
		h -= inset * 2;
		if (w < 3 || h < 3) return;
		g.fill(x + 1, y, x + w - 1, y + 1, color);
		g.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
		g.fill(x, y + 1, x + 1, y + h - 1, color);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	/**
	 * Tramo recto del borde entre los puntos {@code s0} y {@code s1} del contorno (incluidos, del mismo lado),
	 * desplazado {@code out} px hacia fuera y de {@code thick} px de grueso hacia dentro.
	 */
	protected static void run(GuiGraphics g, int[] a, int[] b, int out, int thick, int color) {
		int x1 = Math.min(a[0], b[0]) + a[2] * out;
		int y1 = Math.min(a[1], b[1]) + a[3] * out;
		int x2 = Math.max(a[0], b[0]) + 1 + a[2] * out;
		int y2 = Math.max(a[1], b[1]) + 1 + a[3] * out;
		// El grueso crece hacia dentro de la tarjeta (contra la normal).
		if (a[2] > 0) x1 -= thick - 1;
		if (a[2] < 0) x2 += thick - 1;
		if (a[3] > 0) y1 -= thick - 1;
		if (a[3] < 0) y2 += thick - 1;
		g.fill(x1, y1, x2, y2, color);
	}

	/**
	 * Tramo del contorno de {@code from} a {@code to} (puntos, en el sentido de las agujas del reloj) partido por
	 * las esquinas: un rectángulo por cada lado que toca.
	 */
	protected static void span(GuiGraphics g, int from, int to, int x, int y, int w, int h, int out, int thick, int color) {
		int[] start = point(from, x, y, w, h);
		int s = from;
		for (int i = from + 1; i <= to; i++) {
			int[] next = point(i, x, y, w, h);
			if (next[4] != start[4]) {
				run(g, start, point(i - 1, x, y, w, h), out, thick, color);
				start = next;
				s = i;
			}
		}
		if (s <= to) run(g, start, point(to, x, y, w, h), out, thick, color);
	}

	/** Número pseudoaleatorio estable (0..1) a partir de una semilla. */
	protected static float hash(long seed) {
		long h = seed * 0x9E3779B97F4A7C15L;
		h ^= h >>> 31;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 29;
		return (h & 0xFFFFFF) / (float) 0x1000000;
	}

	protected static int alpha(int rgb, float alpha) {
		return ThemeManager.withAlpha(0xFF000000 | rgb, alpha);
	}

	/** Dibuja un dibujito de '#' centrado en (cx, cy), con un rectángulo por tramo de cada fila. */
	protected static void pattern(GuiGraphics g, String[] rows, int cx, int cy, int color) {
		Draw.art(g, rows, cx - rows[0].length() / 2, cy - rows.length / 2, color);
	}
}
