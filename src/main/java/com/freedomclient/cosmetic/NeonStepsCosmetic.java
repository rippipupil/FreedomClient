package com.freedomclient.cosmetic;

import com.freedomclient.particle.NeonFlameParticle;
import com.freedomclient.particle.NeonFx;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Neon Steps: al andar cada pie deja en el suelo una línea de energía amarillo verdosa muy brillante con llamitas de
 * energía azul que suben un poco, como el muro de Neon en pequeño. El estilo clásico son dos líneas eléctricas finas.
 */
public class NeonStepsCosmetic extends CosmeticModule {
	public final NumberSetting length = add(new NumberSetting("Length", "How long the lines stay on the ground.", 26, 10, 60, 1, " ticks"));
	public final ModeSetting style = add(new ModeSetting("Style", "Energy: glowing lines with little blue flames. Lines: two thin electric lines.",
			"Energy", "Energy", "Lines"));
	private final RandomSource random = RandomSource.create();

	private Vec3 last;
	private double travelled;

	public NeonStepsCosmetic() {
		super("Neon Steps", "Glowing Neon energy lines under your feet with little blue flames, like a mini energy wall.", CosmeticSlot.TRAIL, false);
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
			if (style.is("Energy")) {
				// Línea brillante en el suelo y, de vez en cuando, una llamita azul que sube.
				for (int foot = -1; foot <= 1; foot += 2) {
					double fx = x + px * foot;
					double fz = z + pz * foot;
					NeonFlameParticle.spawn(fx, y + 0.03, fz, 0.1F, 0.0F, 0.0, length.getInt());
					if (random.nextFloat() < 0.35F) {
						NeonFlameParticle.spawn(fx, y + 0.08, fz, 0.13F + random.nextFloat() * 0.08F, 0.35F + random.nextFloat() * 0.4F,
								0.01 + random.nextDouble() * 0.012, 10 + random.nextInt(8));
					}
				}
				continue;
			}
			// Cada línea recorre el degradado a su ritmo, desfasadas para que no sean iguales.
			NeonFx.dot(x + px, y, z + pz, NeonFx.gradient(travelled * 0.5), 0.028F, length.getInt(), true);
			NeonFx.dot(x - px, y, z - pz, NeonFx.gradient(travelled * 0.5 + 0.5), 0.028F, length.getInt(), true);
		}
		last = now;
	}
}
