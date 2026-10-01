package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.CosmeticModule;
import com.freedomclient.cosmetic.CosmeticSlot;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Cosmético "pixel 3D" hecho con {@link Vox}; AngelCosmeticsLayer dibuja todos los que estén activados. */
public abstract class VoxCosmetic extends CosmeticModule {
	protected VoxCosmetic(String name, String description, CosmeticSlot slot) {
		super(name, description, slot, false);
	}

	public abstract void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state);

	/** Coloca el sistema de coordenadas en la espalda en diagonal, como la Soul Scythe (pieza vertical, pivote en el centro). */
	protected static void onBackDiagonal(PlayerModel parent, PoseStack poseStack, AvatarRenderState state, float size, boolean left) {
		placeDiagonal(parent, poseStack, state, size, left, 3.6F + backClearance(state));
	}

	/**
	 * Igual, pero pegado a la espalda: {@code backDepth} es cuánto sobresale la pieza (en píxeles del modelo) hacia el
	 * cuerpo desde su plano central, así su cara de atrás queda justo sobre la espalda (o sobre el peto o los élitros).
	 */
	protected static void onBackDiagonal(PlayerModel parent, PoseStack poseStack, AvatarRenderState state, float size, boolean left, float backDepth) {
		placeDiagonal(parent, poseStack, state, size, left, 2.1F + backClearance(state) + backDepth * size);
	}

	private static void placeDiagonal(PlayerModel parent, PoseStack poseStack, AvatarRenderState state, float size, boolean left, float z) {
		parent.body.translateAndRotate(poseStack);
		poseStack.translate(0.0F, 5.5F / 16.0F, z / 16.0F);
		poseStack.scale(size, size, size);
		if (left) poseStack.scale(-1.0F, 1.0F, 1.0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(-32.0F));
	}
}
