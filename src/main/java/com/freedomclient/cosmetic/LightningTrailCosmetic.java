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
 * Lightning Trail: al moverte dejas detrás el muro de energía de Neon (como en su habilidad): lenguas de energía
 * azul que suben ondulando sobre una base amarillo verdosa y brillante, con chispazos. El estilo clásico es un rayo
 * fino en zigzag con los colores de Neon.
 */
public class LightningTrailCosmetic extends CosmeticModule {
	public final NumberSetting length = add(new NumberSetting("Length", "How long the trail stays behind you.", 12, 6, 30, 1, " ticks"));
	public final NumberSetting thickness = add(new NumberSetting("Thickness", "How thick the lightning line is.", 0.045, 0.02, 0.1, 0.005));
	public final ModeSetting style = add(new ModeSetting("Style", "Energy wall: Neon's flowing blue wall. Lightning: a thin zigzag bolt.",
			"Energy wall", "Energy wall", "Lightning"));
	public final NumberSetting wallHeight = add(new NumberSetting("Wall height", "How tall the energy wall is.", 1.2, 0.4, 2.2, 0.1, " blocks"));
	private final RandomSource random = RandomSource.create();

	private Vec3 last;
	/** Distancia recorrida: decide el color de cada punto para que el degradado avance con el rayo. */
	private double travelled;
	private int zigzag;

	public LightningTrailCosmetic() {
		super("Lightning Trail", "Neon's energy wall behind you while you move: flowing blue energy over a glowing yellow-green base.",
				CosmeticSlot.TRAIL, false);
		thickness.visibleWhen(() -> style.is("Lightning"));
		wallHeight.visibleWhen(() -> style.is("Energy wall"));
	}

	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (client.level == null || player == null || player.isInvisible() || client.isPaused()) {
			last = null;
			return;
		}
		Vec3 now = player.position().add(0, player.getBbHeight() * 0.5, 0);
		if (last == null || last.distanceTo(now) > 4.0) {
			last = now;
			return;
		}
		Vec3 delta = now.subtract(last);
		double distance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
		if (distance < 0.03) {
			last = now;
			return;
		}
		if (style.is("Energy wall")) {
			energyWall(player, delta, distance);
			last = now;
			return;
		}
		// Perpendicular al movimiento (en horizontal) para hacer el zigzag.
		double side = distance > 0 ? 1.0 / distance : 0;
		double px = -delta.z * side;
		double pz = delta.x * side;
		int steps = (int) Math.ceil(distance / 0.07);
		for (int i = 0; i < steps; i++) {
			double t = (i + 1) / (double) steps;
			travelled += distance / steps;
			// Cada 0,25 bloques el rayo cambia de lado: un zigzag limpio en vez de ruido.
			int segment = (int) (travelled / 0.25);
			double within = travelled / 0.25 - segment;
			double offset = ((segment & 1) == 0 ? within : 1.0 - within) * 0.24 - 0.12;
			double x = last.x + delta.x * t + px * offset;
			double y = last.y + delta.y * t + offset * 0.4;
			double z = last.z + delta.z * t + pz * offset;
			NeonFx.dot(x, y, z, NeonFx.gradient(travelled * 0.6), thickness.getFloat(), length.getInt(), true);
		}
		if (++zigzag % 10 == 0) {
			NeonFx.discharge(last.x, last.y, last.z, 0.12F, 5);
		}
		last = now;
	}

	/**
	 * Muro de energía: cada pocos centímetros del camino sale una base brillante en el suelo y varias lenguas de
	 * energía a distintas alturas (amarillas abajo, azules arriba) que suben ondulando y se apagan.
	 */
	private void energyWall(LocalPlayer player, Vec3 delta, double distance) {
		double groundY = player.getY() + 0.02;
		int life = length.getInt() + 8;
		double height = wallHeight.get();
		int steps = (int) Math.ceil(distance / 0.11);
		for (int i = 0; i < steps; i++) {
			double t = (i + 1) / (double) steps;
			travelled += distance / steps;
			double x = last.x + delta.x * t + (random.nextDouble() - 0.5) * 0.08;
			double z = last.z + delta.z * t + (random.nextDouble() - 0.5) * 0.08;
			// Base: línea muy brillante pegada al suelo.
			NeonFlameParticle.spawn(x, groundY + 0.05, z, 0.22F, 0.0F, 0.004, life);
			// Lenguas de energía repartidas por la altura del muro.
			int tongues = 3 + random.nextInt(2);
			for (int k = 0; k < tongues; k++) {
				float level = random.nextFloat();
				double y = groundY + 0.1 + level * level * height;
				float size = 0.22F + random.nextFloat() * 0.2F + (float) height * 0.04F;
				NeonFlameParticle.spawn(x, y, z, size, 0.1F + level * 0.8F, 0.012 + random.nextDouble() * 0.02, life - random.nextInt(6));
			}
			// De vez en cuando una veta clara que sube por el muro.
			if (random.nextFloat() < 0.35F) {
				NeonFlameParticle.spawn(x, groundY + 0.15 + random.nextDouble() * height * 0.6, z, 0.18F + random.nextFloat() * 0.1F, -1.0F,
						0.02 + random.nextDouble() * 0.015, life / 2 + random.nextInt(6));
			}
		}
		if (++zigzag % 7 == 0) {
			NeonFx.discharge(last.x, groundY + 0.2 + random.nextDouble() * height * 0.7, last.z, 0.14F, 5);
		}
	}
}
