package com.freedomclient.cosmetic.vox;

/**
 * Las cuentas de Void Keys, copiadas tal cual del diseño (scripts/textures/void_keys/void_keys.html): el aro de
 * teclas, las partículas, las poses del jugador flotando y la animación de tocar "Flow" sacada de su partitura.
 * Sin nada de Minecraft, así se compara número a número con el diseño (scripts/textures/void_keys/check_java.js).
 * <p>
 * Unidades del diseño: píxeles del modelo (1/16 de bloque), y hacia arriba, el jugador con los pies en el origen
 * mirando a +z; ángulos en radianes y tiempos en segundos.
 */
public final class VoidKeysMotion {
	// ---- El aro ----
	/** 44 teclas: 26 blancas y 18 grises, de do3 a sol6. */
	public static final int WHITES = 26;
	/** Radio del borde de dentro. */
	public static final double INNER = 16;
	/** Altura de las tapas sobre los pies. */
	public static final double HEIGHT = 18.5;
	/** Segundos por vuelta del aro. */
	public static final double SPIN = 50;
	/** Segundos por ola, crestas alrededor del aro, altura de la ola y cabeceo de cada tecla. */
	public static final double WAVE = 6;
	public static final double CRESTS = 2;
	public static final double AMP = 0.6;
	public static final double TILT = 0.08;
	/** Teclas grises entre las blancas (do#, re#, fa#, sol#, la# de cada octava; en la última, hasta fa#). */
	public static final int[] BLACKS = {0, 1, 3, 4, 5, 7, 8, 10, 11, 12, 14, 15, 17, 18, 19, 21, 22, 24};
	public static final int KEYS = WHITES + 18;

	// ---- Flotar ----
	public static final double FLOAT_LIFT = 3;
	public static final double FLOAT_BOB = 0.8;
	public static final double FLOAT_PERIOD = 3.2;

	// ---- Partículas ----
	public static final int STREAKS = 18;
	public static final int ROCKS = 10;
	public static final int ROCK_SHAPES = 3;
	public static final double RISE = 32;

	// ---- Chill ----
	public static final double CHILL_CYCLE = 36;
	public static final double CHILL_RAMP = 3;
	public static final double[] CHILL_RECLINE = {5, 16};
	public static final double[] CHILL_STRETCH = {22, 30};

	// ---- Tocar ----
	public static final double PLAY_SIGMA = 1.5;
	public static final double PLAY_TURN = 0.6;
	public static final double PLAY_FACE_STEP = 0.1;
	public static final double PLAY_PRESS = 1.0;
	public static final double PLAY_GAP = 0.55;
	public static final double PLAY_HOVER = -0.85;
	public static final double PLAY_DOWN = 0.3;
	public static final double PLAY_REACH = 0.3;
	public static final double PLAY_LEAN = 0.12;
	public static final double PLAY_BLEND = 1.5;

	/** Nota (0-11 dentro de la octava) -> tecla blanca o gris de la octava, o -1. */
	private static final int[] WHITE_OF = {0, -1, 1, -1, 2, 3, -1, 4, -1, 5, -1, 6};
	private static final int[] BLACK_OF = {-1, 0, -1, 1, -1, -1, 2, -1, 3, -1, 4, -1};

	/** Partículas (los mismos números aleatorios que el diseño): líneas de luz y trozos de terreno. */
	public static final double[][] STREAK_DATA = new double[STREAKS][];
	public static final double[][] ROCK_DATA = new double[ROCKS][];
	/** Columnas de STREAK_DATA y ROCK_DATA. */
	public static final int A = 0, RAD = 1, Y0 = 2, V = 3, W = 4, TWIST = 5, SIZE = 6, AX = 7, AY = 8, AZ = 9, SPIN_SPEED = 10, SHAPE = 11;

