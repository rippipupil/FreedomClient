package com.freedomclient.cosmetic.vox;

import com.freedomclient.FreedomClient;
import com.freedomclient.cosmetic.CosmeticPreview;
import com.freedomclient.setting.BooleanSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Void Keys (Música): un teclado del vacío de 44 teclas blancas y grises que flota en un aro alrededor del jugador,
 * girando despacio y haciendo olas suaves, con líneas de luz blancas y trozos de terreno oscuro que suben en espiral.
 * En tercera persona el jugador flota (se abalanza al avanzar, salta con los brazos arriba y, quieto, se deja llevar
 * tranquilo); con la tecla de tocar suena "Flow" (Creo) y el jugador la toca despacio: cada nota enciende su tecla.
 * <p>
 * Todo sale del diseño (scripts/textures/void_keys): las piezas de assets/freedomclient/vox/void_keys.vox, la
 * partitura de assets/freedomclient/music/flow_keys.txt y las cuentas de {@link VoidKeysMotion}, comprobadas número a
 * número con el diseño.
 */
public class VoidKeysCosmetic extends MusicGuitarCosmetic {
	public static final String SONG = "guitar.flow";
	private static final String MODEL = "/assets/freedomclient/vox/void_keys.vox";
	private static final String SCORE = "/assets/freedomclient/music/flow_keys.txt";
	/** Duración de la canción (sin el silencio del final). */
	private static final double SONG_LENGTH = 205.3;
	/** Tamaño del modelo del jugador: 0,9375 (AvatarRenderer lo encoge un poco). Píxeles del modelo a bloques. */
	private static final float MODEL_PX = 0.9375F / 16.0F;
	/** Altura del modelo sobre los pies en su espacio (LivingEntityRenderer lo baja 1,501 bloques). */
	private static final float MODEL_FEET = 1.501F;
	/** Centro del cuerpo, donde se inclina y se mece (16 px sobre los pies). */
	private static final float CENTER = 16.0F;

	public final BooleanSetting floating = add(new BooleanSetting("Float",
			"In third person you float instead of walking: you lunge forward when moving and drift calmly when still.", true));
	public final BooleanSetting particles = add(new BooleanSetting("Particles",
			"White light streaks and dark chunks of ground rising in a spiral around you.", true));

	private Vox.Shape whiteKey, whiteCap, whiteCapLit, whiteGlow, blackKey, blackGlow, streak;
	private final Vox.Shape[] rocks = new Vox.Shape[VoidKeysMotion.ROCK_SHAPES];
	private static VoidKeysMotion.Score score;

	/** La pose de tu jugador en este fotograma, por estado de render (la sacan los mixins del jugador). */
	private static final Map<AvatarRenderState, Frame> FRAMES = Collections.synchronizedMap(new WeakHashMap<>());

	/** Lo que se calcula una vez por fotograma para tu jugador. */
	public record Frame(VoidKeysMotion.Pose pose, double t, boolean playing, double songT) {
	}

	/** Solo para las pruebas: una pose fija en vez de la animada (para comprobar en capturas hacia dónde va cada parte). */
	public static VoidKeysMotion.Pose testPose;

	// Estado entre fotogramas: hacia dónde miraba al empezar a tocar y la velocidad suavizada al moverse.
	private static boolean wasPlaying;
	private static double playYawStart;
	private static double speed;
	private static long lastFrame;

	public VoidKeysCosmetic() {
		super("Void Keys", "Music: a floating keyboard of the void that circles around you in soft waves. "
				+ "You float in third person and play Flow (Creo) key by key.");
	}

	@Override
	public String song() {
		return SONG;
	}

	@Override
	public String songTitle() {
		return "Flow — Creo";
	}

	@Override
	public boolean beatNotes() {
		return false;
	}

	public static VoidKeysCosmetic activeKeys() {
		return MusicGuitarCosmetic.active() instanceof VoidKeysCosmetic keys ? keys : null;
	}

	static VoidKeysMotion.Score score() {
		if (score == null) {
			String text = "";
			try (InputStream stream = VoidKeysCosmetic.class.getResourceAsStream(SCORE)) {
				if (stream != null) text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
			} catch (Exception e) {
				FreedomClient.LOGGER.warn("Could not read the Void Keys score", e);
			}
			score = new VoidKeysMotion.Score(text, SONG_LENGTH);
			FreedomClient.LOGGER.info("[voidkeys] score loaded: {} notes", score.notes());
		}
		return score;
	}

