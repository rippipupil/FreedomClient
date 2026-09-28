package com.freedomclient.ui;

import com.freedomclient.particle.NeonFx;
import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Detalles del menú con el tema Neon: bordes, líneas y textos con el degradado de su energía (amarillo, lima,
 * verde agua, cian, azul y violeta) que fluye poco a poco, y chispas que recorren el borde de la ventana.
 */
public final class NeonStyle {
	private NeonStyle() {
	}

	public static boolean on() {
		return ThemeManager.isNeon();
	}

	/** Fase del degradado que avanza con el tiempo (una vuelta a la paleta cada ~8 s). */
	public static double flow() {
		return (System.currentTimeMillis() % 8000L) / 8000.0;
	}

	/**
	 * Color del degradado en {@code t}: va de amarillo a lima, verde agua, cian y azul y vuelve (una ida y vuelta por
	 * cada unidad de {@code t}), así nunca salta de azul a amarillo de golpe y predominan los tonos cálidos.
	 */
	public static int color(double t, float alpha) {
		double wrapped = t - Math.floor(t);
		double pingPong = 1.0 - Math.abs(wrapped * 2.0 - 1.0);
		return ThemeManager.withAlpha(NeonFx.gradient(pingPong * 4.0 / NeonFx.PALETTE.length), alpha);
	}

	/** Línea horizontal con el degradado de {@code t0} a {@code t1}. */
	public static void hLine(GuiGraphics g, int x1, int x2, int y, int thickness, double t0, double t1, float alpha) {
		gradient(g, x1, x2, y, thickness, true, t0, t1, alpha, alpha);
	}

	/** Línea vertical con el degradado de {@code t0} a {@code t1}. */
	public static void vLine(GuiGraphics g, int x, int y1, int y2, int thickness, double t0, double t1, float alpha) {
		gradient(g, y1, y2, x, thickness, false, t0, t1, alpha, alpha);
	}

	/**
	 * Tramo de degradado de {@code from} a {@code to} (en x si {@code horizontal}, si no en y), de {@code thickness}
	 * de grueso desde {@code across}. El degradado es lineal entre cada color de la paleta, así que basta con un
	 * rectángulo con degradado por cada tramo entre colores (unos pocos por línea en vez de uno cada 2 px). Los
	 * horizontales se dibujan girando 90 grados, porque el degradado del juego solo va de arriba abajo.
	 */
	private static void gradient(GuiGraphics g, int from, int to, int across, int thickness, boolean horizontal,
			double t0, double t1, float a0, float a1) {
		int length = to - from;
		if (length <= 0 || thickness <= 0) return;
		if (horizontal) {
			g.pose().pushMatrix();
			g.pose().translate(from, across + thickness);
			g.pose().rotate((float) (-Math.PI / 2.0));
		}
		// Cortes en cada múltiplo de 1/8 de t: ahí cambia de pareja de colores la paleta (y está el pico del ida y
		// vuelta). Se recorren en el sentido de la línea, vaya el degradado hacia delante o hacia atrás.
		double low = Math.min(t0, t1);
		double high = Math.max(t0, t1);
		int cuts = Math.max(0, (int) Math.ceil(high * 8.0) - (int) Math.floor(low * 8.0) - 1);
		int start = 0;
		for (int i = 0; i <= cuts && start < length; i++) {
			int end = length;
			if (i < cuts) {
				double k = t1 >= t0 ? Math.floor(t0 * 8.0) + 1.0 + i : Math.ceil(t0 * 8.0) - 1.0 - i;
				double f = (k / 8.0 - t0) / (t1 - t0);
				end = Math.max(start + 1, Math.min(length, (int) Math.round(f * length)));
			}
			double fs = start / (double) length;
			double fe = end / (double) length;
			int c0 = color(t0 + (t1 - t0) * fs, a0 + (a1 - a0) * (float) fs);
			int c1 = color(t0 + (t1 - t0) * fe, a0 + (a1 - a0) * (float) fe);
			if (horizontal) {
				g.fillGradient(0, start, thickness, end, c0, c1);
			} else {
				g.fillGradient(across, from + start, across + thickness, from + end, c0, c1);
			}
			start = end;
		}
		if (horizontal) g.pose().popMatrix();
	}

	/**
	 * Borde de 1 px con las esquinas recortadas (como {@link Draw#panel}) cuyos colores dan la vuelta al marco:
	 * {@code phase} desplaza el degradado y {@code spread} es cuánta paleta cabe en una vuelta.
	 */
	public static void frame(GuiGraphics g, int x, int y, int w, int h, double phase, double spread, float alpha) {
		double perimeter = 2.0 * (w + h);
		double top = phase;
		double right = phase + spread * w / perimeter;
		double bottom = phase + spread * (w + h) / perimeter;
		double left = phase + spread * (2 * w + h) / perimeter;
		double end = phase + spread;
		hLine(g, x + 1, x + w - 1, y, 1, top, right, alpha);
		vLine(g, x + w - 1, y + 1, y + h - 1, 1, right, bottom, alpha);
		// Abajo y a la izquierda el color va "de vuelta" para que el marco sea continuo.
		hLine(g, x + 1, x + w - 1, y + h - 1, 1, left, bottom, alpha);
		vLine(g, x, y + 1, y + h - 1, 1, end, left, alpha);
	}

	/**
	 * Una chispa que recorre el borde de arriba de vez en cuando: cabeza en zigzag muy brillante y una estela que
	 * se apaga pasando por los colores de la paleta.
	 */
	public static void spark(GuiGraphics g, int x, int y, int w, long periodMs, long offsetMs) {
		long time = (System.currentTimeMillis() + offsetMs) % periodMs;
		long travel = 1100;
		if (time > travel) return;
		float progress = time / (float) travel;
		int head = x + 2 + Math.round((w - 4) * progress);
		int trail = Math.min(42, head - x - 1);
		// Estela: de la cola apagada a la cabeza brillante, en unos pocos tramos de degradado.
		gradient(g, head - trail, head, y, 1, true, trail / 60.0, 0.0, 0.0F, 0.9F);
		// Zigzag de 3 px de alto: un rayito que salta en el borde.
		int flicker = (int) (System.currentTimeMillis() / 60 % 2);
		g.fill(head - 1, y - 1 - flicker, head, y + 2, 0xFFFFF7C8);
		g.fill(head, y - 1 + flicker, head + 1, y + 1 + flicker, color(0.1, 1.0F));
		g.fill(head - 2, y + flicker, head - 1, y + 1 + flicker, color(0.2, 0.8F));
	}

	/** Texto en negrita con cada letra de un color del degradado; devuelve el ancho dibujado. */
	public static int gradientText(GuiGraphics g, Font font, String text, int x, int y, double t0, double spread, boolean bold) {
		int cursor = x;
		for (int i = 0; i < text.length(); i++) {
			Component letter = Component.literal(String.valueOf(text.charAt(i)));
			if (bold) letter = letter.copy().withStyle(ChatFormatting.BOLD);
			double t = t0 + spread * i / Math.max(1, text.length() - 1);
			g.drawString(font, letter, cursor, y, color(t, 1.0F), true);
			cursor += font.width(letter);
		}
		return cursor - x;
	}
}
