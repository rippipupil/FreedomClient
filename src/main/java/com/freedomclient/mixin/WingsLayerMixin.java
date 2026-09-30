package com.freedomclient.mixin;

import com.freedomclient.module.visual.HideArmorModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WingsLayer.class)
public class WingsLayerMixin {
	/** Hide Armor: sin élitra en la espalda de tu jugador si se ha elegido. */
	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
			at = @At("HEAD"), cancellable = true)
	private void freedomclient$hideElytra(PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidRenderState state, float yRot,
			float xRot, CallbackInfo ci) {
		if (HideArmorModule.hidesElytra(state)) ci.cancel();
	}
}