	static {
		long[] seed = {7L};
		java.util.function.DoubleSupplier r = () -> (seed[0] = seed[0] * 16807L % 2147483647L) / 2147483647.0;
		for (int i = 0; i < STREAKS; i++) {
			double a = r.getAsDouble() * 6.283, rad = 6 + r.getAsDouble() * 16, y0 = r.getAsDouble() * RISE, v = 5 + r.getAsDouble() * 4;
			double size = 0.7 + r.getAsDouble() * 0.6;
			STREAK_DATA[i] = new double[] {a, rad, y0, v, 0, 0, size, 0, 0, 0, 0, 0};
		}
		for (int i = 0; i < ROCKS; i++) {
			double a = r.getAsDouble() * 6.283, rad = 12 + r.getAsDouble() * 8, y0 = r.getAsDouble() * RISE, v = 1.4 + r.getAsDouble() * 1.0;
			double w = 0.25 + r.getAsDouble() * 0.15;
			double ax = r.getAsDouble() - 0.5, ay = r.getAsDouble() - 0.5, az = r.getAsDouble() - 0.5;
			double spin = 0.5 + r.getAsDouble() * 1.0, size = 0.8 + r.getAsDouble() * 0.6;
			ROCK_DATA[i] = new double[] {a, rad, y0, v, w, 0.12, size, ax, ay, az, spin, i % ROCK_SHAPES};
		}
	}

	private VoidKeysMotion() {
	}

	/** Tecla de una nota (0-43): índice en el aro, las 26 blancas primero y luego las 18 grises. */
	public static int noteKey(int note) {
		int oct = Math.floorDiv(note, 12), s = Math.floorMod(note, 12);
		return WHITE_OF[s] >= 0 ? WHITE_OF[s] + oct * 7 : WHITES + BLACK_OF[s] + oct * 5;
	}

	/** Ángulo de la tecla {@code key} (índice del aro) sin el giro: 0 = delante del jugador. */
	public static double keyAngle(int key) {
		return key < WHITES ? key * 2 * Math.PI / WHITES : (BLACKS[key - WHITES] + 0.5) * 2 * Math.PI / WHITES;
	}

	/** Pose de una tecla en el tiempo t: {ángulo con el giro, altura con la ola, cabeceo}. */
	public static void keyPose(double angle, double t, double[] out) {
		double phase = 2 * Math.PI * t / WAVE - CRESTS * angle;
		out[0] = angle + 2 * Math.PI * t / SPIN;
		out[1] = HEIGHT + AMP * Math.sin(phase);
		out[2] = TILT * Math.cos(phase);
	}

	/** Estado de una partícula en t: {x, y, z, tamaño (0 al desaparecer), giro}. */
	public static void particle(double[] p, double t, double[] out) {
		double h = (p[Y0] + p[V] * t) % RISE;
		double life = h / RISE;
		double grow = Math.min(1, h / 3);
		double fade = 1 - Math.pow(life, 2.2);
		double ang = p[A] + p[W] * t + p[TWIST] * h;
		double rad = p[RAD] * (1 - 0.35 * life * (p[TWIST] != 0 ? 1 : 0));
		out[0] = Math.sin(ang) * rad;
		out[1] = HEIGHT - 12 + h;
		out[2] = Math.cos(ang) * rad;
		out[3] = p[SIZE] * grow * fade;
		out[4] = p[SPIN_SPEED] * t;
	}

	/** Luz de una tecla {@code age} segundos después de tocarla: se enciende en 0,08 s y se apaga suave en 0,9 s. */
	public static double keyLight(double age) {
		return age >= 0 && age < 0.9 ? Math.min(1, age / 0.08) * Math.pow(1 - age / 0.9, 2) : 0;
	}

	/** La pose completa del jugador (ver {@link #figurePose}). */
	public static final class Pose {
		public double lift, pitch, yaw, roll, headX, headZ, headYaw;
		public double rArmX, rArmZ = -0.05, lArmX, lArmZ = 0.05, rLegX, rLegZ, lLegX, lLegZ;
	}

	/** Lo que hacen las manos al tocar en un instante: hacia dónde mira el cuerpo, cuánto baja cada mano y hacia qué lado. */
	public static final class Play {
		public double facing, look;
		public final double[] down = new double[2], reach = new double[2];
	}

	private static double mix(double v, double to, double k) {
		return v + (to - v) * k;
	}

	private static double clamp01(double v) {
		return Math.max(0, Math.min(1, v));
	}

