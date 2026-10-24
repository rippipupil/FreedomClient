package com.freedomclient.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Las cuentas de NeoLyrics, tal cual el diseño (scripts/textures/neolyrics/neolyrics.html): la letra LRC en líneas,
 * los tiempos de cada palabra y el estado de cada letra en cada instante con la animación «Saltito» (cada letra sale
 * de abajo con un salto que se pasa un poco, se mece mientras se canta su palabra y al acabar la línea sube y se va).
 */
public final class LyricsTimeline {
	/** Una línea dura como mucho 6 s aunque la siguiente tarde más. */
	public static final double MAX_LINE = 6;
	/** Las palabras van saliendo en el 72 % primero de la línea. */
	public static final double SING = 0.72;
	/** Cada letra sale 35 ms después de la anterior. */
	public static final double LETTER_GAP = 0.035;
	/** Lo que tarda una letra en aparecer. */
	public static final double APPEAR = 0.38;
	/** Lo que tardan en irse al acabar la línea. */
	public static final double LEAVE = 0.9;
	/** Grados sobre el horizonte. */
	public static final double ELEVATION = 24;
	/** Alto de una letra en grados. */
	public static final double SIZE = 3.2;
	/** Ancho de una letra en altos de letra. */
	public static final double SPACING = 0.62;

	private static final Pattern LINE = Pattern.compile("^\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]\\s*(.*)$");

	public record Word(String text, double start, double end) {
	}

	public record Line(double t, String text, double dur, List<Word> words) {
		public double end() {
			return t + dur;
		}

		/** Cuántos caracteres ocupa la línea (las palabras con un espacio entre ellas). */
		public int chars() {
			int chars = Math.max(0, words.size() - 1);
			for (Word word : words) chars += word.text.length();
			return chars;
		}
	}

	/** Estado de una letra: y (en altos de letra, hacia arriba), escala, giro, alfa, destello y si se está cantando. */
	public record Letter(double y, double s, double rot, double alpha, double flash, boolean singing) {
	}

	private final List<Line> lines;
	private final double length;

	private LyricsTimeline(List<Line> lines, double length) {
		this.lines = lines;
		this.length = length;
	}

	public List<Line> lines() {
		return lines;
	}

	/** Hasta cuándo hay algo que dibujar (la última línea ya ida). */
	public double length() {
		return length;
	}

	/** Lee un LRC; null si no tiene ninguna línea con texto. */
	public static LyricsTimeline parse(String lrc) {
		if (lrc == null) return null;
		List<double[]> times = new ArrayList<>();
		List<String> texts = new ArrayList<>();
		for (String raw : lrc.split("\n")) {
			Matcher m = LINE.matcher(raw.replace("\r", ""));
			if (!m.matches()) continue;
			times.add(new double[]{Integer.parseInt(m.group(1)) * 60 + Double.parseDouble(m.group(2))});
			texts.add(m.group(3).trim());
		}
		List<Line> timed = new ArrayList<>();
		double length = 0;
		for (int i = 0; i < times.size(); i++) {
			Line line = wordTimes(times.get(i)[0], texts.get(i), i + 1 < times.size() ? times.get(i + 1)[0] : Double.NaN);
			if (line.text.isEmpty()) continue;
			timed.add(line);
			length = Math.max(length, line.end() + LEAVE + 1);
		}
		return timed.isEmpty() ? null : new LyricsTimeline(Collections.unmodifiableList(timed), length);
	}

	/** Tiempos de cada palabra de una línea: se reparten según su largo (más un poco por palabra). */
	static Line wordTimes(double t, String text, double next) {
		List<String> words = new ArrayList<>();
		for (String word : text.split("\\s+")) if (!word.isEmpty()) words.add(word);
		double dur = Math.min(MAX_LINE, (Double.isNaN(next) ? t + MAX_LINE : next) - t);
		double total = 0;
		for (String word : words) total += word.length() + 2;
		List<Word> timed = new ArrayList<>();
		double acc = 0;
		for (String word : words) {
			double start = t + dur * SING * acc / total;
			acc += word.length() + 2;
			timed.add(new Word(word, start, t + dur * SING * acc / total));
		}
		return new Line(t, text, dur, timed);
	}

	public static double backOut(double u) {
		double v = u - 1;
		return 1 + 2.70158 * v * v * v + 1.70158 * v * v;
	}

	public static double ease(double u) {
		u = Math.max(0, Math.min(1, u));
		return u * u * (3 - 2 * u);
	}

	/**
	 * Estado de la letra k de una palabra en t («Saltito»), o null si no se ve. li es el índice de la letra en toda la
	 * línea: se van una detrás de otra.
	 */
	public static Letter letter(double t, Word word, int k, double lineEnd, int li) {
		double born = word.start + k * LETTER_GAP, a = t - born;
		if (a < 0) return null;
		double u = Math.min(1, a / APPEAR);
		double y = (1 - backOut(u)) * -1.4, s = 0.55 + 0.45 * backOut(u), rot = 0;
		double alpha = ease(u), flash = Math.max(0, 1 - a / 0.45);
		boolean singing = t >= word.start && t < word.end + 0.15;
		if (singing) y += Math.sin((t - word.start) * 9 + k * 0.7) * 0.06;
		// Al acabar la línea cada letra sube y se desvanece, una detrás de otra.
		double out = t - lineEnd - li * 0.02;
		if (out > 0) {
			double v = Math.min(1, out / LEAVE);
			y += ease(v) * 2.2;
			alpha *= 1 - ease(v);
			s *= 1 - 0.3 * v;
		}
		return alpha > 0.01 ? new Letter(y, s, rot, alpha, flash, singing) : null;
	}
}
