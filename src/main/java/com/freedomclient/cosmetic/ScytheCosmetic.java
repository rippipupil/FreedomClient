package com.freedomclient.cosmetic;

import com.freedomclient.particle.GlowParticle;
import com.freedomclient.particle.PixelParticles;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Soul Scythe: una guadaña pixel colgada en diagonal a la espalda, con un aura fantasmal gris opcional. */
public class ScytheCosmetic extends CosmeticModule {
	public final ModeSetting style = add(new ModeSetting("Style", "3D: thick pixel scythe with volume. Classic: flat pixel scythe.", "3D", "3D", "Classic"));
	public final BooleanSetting ghostAura = add(new BooleanSetting("Ghost aura", "A pale grey ghostly glow around the scythe and little spirits rising from it.", true));
	public final NumberSetting size = add(new NumberSetting("Size", "How big the scythe is.", 0.85, 0.4, 1.2, 0.05, "x"));
	public final ModeSetting side = add(new ModeSetting("Blade side", "Which shoulder the blade sticks out over.", "Left", "Left", "Right"));

	private final RandomSource random = RandomSource.create();

	public ScytheCosmetic() {
		super("Soul Scythe", "A dark pixel scythe with a silver blade hanging across your back, with an optional ghostly aura.", CosmeticSlot.BACK, false);
	}

	/** Espíritus grises que suben despacio desde la hoja y el mango (solo con el aura fantasmal y en tercera persona). */
	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (!ghostAura.get() || client.level == null || player == null || player.isInvisible() || client.isPaused()) return;
		if (client.options.getCameraType().isFirstPerson() || random.nextFloat() > 0.35F) return;

		// Ejes del jugador: hacia atrás y hacia su derecha según hacia dónde mira el cuerpo.
		float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
		double backX = Mth.sin(yaw), backZ = -Mth.cos(yaw);
		double rightX = -Mth.cos(yaw), rightZ = -Mth.sin(yaw);
		double bladeSide = side.is("Right") ? 1.0 : -1.0;
		double scale = size.get();
		// Un punto al azar de la guadaña: casi siempre en la hoja (arriba, sobre el hombro), a veces en el mango.
		double t = random.nextFloat();
		double lateral, height;
		if (random.nextFloat() < 0.65F) {
			lateral = (0.25 + random.nextDouble() * 0.45) * bladeSide;
			height = 1.35 + random.nextDouble() * 0.45;
		} else {
			lateral = Mth.lerp(t, -0.2, 0.3) * bladeSide;
			height = Mth.lerp(t, 0.55, 1.6);
		}
		lateral *= scale;
		height = 0.9 + (height - 0.9) * scale;
		double back = 0.3 + random.nextDouble() * 0.1;
		double x = player.getX() + rightX * lateral + backX * back;
		double y = player.getY() + height - (player.isCrouching() ? 0.3 : 0.0);
		double z = player.getZ() + rightZ * lateral + backZ * back;
		client.particleEngine.add(new GlowParticle(client.level, x, y, z,
				(random.nextDouble() - 0.5) * 0.01, 0.012 + random.nextDouble() * 0.01, (random.nextDouble() - 0.5) * 0.01,
				PixelParticles.sprite("soul_wisp"), 0.05F, 0.1F, 28 + random.nextInt(14)));
	}
}
