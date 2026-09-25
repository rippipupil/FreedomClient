package com.freedomclient.cosmetic;

import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class PetCosmetic extends CosmeticModule {
	public final ModeSetting side = add(new ModeSetting("Side", "Which shoulder the pet floats next to.", "Right", "Right", "Left"));
	public final NumberSetting size = add(new NumberSetting("Size", "Size of the pet.", 0.45, 0.3, 0.7, 0.05, "x"));
	public final PetFollower follower = new PetFollower();

	public PetCosmetic() {
		super("Angel Devil Pet", "A tiny Angel Devil that follows you, waves, celebrates your kills, naps when you are AFK and hides when you are low.");
	}

	@Override
	public void onTick(Minecraft client) {
		if (client.player == null) {
			follower.reset();
			return;
		}
		follower.tick(client.player, slot(side.is("Right") ? -1.0F : 1.0F), 0.2F);
	}

	/** Sitio de la mascota en el espacio del modelo del jugador (x negativo = derecha, y negativo = arriba). */
	static Vec3 slot(float side) {
		return switch (PetBehavior.mood()) {
			// Con poca vida se esconde detrás de ti, más abajo.
			case HIDE -> new Vec3(side * 5.0 / 16.0, 6.0 / 16.0, 9.0 / 16.0);
			// Dormida baja un poco.
			case SLEEP -> new Vec3(side * 14.0 / 16.0, 3.0 / 16.0, 2.0 / 16.0);
			default -> new Vec3(side * 14.0 / 16.0, -1.0 / 16.0, 2.0 / 16.0);
		};
	}
}
