package com.freedomclient.mixin;

import com.freedomclient.module.visual.FovController;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void freedomclient$modifyFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
		if (useFovSetting) {
			cir.setReturnValue(FovController.modifyFov(cir.getReturnValueF()));
		}
	}

	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void freedomclient$hideHandWhileZooming(float partialTick, boolean sleeping, Matrix4f projection, CallbackInfo ci) {
		if (FovController.shouldHideHand()) {
			ci.cancel();
		}
	}
}
