package com.freedomclient.cosmetic;

import com.freedomclient.particle.NeonFx;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Neon Steps: al andar dejas en el suelo dos líneas eléctricas finas, una por pie, con los colores de Neon. */
public class NeonStepsCosmetic extends CosmeticModule {
	public final NumberSetting length = add(new NumberSetting("Length", "How long the lines stay on the ground.", 26, 10, 60, 1, " ticks"));

	private Vec3 last;
	private double travelled;

	public NeonStepsCosmetic() {
		super("Neon Steps", "Two thin electric lines in Neon colours follow your feet while you walk.", CosmeticSlot.TRAIL, false);
	}

	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (client.level == null || player == null || player.isInvisible() || client.isPaused()) {
			last = null;
			return;
		}
		Vec3 now = player.position();
		if (last == null || last.distanceTo(now) > 4.0 || !player.onGround()) {
			last = now;
			return;
		}
		Vec3 delta = now.subtract(last);
		double distance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
		if (distance < 0.02) {
			last = now;
			return;
		}
		// Los dos pies: a cada lado de la dirección en la que andas.
		double px = -delta.z / distance * 0.15;
		double pz = delta.x / distance * 0.15;
		int steps = (int) Math.ceil(distance / 0.06);
		for (int i = 0; i < steps; i++) {
			double t = (i + 1) / (double) steps;
			travelled += distance / steps;
			double x = last.x + delta.x * t;
			double y = last.y + 0.03;
			double z = last.z + delta.z * t;
			// Cada línea recorre el degradado a su ritmo, desfasadas para que no sean iguales.
			NeonFx.dot(x + px, y, z + pz, NeonFx.gradient(travelled * 0.5), 0.028F, length.getInt(), true);
			NeonFx.dot(x - px, y, z - pz, NeonFx.gradient(travelled * 0.5 + 0.5), 0.028F, length.getInt(), true);
		}
		last = now;
	}
}
