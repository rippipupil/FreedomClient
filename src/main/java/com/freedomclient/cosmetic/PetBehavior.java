package com.freedomclient.cosmetic;

import com.freedomclient.hud.CombatTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Estado de ánimo de las mascotas, igual para todas:
 * saludan al entrar a un mundo, celebran tus kills, se duermen si estás AFK y se esconden con poca vida.
 */
public final class PetBehavior {
	public enum Mood { IDLE, WAVE, CELEBRATE, SLEEP, HIDE }

	private static final long WAVE_MS = 4000;
	private static final long CELEBRATE_MS = 2500;
	private static final long AFK_MS = 30_000;
	private static final float LOW_HEALTH = 6.0F;

	private static ClientLevel lastLevel;
	private static long joinedAt;
	private static long killAt;
	private static long lastActivityAt = System.currentTimeMillis();
	private static int lastKilledId = -1;
	private static double lastX, lastY, lastZ;
	private static float lastYaw, lastPitch;
	private static Mood mood = Mood.IDLE;
	private static long moodSince = System.currentTimeMillis();

	private PetBehavior() {
	}

	/** Se llama cada tick del cliente. */
	public static void tick(Minecraft client) {
		long now = System.currentTimeMillis();
		LocalPlayer player = client.player;
		if (client.level != lastLevel) {
			lastLevel = client.level;
			if (client.level != null) joinedAt = now;
			lastActivityAt = now;
		}
		if (player == null) return;

		// Actividad: moverse, mirar a otro lado o tener una pantalla abierta cuenta como no estar AFK.
		if (player.getX() != lastX || player.getY() != lastY || player.getZ() != lastZ
				|| player.getYRot() != lastYaw || player.getXRot() != lastPitch || client.screen != null || player.swinging) {
			lastActivityAt = now;
		}
		lastX = player.getX();
		lastY = player.getY();
		lastZ = player.getZ();
		lastYaw = player.getYRot();
		lastPitch = player.getXRot();

		// Kill: el último objetivo al que pegaste ha muerto.
		LivingEntity target = CombatTracker.getTarget(3000);
		if (target != null && target.isDeadOrDying() && target.getId() != lastKilledId) {
			lastKilledId = target.getId();
			killAt = now;
		}

		Mood next;
		if (player.isAlive() && player.getHealth() <= LOW_HEALTH && !player.isCreative()) next = Mood.HIDE;
		else if (now - killAt < CELEBRATE_MS) next = Mood.CELEBRATE;
		else if (now - joinedAt < WAVE_MS) next = Mood.WAVE;
		else if (now - lastActivityAt > AFK_MS) next = Mood.SLEEP;
		else next = Mood.IDLE;
		if (next != mood) {
			mood = next;
			moodSince = now;
		}
	}

	public static Mood mood() {
		return mood;
	}

	/** Segundos desde que empezó el estado de ánimo actual, para las animaciones. */
	public static float moodSeconds() {
		return (System.currentTimeMillis() - moodSince) / 1000.0F;
	}

	/** Para pruebas: fuerza un estado de ánimo durante un rato. */
	public static void forceMood(Mood forced) {
		long now = System.currentTimeMillis();
		// Se borran los demás estados para que el forzado no quede tapado por uno con más prioridad.
		joinedAt = 0;
		killAt = 0;
		lastActivityAt = now;
		switch (forced) {
			case WAVE -> joinedAt = now;
			case CELEBRATE -> killAt = now;
			case SLEEP -> lastActivityAt = now - AFK_MS - 1;
			default -> {
			}
		}
	}
}
