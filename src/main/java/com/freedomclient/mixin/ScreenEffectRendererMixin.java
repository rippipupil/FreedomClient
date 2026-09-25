package com.freedomclient.mixin;

import com.freedomclient.module.visual.TotemPopModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
	/** TotemPop: con tamaño 0 no se dibuja la animación del tótem en pantalla. */
	@Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true)
	private void freedomclient$hideTotem(PoseStack poseStack, float partialTick, SubmitNodeCollector collector, CallbackInfo ci) {
		if (TotemPopModule.animationScale() <= 0.0F) ci.cancel();
	}

	/** TotemPop: la animación del tótem más pequeña (se escala el mismo escalado que ya aplica vanilla). */
	@ModifyArg(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 0), index = 0)
	private float freedomclient$scaleX(float value) {
		return value * TotemPopModule.animationScale();
	}

	@ModifyArg(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 0), index = 1)
	private float freedomclient$scaleY(float value) {
		return value * TotemPopModule.animationScale();
	}

	@ModifyArg(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 0), index = 2)
	private float freedomclient$scaleZ(float value) {
		return value * TotemPopModule.animationScale();
	}
}
