package com.freedomclient.mixin;

import com.freedomclient.module.pvp.FreelookModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
	/** Con Freelook activo, el ratón mueve solo la cámara y el jugador sigue mirando hacia delante. */
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void freedomclient$freelook(double yaw, double pitch, CallbackInfo ci) {
		if ((Object) this == Minecraft.getInstance().player && FreelookModule.consumeTurn(yaw, pitch)) {
			ci.cancel();
		}
	}
}
