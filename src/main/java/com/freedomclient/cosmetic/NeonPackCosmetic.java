package com.freedomclient.cosmetic;

import com.freedomclient.particle.NeonFx;
import com.freedomclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Neon Pack: la mochila con la que Neon cataliza su energía, con el anillo cian brillante y rayos de vez en cuando. */
public class NeonPackCosmetic extends CosmeticModule {
	public final BooleanSetting sparks = add(new BooleanSetting("Lightning", "Little lightning bolts jump around the pack now and then (third person).", true));

	private final RandomSource random = RandomSource.create();
	private int nextSpark = 20;

	public NeonPackCosmetic() {
		super("Neon Pack", "Neon's energy pack on your back: copper plates, a glowing cyan ring and little lightning bolts.", CosmeticSlot.BACK, false);
	}

	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (!sparks.get() || client.level == null || player == null || player.isInvisible() || client.isPaused()) return;
		if (client.options.getCameraType().isFirstPerson() || --nextSpark > 0) return;
		nextSpark = 18 + random.nextInt(40);
		// Un chispazo de 2 o 3 rayitos por los bordes de la mochila.
		float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
		double backX = Mth.sin(yaw), backZ = -Mth.cos(yaw);
		double rightX = -Mth.cos(yaw), rightZ = -Mth.sin(yaw);
		double base = player.getY() + (player.isCrouching() ? 0.95 : 1.2);
		int count = 2 + random.nextInt(2);
		for (int i = 0; i < count; i++) {
			double lateral = (random.nextDouble() - 0.5) * 0.55;
			double height = base + (random.nextDouble() - 0.3) * 0.45;
			double back = 0.28 + random.nextDouble() * 0.12;
			double x = player.getX() + rightX * lateral + backX * back;
			double z = player.getZ() + rightZ * lateral + backZ * back;
			NeonFx.discharge(x, height, z, 0.09F + random.nextFloat() * 0.05F, 4 + random.nextInt(3));
		}
	}
}
