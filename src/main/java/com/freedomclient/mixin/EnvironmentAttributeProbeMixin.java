package com.freedomclient.mixin;

import com.freedomclient.module.visual.CustomSkyModule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnvironmentAttributeProbe.class)
public class EnvironmentAttributeProbeMixin {
	/** Custom Sky: cambia los colores del cielo, la niebla, las nubes y el amanecer que lee la cámara. */
	@Inject(method = "getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;", at = @At("RETURN"), cancellable = true)
	private void freedomclient$customSky(EnvironmentAttribute<?> attribute, float partialTick, CallbackInfoReturnable<Object> cir) {
		Object themed = CustomSkyModule.override((EnvironmentAttributeProbe) (Object) this, attribute, cir.getReturnValue());
		if (themed != null) cir.setReturnValue(themed);
	}
}
