package com.freedomclient.mixin;

import com.freedomclient.module.visual.VisualsModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Visuals, física de objetos: los objetos tirados no flotan ni giran, y los planos quedan tumbados en el suelo. */
@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
	private static final String SUBMIT = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;"
			+ "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
			+ "Lnet/minecraft/client/renderer/state/CameraRenderState;)V";

	@Unique
	private ItemEntityRenderState freedomclient$state;

	@Inject(method = SUBMIT, at = @At("HEAD"))
	private void freedomclient$captureState(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera, CallbackInfo ci) {
		freedomclient$state = state;
	}

	/** Sin el balanceo arriba y abajo. */
	@Redirect(method = SUBMIT, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;sin(D)F"))
	private float freedomclient$noBob(double angle) {
		return VisualsModule.itemPhysics() ? -1.0F : Mth.sin(angle);
	}

	/** Sin girar: cada objeto se queda con un ángulo fijo (distinto para cada uno). */
	@Redirect(method = SUBMIT, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;getSpin(FF)F"))
	private float freedomclient$noSpin(float ageInTicks, float bobOffset) {
		return VisualsModule.itemPhysics() ? bobOffset : ItemEntity.getSpin(ageInTicks, bobOffset);
	}

	/** Los objetos planos se bajan hasta el suelo para quedar tumbados. */
	@ModifyArg(method = SUBMIT, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0), index = 1)
	private float freedomclient$lowerFlatItems(float y) {
		return VisualsModule.itemPhysics() && freedomclient$isFlat() ? 0.02F : y;
	}

	@Inject(method = SUBMIT, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V", shift = At.Shift.AFTER))
	private void freedomclient$layFlat(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera, CallbackInfo ci) {
		if (VisualsModule.itemPhysics() && freedomclient$isFlat()) {
			poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
		}
	}

	@Unique
	private boolean freedomclient$isFlat() {
		if (freedomclient$state == null) return false;
		AABB box = freedomclient$state.item.getModelBoundingBox();
		return box.maxZ - box.minZ < 0.1;
	}
}
