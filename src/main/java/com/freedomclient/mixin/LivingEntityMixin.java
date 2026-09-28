package com.freedomclient.mixin;

import com.freedomclient.module.pvp.ViewModelModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	/** ViewModel "Swing speed": cambia lo que dura la animación del golpe de tu propia mano. */
	@Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
	private void freedomclient$swingSpeed(CallbackInfoReturnable<Integer> cir) {
		if ((Object) this == Minecraft.getInstance().player) {
			cir.setReturnValue(ViewModelModule.swingDuration(cir.getReturnValueI()));
		}
	}
}