	// ------------------------------------------------------------------------------------------------
	// Pose del jugador (la llaman los mixins de AvatarRenderer y PlayerModel).
	// ------------------------------------------------------------------------------------------------

	/** Al sacar el estado de render de un jugador: si eres tú con Void Keys flotando o tocando, calcula tu pose. */
	public static void extract(Avatar entity, AvatarRenderState state) {
		Minecraft client = Minecraft.getInstance();
		VoidKeysCosmetic keys = activeKeys();
		LocalPlayer player = client.player;
		if (keys == null || player == null || entity != player) {
			FRAMES.remove(state);
			return;
		}
		boolean playing = GuitarEmote.isPlaying();
		boolean canFloat = keys.floating.get() && !client.options.getCameraType().isFirstPerson() && !state.isFallFlying
				&& !state.isVisuallySwimming && !state.isPassenger && !state.isCrouching
				&& !state.hasPose(net.minecraft.world.entity.Pose.SLEEPING) && player.isAlive();
		if (!playing) wasPlaying = false;
		if (!playing && !canFloat) {
			FRAMES.remove(state);
			return;
		}
		double t = state.ageInTicks / 20.0;
		long now = System.nanoTime();
		double dt = lastFrame == 0 ? 0.05 : Math.min(0.1, (now - lastFrame) / 1.0E9);
		lastFrame = now;
		VoidKeysMotion.Pose pose;
		double songT = 0;
		if (playing) {
			if (!wasPlaying) {
				// Empieza mirando hacia donde mirabas (giro del cuerpo en el sentido del diseño).
				playYawStart = -Math.toRadians(state.bodyRot);
				wasPlaying = true;
			}
			double seconds = GuitarEmote.seconds();
			songT = seconds % SONG_LENGTH;
			double in = smooth(Math.min(1, seconds / VoidKeysMotion.PLAY_BLEND));
			VoidKeysMotion.Play play = score().play(songT, new VoidKeysMotion.Play());
			pose = VoidKeysMotion.figurePose(t, true, 0, 0, play, in);
			// El cuerpo mira hacia donde suenan las notas, con el giro del aro; al empezar se gira despacio.
			double target = play.facing + 2 * Math.PI * t / VoidKeysMotion.SPIN;
			double yaw = playYawStart + Math.atan2(Math.sin(target - playYawStart), Math.cos(target - playYawStart)) * in;
			state.bodyRot = (float) -Math.toDegrees(yaw);
			state.yRot = 0.0F;
		} else {
			// Velocidad: 1 andando (0,216 bloques por tick), 1,4 corriendo; suavizada como en el diseño.
			double moved = Math.hypot(player.getX() - player.xo, player.getZ() - player.zo) / 0.2158;
			speed += (Math.min(1.4, moved) - speed) * Math.min(1, dt * 4);
			double vy = player.onGround() ? 0 : player.getDeltaMovement().y;
			pose = testPose != null ? testPose : VoidKeysMotion.figurePose(t, true, speed, vy, null, 0);
		}
		FRAMES.put(state, new Frame(pose, t, playing, songT));
	}

	/** La pose de este estado (tu jugador en el juego, nunca la vista previa del menú), o null. */
	public static Frame frameOf(AvatarRenderState state) {
		return CosmeticPreview.of(state) != null ? null : FRAMES.get(state);
	}

	/**
	 * En AvatarRenderer.setupRotations (ya girado hacia donde mira el cuerpo, y hacia arriba): sube el cuerpo y lo
	 * inclina y mece sobre su centro. En este espacio el jugador mira a -z (en el diseño, a +z): el cabeceo y el
	 * balanceo cambian de signo.
	 */
	public static void applyFloat(AvatarRenderState state, PoseStack poseStack) {
		Frame frame = frameOf(state);
		if (frame != null) floatTransform(frame.pose(), poseStack);
	}

	/** Lo que hace {@link #applyFloat}: subir el cuerpo, inclinarlo y mecerlo sobre su centro. */
	public static void floatTransform(VoidKeysMotion.Pose p, PoseStack poseStack) {
		poseStack.translate(0.0F, (float) (CENTER + p.lift) * MODEL_PX, 0.0F);
		poseStack.mulPose(Axis.YP.rotation((float) p.yaw));
		poseStack.mulPose(Axis.XP.rotation((float) -p.pitch));
		poseStack.mulPose(Axis.ZP.rotation((float) -p.roll));
		poseStack.translate(0.0F, -CENTER * MODEL_PX, 0.0F);
	}

