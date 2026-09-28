package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.PetBehavior;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;

/** Windy Bee (Bee Swarm): abeja cubo blanca de viento con ojos en espiral, alas planas y ráfagas que la rodean. */
public class WindyBeeCosmetic extends FollowPetCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("windy_bee",
			'w', 0xFFE6EEF2, 'W', 0xFFC8D4DC, 'e', 0xFF4A5C7A, 'x', 0xFF3E5064, 'X', 0xFF55687C, 'a', 0x88FFFFFF);
	private Vox.Shape body;
	private Vox.Shape wing;
	private Vox.Shape swirl;

	public WindyBeeCosmetic() {
		super("Windy Bee", "Bee Swarm: the white wind bee with spiral eyes, flat wings and gusts swirling around it. Does clean pirouettes.");
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
						"...ee...",
						"..e..e..",
						"...ee...",
				}, -4.0F, -7.2F, -4.4F, 0.4F);
		wing = new Vox.Shape(PALETTE).box('x', 0.0F, -0.2F, -2.0F, 10.0F, 0.4F, 4.0F).box('X', 0.0F, -0.25F, -2.0F, 10.0F, 0.1F, 0.8F);
		// Ráfagas de viento: tramos de un aro alrededor de la abeja y una voluta que sale por arriba.
		swirl = new Vox.Shape(PALETTE);
		for (int i = 0; i < 20; i++) {
			if (i % 10 > 6) continue;
			double angle = i * Math.PI * 2 / 20;
			swirl.box('a', (float) Math.cos(angle) * 6.2F - 0.6F, -4.5F + (i % 10) * 0.35F, (float) Math.sin(angle) * 6.2F - 0.6F, 1.2F, 0.6F, 1.2F);
		}
		swirl.box('a', -0.4F, -12.0F, 0.5F, 0.8F, 3.0F, 0.8F).box('a', 0.3F, -14.0F, 1.2F, 0.8F, 2.2F, 0.8F).box('a', 1.0F, -15.2F, 2.0F, 2.0F, 0.8F, 0.8F);
	}

	@Override
	protected void renderPet(PoseStack poseStack, SubmitNodeCollector collector, int light, float time, PetBehavior.Mood mood, float moodSeconds) {
		if (body == null) build();
		BeeMotion.apply(poseStack, time, mood, moodSeconds);
		body.draw(poseStack, collector, light);
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
