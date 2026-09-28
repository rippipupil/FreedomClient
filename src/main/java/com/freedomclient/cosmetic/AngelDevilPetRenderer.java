package com.freedomclient.cosmetic;

import com.freedomclient.cosmetic.vox.Vox;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Mascota Angel Devil, fiel al personaje: pelo naranja rojizo largo y despeinado con el flequillo cayendo sobre los
 * ojos, mirada cansada de ojos granate, traje negro con camisa blanca y corbata negra, grandes alas blancas de
 * plumas, halo dorado flotando ladeado y su cucurucho de helado. Flota junto a tu hombro.
 */
public final class AngelDevilPetRenderer {
	private static final Vox.Palette PALETTE = new Vox.Palette("angel_devil_pet",
			's', 0xFFF4DCCB, 'S', 0xFFE2C2AE, 'h', 0xFFD2653A, 'H', 0xFFA8452A, 'y', 0xFFEB8A4E,
			'e', 0xFF8A2230, 'L', 0xFF3A1A1A, 'm', 0xFFC47A78, 'k', 0xFF1E1E24, 'K', 0xFF0F0F13,
			'w', 0xFFF4F2EE, 't', 0xFF101014, 'W', 0xFFD8D4CC, 'b', 0xFFFFFFFF, 'o', 0xFFA8A29A,
			'l', 0xFFE8E4DC, 'g', 0xFFF2D04A, 'c', 0xFFD8A060, 'i', 0xFFFFF8F0);
	/** Ala derecha (la columna 0 junto a la espalda): b = borde, w = blanco, l = claro, W = sombra, o = contorno. */
	private static final String[] WING = {
			"........bbbb",
			"......bbwwwo",
			".....bwwwwlo",
			"....bwwwwWlo",
			"...bwwwwWlo.",
			"..bwwwwWlo..",
			".bwwwwWlwo..",
			"bwwwwWlwo...",
			"wwwwWlwo....",
			"wwwWlwo.....",
			"wwWlwo......",
			"wWlo........",
			"Wlo.........",
	};

	private Vox.Shape body;
	private Vox.Shape head;
	private Vox.Shape sleepingFace;
	private Vox.Shape awakeFace;
	private Vox.Shape arm;
	private Vox.Shape wing;
	private Vox.Shape halo;
	private Vox.Shape cone;

	private void build() {
		body = new Vox.Shape(PALETTE)
				// Pantalón y zapatos negros.
				.box('k', -3.0F, -5.0F, -1.5F, 2.8F, 5.0F, 3.0F)
				.box('k', 0.2F, -5.0F, -1.5F, 2.8F, 5.0F, 3.0F)
				.box('K', -3.1F, -1.0F, -1.9F, 3.0F, 1.0F, 3.4F)
				.box('K', 0.1F, -1.0F, -1.9F, 3.0F, 1.0F, 3.4F)
				// Americana negra con la camisa blanca en V, la corbata y las solapas.
				.box('k', -3.0F, -11.0F, -1.6F, 6.0F, 6.2F, 3.2F)
				.box('w', -1.2F, -11.0F, -1.75F, 2.4F, 3.0F, 0.3F)
				.box('w', -0.6F, -8.0F, -1.75F, 1.2F, 1.0F, 0.3F)
				.box('t', -0.4F, -10.6F, -1.9F, 0.8F, 4.0F, 0.3F)
				.box('K', -1.8F, -11.0F, -1.85F, 0.6F, 3.4F, 0.3F)
				.box('K', 1.2F, -11.0F, -1.85F, 0.6F, 3.4F, 0.3F);
		head = new Vox.Shape(PALETTE)
				.box('s', -4.0F, -8.0F, -4.0F, 8, 8, 8)
				.box('S', -4.0F, -1.0F, -4.05F, 8.0F, 1.0F, 0.1F)
				// Pelo: casquete, melena larga por detrás y mechones a los lados hasta la mandíbula.
				.box('h', -4.4F, -8.5F, -4.4F, 8.8F, 2.6F, 8.8F)
				.box('h', -4.4F, -6.0F, 3.4F, 8.8F, 8.5F, 1.2F)
				.box('H', -4.1F, 2.5F, 3.6F, 8.2F, 1.5F, 1.0F)
				.box('h', -4.6F, -6.0F, -4.2F, 0.8F, 6.2F, 7.6F)
				.box('h', 3.8F, -6.0F, -4.2F, 0.8F, 6.2F, 7.6F)
				.box('H', -4.7F, 0.0F, -3.6F, 0.8F, 2.0F, 1.2F)
				.box('H', 3.9F, 0.0F, -3.4F, 0.8F, 2.6F, 1.2F)
				.box('y', -3.2F, -8.7F, -3.0F, 2.0F, 0.3F, 4.0F);
		// Flequillo despeinado delante de la cara, con mechones que caen sobre los ojos.
		head.art(new String[] {
				"hhhhhhhh",
				"hyhhhyhh",
				"hhhHhhhH",
				"h.hHh.hh",
				".h.h.hh.",
				"....h...",
		}, -4.0F, -8.0F, -4.5F, 0.4F);
		awakeFace = new Vox.Shape(PALETTE).art(new String[] {
				".LL..LL.",
				".ee..ee.",
				"........",
				"...mm...",
		}, -4.0F, -5.0F, -4.2F, 0.2F);
		sleepingFace = new Vox.Shape(PALETTE).art(new String[] {
				"........",
				".LL..LL.",
				"........",
				"...mm...",
		}, -4.0F, -5.0F, -4.2F, 0.2F);
		arm = new Vox.Shape(PALETTE)
				.box('k', -1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F)
				.box('w', -1.05F, 4.4F, -1.05F, 2.1F, 0.6F, 2.1F)
				.box('s', -0.9F, 5.0F, -0.9F, 1.8F, 1.0F, 1.8F);
		wing = new Vox.Shape(PALETTE).art(WING, 0.0F, 0.0F, 0.0F, 1.0F);
		halo = new Vox.Shape(PALETTE).ring('g', 0.0F, 0.0F, 0.0F, 3.9F, 2.9F, 0.8F);
		cone = new Vox.Shape(PALETTE).box('c', -0.5F, 0.0F, -0.5F, 1.0F, 1.8F, 1.0F).sphere('i', 0.0F, -0.4F, 0.0F, 0.9F);
	}