	/**
	 * Pose del jugador. {@code speed}: 0 quieto, 1 andando, 1,4 corriendo; {@code vy}: velocidad vertical en bloques
	 * por tick; {@code play}: lo que hacen las manos si está tocando (o null) y {@code playIn} (0-1) cuánto ha entrado
	 * en la pose de tocar. Devuelve lo que sube y el giro de todo el cuerpo sobre su centro (16 px) y [x, z] de cada
	 * parte (brazos y piernas: x negativo = hacia delante, z hacia fuera con el signo de su lado).
	 */
	public static Pose figurePose(double t, boolean floating, double speed, double vy, Play play, double playIn) {
		Pose p = new Pose();
		double b = Math.sin(2 * Math.PI * t / FLOAT_PERIOD);
		double lift = FLOAT_LIFT + FLOAT_BOB * b;
		double rightLeg = 0.55 + 0.06 * b, rightLegOut = 0.06, leftLeg = -0.18 - 0.04 * b, leftLegOut = 0.04;
		double armOut = 0.18 + 0.03 * b, armBack = 0.12, headTilt = 0.1, headNod = 0.08;
		double leftArmUp = 0.5 + 0.04 * b, leftArmForward = -0.3;
		if (play != null) {
			double k = playIn;
			p.lift = lift;
			p.pitch = (PLAY_LEAN + 0.03 * Math.sin(2 * Math.PI * t / 4)) * k;
			p.rArmX = mix(armBack, PLAY_HOVER + PLAY_DOWN * play.down[0], k);
			p.rArmZ = mix(-armOut, -0.2 + PLAY_REACH * Math.min(0, play.reach[0]), k);
			p.lArmX = mix(leftArmForward, PLAY_HOVER + PLAY_DOWN * play.down[1], k);
			p.lArmZ = mix(leftArmUp, 0.2 + PLAY_REACH * Math.max(0, play.reach[1]), k);
			p.headX = mix(headNod, 0.3, k);
			p.headZ = mix(headTilt, 0, k);
			p.headYaw = 0.4 * play.look * k;
			p.rLegX = rightLeg;
			p.rLegZ = -rightLegOut;
			p.lLegX = leftLeg;
			p.lLegZ = leftLegOut;
			return p;
		}
		if (!floating) {
			double s = Math.sin(t * 6) * 0.5 * Math.min(1, speed + Math.abs(vy) * 3);
			p.rLegX = s;
			p.lLegX = -s;
			p.rArmX = -s;
			p.lArmX = s;
			return p;
		}
		p.lift = lift;
		p.rArmX = armBack;
		p.rArmZ = -armOut;
		p.lArmX = leftArmForward;
		p.lArmZ = leftArmUp;
		p.rLegX = rightLeg;
		p.rLegZ = -rightLegOut;
		p.lLegX = leftLeg;
		p.lLegZ = leftLegOut;
		p.headX = headNod;
		p.headZ = headTilt;
		// Quieto: chill (se apaga en cuanto se mueve o salta).
		double idle = Math.max(0, 1 - speed * 4) * (1 - Math.min(1, Math.abs(vy) * 6));
		if (idle > 0) {
			double c = t % CHILL_CYCLE;
			double r = window(c, CHILL_RECLINE) * idle, st = window(c, CHILL_STRETCH) * idle;
			p.yaw += 0.35 * Math.sin(2 * Math.PI * t / 14) * idle;
			p.roll += 0.06 * Math.sin(2 * Math.PI * t / 9) * idle;
			p.pitch += 0.04 * Math.sin(2 * Math.PI * t / 11) * idle;
			p.pitch -= 0.45 * r;
			p.rLegX = mix(p.rLegX, -0.3, r);
			p.lLegX = mix(p.lLegX, -0.15, r);
			p.rArmX = mix(p.rArmX, 0.3, r);
			p.rArmZ = mix(p.rArmZ, -0.4, r);
			p.lArmX = mix(p.lArmX, 0.3, r);
			p.lArmZ = mix(p.lArmZ, 0.4, r);
			p.headX = mix(p.headX, -0.2, r);
			p.rArmX = mix(p.rArmX, -2.9, st);
			p.rArmZ = mix(p.rArmZ, -0.12, st);
			p.lArmX = mix(p.lArmX, -2.9, st);
			p.lArmZ = mix(p.lArmZ, 0.12, st);
			p.headX = mix(p.headX, -0.3, st);
			p.pitch -= 0.25 * st;
			p.lift += 1 * st;
		}
		// Avanzando: se abalanza con el brazo derecho por delante y las piernas atrás.
		double mk = Math.min(1.4, speed), sway = Math.sin(2 * Math.PI * t / 1.6) * Math.min(1, mk);
		double k = Math.min(1, mk), lean = 0.4 * Math.min(1, mk) + 0.1 * Math.max(0, mk - 1);
		p.pitch += lean;
		p.rArmX = mix(p.rArmX, -2.45 - 0.1 * sway, k);
		p.rArmZ = mix(p.rArmZ, -0.12, k);
		p.lArmX = mix(p.lArmX, 0.5 + 0.1 * sway, k);
		p.lArmZ = mix(p.lArmZ, 0.18, k);
		p.rLegX = mix(p.rLegX, 0.3 + 0.08 * sway, k);
		p.lLegX = mix(p.lLegX, 0.62 - 0.08 * sway, k);
		p.headX -= lean * 0.6;
		// Saltando: brazos arriba y piernas recogidas al subir; brazos algo abiertos al caer.
		double up = clamp01(vy / 0.35), down = clamp01(-vy / 0.35), jw = Math.max(up, down);
		double arms = -2.7 * up + 0.1 * down, armsOut = 0.2 * up + 0.5 * down, legs = -0.6 * up + 0.3 * down;
		p.rArmX = mix(p.rArmX, arms, jw);
		p.rArmZ = mix(p.rArmZ, -armsOut, jw);
		p.lArmX = mix(p.lArmX, arms, jw);
		p.lArmZ = mix(p.lArmZ, armsOut, jw);
		p.rLegX = mix(p.rLegX, legs, jw);
		p.lLegX = mix(p.lLegX, legs + 0.15, jw);
		return p;
	}