	/**
	 * Del espacio del modelo (y hacia abajo, origen 1,501 bloques sobre los pies) al del aro en el diseño: los pies en
	 * el origen, y hacia arriba, sin lo que flota e inclina el cuerpo ({@code p}, o null) ni el giro del cuerpo.
	 */
	public static void ringFrame(VoidKeysMotion.Pose p, float bodyRot, PoseStack poseStack) {
		poseStack.translate(0.0F, MODEL_FEET, 0.0F);
		poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
		if (p != null) {
			poseStack.translate(0.0F, CENTER / 16.0F, 0.0F);
			poseStack.mulPose(Axis.ZP.rotation((float) -p.roll));
			poseStack.mulPose(Axis.XP.rotation((float) -p.pitch));
			poseStack.mulPose(Axis.YP.rotation((float) -p.yaw));
			poseStack.translate(0.0F, (float) -(CENTER + p.lift) / 16.0F, 0.0F);
		}
		// El mundo es Ry(-bodyRot) del diseño: se deshace para que el aro no gire con el cuerpo.
		poseStack.mulPose(Axis.YP.rotationDegrees(bodyRot));
	}

	private static final Quaternionf PART = new Quaternionf();
	private static final Vector3f EULER = new Vector3f();

	/**
	 * Gira una parte como en el diseño: allí el giro es [x, y, z] en orden XYZ con y hacia arriba; en el modelo (y
	 * hacia abajo, z al revés) es Rx(x)·Ry(-y)·Rz(-z), y Minecraft lo aplica como ángulos ZYX.
	 */
	private static void setPart(ModelPart part, double x, double y, double z) {
		PART.rotationXYZ((float) x, (float) -y, (float) -z).getEulerAnglesZYX(EULER);
		part.xRot = EULER.x;
		part.yRot = EULER.y;
		part.zRot = EULER.z;
	}

	/** Al final de PlayerModel.setupAnim: pone los brazos, las piernas y la cabeza de la pose. */
	public static void applyPose(PlayerModel model, Frame frame) {
		VoidKeysMotion.Pose p = frame.pose();
		// Brazos y piernas giran sobre los mismos pivotes que en el diseño (brazos en ±5, piernas en ±1,9).
		setPart(model.rightArm, p.rArmX, 0, p.rArmZ);
		setPart(model.leftArm, p.lArmX, 0, p.lArmZ);
		setPart(model.rightLeg, p.rLegX, 0, p.rLegZ);
		setPart(model.leftLeg, p.lLegX, 0, p.lLegZ);
		if (frame.playing()) {
			setPart(model.head, p.headX, p.headYaw, p.headZ);
		} else {
			// Flotando, la cabeza sigue mirando hacia donde miras, con la pose por encima.
			setPart(model.head, p.headX + model.head.xRot, -model.head.yRot, p.headZ);
		}
		model.body.xRot = 0.0F;
		model.body.yRot = 0.0F;
		model.body.zRot = 0.0F;
	}

	// ------------------------------------------------------------------------------------------------
	// El aro, las partículas y los destellos.
	// ------------------------------------------------------------------------------------------------

