package com.freedomclient.music;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Dónde va cada letra de NeoLyrics y de qué color, tal cual el diseño (scripts/textures/neolyrics/neolyrics_estilos.html):
 * «Cartel 3D» y «Recta» (la línea entera), «Flotante» (palabras inclinadas y de tamaños distintos, en filas) y
 * «Disperso» (cada palabra en un sitio del cielo). Las medidas van en altos de letra (em).
 */
public final class NeoStyle {
	/** Espacio extra entre letras (em). */
	public static final double TRACKING = 0.03;
	/** Ancho máximo de una fila en Flotante (em). */
	public static final double ROW_MAX = 8;
	/** Entre filas (em). */
	public static final double ROW_GAP = 1.4;
	/** Grosor del relieve (em) y sus capas. */
	public static final double DEPTH = 0.16;
	public static final int LAYERS = 5;
	/** Disperso: los sitios del cielo donde caen las palabras (yaw y altura relativos, en grados, y tamaño). */
	public static final double[][] SLOTS = {{-28, 14, 1.5}, {22, -6, 1.7}, {-30, -10, 0.8}, {-10, 0, 0.7}, {30, 12, 1.0}, {6, 16, 1.2},
			{-6, -12, 1.0}, {16, 4, 0.9}};

	public enum Layout { CARTEL, FLOTANTE, DISPERSO, RECTA }

	public enum Color { BLANCO, LILA, ROSA, CIAN, DORADO, ARCOIRIS, KARAOKE }

	/** Una letra colocada: centro (x, y en em, y hacia arriba), giro y escala de su palabra; group = palabra en Disperso. */
	public record Item(String ch, int wi, int k, double x, double y, double rot, double scale, int group) {
	}

	private NeoStyle() {
	}

	/** El sitio (de {@link #SLOTS}) de la palabra wi de la línea li en Disperso. */
	public static double[] slot(int li, int wi) {
		int first = (int) Math.floor(LyricsTimeline.hash(li * 7 + 3) * 8);
		return SLOTS[(first + wi * 3) % 8];
	}

	/** Dónde va cada letra de una línea. {@code width} da el ancho de un carácter en em. */
	public static List<Item> layout(LyricsTimeline.Line line, int li, Layout mode, ToDoubleFunction<String> width) {
		List<Item> items = new ArrayList<>();
		List<LyricsTimeline.Word> words = line.words();
		double space = width.applyAsDouble(" ") + TRACKING;
		if (mode == Layout.DISPERSO) {
			for (int wi = 0; wi < words.size(); wi++) {
				place(items, words, wi, 0, 0, (LyricsTimeline.hash(li * 31 + wi + 101) - 0.5) * 0.25, slot(li, wi)[2], wi, width);
			}
		} else if (mode == Layout.FLOTANTE) {
			double[] sc = new double[words.size()];
			for (int wi = 0; wi < words.size(); wi++) sc[wi] = 0.85 + LyricsTimeline.hash(li * 31 + wi + 7) * 0.35;
			List<List<Integer>> rows = new ArrayList<>();
			rows.add(new ArrayList<>());
			double used = 0;
			for (int wi = 0; wi < words.size(); wi++) {
				double ww = wordWidth(words.get(wi), width) * sc[wi];
				List<Integer> last = rows.get(rows.size() - 1);
				if (!last.isEmpty() && used + space + ww > ROW_MAX) {
					rows.add(new ArrayList<>());
					used = 0;
				}
				last = rows.get(rows.size() - 1);
				used += (last.isEmpty() ? 0 : space) + ww;
				last.add(wi);
			}
			for (int r = 0; r < rows.size(); r++) {
				List<Integer> row = rows.get(r);
				double total = space * (row.size() - 1);
				for (int wi : row) total += wordWidth(words.get(wi), width) * sc[wi];
				double x = -total / 2;
				double ry = ((rows.size() - 1) / 2.0 - r) * ROW_GAP;
				for (int wi : row) {
					double ww = wordWidth(words.get(wi), width) * sc[wi];
					place(items, words, wi, x + ww / 2, ry + (LyricsTimeline.hash(li * 31 + wi + 211) - 0.5) * 0.3,
							(LyricsTimeline.hash(li * 31 + wi + 101) - 0.5) * 0.2, sc[wi], -1, width);
					x += ww + space;
				}
			}
		} else {
			double total = space * (words.size() - 1);
			for (LyricsTimeline.Word w : words) total += wordWidth(w, width);
			double x = -total / 2;
			for (int wi = 0; wi < words.size(); wi++) {
				double ww = wordWidth(words.get(wi), width);
				place(items, words, wi, x + ww / 2, 0, 0, 1, -1, width);
				x += ww + space;
			}
		}
		return items;
	}

