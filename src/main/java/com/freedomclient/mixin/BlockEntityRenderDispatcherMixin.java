package com.freedomclient.mixin;

import com.freedomclient.module.performance.EntityCullingModule;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
	/** Entity Culling: los cofres, carteles, etc. tapados no se preparan para dibujar (vanilla ya admite null aquí). */
	@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
	private void freedomclient$cull(BlockEntity blockEntity, float partialTick, ModelFeatureRenderer.CrumblingOverlay crumbling,
			CallbackInfoReturnable<BlockEntityRenderState> cir) {
		if (EntityCullingModule.isCulled(blockEntity)) {
			cir.setReturnValue(null);
		}
	}
}
