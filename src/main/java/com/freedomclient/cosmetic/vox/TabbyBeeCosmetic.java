package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.PetBehavior;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;

/**
 * Tabby Bee (Bee Swarm): abeja gato atigrada. Vuela y hace piruetas como una abeja y además cosas de gato:
 * menea la cola, mueve las orejas y de vez en cuando se estira.
 */
public class TabbyBeeCosmetic extends FollowPetCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("tabby_bee",
			'o', 0xFFF2A33A, 'O', 0xFFE08A22, 'd', 0xFF5A3410, 'e', 0xFFF6C870, 'k', 0xFF1E140C, 'x', 0x99A8C8E0);
	private Vox.Shape body;
	private Vox.Shape ear;
	private Vox.Shape tail;
	private Vox.Shape wing;

	public TabbyBeeCosmetic() {
		super("Tabby Bee", "Bee Swarm: the tabby cat bee. Flies and does pirouettes like a bee, and wags its tail, twitches its ears and stretches like a cat.",
				"Right");
	}

	private void build() {
		body = new Vox.Shape(PALETTE)
				.box('o', -4.0F, -8.0F, -4.0F, 8, 8, 8)
				.box('d', -4.05F, -8.05F, 0.2F, 8.1F, 8.1F, 1.4F)
				.box('d', -4.05F, -8.05F, 2.4F, 8.1F, 8.1F, 1.2F)
				.art(new String[] {
						"..d.d.d.",
						"........",
						".kk..kk.",
						".kk..kk.",
						"...kk...",
						"..k..k..",
						"........",
				}, -4.0F, -7.6F, -4.4F, 0.4F);
		// Bigotes a los dos lados de la cara.
		for (int side = -1; side <= 1; side += 2) {
			float x = side < 0 ? -6.6F : 4.0F;
			body.box('k', x, -3.4F, -4.4F, 2.6F, 0.3F, 0.3F).box('k', x, -2.6F, -4.4F, 2.6F, 0.3F, 0.3F);
		}
		ear = new Vox.Shape(PALETTE)
				.box('e', -1.3F, -1.0F, -0.6F, 2.6F, 1.0F, 1.2F)
				.box('e', -0.9F, -2.0F, -0.6F, 1.8F, 1.0F, 1.2F)
				.box('e', -0.5F, -2.8F, -0.6F, 1.0F, 0.8F, 1.2F);
		tail = new Vox.Shape(PALETTE);
		for (int i = 0; i < 7; i++) tail.box(i == 6 ? 'O' : 'o', -0.7F, -i - 1.0F, -0.7F + i * 0.25F, 1.4F, 1.0F, 1.4F);
		wing = new Vox.Shape(PALETTE).box('x', 0.0F, -0.2F, -2.0F, 9.0F, 0.4F, 4.0F);
	}

	@Override
	protected void renderPet(PoseStack poseStack, SubmitNodeCollector collector, int light, float time, PetBehavior.Mood mood, float moodSeconds) {
		if (body == null) build();
		BeeMotion.apply(poseStack, time, mood, moodSeconds);
		// Estiramiento de gato cada ~9 s: se alarga hacia delante y se encoge de alto un momento.
		float stretchPhase = (time % 180.0F) / 180.0F;
		float stretch = stretchPhase > 0.85F ? Mth.sin((stretchPhase - 0.85F) / 0.15F * Mth.PI) : 0.0F;
		poseStack.pushPose();
		poseStack.translate(0.0F, -4.0F / 16.0F, 0.0F);
		poseStack.scale(1.0F, 1.0F - stretch * 0.15F, 1.0F + stretch * 0.2F);
		poseStack.translate(0.0F, 4.0F / 16.0F, 0.0F);
		body.draw(poseStack, collector, light);
		PetEmotes.blush(poseStack, collector, light, 2.6F, -3.0F, -4.45F);
		for (int side = -1; side <= 1; side += 2) {
			poseStack.pushPose();
			poseStack.translate(side * 2.5F / 16.0F, -8.0F / 16.0F, -2.8F / 16.0F);
			float twitch = (time % 97.0F) < 6.0F && side > 0 ? Mth.sin(time * 1.5F) * 18.0F : 0.0F;
			poseStack.mulPose(Axis.ZP.rotationDegrees(side * 10.0F + twitch));
			ear.draw(poseStack, collector, light);
			poseStack.popPose();
		}
		poseStack.pushPose();
		poseStack.translate(0.0F, -6.5F / 16.0F, 4.0F / 16.0F);
		poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.18F) * 22.0F));
		tail.draw(poseStack, collector, light);
		poseStack.popPose();
		poseStack.popPose();
		float flap = BeeMotion.flap(time, mood);
		for (int side = -1; side <= 1; side += 2) {
			poseStack.pushPose();
			poseStack.translate(side * 4.0F / 16.0F, -6.5F / 16.0F, 0.5F / 16.0F);
			if (side < 0) poseStack.scale(-1.0F, 1.0F, 1.0F);
			poseStack.mulPose(Axis.ZP.rotationDegrees(-flap));
			wing.drawTranslucent(poseStack, collector, light);
			poseStack.popPose();
		}
	}
}