	/** Ventana suave (sube y baja en CHILL_RAMP segundos) entre los dos tiempos del ciclo. */
	private static double window(double c, double[] ab) {
		return smooth((c - ab[0]) / CHILL_RAMP) * smooth((ab[1] - c) / CHILL_RAMP);
	}

	private static double smooth(double u) {
		u = clamp01(u);
		return u * u * (3 - 2 * u);
	}

	private static double wrapAngle(double a) {
		return Math.atan2(Math.sin(a), Math.cos(a));
	}

	/**
	 * La partitura de una canción ("milisegundo nota fuerza" por línea) y todo lo que se saca de ella una vez: hacia
	 * dónde mira el cuerpo en cada momento, cuándo baja cada mano y cuándo suena cada tecla.
	 */
	public static final class Score {
		public final double length;
		final double[] time;
		final int[] note;
		final double[] strength;
		final double[] faces;
		/** Pulsaciones: tiempo, mano (0 derecha, 1 izquierda), fuerza 0-1 y ángulo de la tecla respecto a donde mira. */
		final double[][] presses;
		/** Por tecla del aro, los tiempos en que suena, en orden. */
		final double[][] keyHits;

		public Score(String text, double length) {
			this.length = length;
			java.util.List<double[]> rows = new java.util.ArrayList<>();
			for (String line : text.split("\n")) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) continue;
				String[] p = line.split("\\s+");
				rows.add(new double[] {Double.parseDouble(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2])});
			}
			int n = rows.size();
			time = new double[n];
			note = new int[n];
			strength = new double[n];
			for (int i = 0; i < n; i++) {
				time[i] = rows.get(i)[0] / 1000;
				note[i] = (int) rows.get(i)[1];
				strength[i] = rows.get(i)[2];
			}
			faces = buildFaces();
			presses = buildPresses();
			java.util.List<java.util.List<Double>> hits = new java.util.ArrayList<>();
			for (int k = 0; k < KEYS; k++) hits.add(new java.util.ArrayList<>());
			for (int i = 0; i < n; i++) hits.get(noteKey(note[i])).add(time[i]);
			keyHits = new double[KEYS][];
			for (int k = 0; k < KEYS; k++) keyHits[k] = hits.get(k).stream().mapToDouble(Double::doubleValue).toArray();
		}

		public int notes() {
			return time.length;
		}

		/** Primera nota con tiempo >= t. */
		int firstNote(double t) {
			int lo = 0, hi = time.length;
			while (lo < hi) {
				int m = (lo + hi) >>> 1;
				if (time[m] < t) lo = m + 1;
				else hi = m;
			}
			return lo;
		}

		/**
		 * Hacia dónde mira el cuerpo cada 0,1 s: el objetivo es la media de las teclas de alrededor (±3,5 s, pesando
		 * más las cercanas y las fuertes); si están repartidas por todos lados no cambia, y lo sigue como un muelle
		 * suave que nunca gira más de 0,6 rad/s.
		 */
		private double[] buildFaces() {
			java.util.List<Double> list = new java.util.ArrayList<>();
			double cur = 0, vel = 0;
			for (int i = 0; i * PLAY_FACE_STEP <= length + PLAY_FACE_STEP; i++) {
				double t = i * PLAY_FACE_STEP;
				double sx = 0, cz = 0, wsum = 0;
				for (int n = firstNote(t - 3.5); n < time.length && time[n] < t + 3.5; n++) {
					double d = (time[n] - t) / PLAY_SIGMA, w = Math.exp(-0.5 * d * d) * (0.4 + strength[n] / 100);
					double a = keyAngle(noteKey(note[n]));
					sx += w * Math.sin(a);
					cz += w * Math.cos(a);
					wsum += w;
				}
				double target = Math.hypot(sx, cz) > 0.35 * wsum && wsum > 0.2 ? cur + wrapAngle(Math.atan2(sx, cz) - cur) : cur;
				double want = Math.max(-PLAY_TURN, Math.min(PLAY_TURN, (target - cur) * 1.2));
				vel += (want - vel) * 0.25;
				cur += vel * PLAY_FACE_STEP;
				list.add(cur);
			}
			return list.stream().mapToDouble(Double::doubleValue).toArray();
		}

		/** Hacia dónde mira el cuerpo en el aro (sin su giro) en el instante songT de la canción. */
		public double facing(double songT) {
			double x = Math.max(0, songT / PLAY_FACE_STEP);
			int i = (int) Math.min(faces.length - 2, Math.floor(x));
			return faces[i] + (faces[i + 1] - faces[i]) * Math.min(1, x - i);
		}

		private double[][] buildPresses() {
			java.util.List<double[]> list = new java.util.ArrayList<>();
			double[] last = {-9, -9};
			for (int i = 0; i < time.length; i++) {
				double t = time[i], rel = wrapAngle(keyAngle(noteKey(note[i])) - facing(t));
				int hand = rel > 0 ? 1 : 0;
				if (t - last[hand] < PLAY_GAP) continue;
				last[hand] = t;
				list.add(new double[] {t, hand, strength[i] / 100, rel});
			}
			return list.toArray(new double[0][]);
		}

		/** Lo que hacen las manos en el instante songT (ver {@link Play}). */
		public Play play(double songT, Play out) {
			out.down[0] = out.down[1] = 0;
			double[] reach = {0, 0}, wsum = {0.15, 0.15};
			int lo = 0, hi = presses.length;
			while (lo < hi) {
				int m = (lo + hi) >>> 1;
				if (presses[m][0] < songT - PLAY_PRESS) lo = m + 1;
				else hi = m;
			}
			for (int i = lo; i < presses.length && presses[i][0] <= songT; i++) {
				double u = (songT - presses[i][0]) / PLAY_PRESS;
				double b = Math.pow(Math.sin(Math.PI * u), 2) * (0.6 + 0.4 * presses[i][2]);
				int h = (int) presses[i][1];
				out.down[h] = Math.max(out.down[h], b);
				reach[h] += b * Math.max(-1, Math.min(1, presses[i][3]));
				wsum[h] += b;
			}
			out.facing = facing(songT);
			out.reach[0] = reach[0] / wsum[0];
			out.reach[1] = reach[1] / wsum[1];
			out.look = (reach[0] + reach[1]) / (wsum[0] + wsum[1]);
			return out;
		}

		/** Cuándo sonó por última vez la tecla {@code key} hasta songT, o NaN si aún no ha sonado. */
		public double lastHit(int key, double songT) {
			double[] hits = keyHits[key];
			int lo = 0, hi = hits.length;
			while (lo < hi) {
				int m = (lo + hi) >>> 1;
				if (hits[m] <= songT) lo = m + 1;
				else hi = m;
			}
			return lo == 0 ? Double.NaN : hits[lo - 1];
		}

		/** Notas que empiezan en [from, to): índices de la partitura. */
		public int[] notesBetween(double from, double to) {
			int a = firstNote(from), b = firstNote(to);
			int[] out = new int[Math.max(0, b - a)];
			for (int i = 0; i < out.length; i++) out[i] = a + i;
			return out;
		}

		public double time(int i) {
			return time[i];
		}

		public int key(int i) {
			return noteKey(note[i]);
		}

		public double strength(int i) {
			return strength[i] / 100;
		}
	}
}
