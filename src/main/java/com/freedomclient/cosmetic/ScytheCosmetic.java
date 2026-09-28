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
	public final BooleanSetting ghostAura = add(new BooleanSetting("Ghost aura", "Pale grey ghostly mist around the blade and little spirits rising from the scythe (third person only).", true));
	public final NumberSetting size = add(new NumberSetting("Size", "How big the scythe is.", 0.85, 0.4, 1.2, 0.05, "x"));
	public final ModeSetting side = add(new ModeSetting("Blade side", "Which shoulder the blade sticks out over.", "Left", "Left", "Right"));

	private final RandomSource random = RandomSource.create();

	public ScytheCosmetic() {
		super("Soul Scythe", "A dark pixel scythe with a silver blade hanging across your back, with an optional ghostly aura.", CosmeticSlot.BACK, false);
	}

	/**
	 * Aura fantasmal: niebla gris muy suave alrededor de la hoja y espíritus que suben desde la guadaña.
	 * Solo en tercera persona: en primera persona no sale nada que tape la vista.
	 */
	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (!ghostAura.get() || client.level == null || player == null || player.isInvisible() || client.isPaused()) return;
		if (client.options.getCameraType().isFirstPerson()) return;

		boolean mist = random.nextFloat() < 0.35F;
		if (!mist && random.nextFloat() > 0.5F) return;
		// Ejes del jugador: hacia atrás y hacia su derecha según hacia dónde mira el cuerpo.
		float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
		double backX = Mth.sin(yaw), backZ = -Mth.cos(yaw);
		double rightX = -Mth.cos(yaw), rightZ = -Mth.sin(yaw);
		double bladeSide = side.is("Right") ? 1.0 : -1.0;
		double scale = size.get();
		// Un punto al azar: la niebla y casi todos los espíritus en la hoja (sobre el hombro), algunos en el mango.
		double lateral, height;
		if (mist || random.nextFloat() < 0.65F) {
			lateral = (0.25 + random.nextDouble() * 0.45) * bladeSide;
			height = 1.3 + random.nextDouble() * 0.5;
		} else {
			double t = random.nextDouble();
			lateral = Mth.lerp(t, -0.2, 0.3) * bladeSide;
			height = Mth.lerp(t, 0.55, 1.6);
		}
		lateral *= scale;
		height = 0.9 + (height - 0.9) * scale;
		double back = 0.3 + random.nextDouble() * 0.12;
		double x = player.getX() + rightX * lateral + backX * back;
		double y = player.getY() + height - (player.isCrouching() ? 0.3 : 0.0);
		double z = player.getZ() + rightZ * lateral + backZ * back;
		if (mist) {
			client.particleEngine.add(new GlowParticle(client.level, x, y, z,
					(random.nextDouble() - 0.5) * 0.006, 0.003, (random.nextDouble() - 0.5) * 0.006,
					PixelParticles.sprite("soul_mist"), 0.16F, 0.28F, 40 + random.nextInt(20)).thirdPersonOnly());
		} else {
			client.particleEngine.add(new GlowParticle(client.level, x, y, z,
					(random.nextDouble() - 0.5) * 0.01, 0.012 + random.nextDouble() * 0.01, (random.nextDouble() - 0.5) * 0.01,
					PixelParticles.sprite("soul_wisp"), 0.09F, 0.16F, 30 + random.nextInt(16)).thirdPersonOnly());
		}
	}
}
