package com.freedomclient.mixin;

import com.freedomclient.module.performance.NoBreakParticlesModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
	/** No Break Particles: sin trozos al romperse un bloque. */
	@Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
	private void freedomclient$noDestroyParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
		if (NoBreakParticlesModule.hidesBreaking()) ci.cancel();
	}

	/** No Break Particles: sin trocitos mientras se pica un bloque. */
	@Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
	private void freedomclient$noBreakingParticles(BlockPos pos, Direction side, CallbackInfo ci) {
		if (NoBreakParticlesModule.hidesMining()) ci.cancel();
	}
}
