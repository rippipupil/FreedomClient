package com.freedomclient.mixin;

import com.freedomclient.module.pvp.ViewModelModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
	@Shadow private ItemStack mainHandItem;
	@Shadow private ItemStack offHandItem;
	@Shadow private float mainHandHeight;
	@Shadow private float oMainHandHeight;
	@Shadow private float offHandHeight;
	@Shadow private float oOffHandHeight;

	/** ViewModel "No item lowering": el objeto nunca baja (ni al cambiar de objeto ni tras un golpe). */
	@Inject(method = "tick", at = @At("TAIL"))
	private void freedomclient$noLowering(CallbackInfo ci) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !ViewModelModule.noLowering()) return;
		mainHandItem = player.getMainHandItem();
		offHandItem = player.getOffhandItem();
		mainHandHeight = 1.0F;
		oMainHandHeight = 1.0F;
		offHandHeight = 1.0F;
		oOffHandHeight = 1.0F;
	}

	/** ViewModel "Hide offhand": no dibuja la mano izquierda en primera persona. */
	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void freedomclient$hideOffhand(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swingProgress,
			ItemStack stack, float equipProgress, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		if (hand == InteractionHand.OFF_HAND && ViewModelModule.hideOffhand()) ci.cancel();
	}

	/** Aplica la posición, rotación y tamaño de ViewModel (y la bajada del escudo) a cada mano. */
	@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER, ordinal = 0))
	private void freedomclient$viewModel(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swingProgress,
			ItemStack stack, float equipProgress, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		ViewModelModule.apply(poseStack, player, hand, stack);
	}
}
