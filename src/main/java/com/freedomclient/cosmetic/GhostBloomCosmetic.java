package com.freedomclient.cosmetic;

import com.freedomclient.cosmetic.vox.GhostMotion;
import com.freedomclient.particle.BloomParticle;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;

/**
 * Ghost Bloom: al andar brotan flores blancas, rosas y rosa fuerte a un lado y otro del pie (una cada 0,6 bloques),
 * con un saltito, se mecen y al final se cierran; al brotar sueltan dos pétalos que suben dando vueltas. Como en el
 * diseño (scripts/textures/ghost/ghost.html): la mitad blancas, un 35 % rosas y el resto rosa fuerte.
 */
public class GhostBloomCosmetic extends CosmeticModule {
	private final NumberSetting amount = add(new NumberSetting("Amount", "How many flowers bloom as you walk.", 1, 0.5, 2, 0.25, "x"));
	private final NumberSetting duration = add(new NumberSetting("Duration", "How long the flowers stay.", 3, 1, 6, 0.5, "s"));
	private final RandomSource random = RandomSource.create();
	private double lastX, lastZ, distance;
	private boolean placed;

	public GhostBloomCosmetic() {
		super("Ghost Bloom", "White and pink pixel flowers bloom where you walk with a little hop, sway and close, "
				+ "letting petals fly up.", CosmeticSlot.TRAIL, false);
	}

	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (client.level == null || player == null || player.isInvisible() || client.isPaused()) return;
		if (!placed) {
			lastX = player.getX();
			lastZ = player.getZ();
			placed = true;
		}
		double dx = player.getX() - lastX, dz = player.getZ() - lastZ;
		lastX = player.getX();
		lastZ = player.getZ();
		if (!player.onGround() || dx * dx + dz * dz > 4.0) return;
		distance += Math.sqrt(dx * dx + dz * dz);
		double step = GhostMotion.FLOWER_STEP / amount.get();
		while (distance >= step) {
			distance -= step;
			// A un lado u otro del pie: de 1,6 a 6,4 px (0,1 a 0,4 bloques).
			double angle = random.nextDouble() * Math.PI * 2, radius = (1.6 + random.nextDouble() * 4.8) / 16.0;
			double r = random.nextDouble();
			String kind = r < 0.5 ? "white" : r < 0.85 ? "pink" : "sakura";
			double x = player.getX() + Math.cos(angle) * radius, z = player.getZ() + Math.sin(angle) * radius, y = player.getY() + 0.01;
			client.particleEngine.add(new BloomParticle(client.level, x, y, z, kind, random.nextDouble(), duration.get()));
			for (int k = 0; k < 2; k++) {
				client.particleEngine.add(new BloomParticle.Petal(client.level, x, y, z, kind, (random.nextDouble() - 0.5) * 6,
						(random.nextDouble() - 0.5) * 6, 3 + random.nextDouble() * 4));
			}
		}
	}
}
