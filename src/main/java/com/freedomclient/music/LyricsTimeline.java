package com.freedomclient.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Las cuentas de NeoLyrics, tal cual el diseño (scripts/textures/neolyrics/neolyrics_estilos.html): la letra LRC en
 * líneas, los tiempos de cada palabra y el estado de cada letra en cada instante según la aparición (Saltito, Fundido,
 * Máquina de escribir, Zoom) y la desaparición (Subir, Desvanecer, Caer, Viento, Romperse, Encoger). Dónde va cada
 * letra y su color están en {@link NeoStyle}.
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
	public static final double ELEVATION = 16;
	/** Alto de una letra en grados. */
	public static final double SIZE = 5;
	/** Bloques hasta el cartel 3D. */
	public static final double DISTANCE = 30;
	/** El título sale como mucho estos segundos al empezar. */
	public static final double INTRO = 6;

	/** Cómo aparece cada letra. */
	public enum Appear { SALTO, FUNDIDO, MAQUINA, ZOOM }

	/** Cómo se va cada letra al acabar la línea. */
	public enum Leave { SUBIR, DESVANECER, CAER, VIENTO, ROMPER, ENCOGER }

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

	/**
	 * Estado de una letra: desplazamiento (x, y en altos de letra, y hacia arriba), escala, giro (rad), alfa, destello,
	 * si se está cantando y «burst» (0..1, lo que lleva rota con Romperse).
	 */
	public record Letter(double x, double y, double s, double rot, double alpha, double flash, boolean singing, double burst) {
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

	/** Número pseudoaleatorio fijo en [0, 1) (las mismas cuentas enteras que el diseño). */
	public static double hash(int n) {
		int h = (n ^ 0x5bd1e995) * 0x2c1b3c6d;
		h ^= h >>> 15;
		h *= 0x297a2d39;
		h ^= h >>> 13;
		return (h & 0xFFFFFFFFL) / 4294967296.0;
	}

	/**
	 * Estado de la letra k de una palabra en t, o null si no se ve. n es el índice de la letra en toda la línea: se van
	 * una detrás de otra.
	 */
	public static Letter letter(Appear ap, Leave lv, double t, Word word, int k, double lineEnd, int n) {
		double born = word.start + k * LETTER_GAP, a = t - born;
		if (a < 0) return null;
		double u = Math.min(1, a / APPEAR);
		double x = 0, y = 0, s = 1, rot = 0, alpha = ease(u), flash = Math.max(0, 1 - a / 0.45), burst = 0;
		switch (ap) {
			case SALTO -> {
				y = (1 - backOut(u)) * -1.4;
				s = 0.55 + 0.45 * backOut(u);
			}
			case FUNDIDO -> {
				y = (1 - ease(u)) * -0.25;
				flash *= 0.5;
			}
			case MAQUINA -> {
				alpha = 1;
				s = 1 + 0.3 * Math.max(0, 1 - a / 0.12);
			}
			case ZOOM -> {
				s = 1.9 - 0.9 * ease(u);
				rot = (1 - ease(u)) * -0.35;
			}
		}
		boolean singing = t >= word.start && t < word.end + 0.15;
		if (singing) y += Math.sin((t - word.start) * 9 + k * 0.7) * 0.06;
		double stagger = lv == Leave.DESVANECER ? 0 : lv == Leave.CAER ? 0.03 : 0.02;
		double out = t - lineEnd - n * stagger;
		if (out > 0) {
			double v = Math.min(1, out / LEAVE), e = ease(v), r = hash(n) - 0.5;
			switch (lv) {
				case SUBIR -> {
					y += e * 2.2;
					alpha *= 1 - e;
					s *= 1 - 0.3 * v;
				}
				case DESVANECER -> alpha *= 1 - e;
				case CAER -> {
					y -= 5 * v * v;
					x += r * 1.2 * v;
					rot += r * 2.4 * v;
					alpha *= 1 - ease((v - 0.4) / 0.6);
				}
				case VIENTO -> {
					x += e * 5;
					y += Math.sin(v * 5 + n * 0.6) * 0.35 * v;
					rot += (0.4 + r) * 1.4 * e;
					alpha *= 1 - e;
				}
				case ROMPER -> {
					burst = v;
					alpha *= 1 - e;
				}
				case ENCOGER -> {
					s *= 1 - e;
					rot += r * 1.6 * v;
					alpha *= 1 - e * e;
				}
			}
		}
		return alpha > 0.01 ? new Letter(x, y, s, rot, alpha, flash, singing, burst) : null;
	}
}
