package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.PetBehavior;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;

/**
 * Windy Bee (Bee Swarm): abeja cubo blanca de viento con remolinos azulados pintados a los lados, ojos en espiral,
 * antenas rizadas, aguijón, alas planas, nubecitas esponjosas debajo y ráfagas que la rodean.
 */
public class WindyBeeCosmetic extends FollowPetCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("windy_bee",
			'w', 0xFFE6EEF2, 'W', 0xFFC8D4DC, 'e', 0xFF4A5C7A, 'E', 0xFF7FA6C8, 'x', 0xFF3E5064, 'X', 0xFF55687C,
			'a', 0x88FFFFFF, 'k', 0xFF2A3440, 'm', 0xFFE8A0B4, 'c', 0xC8F4F8FF);
	/** Remolino de viento pintado en cada lado (plano YZ, 8x8). */
	private static final String[] SWIRL = {
			"..EEEE..",
			".E....E.",
			"E..EE..E",
			"E.E..E.E",
			"E.E.EE.E",
			"E..E...E",
			".E....E.",
			"..EEE...",
	};
	private Vox.Shape body;
	private Vox.Shape wing;
	private Vox.Shape swirl;
	private Vox.Shape clouds;

	public WindyBeeCosmetic() {
		super("Windy Bee", "Bee Swarm: the white wind bee with spiral eyes, swirls on its sides, fluffy clouds and gusts around it. Does clean pirouettes.");
	}

	private void build() {
		body = new Vox.Shape(PALETTE)
				.box('w', -4.0F, -8.0F, -4.0F, 8, 8, 8)
				.box('W', -4.05F, -1.2F, -4.05F, 8.1F, 1.2F, 8.1F)
				.art(new String[] {
						".ee..ee.",
						"e.e.e.e.",
						".ee..ee.",
						"........",
						"m......m",
						"..k..k..",
						"...kk...",
				}, -4.0F, -7.2F, -4.4F, 0.4F)
				.side(SWIRL, 4.0F, -8.0F, -4.0F, 0.3F)
				.side(SWIRL, -4.3F, -8.0F, -4.0F, 0.3F)
				// Antenas rizadas hacia fuera.
				.box('k', -2.4F, -10.2F, -2.6F, 0.5F, 2.2F, 0.5F).box('k', -3.3F, -10.6F, -2.6F, 1.4F, 0.5F, 0.5F)
				.box('k', -3.3F, -10.2F, -2.6F, 0.5F, 0.5F, 0.5F)
				.box('k', 1.9F, -10.2F, -2.6F, 0.5F, 2.2F, 0.5F).box('k', 1.9F, -10.6F, -2.6F, 1.4F, 0.5F, 0.5F)
				.box('k', 2.8F, -10.2F, -2.6F, 0.5F, 0.5F, 0.5F)
				// Aguijón.
				.box('W', -0.6F, -4.6F, 4.0F, 1.2F, 1.2F, 1.0F).box('k', -0.3F, -4.3F, 5.0F, 0.6F, 0.6F, 0.8F);
		wing = new Vox.Shape(PALETTE).box('x', 0.0F, -0.2F, -2.0F, 10.0F, 0.4F, 4.0F).box('X', 0.0F, -0.25F, -2.0F, 10.0F, 0.1F, 0.8F);
		// Ráfagas de viento: tramos de un aro alrededor de la abeja y una voluta que sale por arriba.
		swirl = new Vox.Shape(PALETTE);
		for (int i = 0; i < 20; i++) {
			if (i % 10 > 6) continue;
			double angle = i * Math.PI * 2 / 20;
			swirl.box('a', (float) Math.cos(angle) * 6.2F - 0.6F, -4.5F + (i % 10) * 0.35F, (float) Math.sin(angle) * 6.2F - 0.6F, 1.2F, 0.6F, 1.2F);
		}
		swirl.box('a', -0.4F, -12.0F, 0.5F, 0.8F, 3.0F, 0.8F).box('a', 0.3F, -14.0F, 1.2F, 0.8F, 2.2F, 0.8F).box('a', 1.0F, -15.2F, 2.0F, 2.0F, 0.8F, 0.8F);
		// Nubecitas esponjosas debajo, como si flotara sobre una nube.
		clouds = new Vox.Shape(PALETTE)
				.sphere('c', -2.6F, 1.4F, 0.6F, 1.8F)
				.sphere('c', 1.9F, 1.6F, 1.0F, 2.0F)
				.sphere('c', -0.2F, 2.0F, -1.6F, 1.5F);
	}

	@Override
	protected void renderPet(PoseStack poseStack, SubmitNodeCollector collector, int light, float time, PetBehavior.Mood mood, float moodSeconds) {
		if (body == null) build();
		BeeMotion.apply(poseStack, time, mood, moodSeconds);
		body.draw(poseStack, collector, light);
		poseStack.pushPose();
		poseStack.translate(0.0F, Mth.sin(time * 0.1F) * 0.4F / 16.0F, 0.0F);
		clouds.drawTranslucent(poseStack, collector, light);
		poseStack.popPose();
		float flap = BeeMotion.flap(time, mood);
		for (int side = -1; side <= 1; side += 2) {
			poseStack.pushPose();
			poseStack.translate(side * 4.0F / 16.0F, -6.0F / 16.0F, 0.5F / 16.0F);
			if (side < 0) poseStack.scale(-1.0F, 1.0F, 1.0F);
			poseStack.mulPose(Axis.ZP.rotationDegrees(-flap));
			wing.draw(poseStack, collector, light);
			poseStack.popPose();
		}
		poseStack.pushPose();
		poseStack.translate(0.0F, -4.0F / 16.0F, 0.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(time * 6.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(14.0F + Mth.sin(time * 0.1F) * 6.0F));
		poseStack.translate(0.0F, 4.0F / 16.0F, 0.0F);
		swirl.drawTranslucent(poseStack, collector, light);
		poseStack.popPose();
	}
}
