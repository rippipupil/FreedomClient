package com.freedomclient.mixin;

import com.freedomclient.module.pvp.HealthIndicatorsModule;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
	/** Añade la vida al lado del nombre de los jugadores (Health Indicators). */
	@Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
	private void freedomclient$healthInName(Entity entity, CallbackInfoReturnable<Component> cir) {
		Component name = cir.getReturnValue();
		if (name != null) {
			Component modified = HealthIndicatorsModule.decorateName(entity, name);
			if (modified != name) cir.setReturnValue(modified);
		}
	}
}
