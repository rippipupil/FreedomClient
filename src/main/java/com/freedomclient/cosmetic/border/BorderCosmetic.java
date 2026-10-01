package com.freedomclient.cosmetic.border;

import com.freedomclient.FreedomClient;
import com.freedomclient.cosmetic.CosmeticModule;
import com.freedomclient.cosmetic.CosmeticSlot;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.ModeSetting;
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

	// ---------- Ayudas para recorrer el borde ----------

	/** Píxeles que tiene el contorno de un rectángulo de w x h. */
	protected static int perimeter(int w, int h) {
		return Math.max(4, 2 * (w - 1) + 2 * (h - 1));
	}

	/**
	 * Punto del contorno a {@code s} píxeles desde la esquina de arriba a la izquierda, en el sentido de las agujas
	 * del reloj: {x, y, nx, ny} con la normal hacia fuera.
	 */
	protected static int[] point(int s, int x, int y, int w, int h) {
		int p = perimeter(w, h);
		s = Math.floorMod(s, p);
		int top = w - 1;
		int right = top + h - 1;
		int bottom = right + w - 1;
		if (s < top) return new int[] {x + s, y, 0, -1};
		if (s < right) return new int[] {x + w - 1, y + s - top, 1, 0};
		if (s < bottom) return new int[] {x + w - 1 - (s - right), y + h - 1, 0, 1};
		return new int[] {x, y + h - 1 - (s - bottom), -1, 0};
	}

	protected static void pixel(GuiGraphics g, int x, int y, int color) {
		g.fill(x, y, x + 1, y + 1, color);
	}

	/** Contorno de 1 px. */
	protected static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x, y + h - 1, x + w, y + h, color);
		g.fill(x, y + 1, x + 1, y + h - 1, color);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
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

	/** Dibuja un dibujito de '#' centrado en (cx, cy). */
	protected static void pattern(GuiGraphics g, String[] rows, int cx, int cy, int color) {
		int x0 = cx - rows[0].length() / 2;
		int y0 = cy - rows.length / 2;
		for (int row = 0; row < rows.length; row++) {
			for (int col = 0; col < rows[row].length(); col++) {
				if (rows[row].charAt(col) == '#') pixel(g, x0 + col, y0 + row, color);
			}
		}
	}
}
