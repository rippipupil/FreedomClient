package com.freedomclient.mixin;

import com.freedomclient.module.pvp.ViewModelModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
	/** Aplica la posición, rotación y tamaño de ViewModel (y la bajada del escudo) a cada mano. */
	@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER, ordinal = 0))
	private void freedomclient$viewModel(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swingProgress,
			ItemStack stack, float equipProgress, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		ViewModelModule.apply(poseStack, player, hand, stack);
	}
}
