package com.freedomclient.cosmetic.vox;

/**
 * Las cuentas de los cosméticos fantasmita, copiadas tal cual del diseño (scripts/textures/ghost/ghost.html): la
 * mascota que vuela a tu alrededor (y lo que hace en cada estado), el aura de fantasmas con sus destellos y la vida de
 * las flores y pétalos de la estela. Sin nada de Minecraft, así se compara número a número con el diseño
 * (scripts/textures/ghost/check_java.js).
 * <p>
 * Unidades del diseño: píxeles del modelo (1/16 de bloque), y hacia arriba, el jugador con los pies en el origen
 * mirando a +z; ángulos en radianes y tiempos en segundos.
 */
public final class GhostMotion {
	// ---- La mascota ----
	public static final double RADIUS = 20, WOBBLE = 3, HEIGHT = 25, PERIOD = 11, BOB = 1.2, BOB_PERIOD = 2.4, BLEND = 0.8, FACE_EVERY = 4,
			SCALE = 0.8, HURT = 1.4;
	/** Caras de la mascota tranquila, una cada 4 s (índices de {@link com.freedomclient.ui.GhostArt#FACES}). */
	public static final int[] IDLE_FACES = {1, 0, 2, 1, 3, 4};

	/** Estados de la mascota (como en el diseño). */
	public enum Mood { IDLE, WAVE, CELEBRATE, SLEEP, HIDE, HURT, FLY }

	/** Emociones sueltas (las de PetBehavior). */
	public enum Emotion { NONE, HAPPY, BLUSH, HEARTS }

	/** Dónde está la mascota y cómo está girada: x, y, z, giro (yaw), cabeceo (pitch) y balanceo (roll). */
	public static final class Pose {
		public double x, y, z, yaw, pitch, roll;

		public Pose set(double x, double y, double z, double yaw, double pitch, double roll) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.yaw = yaw;
			this.pitch = pitch;
			this.roll = roll;
			return this;
		}