	public void render(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, PetCosmetic module) {
		Vec3 position = module.follower.modelPosition(state.x, state.y, state.z, state.bodyRot, state.scale, state.ageInTicks % 1.0F);
		if (position == null) return;
		if (body == null) build();
		float time = state.ageInTicks;
		PetBehavior.Mood mood = PetBehavior.mood();
		float moodTime = PetBehavior.moodSeconds();

		// Pose normal: flota, aletea despacio, mira un poco a los lados y sujeta el helado.
		float bob = Mth.sin(time * 0.1F) * 1.2F;
		float flap = 28.0F + Mth.sin(time * 0.09F) * 10.0F;
		float rightArm = Mth.sin(time * 0.1F) * 8.0F;
		float leftArm = -55.0F;
		float headTurn = Mth.sin(time * 0.03F) * 14.0F;
		float headTilt = 6.0F + Mth.sin(time * 0.05F) * 3.0F;
		float shake = 0.0F;
		boolean sleeping = false;
		boolean holdsCone = true;

		switch (mood) {
			case WAVE -> {
				rightArm = -155.0F + Mth.sin(moodTime * 12.0F) * 18.0F;
				headTurn = -15.0F;
			}
			case CELEBRATE -> {
				rightArm = -160.0F;
				leftArm = -160.0F;
				holdsCone = false;
				bob -= Math.abs(Mth.sin(moodTime * 9.0F)) * 3.0F;
				flap = 28.0F + Mth.sin(time * 0.6F) * 22.0F;
			}
			case SLEEP -> {
				headTilt = 24.0F;
				headTurn = 0.0F;
				flap = 8.0F;
				bob = Mth.sin(time * 0.05F) * 0.6F;
				rightArm = 0.0F;
				leftArm = 0.0F;
				holdsCone = false;
				sleeping = true;
			}
			case HIDE -> {
				headTilt = 16.0F;
				flap = 10.0F;
				shake = Mth.sin(time * 3.0F) * 0.3F;
				rightArm = -70.0F;
				leftArm = -70.0F;
				holdsCone = false;
			}
			default -> {
			}
		}

		poseStack.pushPose();
		poseStack.translate(position.x + shake / 16.0F, position.y + bob / 16.0F, position.z);
		float size = module.size.getFloat();
		poseStack.scale(size, size, size);
		body.draw(poseStack, collector, light);
		// Alas grandes a la espalda, abiertas y moviéndose despacio.
		for (int side = -1; side <= 1; side += 2) {
			poseStack.pushPose();
			poseStack.translate(side * 1.0F / 16.0F, -17.0F / 16.0F, 1.8F / 16.0F);
			if (side < 0) poseStack.scale(-1.0F, 1.0F, 1.0F);
			poseStack.mulPose(Axis.YP.rotationDegrees(-flap));
			wing.draw(poseStack, collector, light);
			poseStack.popPose();
		}
		for (int side = -1; side <= 1; side += 2) {
			poseStack.pushPose();
			poseStack.translate(side * 4.0F / 16.0F, -10.8F / 16.0F, 0.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(side < 0 ? rightArm : leftArm));
			poseStack.mulPose(Axis.ZP.rotationDegrees(side * 6.0F));
			arm.draw(poseStack, collector, light);
			if (side > 0 && holdsCone) {
				poseStack.translate(0.0F, 5.4F / 16.0F, -0.6F / 16.0F);
				poseStack.mulPose(Axis.XP.rotationDegrees(-leftArm - 20.0F));
				poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
				cone.draw(poseStack, collector, light);
			}
			poseStack.popPose();
		}
		poseStack.translate(0.0F, -11.0F / 16.0F, 0.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(headTurn));
		poseStack.mulPose(Axis.XP.rotationDegrees(headTilt));
		head.draw(poseStack, collector, light);
		(sleeping ? sleepingFace : awakeFace).draw(poseStack, collector, light);
		// Halo dorado flotando por encima, ladeado como en las imágenes, girando despacio.
		poseStack.translate(0.5F / 16.0F, (-10.5F + Mth.sin(time * 0.08F) * 0.4F) / 16.0F, 0.0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(-12.0F));
		poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F));
		poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.5F));
		halo.drawGlow(poseStack, collector);
		poseStack.popPose();
	}
}
