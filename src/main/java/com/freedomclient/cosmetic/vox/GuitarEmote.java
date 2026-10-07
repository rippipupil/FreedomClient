package com.freedomclient.cosmetic.vox;

import com.freedomclient.FreedomClient;
import com.freedomclient.particle.GlowParticle;
import com.freedomclient.particle.PixelParticles;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Input;

/**
 * Emote de tocar la guitarra (tecla X por defecto): la cámara pasa a tercera persona, la guitarra va a las manos, la
 * canción empieza desde el principio a volumen normal y los brazos rasguean al ritmo. Se acaba al moverte, saltar,
 * agacharte, atacar o usar algo, al recibir daño o al pulsar la tecla otra vez; entonces todo vuelve como estaba.
 */
public final class GuitarEmote {
	/** Ticks del principio en los que no se cancela (para no cortarlo con la propia pulsación). */
	private static final int GRACE_TICKS = 4;

	private static boolean playing;
	private static long startedAt;
	private static int ticks;
	private static float bpm = 110.0F;
	private static float motion = 1.0F;
	private static CameraType savedCamera;
	private static boolean keyWasDown;
	private static int lastBeat = -1;
	/** Tiempo de daño del tick anterior: si sube, te acaban de golpear. */
	private static int lastHurtTime;
	private static final RandomSource RANDOM = RandomSource.create();

	private GuitarEmote() {
	}

	public static boolean isPlaying() {
		return playing;
	}

	/** Si este estado de render es tu jugador. */
	public static boolean isLocal(AvatarRenderState state) {
		Minecraft client = Minecraft.getInstance();
		return client.player != null && state.id == client.player.getId();
	}

	/** Segundos desde que empezó a tocar (la canción empieza a la vez), o 0 si no está tocando. */
	public static double seconds() {
		return playing ? (System.currentTimeMillis() - startedAt) / 1000.0 : 0.0;
	}

	/** Pulsos desde que empezó (con decimales). */
	public static float beats() {
		if (!playing) return 0.0F;
		long ms = System.currentTimeMillis() - startedAt;
		return ms * bpm / 60_000.0F;
	}

	/** Rasgueo de -1 a 1: baja en cada pulso y sube entre pulsos. */
	public static float strum() {
		if (!playing) return 0.0F;
		float phase = beats() % 1.0F;
		return Mth.cos(phase * Mth.TWO_PI);
	}

	/** Cuánto se mueve el jugador con la guitarra que toca (ver {@link MusicGuitarCosmetic#motion()}). */
	public static float motion() {
		return motion;
	}

	public static void start(Minecraft client) {
		MusicGuitarCosmetic guitar = MusicGuitarCosmetic.active();
		LocalPlayer player = client.player;
		if (guitar == null || player == null || playing) return;
		playing = true;
		ticks = 0;
		lastBeat = -1;
		lastHurtTime = player.hurtTime;
		bpm = guitar.bpm();
		motion = guitar.motion();
		startedAt = System.currentTimeMillis();
		if (client.options.getCameraType().isFirstPerson()) {
			savedCamera = client.options.getCameraType();
			client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		}
		GuitarMusic.restart(client);
		if (guitar.songTitle() != null) player.displayClientMessage(Component.literal("♪ " + guitar.songTitle()), true);
		FreedomClient.LOGGER.info("[guitar] emote start ({}, {} bpm)", guitar.getName(), bpm);
	}

	public static void stop(Minecraft client, String reason) {
		if (!playing) return;
		playing = false;
		if (savedCamera != null) {
			client.options.setCameraType(savedCamera);
			savedCamera = null;
		}
		FreedomClient.LOGGER.info("[guitar] emote stop ({})", reason);
	}

	/** Llamado cada tick: la tecla, lo que corta el emote y las notas al ritmo. */
	public static void tick(Minecraft client) {
		MusicGuitarCosmetic guitar = MusicGuitarCosmetic.active();
		LocalPlayer player = client.player;
		boolean keyDown = guitar != null && guitar.playKey.isBound() && client.screen == null
				&& InputConstants.isKeyDown(client.getWindow(), guitar.playKey.get());
		boolean pressed = keyDown && !keyWasDown;
		keyWasDown = keyDown;

		if (!playing) {
			if (pressed && player != null && player.isAlive()) start(client);
			return;
		}
		ticks++;
		if (guitar == null || player == null || client.level == null || !player.isAlive()) {
			stop(client, "guitar or player gone");
			return;
		}
		if (pressed && ticks > GRACE_TICKS) {
			stop(client, "key");
			return;
		}
		if (ticks > GRACE_TICKS) {
			Input input = player.input.keyPresses;
			if (input.forward() || input.backward() || input.left() || input.right() || input.jump() || input.shift()) {
				stop(client, "moved");
				return;
			}
			if (client.options.keyAttack.isDown() || client.options.keyUse.isDown()) {
				stop(client, "attack or use");
				return;
			}
		}
		// Un golpe nuevo sube el tiempo de daño (aunque aún quedara del anterior).
		if (player.hurtTime > lastHurtTime) {
			stop(client, "hurt");
			return;
		}
		lastHurtTime = player.hurtTime;
		// En cada pulso salen un par de notas de la guitarra.
		int beat = (int) beats();
		if (beat != lastBeat) {
			lastBeat = beat;
			if (guitar.beatNotes()) spawnNotes(client, player);
		}
	}

	private static void spawnNotes(Minecraft client, LocalPlayer player) {
		float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
		// Delante del jugador, a la altura de la guitarra.
		double frontX = -Mth.sin(yaw);
		double frontZ = Mth.cos(yaw);
		for (int i = 0; i < 2; i++) {
			double side = (RANDOM.nextDouble() - 0.5) * 0.6;
			double x = player.getX() + frontX * 0.45 + frontZ * side;
			double y = player.getY() + 0.8 + RANDOM.nextDouble() * 0.3;
			double z = player.getZ() + frontZ * 0.45 - frontX * side;
			GlowParticle note = new GlowParticle(client.level, x, y, z, frontX * 0.02 + (RANDOM.nextDouble() - 0.5) * 0.02, 0.035,
					frontZ * 0.02 + (RANDOM.nextDouble() - 0.5) * 0.02, PixelParticles.sprite("note"), 0.08F, 0.11F, 26 + RANDOM.nextInt(12));
			client.particleEngine.add(note.thirdPersonOnly());
		}
	}
}
