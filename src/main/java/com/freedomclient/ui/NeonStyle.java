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
	/** Tamaño de cada tramo de color: más grande = menos rectángulos por fotograma. */
	private static final int STEP = 2;

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
		int span = Math.max(1, x2 - x1);
		for (int x = x1; x < x2; x += STEP) {
			double f = (x - x1) / (double) span;
			g.fill(x, y, Math.min(x + STEP, x2), y + thickness, color(t0 + (t1 - t0) * f, alpha));
		}
	}

	/** Línea vertical con el degradado de {@code t0} a {@code t1}. */
	public static void vLine(GuiGraphics g, int x, int y1, int y2, int thickness, double t0, double t1, float alpha) {
		int span = Math.max(1, y2 - y1);
		for (int y = y1; y < y2; y += STEP) {
			double f = (y - y1) / (double) span;
			g.fill(x, y, x + thickness, Math.min(y + STEP, y2), color(t0 + (t1 - t0) * f, alpha));
		}
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
		for (int i = 0; i < trail; i += STEP) {
			float fade = 1.0F - i / (float) trail;
			g.fill(head - i - STEP, y, head - i, y + 1, color(i / 60.0, fade * 0.9F));
		}
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
