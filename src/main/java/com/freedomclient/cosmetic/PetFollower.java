package com.freedomclient.cosmetic;

import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Hace que una mascota te siga con suavidad en vez de ir pegada al cuerpo: cada tick su posición en el mundo
 * se acerca poco a poco al sitio que le toca junto a tu hombro. Al dibujarla se pasa esa posición al espacio
 * del modelo del jugador (el mismo que usan las capas de render).
 */
public final class PetFollower {
	/** Escala del modelo del jugador en LivingEntityRenderer (AvatarRenderer usa 0.9375). */
	private static final float PLAYER_MODEL_SCALE = 0.9375F;
	/** Desplazamiento vertical que aplica LivingEntityRenderer antes de dibujar el modelo. */
	private static final float MODEL_Y_OFFSET = 1.501F;

	private Vec3 previous;
	private Vec3 current;

	/** Mueve la mascota hacia su sitio. {@code slot} está en el espacio del modelo del jugador, en bloques. */
	public void tick(LocalPlayer player, Vec3 slot, float followSpeed) {
		float scale = PLAYER_MODEL_SCALE * player.getScale();
		Vec3 target = player.position().add(modelToWorld(slot, player.yBodyRot, scale));
		if (current == null || current.distanceToSqr(target) > 64.0) {
			previous = target;
			current = target;
			return;
		}
		previous = current;
		current = current.lerp(target, followSpeed);
	}

	/** Posición de la mascota en el espacio del modelo del jugador, o null si todavía no se ha colocado. */
	public Vec3 modelPosition(double entityX, double entityY, double entityZ, float bodyRot, float entityScale, float partialTick) {
		if (current == null) return null;
		Vec3 world = previous.lerp(current, partialTick);
		return worldToModel(world.subtract(entityX, entityY, entityZ), bodyRot, PLAYER_MODEL_SCALE * entityScale);
	}

	public void reset() {
		previous = null;
		current = null;
	}

	private static Vec3 modelToWorld(Vec3 model, float bodyRot, float scale) {
		Vector3f v = new Vector3f((float) -model.x * scale, (float) -(model.y - MODEL_Y_OFFSET) * scale, (float) model.z * scale);
		Axis.YP.rotationDegrees(180.0F - bodyRot).transform(v);
		return new Vec3(v.x, v.y, v.z);
	}

	private static Vec3 worldToModel(Vec3 offset, float bodyRot, float scale) {
		Vector3f v = new Vector3f((float) offset.x, (float) offset.y, (float) offset.z);
		Axis.YP.rotationDegrees(-(180.0F - bodyRot)).transform(v);
		return new Vec3(-v.x / scale, -v.y / scale + MODEL_Y_OFFSET, v.z / scale);
	}
}
