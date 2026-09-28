package com.freedomclient.cosmetic.vox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;

/**
 * Niko (OneShot): Niko sentado en tu hombro con su sombrero de gato, la gabardina, la bufanda y el Sol en brazos.
 * Hace tonterías: ladea la cabeza, da saltitos, gira, se aplasta como un pancake, levanta el Sol y se tambalea.
 */
public class NikoPetCosmetic extends ShoulderPetCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("niko_pet",
			'n', 0xFF3B3052, 'H', 0xFF2A1F3A, 'c', 0xFF5A3128, 'C', 0xFF46251E, 'p', 0xFF5B3C9E,
			'y', 0xFFFFD447, 'k', 0xFF120C18, 'm', 0xFFE08AB0, 'K', 0xFF1C1620);
	private Vox.Shape body;
	private Vox.Shape head;
	private Vox.Shape arm;
	private Vox.Shape hat;
	private Vox.Shape sun;

	public NikoPetCosmetic() {
		super("Niko", "OneShot: Niko sitting on your shoulder with the Sun in their arms, being silly: head tilts, hops, spins, squishes and wobbles.");
	}

	private void build() {
		body = new Vox.Shape(PALETTE)
				.box('K', -2.4F, -1.8F, -3.4F, 1.8F, 1.8F, 3.6F)
				.box('K', 0.6F, -1.8F, -3.4F, 1.8F, 1.8F, 3.6F)
				// Gabardina que se abre abajo y la bufanda en el cuello.
				.box('c', -3.0F, -2.6F, -2.2F, 6.0F, 1.2F, 4.4F)
				.box('c', -2.5F, -6.6F, -1.8F, 5.0F, 4.2F, 3.6F)
				.box('C', -0.3F, -6.4F, -1.95F, 0.6F, 4.0F, 0.3F)
				.box('p', -2.8F, -7.4F, -2.1F, 5.6F, 1.1F, 4.2F)
				.box('p', 1.0F, -6.6F, 2.0F, 1.4F, 3.0F, 0.5F);
		head = new Vox.Shape(PALETTE)
				.box('n', -3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F)
				.box('H', -3.2F, -6.4F, -3.2F, 6.4F, 1.6F, 6.4F)
				.box('H', -3.2F, -5.0F, -3.3F, 1.2F, 1.4F, 0.4F)
				.box('H', 2.0F, -5.0F, -3.3F, 1.2F, 1.4F, 0.4F)
				.box('H', -3.2F, -5.0F, 2.2F, 6.4F, 4.2F, 1.0F)
				// Ojos grandes amarillos de gato con la pupila y una boquita.
				.box('y', -2.2F, -4.2F, -3.3F, 1.5F, 1.9F, 0.4F)
				.box('y', 0.7F, -4.2F, -3.3F, 1.5F, 1.9F, 0.4F)
				.box('k', -1.7F, -3.8F, -3.45F, 0.6F, 1.2F, 0.2F)
				.box('k', 1.2F, -3.8F, -3.45F, 0.6F, 1.2F, 0.2F)
				.box('m', -0.4F, -1.8F, -3.3F, 0.8F, 0.4F, 0.3F);
		arm = new Vox.Shape(PALETTE).box('c', -0.8F, 0.0F, -0.8F, 1.6F, 3.6F, 1.6F).box('n', -0.7F, 3.4F, -0.7F, 1.4F, 0.8F, 1.4F);
		hat = NikoHatCosmetic.build();
		sun = SunBackpackCosmetic.sun(1.8F);
	}

	@Override
	protected int emotes() {
		return 6;
	}

	@Override
	protected void renderPet(PoseStack poseStack, SubmitNodeCollector collector, int light, float time, int emote, float progress, boolean sleeping) {
		if (body == null) build();
		float wave = Mth.sin(progress * Mth.PI);
		float headTilt = sleeping ? 20.0F : Mth.sin(time * 0.06F) * 6.0F;
		float hop = 0.0F;
		float spin = 0.0F;
		float squish = 0.0F;
		float wobble = 0.0F;
		float sunUp = 0.0F;
		switch (emote) {
			case 0 -> headTilt = Mth.sin(progress * Mth.PI * 4.0F) * 30.0F * wave; // ladea la cabeza de lado a lado
			case 1 -> hop = Math.abs(Mth.sin(progress * Mth.PI * 4.0F)) * 2.2F; // saltitos
			case 2 -> spin = progress * 720.0F; // gira dos veces
			case 3 -> squish = wave; // pancake
			case 4 -> sunUp = wave; // levanta el Sol
			case 5 -> wobble = Mth.sin(progress * Mth.PI * 6.0F) * 16.0F * wave; // se tambalea
			default -> {
			}
		}
		poseStack.translate(0.0F, -hop / 16.0F, 0.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(spin));
		poseStack.mulPose(Axis.ZP.rotationDegrees(wobble));
		poseStack.scale(1.0F + squish * 0.35F, 1.0F - squish * 0.45F, 1.0F + squish * 0.35F);
		body.draw(poseStack, collector, light);
		// Brazos hacia delante sujetando el Sol (o levantándolo por encima de la cabeza).
		float armAngle = -70.0F - sunUp * 100.0F;
		for (int side = -1; side <= 1; side += 2) {
			poseStack.pushPose();
			poseStack.translate(side * 2.9F / 16.0F, -6.4F / 16.0F, 0.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(armAngle));
			poseStack.mulPose(Axis.ZP.rotationDegrees(side * -12.0F));
			arm.draw(poseStack, collector, light);
			poseStack.popPose();
		}
		poseStack.pushPose();
		poseStack.translate(0.0F, (-5.0F - sunUp * 9.0F) / 16.0F, (-4.2F + sunUp * 3.0F) / 16.0F);
		sun.drawGlow(poseStack, collector);
		poseStack.popPose();
		poseStack.translate(0.0F, -7.4F / 16.0F, 0.0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(headTilt));
		head.draw(poseStack, collector, light);
		poseStack.translate(0.0F, -5.6F / 16.0F, 0.0F);
		poseStack.scale(0.62F, 0.62F, 0.62F);
		hat.draw(poseStack, collector, light);
	}
}