		public Pose copy(Pose o) {
			return set(o.x, o.y, o.z, o.yaw, o.pitch, o.roll);
		}
	}

	private GhostMotion() {
	}

	/** La vuelta alrededor tuyo: 11 s a unos 20 px, a la altura de la cabeza, mirando hacia donde va. */
	public static Pose orbit(double t, Pose out) {
		double a = 2 * Math.PI * t / PERIOD;
		double r = RADIUS + WOBBLE * Math.sin(2 * Math.PI * t / 7.3);
		double y = HEIGHT + BOB * Math.sin(2 * Math.PI * t / BOB_PERIOD) + 2 * Math.sin(2 * Math.PI * t / 13);
		return out.set(Math.sin(a) * r, y, Math.cos(a) * r, a + Math.PI / 2 + 0.6, 0.06 * Math.sin(2 * Math.PI * t / BOB_PERIOD), -0.2);
	}

	private static double look(double x, double z) {
		return Math.atan2(-x, -z);
	}

	/** Sitio fijo de cada estado (relativo a ti, mirando a +z); false si sigue dando vueltas. */
	public static boolean anchor(Mood mood, double t, double s, Pose out) {
		switch (mood) {
			case WAVE -> out.set(-9, 27 + 0.8 * Math.sin(2 * Math.PI * t / 2), 17, look(-9, 17), 0, 0);
			case CELEBRATE -> out.set(0, 40 + Math.abs(Math.sin(Math.PI * s * 2.5)) * 3, 3, s * 2 * Math.PI * 1.5, 0, 0);
			case SLEEP -> out.set(12, 21 + 0.5 * Math.sin(2 * Math.PI * t / 4), 3, 0.35, 0.1, 0.25);
			case HIDE -> out.set(5, 19, -11, look(5, -11) + 0.15 * Math.sin(t * 40), 0, 0.05 * Math.sin(t * 33));
			case FLY -> out.set(-7, 31 + 1.5 * Math.sin(2 * Math.PI * t / 1.6), -15, 0, 0.45, 0.2 * Math.sin(2 * Math.PI * t / 2.2));
			default -> {
				return false;
			}
		}
		return true;
	}

	/** Golpe: se aparta de un salto (hacia fuera y arriba) y tiembla, 1,2 s. */
	public static Pose hurt(Pose o, double s) {
		double k = Math.sin(Math.PI * Math.min(1, s / 1.2));
		double d = Math.hypot(o.x, o.z);
		if (d == 0) d = 1;
		return o.set(o.x + o.x / d * 6 * k, o.y + 3 * k, o.z + o.z / d * 6 * k, o.yaw, o.pitch, o.roll + 0.25 * k * Math.sin(s * 50));
	}

	/** La pose de este momento: la vuelta, el golpe o el sitio del estado, mezclada desde {@code from} al cambiar. */
	public static Pose pose(Mood mood, double t, double s, Pose from, Pose out) {
		orbit(t, out);
		if (mood == Mood.HURT) hurt(out, s);
		anchor(mood, t, s, out);
		if (from != null && s < BLEND) {
			double k = smooth(s / BLEND);
			out.set(from.x + (out.x - from.x) * k, from.y + (out.y - from.y) * k, from.z + (out.z - from.z) * k,
					from.yaw + Math.atan2(Math.sin(out.yaw - from.yaw), Math.cos(out.yaw - from.yaw)) * k, from.pitch + (out.pitch - from.pitch) * k,
					from.roll + (out.roll - from.roll) * k);
		}
		return out;
	}

	/** Cara de la mascota: la del estado; tranquila, una distinta cada 4 s. */
	public static int face(Mood mood, Emotion emotion, double t, double s) {
		if (mood == Mood.HURT && s > HURT) mood = Mood.IDLE;
		switch (mood) {
			case WAVE -> {
				return 4;
			}
			case CELEBRATE -> {
				return 0;
			}
			case SLEEP -> {
				return 6;
			}
			case HIDE -> {
				return 7;
			}
			case HURT -> {
				return 5;
			}
			case FLY -> {
				return 2;
			}
			default -> {
			}
		}
		if (emotion == Emotion.HAPPY) return 0;
		if (emotion == Emotion.BLUSH) return 3;
		if (emotion == Emotion.HEARTS) return 4;
		return IDLE_FACES[(int) (Math.floor(t / FACE_EVERY) % IDLE_FACES.length)];
	}

	/** Bracitos (giro en z, + hacia arriba) y saltito: {izquierdo, derecho, salto}. {@code e} son los segundos de la emoción. */
	public static void limbs(Mood mood, Emotion emotion, double t, double s, double e, double[] out) {
		double sway = 0.15 * Math.sin(2 * Math.PI * t / 2.4);
		double left = sway, right = -sway, hop = 0;
		if (mood == Mood.WAVE) right = 1.1 + 0.55 * Math.sin(2 * Math.PI * s * 2.2);
		if (mood == Mood.CELEBRATE) {
			left = 1.2 + 0.3 * Math.sin(s * 18);
			right = 1.2 + 0.3 * Math.sin(s * 18 + 1);
		}
		if (mood == Mood.HIDE) {
			left = -0.5;
			right = -0.5;
		}
		if (mood == Mood.FLY) {
			left = -0.6;
			right = -0.6;
		}
		if (emotion == Emotion.HAPPY && e < 1.6) hop = Math.abs(Math.sin(e * Math.PI * 2.5)) * 2.2 * (1 - e / 1.6 * 0.4);
		out[0] = left;
		out[1] = right;
		out[2] = hop;
	}

	// ---- Aura de fantasmas ----
	public static final int AURA_COUNT = 6;
	public static final double AURA_LIFE = 7.2, AURA_SIZE = 0.42;
	public static final int[] AURA_FACES = {1, 0, 3, 2, 4, 6};
	private static final int[] AURA_LEVEL = {0, 3, 1, 4, 2, 5};

	/** Fantasmita i del aura en t: {x, y, z, giro, balanceo, tamaño, cara, blanco (1/0)}. */
	public static void aura(int i, double t, double[] out) {
		int ring = i % 3;
		double tt = t + i * AURA_LIFE / AURA_COUNT;
		double a = i * 2 * Math.PI / AURA_COUNT + 2 * Math.PI * t / (16 + 4 * ring);
		double r = 12 + 3.5 * ring;
		double u = (tt % AURA_LIFE) / AURA_LIFE;
		long cycle = (long) Math.floor(tt / AURA_LIFE);
		double grow = smooth(Math.min(1, u / 0.12)) * smooth(Math.min(1, (1 - u) / 0.12));
		out[0] = Math.sin(a) * r;
		out[1] = 5 + AURA_LEVEL[i] * 4.5 + 6 * u + 1.2 * Math.sin(2 * Math.PI * t / 3.1 + i);
		out[2] = Math.cos(a) * r;
		out[3] = a + Math.PI / 2 + 0.9;
		out[4] = 0.15 * Math.sin(2 * Math.PI * t / 2.7 + i);
		out[5] = AURA_SIZE * grow;
		out[6] = AURA_FACES[(int) ((i * 3 + cycle) % AURA_FACES.length)];
		out[7] = (i + cycle) % 2 == 0 ? 1 : 0;
	}

	/** Destellos ✦ del aura (los mismos números al azar que el diseño: semilla 5): ángulo, radio, y0, velocidad, giro, rosa. */
	public static final double[][] SPARKLES = new double[10][];

	static {
		long[] seed = {5L};
		java.util.function.DoubleSupplier r = () -> (seed[0] = seed[0] * 16807L % 2147483647L) / 2147483647.0;
		for (int i = 0; i < SPARKLES.length; i++) {
			double a = r.getAsDouble() * 6.283, rad = 9 + r.getAsDouble() * 10, y0 = r.getAsDouble() * 34, v = 3 + r.getAsDouble() * 2.5;
			double spin = 1 + r.getAsDouble() * 2, pink = r.getAsDouble() < 0.5 ? 1 : 0;
			SPARKLES[i] = new double[] {a, rad, y0, v, spin, pink};
		}
	}

	/** Destello en t: {x, y, z, tamaño, giro}. */
	public static void sparkle(double[] p, double t, double[] out) {
		double h = ((p[2] + p[3] * t) % 34 + 34) % 34, life = h / 34;
		out[0] = Math.sin(p[0] + t * 0.2) * p[1];
		out[1] = 2 + h;
		out[2] = Math.cos(p[0] + t * 0.2) * p[1];
		out[3] = 0.7 * Math.min(1, h / 3) * (1 - life * life) * (0.75 + 0.25 * Math.sin(t * 5 + p[0] * 3));
		out[4] = t * p[4];
	}

	// ---- Estela de flores ----
	public static final double FLOWER_SIZE = 1.5, FLOWER_GROW = 0.35, FLOWER_WILT = 0.6, FLOWER_STEP = 0.6;

	/** Flor de la estela a {@code age} segundos de una vida de {@code life}: {tamaño, vaivén, cierre}; false si ya no está. */
	public static boolean flower(double age, double seed, double life, double[] out) {
		if (age < 0 || age > life) return false;
		double g = backOut(Math.min(1, age / FLOWER_GROW)), w = age > life - FLOWER_WILT ? smooth((life - age) / FLOWER_WILT) : 1;
		out[0] = FLOWER_SIZE * g * w;
		out[1] = 0.12 * Math.sin(2 * Math.PI * age / 1.6 + seed * 6.283);
		out[2] = 1 - w;
		return true;
	}

	/** Pétalo suelto a {@code age} segundos: {x, y, z, giro, tamaño}; false si ya no está. */
	public static boolean petal(double age, double dx, double dz, double spin, double[] out) {
		if (age < 0 || age > 1.2) return false;
		out[0] = dx * age;
		out[1] = 2 + 5 * age - 2 * age * age;
		out[2] = dz * age;
		out[3] = spin * age;
		out[4] = 1 - age / 1.2;
		return true;
	}

	public static double smooth(double u) {
		u = Math.max(0, Math.min(1, u));
		return u * u * (3 - 2 * u);
	}

	public static double backOut(double u) {
		double v = u - 1;
		return 1 + 2.70158 * v * v * v + 1.70158 * v * v;
	}
}
