package com.freedomclient.cosmetic;

import com.freedomclient.particle.NeonFx;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Lightning Trail: al moverte dejas detrás un rayo fino en zigzag con los colores de Neon mezclándose
 * (amarillo, lima, cian, azul y violeta) que se apaga enseguida.
 */
public class LightningTrailCosmetic extends CosmeticModule {
	public final NumberSetting length = add(new NumberSetting("Length", "How long the trail stays behind you.", 12, 6, 30, 1, " ticks"));
	public final NumberSetting thickness = add(new NumberSetting("Thickness", "How thick the lightning line is.", 0.045, 0.02, 0.1, 0.005));

	private Vec3 last;
	/** Distancia recorrida: decide el color de cada punto para que el degradado avance con el rayo. */
	private double travelled;
	private int zigzag;

	public LightningTrailCosmetic() {
		super("Lightning Trail", "A clean zigzag lightning trail in Neon colours behind you while you move.", CosmeticSlot.TRAIL, false);
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
}
