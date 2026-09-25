package com.freedomclient.mixin;

import com.freedomclient.module.visual.VisualsModule;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.ClientLevelData.class)
public class ClientLevelDataMixin {
	/** Visuals: la hora que ves puede ser distinta de la del servidor. */
	@Inject(method = "getDayTime", at = @At("RETURN"), cancellable = true)
	private void freedomclient$dayTime(CallbackInfoReturnable<Long> cir) {
		long time = VisualsModule.dayTime(cir.getReturnValueJ());
		if (time != cir.getReturnValueJ()) cir.setReturnValue(time);
	}
}
