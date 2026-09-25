package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Mascota nube: una nubecita pixel con carita, alitas de plumas y un mini halo, que te sigue flotando. */
public final class CloudPetRenderer {
	private static final Identifier TEXTURE = FreedomClient.id("textures/cosmetic/cloud_pet.png");
	private static final Identifier SLEEP_TEXTURE = FreedomClient.id("textures/cosmetic/cloud_pet_sleep.png");
	private static final int STRIP_WING = 40;
	private static final int STRIP_WING_LIGHT = 44;
	private static final int STRIP_WING_OUTLINE = 52;
	private static final int STRIP_HALO = 56;
	/** Ala derecha de 7x6 (la columna 0 junto a la nube): b/w = blanco, l = claro, o = contorno. */
	private static final String[] WING = {
			".....bb",
			"...bbwo",
			"..bwwlo",
			".bwwlo.",
			"bwwlo..",
			"wlo....",
	};

	private final ModelPart root;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart halo;

	public CloudPetRenderer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition parts = mesh.getRoot();
		// En píxeles, con la base de la nube en y = 0 (y negativo es arriba) y la carita hacia -z.
		parts.addOrReplaceChild("cloud", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-5.0F, -6.0F, -3.0F, 10, 5, 6)
				.texOffs(0, 16).addBox(-4.0F, -8.0F, -2.0F, 4, 3, 4)
				.texOffs(16, 16).addBox(0.0F, -9.0F, -2.0F, 5, 4, 4)
				.texOffs(36, 0).addBox(-7.0F, -5.0F, -2.0F, 3, 3, 4)
				.texOffs(36, 8).addBox(4.0F, -5.0F, -2.0F, 3, 3, 4), PartPose.ZERO);
		parts.addOrReplaceChild("right_wing", wing(true), PartPose.offset(-3.0F, -9.0F, 3.0F));
		parts.addOrReplaceChild("left_wing", wing(false), PartPose.offset(3.0F, -9.0F, 3.0F));

		PartDefinition haloPart = parts.addOrReplaceChild("halo", CubeListBuilder.create(), PartPose.offset(1.0F, -12.0F, 0.0F));
		int segments = 8;
		float radius = 2.5F;
		for (int i = 0; i < segments; i++) {
			float angle = (float) (i * Math.PI * 2 / segments);
			haloPart.addOrReplaceChild("segment" + i,
					CubeListBuilder.create().texOffs(0, STRIP_HALO).addBox(-1.0F, -0.5F, -0.5F, 2, 1, 1),
					PartPose.offsetAndRotation(Mth.cos(angle) * radius, 0.0F, Mth.sin(angle) * radius, 0.0F, -angle + (float) Math.PI / 2, 0.0F));
		}

		root = LayerDefinition.create(mesh, 64, 64).bakeRoot();
		rightWing = root.getChild("right_wing");
		leftWing = root.getChild("left_wing");
		halo = root.getChild("halo");
	}

	private static CubeListBuilder wing(boolean right) {
		CubeListBuilder builder = CubeListBuilder.create();
		for (int row = 0; row < WING.length; row++) {
			String line = WING[row];
			for (int x = 0; x < line.length(); ) {
				char c = line.charAt(x);
				int end = x + 1;
				while (end < line.length() && line.charAt(end) == c) end++;
				if (c != '.') {
					int length = end - x;
					int strip = c == 'l' ? STRIP_WING_LIGHT : c == 'o' ? STRIP_WING_OUTLINE : STRIP_WING;
					builder.texOffs(0, strip).addBox(right ? -x - length : x, row, 0.0F, length, 1, 1);
				}
				x = end;
			}
		}
		return builder;
	}

	public void render(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, CloudPetCosmetic module) {
		Vec3 position = module.follower.modelPosition(state.x, state.y, state.z, state.bodyRot, state.scale, state.ageInTicks % 1.0F);
		if (position == null) return;
		float time = state.ageInTicks;
		float moodTime = PetBehavior.moodSeconds();

		float bob = Mth.sin(time * 0.08F) * 1.5F;
		float flap = 0.4F + Mth.sin(time * 0.35F) * 0.35F;
		float spin = Mth.sin(time * 0.04F) * 0.2F;
		float tilt = 0.0F;
		float shake = 0.0F;
		Identifier texture = TEXTURE;
		switch (PetBehavior.mood()) {
			case WAVE -> {
				// Saluda moviéndose de lado a lado y aleteando rápido.
				tilt = Mth.sin(moodTime * 10.0F) * 0.3F;
				flap = 0.4F + Mth.sin(time * 1.4F) * 0.5F;
			}
			case CELEBRATE -> {
				// Da vueltas y saltitos.
				spin = moodTime * 9.0F;
				bob -= Math.abs(Mth.sin(moodTime * 8.0F)) * 3.0F;
				flap = 0.4F + Mth.sin(time * 1.4F) * 0.5F;
			}
			case SLEEP -> {
				// Ojos cerrados, alas plegadas y respira despacio, un poco ladeada.
				texture = SLEEP_TEXTURE;
				flap = 0.05F;
				bob = Mth.sin(time * 0.04F) * 0.8F;
				tilt = 0.15F;
				spin = 0.0F;
			}
			case HIDE -> {
				flap = 0.1F;
				shake = Mth.sin(time * 3.0F) * 0.35F;
				spin = 0.0F;
			}
			default -> {
			}
		}
		rightWing.yRot = flap;
		leftWing.yRot = -flap;
		halo.yRot = time * 0.05F;

		poseStack.pushPose();
		poseStack.translate(position.x + shake / 16.0F, position.y + bob / 16.0F, position.z);
		float size = module.size.getFloat();
		poseStack.scale(size, size, size);
		poseStack.mulPose(Axis.YP.rotation(spin));
		poseStack.mulPose(Axis.ZP.rotation(tilt));
		collector.submitModelPart(root, poseStack, RenderTypes.entityCutoutNoCull(texture), light, OverlayTexture.NO_OVERLAY, null);
		poseStack.popPose();
	}
}
