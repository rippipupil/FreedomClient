package com.freedomclient.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Sigue tus golpes para el combo (golpes seguidos sin recibir daño) y el alcance (distancia del último golpe).
 * Un golpe cuenta en el combo cuando el objetivo muestra la animación de daño justo después de atacarlo.
 */
public final class CombatTracker {
	private static final long COMBO_TIMEOUT_MS = 2000;
	private static final int HIT_CONFIRM_TICKS = 4;

	private static int combo;
	private static long lastHitAt;
	private static double lastReach = -1;
	private static long lastReachAt;

	private static LivingEntity pendingTarget;
	private static int pendingTicks;
	private static int lastPlayerHurtTime;

	private CombatTracker() {
	}

	/** Llamado al atacar a una entidad (evento de Fabric). */
	public static void onAttack(LocalPlayer player, Entity target, EntityHitResult hitResult) {
		Vec3 eye = player.getEyePosition();
		Vec3 hit = hitResult != null ? hitResult.getLocation() : closestPoint(target.getBoundingBox(), eye);
		lastReach = eye.distanceTo(hit);
		lastReachAt = System.currentTimeMillis();

		if (target instanceof LivingEntity living) {
			pendingTarget = living;
			pendingTicks = HIT_CONFIRM_TICKS;
		}
	}

	private static Vec3 closestPoint(AABB box, Vec3 point) {
		return new Vec3(
				Math.clamp(point.x, box.minX, box.maxX),
				Math.clamp(point.y, box.minY, box.maxY),
				Math.clamp(point.z, box.minZ, box.maxZ));
	}

	public static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			combo = 0;
			pendingTarget = null;
			return;
		}

		// Recibir daño corta el combo.
		if (player.hurtTime > lastPlayerHurtTime) combo = 0;
		lastPlayerHurtTime = player.hurtTime;

		if (pendingTarget != null) {
			if (pendingTarget.hurtTime > 0 && pendingTarget.hurtTime >= pendingTarget.hurtDuration - HIT_CONFIRM_TICKS) {
				combo++;
				lastHitAt = System.currentTimeMillis();
				pendingTarget = null;
			} else if (--pendingTicks <= 0) {
				pendingTarget = null;
			}
		}

		if (combo > 0 && System.currentTimeMillis() - lastHitAt > COMBO_TIMEOUT_MS) combo = 0;
	}

	public static int getCombo() {
		return combo;
	}

	/** Alcance del último golpe en bloques, o -1 si hace más de {@code maxAgeMs} que no golpeas. */
	public static double getReach(long maxAgeMs) {
		return System.currentTimeMillis() - lastReachAt <= maxAgeMs ? lastReach : -1;
	}
}
