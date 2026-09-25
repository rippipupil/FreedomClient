package com.freedomclient.mixin;

import com.freedomclient.module.performance.CullLeavesModule;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeavesBlock.class)
public class LeavesBlockMixin {
	/** Cull Leaves: no se dibujan las caras entre dos bloques de hojas, aunque las hojas sean transparentes. */
	@Inject(method = "skipRendering", at = @At("HEAD"), cancellable = true)
	private void freedomclient$cullLeaves(BlockState state, BlockState adjacent, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (CullLeavesModule.active() && adjacent.getBlock() instanceof LeavesBlock) {
			cir.setReturnValue(true);
		}
	}
}
