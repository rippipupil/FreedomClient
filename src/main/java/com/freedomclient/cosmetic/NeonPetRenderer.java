package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import com.mojang.blaze3d.vertex.PoseStack;
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

/**
 * Mascota Neon al estilo Funko Pop pixel: cabeza grande y cuadrada con ojos negros, pelo azul con dos moños con
 * punta y las puntas amarillas, traje azul marino con rayas, guantes, zapatillas y la mochila con el punto cian.
 * La textura está en neon_pet.png (ver tools/make_cosmetics.py).
 */
public final class NeonPetRenderer {
	private static final Identifier TEXTURE = FreedomClient.id("textures/cosmetic/neon_pet.png");
	private static final Identifier SLEEP_TEXTURE = FreedomClient.id("textures/cosmetic/neon_pet_sleep.png");

	private final ModelPart root;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;

	public NeonPetRenderer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition parts = mesh.getRoot();
		// Coordenadas en píxeles con los pies en y = 0 (y negativo es hacia arriba) y la cara hacia -z.
		PartDefinition headPart = parts.addOrReplaceChild("head", CubeListBuilder.create()
						.texOffs(0, 0).addBox(-5.0F, -9.0F, -4.5F, 10, 9, 9)
						// Casquete de pelo por arriba y atrás, flequillo, melena y mechones a los lados.
						.texOffs(40, 0).addBox(-5.5F, -10.0F, -4.0F, 11, 3, 10)
						.texOffs(0, 20).addBox(-5.0F, -8.5F, -5.2F, 10, 2, 1)
						.texOffs(84, 0).addBox(-5.0F, -7.0F, 4.0F, 10, 7, 2)
						.texOffs(24, 20).addBox(-6.0F, -7.5F, -4.0F, 1, 5, 1)
						.texOffs(24, 20).addBox(5.0F, -7.5F, -4.0F, 1, 5, 1),
				PartPose.offset(0.0F, -9.0F, 0.0F));
		// Dos moños arriba con una punta cada uno, inclinados hacia fuera como en su peinado.
		for (int side = -1; side <= 1; side += 2) {
			PartDefinition bun = headPart.addOrReplaceChild(side < 0 ? "right_bun" : "left_bun", CubeListBuilder.create()
							.texOffs(84, 12).addBox(-1.5F, -3.0F, -1.5F, 3, 3, 3),
					PartPose.offsetAndRotation(side * 3.5F, -9.5F, 1.0F, -0.2F, 0.0F, side * 0.45F));
			bun.addOrReplaceChild("spike", CubeListBuilder.create().texOffs(100, 12).addBox(-0.5F, -3.0F, -0.5F, 1, 3, 1),
					PartPose.offsetAndRotation(side * 0.5F, -2.5F, 0.0F, 0.0F, 0.0F, side * 0.5F));
		}
		parts.addOrReplaceChild("body", CubeListBuilder.create()
						.texOffs(0, 28).addBox(-3.0F, 0.0F, -2.0F, 6, 5, 4)
						.texOffs(40, 20).addBox(-2.0F, 0.5F, 2.0F, 4, 3, 1),
				PartPose.offset(0.0F, -9.0F, 0.0F));
		parts.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 28).addBox(-2.0F, 0.0F, -1.0F, 2, 5, 2),
				PartPose.offset(-3.0F, -9.0F, 0.0F));
		parts.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 28).addBox(0.0F, 0.0F, -1.0F, 2, 5, 2),
				PartPose.offset(3.0F, -9.0F, 0.0F));
		parts.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 40).addBox(-3.0F, 0.0F, -1.5F, 3, 4, 3),
				PartPose.offset(0.0F, -4.0F, 0.0F));
		parts.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(12, 40).addBox(0.0F, 0.0F, -1.5F, 3, 4, 3),
				PartPose.offset(0.0F, -4.0F, 0.0F));

		root = LayerDefinition.create(mesh, 128, 64).bakeRoot();
		head = root.getChild("head");
		rightArm = root.getChild("right_arm");
		leftArm = root.getChild("left_arm");
	}

	public void render(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, NeonPetCosmetic module) {
		Vec3 position = module.follower.modelPosition(state.x, state.y, state.z, state.bodyRot, state.scale, state.ageInTicks % 1.0F);
		if (position == null) return;
		float time = state.ageInTicks;
		PetBehavior.Mood mood = PetBehavior.mood();
		float moodTime = PetBehavior.moodSeconds();

		// Flota, balancea la cabeza grande como un Funko y mueve un poco los brazos.
		float bob = Mth.sin(time * 0.12F) * 1.0F;
		rightArm.xRot = Mth.sin(time * 0.12F) * 0.2F;
		leftArm.xRot = -Mth.sin(time * 0.12F) * 0.2F;
		rightArm.zRot = 0.15F;
		leftArm.zRot = -0.15F;
		head.yRot = Mth.sin(time * 0.035F) * 0.3F;
		head.zRot = Mth.sin(time * 0.08F) * 0.06F;
		head.xRot = 0.0F;
		float shake = 0.0F;
		Identifier texture = TEXTURE;

		switch (mood) {
			case WAVE -> {
				rightArm.xRot = -2.8F;
				rightArm.zRot = 0.2F + Mth.sin(moodTime * 12.0F) * 0.35F;
				head.yRot = -0.3F;
			}
			case CELEBRATE -> {
				// Puño arriba y saltitos, como su pose.
				rightArm.xRot = -3.0F;
				rightArm.zRot = 0.25F;
				leftArm.xRot = -1.0F;
				bob -= Math.abs(Mth.sin(moodTime * 9.0F)) * 3.0F;
			}
			case SLEEP -> {
				head.xRot = 0.4F;
				head.yRot = 0.0F;
				bob = Mth.sin(time * 0.05F) * 0.5F;
				rightArm.xRot = 0.0F;
				leftArm.xRot = 0.0F;
				texture = SLEEP_TEXTURE;
			}
			case HIDE -> {
				head.xRot = 0.3F;
				shake = Mth.sin(time * 3.0F) * 0.3F;
				rightArm.xRot = -1.2F;
				leftArm.xRot = -1.2F;
			}
			default -> {
			}
		}

		poseStack.pushPose();
		poseStack.translate(position.x + shake / 16.0F, position.y + bob / 16.0F, position.z);
		float size = module.size.getFloat();
		poseStack.scale(size, size, size);
		collector.submitModelPart(root, poseStack, RenderTypes.entityCutoutNoCull(texture), light, OverlayTexture.NO_OVERLAY, null);
		poseStack.popPose();
	}
}