	private static double wordWidth(LyricsTimeline.Word word, ToDoubleFunction<String> width) {
		double total = 0;
		for (String ch : chars(word.text())) total += width.applyAsDouble(ch) + TRACKING;
		return total - TRACKING;
	}

	/** Las letras de una palabra centradas en (cx, cy), giradas y escaladas como la palabra. */
	private static void place(List<Item> items, List<LyricsTimeline.Word> words, int wi, double cx, double cy, double rot, double scale, int group,
							  ToDoubleFunction<String> width) {
		LyricsTimeline.Word w = words.get(wi);
		double lx = -wordWidth(w, width) / 2;
		List<String> chars = chars(w.text());
		for (int k = 0; k < chars.size(); k++) {
			String ch = chars.get(k);
			double half = width.applyAsDouble(ch) / 2, px = (lx + half) * scale;
			items.add(new Item(ch, wi, k, cx + px * Math.cos(rot), cy + px * Math.sin(rot), rot, scale, group));
			lx += width.applyAsDouble(ch) + TRACKING;
		}
	}

	/** Los caracteres de un texto (por puntos de código, como «[...texto]» en el diseño). */
	public static List<String> chars(String text) {
		List<String> out = new ArrayList<>();
		text.codePoints().forEach(cp -> out.add(new String(Character.toChars(cp))));
		return out;
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Colores (RGB)

	private static final int[][] BASE = {{0xFFFFFF, 0xDCEBFF}, {0xF1E6FF, 0xB48CFF}, {0xFFE3F1, 0xFF3FA4}, {0xEAFCFF, 0x3FD7FF}, {0xFFF3C4, 0xFFB43F}};

	/** {relleno, halo} de una letra. */
	public static int[] letterColor(Color mode, double t, LyricsTimeline.Word word, int n, LyricsTimeline.Letter st) {
		int fill, halo;
		if (mode == Color.ARCOIRIS) {
			double h = (n * 22 + t * 90) % 360;
			fill = hsl(h, 0.85, 0.82);
			halo = hsl(h, 0.95, 0.62);
		} else if (mode == Color.KARAOKE) {
			boolean sung = t >= word.start();
			fill = sung ? 0xD6B8FF : 0xB9B4C8;
			halo = sung ? 0xA66BFF : 0x6D6680;
			return new int[]{st.flash() > 0.5 ? 0xFFFFFF : fill, halo};
		} else {
			fill = BASE[mode.ordinal()][0];
			halo = BASE[mode.ordinal()][1];
		}
		return new int[]{st.flash() > 0.5 || st.singing() ? 0xFFFFFF : fill, halo};
	}

	/** Los colores del título (Arcoíris y Karaoke usan los de Lila). */
	public static int[] titleColor(Color mode) {
		return mode == Color.ARCOIRIS || mode == Color.KARAOKE ? BASE[1] : BASE[mode.ordinal()];
	}

	/** h en grados, s y l de 0 a 1. */
	public static int hsl(double h, double s, double l) {
		double c = (1 - Math.abs(2 * l - 1)) * s, x = c * (1 - Math.abs((h / 60) % 2 - 1)), m = l - c / 2;
		double r, g, b;
		if (h < 60) { r = c; g = x; b = 0; }
		else if (h < 120) { r = x; g = c; b = 0; }
		else if (h < 180) { r = 0; g = c; b = x; }
		else if (h < 240) { r = 0; g = x; b = c; }
		else if (h < 300) { r = x; g = 0; b = c; }
		else { r = c; g = 0; b = x; }
		return (int) Math.round((r + m) * 255) << 16 | (int) Math.round((g + m) * 255) << 8 | (int) Math.round((b + m) * 255);
	}

	/** Mezcla de dos colores RGB (t = 0: a, 1: b). */
	public static int mix(int a, int b, double t) {
		int r = (int) Math.round((a >> 16 & 0xFF) * (1 - t) + (b >> 16 & 0xFF) * t);
		int g = (int) Math.round((a >> 8 & 0xFF) * (1 - t) + (b >> 8 & 0xFF) * t);
		int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return r << 16 | g << 8 | bl;
	}
}
