package com.freedomclient.cosmetic.border;

import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.ui.Draw;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Borde arcoíris: un degradado de todos los colores que gira alrededor de la tarjeta, con un brillo suave por fuera.
 * Cada lado se dibuja con unos pocos rectángulos con degradado (el color va de un tramo a otro sin saltos), en vez
 * de uno por píxel, y sigue las esquinas recortadas de las tarjetas.
 */
public class RainbowBorderCosmetic extends BorderCosmetic {
	/** Tramos con degradado por lado: con cuatro el arcoíris ya se ve continuo. */
	private static final int PIECES = 4;

	private final NumberSetting speed = add(new NumberSetting("Speed", "How fast the colors go around the card.", 4, 1, 10, 1));
	private final NumberSetting thickness = add(new NumberSetting("Thickness", "How thick the rainbow is.", 2, 1, 3, 1, " px"));
	private final NumberSetting saturation = add(new NumberSetting("Saturation", "How strong the colors are (low = pastel).", 80, 30, 100, 5, "%"));
	private final BooleanSetting glow = add(new BooleanSetting("Glow", "A soft glow outside the border.", true));

	public RainbowBorderCosmetic() {
		super("Rainbow Border", "A rainbow that flows around your enabled mod cards, with a soft glow.");
	}

	@Override
	public void draw(GuiGraphics g, int x, int y, int w, int h, int categoryColor, float strength, float hover) {
		if (w < 4 || h < 4) return;
		int t = thickness.getInt();
		float sat = saturation.getFloat() / 100.0F;
		// El reloj se reduce antes de multiplicar: con los milisegundos enteros el float perdía los decimales y
		// todo el borde salía del mismo color.
		double shift = (System.currentTimeMillis() % 1_000_000L) * speed.get() * 0.00006;
		double perimeter = 2.0 * (w + h);
		boolean halo = glow.get();
		// Arriba (de izquierda a derecha), derecha (de arriba abajo), abajo (de derecha a izquierda) e izquierda
		// (de abajo arriba): la posición en el contorno de cada extremo da el tono.
		for (int k = 0; k < PIECES; k++) {
			// Arriba.
			int ax = x + 1 + (w - 2) * k / PIECES;
			int bx = x + 1 + (w - 2) * (k + 1) / PIECES;
			int c0 = hue(ax - x, perimeter, shift, sat);
			int c1 = hue(bx - x, perimeter, shift, sat);
			Draw.hGradient(g, ax, y, bx, y + t, alpha(c0, strength), alpha(c1, strength));
			if (halo) Draw.hGradient(g, ax, y - 1, bx, y, alpha(c0, strength * 0.3F), alpha(c1, strength * 0.3F));
			// Abajo: el contorno va de derecha a izquierda.
			c0 = hue(w + h + (x + w - ax), perimeter, shift, sat);
			c1 = hue(w + h + (x + w - bx), perimeter, shift, sat);
			Draw.hGradient(g, ax, y + h - t, bx, y + h, alpha(c0, strength), alpha(c1, strength));
			if (halo) Draw.hGradient(g, ax, y + h, bx, y + h + 1, alpha(c0, strength * 0.3F), alpha(c1, strength * 0.3F));
			// Derecha (de arriba abajo).
			int ay = y + 1 + (h - 2) * k / PIECES;
			int by = y + 1 + (h - 2) * (k + 1) / PIECES;
			c0 = hue(w + (ay - y), perimeter, shift, sat);
			c1 = hue(w + (by - y), perimeter, shift, sat);
			g.fillGradient(x + w - t, ay, x + w, by, alpha(c0, strength), alpha(c1, strength));
			if (halo) g.fillGradient(x + w, ay, x + w + 1, by, alpha(c0, strength * 0.3F), alpha(c1, strength * 0.3F));
			// Izquierda: el contorno sube.
			c0 = hue(2 * w + h + (y + h - ay), perimeter, shift, sat);
			c1 = hue(2 * w + h + (y + h - by), perimeter, shift, sat);
			g.fillGradient(x, ay, x + t, by, alpha(c0, strength), alpha(c1, strength));
			if (halo) g.fillGradient(x - 1, ay, x, by, alpha(c0, strength * 0.3F), alpha(c1, strength * 0.3F));
		}
	}

	private static int hue(double s, double perimeter, double shift, float sat) {
		double hue = s / perimeter - shift;
		return java.awt.Color.HSBtoRGB((float) (hue - Math.floor(hue)), sat, 1.0F) & 0xFFFFFF;
	}
}
