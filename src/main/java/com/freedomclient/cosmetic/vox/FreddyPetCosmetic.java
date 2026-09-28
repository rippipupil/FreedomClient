package com.freedomclient.cosmetic.vox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;

/**
 * Freddy (FNAF): peluche de Freddy sentado en tu hombro, con su sombrero de copa y pajarita. Hace emotes monos:
 * saludar, dar palmas, saltar, girar, bailar y ladear la cabeza.
 */
public class FreddyPetCosmetic extends ShoulderPetCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("freddy_pet",
			'b', 0xFF8A4E28, 'B', 0xFF6E3C1E, 'l', 0xFFC9884E, 'o', 0xFFE09A52, 'k', 0xFF141417,
			'w', 0xFFF4F4F4, 'u', 0xFF4A7AE0, 'd', 0xFF3A2010, 'g', 0xFF8C8E96);
	private Vox.Shape body;
	private Vox.Shape head;
	private Vox.Shape arm;
	private Vox.Shape hat;

	public FreddyPetCosmetic() {
		super("Freddy", "FNAF: a Freddy plush sitting on your shoulder with his top hat and bow tie. Waves, claps, hops, spins and dances.");
	}

	private void build() {
		body = new Vox.Shape(PALETTE)
				// Piernas sentado hacia delante con las almohadillas de los pies.
				.box('b', -3.2F, -2.4F, -4.0F, 2.6F, 2.4F, 4.5F)
				.box('b', 0.6F, -2.4F, -4.0F, 2.6F, 2.4F, 4.5F)
				.box('o', -3.0F, -2.2F, -4.3F, 2.2F, 2.0F, 0.4F)
				.box('o', 0.8F, -2.2F, -4.3F, 2.2F, 2.0F, 0.4F)
				.box('b', -3.0F, -7.0F, -2.0F, 6.0F, 5.2F, 4.0F)
				.box('l', -1.8F, -5.8F, -2.3F, 3.6F, 3.4F, 0.4F)
				// Pajarita.
				.box('k', -1.8F, -7.2F, -2.4F, 1.4F, 1.2F, 0.5F)
				.box('k', 0.4F, -7.2F, -2.4F, 1.4F, 1.2F, 0.5F)
				.box('g', -0.4F, -7.0F, -2.5F, 0.8F, 0.8F, 0.5F);
		head = new Vox.Shape(PALETTE)
				.box('b', -3.5F, -6.0F, -3.0F, 7.0F, 6.0F, 6.0F)
				.box('l', -2.0F, -2.8F, -4.2F, 4.0F, 2.4F, 1.4F)
				.box('k', -0.8F, -3.2F, -4.5F, 1.6F, 0.9F, 0.6F)
				.box('w', -2.8F, -4.6F, -3.3F, 1.8F, 1.8F, 0.4F)
				.box('w', 1.0F, -4.6F, -3.3F, 1.8F, 1.8F, 0.4F)
				.box('u', -2.3F, -4.2F, -3.5F, 1.0F, 1.0F, 0.3F)
				.box('u', 1.3F, -4.2F, -3.5F, 1.0F, 1.0F, 0.3F)
				.box('k', -2.0F, -3.9F, -3.6F, 0.5F, 0.5F, 0.2F)
				.box('k', 1.6F, -3.9F, -3.6F, 0.5F, 0.5F, 0.2F)
				.box('d', -3.0F, -5.2F, -3.25F, 2.0F, 0.4F, 0.3F)
				.box('d', 1.0F, -5.2F, -3.25F, 2.0F, 0.4F, 0.3F)
				// Orejas redondas con el interior naranja.
				.box('b', -4.0F, -7.6F, -0.8F, 2.4F, 2.4F, 1.4F)
				.box('b', 1.6F, -7.6F, -0.8F, 2.4F, 2.4F, 1.4F)
				.box('o', -3.5F, -7.1F, -1.0F, 1.4F, 1.4F, 0.3F)
				.box('o', 2.1F, -7.1F, -1.0F, 1.4F, 1.4F, 0.3F);
		arm = new Vox.Shape(PALETTE).box('b', -1.0F, 0.0F, -1.0F, 2.0F, 4.4F, 2.0F).box('o', -0.9F, 3.8F, -1.1F, 1.8F, 0.6F, 1.8F);
		hat = FreddyHatCosmetic.build(2.2F, 1.5F, 3);
	}

	@Override
	protected int emotes() {
		return 6;
	}

	@Override
	protected void renderPet(PoseStack poseStack, SubmitNodeCollector collector, int light, float time, int emote, float progress, boolean sleeping) {
		if (body == null) build();
		float wave = Mth.sin(progress * Mth.PI);
		float headTilt = sleeping ? 18.0F : Mth.sin(time * 0.05F) * 5.0F;
		float headTurn = sleeping ? 0.0F : Mth.sin(time * 0.03F) * 12.0F;
		float rightArm = 0.0F;
		float leftArm = 0.0F;
		float armsIn = 0.0F;
		float hop = 0.0F;
		float spin = 0.0F;
		float sway = 0.0F;
		switch (emote) {
			case 0 -> rightArm = -150.0F + Mth.sin(progress * 30.0F) * 25.0F * wave; // saludar
			case 1 -> armsIn = wave * (0.6F + 0.4F * Mth.sin(progress * 40.0F)); // palmas
			case 2 -> hop = Math.abs(Mth.sin(progress * Mth.PI * 3.0F)) * 2.5F; // saltitos
			case 3 -> spin = progress * 360.0F; // vuelta
			case 4 -> sway = Mth.sin(progress * Mth.PI * 4.0F) * 14.0F * wave; // baile
			case 5 -> headTilt = 28.0F * wave; // ladea la cabeza
			default -> {
			}
		}
		if (emote == 4) {
			rightArm = -60.0F * wave;
			leftArm = -60.0F * wave;
		}
		poseStack.translate(0.0F, -hop / 16.0F, 0.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(spin));
		poseStack.mulPose(Axis.ZP.rotationDegrees(sway));
		// Respira: sube y baja muy poco.
		float breath = sleeping ? Mth.sin(time * 0.06F) * 0.3F : Mth.sin(time * 0.1F) * 0.15F;
		poseStack.translate(0.0F, breath / 16.0F, 0.0F);
		body.draw(poseStack, collector, light);
		for (int side = -1; side <= 1; side += 2) {
			poseStack.pushPose();
			poseStack.translate(side * 3.6F / 16.0F, -6.6F / 16.0F, 0.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(side < 0 ? rightArm : leftArm));
			poseStack.mulPose(Axis.ZP.rotationDegrees(side * (12.0F - armsIn * 38.0F)));
			arm.draw(poseStack, collector, light);
			poseStack.popPose();
		}
		poseStack.translate(0.0F, -7.0F / 16.0F, 0.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(headTurn));
		poseStack.mulPose(Axis.ZP.rotationDegrees(headTilt));
		head.draw(poseStack, collector, light);
		poseStack.translate(0.6F / 16.0F, -6.0F / 16.0F, 0.0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(-8.0F));
		hat.draw(poseStack, collector, light);
	}
}
