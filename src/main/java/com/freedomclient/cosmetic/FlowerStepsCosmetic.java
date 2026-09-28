package com.freedomclient.cosmetic;

import com.freedomclient.particle.FlowerParticle;
import com.freedomclient.particle.GlowParticle;
import com.freedomclient.particle.PixelParticles;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Flores al andar: por donde pisas brotan flores finas "pixel 3D" que crecen, se mecen y desaparecen.
 * De muchos colores, o en Abyss Flowers azules oscuras con un rastro fino y tenebroso.
 */
public class FlowerStepsCosmetic extends CosmeticModule {
	private static final String[] COLORFUL = {"pink", "yellow", "blue", "purple", "white", "red", "orange"};
	private static final String[] ABYSS = {"abyss_blue", "abyss_dark"};

	private final boolean abyss;
	private final NumberSetting amount = add(new NumberSetting("Amount", "How many flowers grow as you walk.", 1, 0.5, 2, 0.25, "x"));
	private final NumberSetting duration = add(new NumberSetting("Duration", "How long the flowers stay.", 3, 1, 6, 0.5, "s"));
	private final RandomSource random = RandomSource.create();
	private double lastX;
	private double lastZ;
	private double distance;
	private boolean placed;

	protected FlowerStepsCosmetic(String name, String description, boolean abyss) {
		super(name, description, CosmeticSlot.TRAIL, false);
		this.abyss = abyss;
	}

	public FlowerStepsCosmetic() {
		this("Flower Steps", "Thin pixel 3D flowers of every color grow where you walk and fade away.", false);
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
		double dx = player.getX() - lastX;
		double dz = player.getZ() - lastZ;
		lastX = player.getX();
		lastZ = player.getZ();
		if (!player.onGround() || dx * dx + dz * dz > 4.0) return;
		distance += Math.sqrt(dx * dx + dz * dz);
		double step = 0.6 / amount.get();
		while (distance >= step) {
			distance -= step;
			// A un lado y otro del camino, un poco al azar.
			double angle = random.nextDouble() * Math.PI * 2;
			double radius = 0.1 + random.nextDouble() * 0.3;
			double x = player.getX() + Math.cos(angle) * radius;
			double z = player.getZ() + Math.sin(angle) * radius;
			String[] flowers = abyss ? ABYSS : COLORFUL;
			int lifetime = (int) (duration.get() * 20) + random.nextInt(10);
			float size = 0.12F + random.nextFloat() * 0.05F;
			client.particleEngine.add(new FlowerParticle(client.level, x, player.getY() + 0.01, z, flowers[random.nextInt(flowers.length)], size, lifetime));
		}
		// Abyss: un hilo de niebla azul oscura que se queda detrás un momento.
		if (abyss && dx * dx + dz * dz > 0.0004 && random.nextFloat() < 0.8F) {
			GlowParticle wisp = new GlowParticle(client.level, player.getX() - dx * 2 + (random.nextDouble() - 0.5) * 0.15, player.getY() + 0.05,
					player.getZ() - dz * 2 + (random.nextDouble() - 0.5) * 0.15, 0.0, 0.004, 0.0, PixelParticles.sprite("soul_mist"), 0.06F, 0.1F,
					24 + random.nextInt(12));
			wisp.setColor(0.16F + random.nextFloat() * 0.1F, 0.22F, 0.55F + random.nextFloat() * 0.2F);
			client.particleEngine.add(wisp);
		}
	}
}
