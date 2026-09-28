package com.freedomclient.mixin;

import com.freedomclient.module.performance.GameOptimizerModule;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Límite de FPS con un menú abierto dentro del mundo (FPS Optimizer). */
@Mixin(FramerateLimitTracker.class)
public class FramerateLimitTrackerMixin {
	@Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
	private void freedomclient$limitMenus(CallbackInfoReturnable<Integer> cir) {
		int limit = GameOptimizerModule.limit(cir.getReturnValue());
		if (limit != cir.getReturnValue()) cir.setReturnValue(limit);
	}
}
