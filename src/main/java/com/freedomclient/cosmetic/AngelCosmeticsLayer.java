package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Dibuja las alas y el halo 3D (modelos hechos de cubos) sobre el jugador. */
public class AngelCosmeticsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static final Identifier WINGS_TEXTURE = FreedomClient.id("textures/cosmetic/wings.png");
	private static final Identifier HALO_TEXTURE = FreedomClient.id("textures/cosmetic/halo.png");
	private static final int HALO_SEGMENTS = 16;

	private final ModelPart wings = createWings();
	private final ModelPart halo = createHalo();

	public AngelCosmeticsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	/** Dos alas planas (20x16x1) recortadas con la textura, unidas a la espalda. */
	private static ModelPart createWings() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("left", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -12.0F, 0.0F, 20, 16, 1),
				PartPose.offset(1.0F, 3.0F, 2.5F));
		root.addOrReplaceChild("right", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-20.0F, -12.0F, 0.0F, 20, 16, 1),
				PartPose.offset(-1.0F, 3.0F, 2.5F));
		return LayerDefinition.create(mesh, 64, 32).bakeRoot();
	}

	/** Anillo de pequeños cubos alrededor de un círculo. */
	private static ModelPart createHalo() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		float radius = 4.5F;
		for (int i = 0; i < HALO_SEGMENTS; i++) {
			float angle = (float) (i * Math.PI * 2 / HALO_SEGMENTS);
			root.addOrReplaceChild("segment" + i,
					CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -0.5F, -0.5F, 2.0F, 1.0F, 1.0F),
					PartPose.offsetAndRotation(Mth.cos(angle) * radius, 0.0F, Mth.sin(angle) * radius, 0.0F, -angle + (float) Math.PI / 2, 0.0F));
		}
		return LayerDefinition.create(mesh, 16, 16).bakeRoot();
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
		WingsCosmetic wingsModule = CosmeticModule.get(WingsCosmetic.class);
		if (wingsModule != null && wingsModule.shouldRender(state)) {
			renderWings(poseStack, collector, light, state, wingsModule);
		}

		HaloCosmetic haloModule = CosmeticModule.get(HaloCosmetic.class);
		if (haloModule != null && haloModule.shouldRender(state)) {
			renderHalo(poseStack, collector, light, state, haloModule);
		}
	}

	private void renderWings(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, WingsCosmetic module) {
		float time = state.ageInTicks;
		boolean fast = state.fallFlyingTimeInTicks > 0 || state.walkAnimationSpeed > 0.8F;
		float flap = module.flap.get() ? Mth.sin(time * (fast ? 0.45F : 0.12F)) * (fast ? 0.35F : 0.12F) : 0.0F;

		ModelPart left = wings.getChild("left");
		ModelPart right = wings.getChild("right");
		left.yRot = -0.45F - flap;
		left.zRot = -0.25F;
		right.yRot = 0.45F + flap;
		right.zRot = 0.25F;

		poseStack.pushPose();
		getParentModel().body.translateAndRotate(poseStack);
		float size = module.size.getFloat();
		poseStack.scale(size, size, size);
		collector.submitModelPart(wings, poseStack, RenderTypes.entityCutoutNoCull(WINGS_TEXTURE), light, OverlayTexture.NO_OVERLAY, null);
		poseStack.popPose();
	}

	private void renderHalo(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, HaloCosmetic module) {
		float time = state.ageInTicks;
		float bob = module.spin.get() ? Mth.sin(time * 0.08F) * 0.6F : 0.0F;
		halo.yRot = module.spin.get() ? time * 0.03F : 0.0F;
		halo.y = -8.0F - module.height.getFloat() - bob;

		poseStack.pushPose();
		getParentModel().head.translateAndRotate(poseStack);
		collector.submitModelPart(halo, poseStack, RenderTypes.entityCutoutNoCull(HALO_TEXTURE), light, OverlayTexture.NO_OVERLAY, null);
		poseStack.popPose();
	}
}
