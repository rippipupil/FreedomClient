package com.freedomclient.mixin;

import com.freedomclient.module.visual.FcNametagModule;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
	/** FC Nametag: tu propio nombre (con su degradado) también se ve en tercera persona. */
	@Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("RETURN"), cancellable = true)
	private void freedomclient$showOwnName(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && FcNametagModule.showOwnName(entity)) {
			cir.setReturnValue(true);
		}
	}
}
