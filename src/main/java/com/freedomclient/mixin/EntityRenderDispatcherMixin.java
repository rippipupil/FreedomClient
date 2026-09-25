package com.freedomclient.mixin;

import com.freedomclient.module.performance.EntityCullingModule;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
	/** Entity Culling: las entidades tapadas por bloques no se dibujan. */
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void freedomclient$cull(Entity entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
		if (EntityCullingModule.isCulled(entity)) {
			cir.setReturnValue(false);
		}
	}
}
