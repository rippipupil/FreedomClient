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
	protected static void onBackDiagonal(PlayerModel parent, PoseStack poseStack, float size, boolean left) {
		parent.body.translateAndRotate(poseStack);
		poseStack.translate(0.0F, 5.5F / 16.0F, 3.6F / 16.0F);
		poseStack.scale(size, size, size);
		if (left) poseStack.scale(-1.0F, 1.0F, 1.0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(-32.0F));
	}
}