	private void build() {
		List<String[]> lines = new ArrayList<>();
		List<Object> palette = new ArrayList<>();
		List<String> glowing = new ArrayList<>();
		List<String> names = new ArrayList<>();
		InputStream stream = VoidKeysCosmetic.class.getResourceAsStream(MODEL);
		if (stream == null) {
			FreedomClient.LOGGER.warn("Missing Void Keys model {}", MODEL);
			return;
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) continue;
				String[] p = line.trim().split("\\s+");
				if (p[0].equals("palette")) {
					names.add(p[1]);
					palette.add((char) ('a' + names.size() - 1));
					palette.add(0xFF000000 | Integer.parseInt(p[2], 16));
					if (p[3].equals("1")) glowing.add(p[1]);
				} else {
					lines.add(p);
				}
			}
		} catch (Exception e) {
			FreedomClient.LOGGER.warn("Could not read Void Keys model {}", MODEL, e);
			return;
		}
		Vox.Palette pal = new Vox.Palette("void_keys", palette.toArray());
		whiteKey = new Vox.Shape(pal);
		whiteCap = new Vox.Shape(pal);
		whiteCapLit = new Vox.Shape(pal);
		whiteGlow = new Vox.Shape(pal);
		blackKey = new Vox.Shape(pal);
		blackGlow = new Vox.Shape(pal);
		streak = new Vox.Shape(pal);
		for (int i = 0; i < rocks.length; i++) rocks[i] = new Vox.Shape(pal);
		for (String[] p : lines) {
			String piece = p[0], color = p[1];
			char c = (char) ('a' + names.indexOf(color));
			float x = Float.parseFloat(p[2]), y = Float.parseFloat(p[3]), z = Float.parseFloat(p[4]);
			float w = Float.parseFloat(p[5]), h = Float.parseFloat(p[6]), d = Float.parseFloat(p[7]);
			boolean glow = glowing.contains(color);
			Vox.Shape shape = switch (piece) {
				case "white" -> color.equals("cap") || color.equals("capHi") ? whiteCap : glow ? whiteGlow : whiteKey;
				case "black" -> glow ? blackGlow : blackKey;
				case "streak" -> streak;
				default -> rocks[piece.charAt(4) - '0'];
			};
			shape.box(c, x, y, z, w, h, d);
			// La tapa encendida: las mismas cajas, en blanco que brilla.
			if (shape == whiteCap) whiteCapLit.box((char) ('a' + names.indexOf("lit")), x, y, z, w, h, d);
		}
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		if (whiteKey == null) {
			build();
			if (whiteKey == null) return;
		}
		boolean preview = CosmeticPreview.of(state) != null;
		Frame frame = frameOf(state);
		double t = preview || frame == null ? state.ageInTicks / 20.0 : frame.t();
		poseStack.pushPose();
		if (preview) {
			// En la tarjeta del menú, el aro más pequeño para que quepa.
			ringFrame(null, 0.0F, poseStack);
			poseStack.translate(0.0F, 12.0F / 16.0F, 0.0F);
			poseStack.scale(0.55F, 0.55F, 0.55F);
			poseStack.translate(0.0F, -12.0F / 16.0F, 0.0F);
		} else {
			// El aro no flota, ni se inclina, ni gira con el cuerpo: se deshace lo que hizo applyFloat y el giro.
			ringFrame(frame != null ? frame.pose() : null, state.bodyRot, poseStack);
		}
		drawKeys(poseStack, collector, light, t, frame);
		if (particles.get() && !preview) {
			drawParticles(poseStack, collector, light, t);
			if (frame != null && frame.playing()) drawBursts(poseStack, collector, light, t, frame.songT());
		}
		poseStack.popPose();
	}

	private final double[] pose = new double[5];

	private void drawKeys(PoseStack poseStack, SubmitNodeCollector collector, int light, double t, Frame frame) {
		boolean playing = frame != null && frame.playing();
		for (int key = 0; key < VoidKeysMotion.KEYS; key++) {
			VoidKeysMotion.keyPose(VoidKeysMotion.keyAngle(key), t, pose);
			double lit = 0;
			if (playing) {
				double hit = score().lastHit(key, frame.songT());
				if (!Double.isNaN(hit)) lit = VoidKeysMotion.keyLight(frame.songT() - hit);
			}
			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotation((float) pose[0]));
			poseStack.translate(0.0F, (float) (pose[1] - lit * 0.4) / 16.0F, 0.0F);
			poseStack.mulPose(Axis.XP.rotation((float) pose[2]));
			poseStack.translate(0.0F, 0.0F, (float) VoidKeysMotion.INNER / 16.0F);
			if (key < VoidKeysMotion.WHITES) {
				whiteKey.draw(poseStack, collector, light);
				if (lit > 0.15) whiteCapLit.drawGlow(poseStack, collector);
				else whiteCap.draw(poseStack, collector, light);
				whiteGlow.drawGlow(poseStack, collector);
			} else {
				blackKey.draw(poseStack, collector, light);
				blackGlow.drawGlow(poseStack, collector);
			}
			poseStack.popPose();
		}
	}

	private final double[] particle = new double[5];

	private void drawParticles(PoseStack poseStack, SubmitNodeCollector collector, int light, double t) {
		for (double[] p : VoidKeysMotion.STREAK_DATA) {
			VoidKeysMotion.particle(p, t, particle);
			if (particle[3] <= 0.02) continue;
			poseStack.pushPose();
			poseStack.translate((float) particle[0] / 16.0F, (float) particle[1] / 16.0F, (float) particle[2] / 16.0F);
			poseStack.scale(1.0F, (float) particle[3], 1.0F);
			streak.drawGlow(poseStack, collector);
			poseStack.popPose();
		}
		for (double[] p : VoidKeysMotion.ROCK_DATA) {
			VoidKeysMotion.particle(p, t, particle);
			if (particle[3] <= 0.02) continue;
			poseStack.pushPose();
			poseStack.translate((float) particle[0] / 16.0F, (float) particle[1] / 16.0F, (float) particle[2] / 16.0F);
			poseStack.mulPose(new Quaternionf().rotationAxis((float) particle[4], new Vector3f((float) p[VoidKeysMotion.AX], (float) p[VoidKeysMotion.AY],
					(float) p[VoidKeysMotion.AZ]).normalize()));
			float s = (float) particle[3];
			poseStack.scale(s, s, s);
			rocks[(int) p[VoidKeysMotion.SHAPE]].draw(poseStack, collector, light);
			poseStack.popPose();
		}
	}

	/**
	 * Al tocar una tecla salen hacia arriba unas líneas de luz y, en las notas fuertes, un trozo de terreno, como en
	 * el diseño (que frena su subida un 3 % por fotograma a 60 por segundo). Los números al azar salen de cada nota,
	 * así no hay que guardar nada.
	 */
	private void drawBursts(PoseStack poseStack, SubmitNodeCollector collector, int light, double t, double songT) {
		VoidKeysMotion.Score s = score();
		for (int i : s.notesBetween(songT - 1.6, songT + 1.0E-9)) {
			double age = songT - s.time(i), strength = s.strength(i);
			VoidKeysMotion.keyPose(VoidKeysMotion.keyAngle(s.key(i)), t - age, pose);
			int n = 2 + (int) Math.round(strength * 3);
			for (int j = 0; j < n; j++) {
				double life = 0.6 + rand(i, j, 3) * 0.4;
				if (age >= life) continue;
				double a = pose[0] + (rand(i, j, 0) - 0.5) * 0.2, rad = VoidKeysMotion.INNER + 1 + rand(i, j, 1) * 3;
				double rise = rising(10 + rand(i, j, 2) * 8, age);
				poseStack.pushPose();
				poseStack.translate((float) (Math.sin(a) * rad) / 16.0F, (float) (pose[1] + 0.5 + rise) / 16.0F, (float) (Math.cos(a) * rad) / 16.0F);
				poseStack.scale(1.0F, (float) (1.3 * (1 - age / life)), 1.0F);
				streak.drawGlow(poseStack, collector);
				poseStack.popPose();
			}
			if (strength > 0.55 && age < 1.6) {
				double rad = VoidKeysMotion.INNER + 2;
				poseStack.pushPose();
				poseStack.translate((float) (Math.sin(pose[0]) * rad) / 16.0F, (float) (pose[1] - 1 + rising(3.5, age)) / 16.0F,
						(float) (Math.cos(pose[0]) * rad) / 16.0F);
				poseStack.mulPose(Axis.YP.rotation((float) (age * 2)));
				poseStack.mulPose(Axis.XP.rotation((float) (age * 1.3)));
				float f = (float) (1 - age / 1.6);
				poseStack.scale(f, f, f);
				rocks[i % rocks.length].draw(poseStack, collector, light);
				poseStack.popPose();
			}
		}
	}

	/** Lo que sube algo que sale a {@code speed} px/s y frena un 3 % por fotograma a 60 por segundo. */
	private static double rising(double speed, double age) {
		double frames = age * 60;
		return speed / 60 * (1 - Math.pow(0.97, frames)) / 0.03;
	}

	/** Número al azar (0-1) fijo para la nota i, la partícula j y el dato k. */
	private static double rand(int i, int j, int k) {
		long h = i * 73856093L ^ j * 19349663L ^ k * 83492791L;
		h = (h ^ (h >>> 13)) * 0x5bd1e995L;
		h ^= h >>> 15;
		return (h & 0xFFFFFF) / (double) 0x1000000;
	}

	private static double smooth(double u) {
		u = Math.max(0, Math.min(1, u));
		return u * u * (3 - 2 * u);
	}
}
