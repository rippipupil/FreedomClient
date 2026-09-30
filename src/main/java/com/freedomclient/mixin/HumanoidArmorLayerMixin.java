package com.freedomclient.mixin;

import com.freedomclient.module.visual.HideArmorModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin {
	/** Hide Armor: se salta las piezas elegidas en tu jugador. */
	@Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
	private void freedomclient$hideArmor(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, EquipmentSlot slot, int light,
			HumanoidRenderState state, CallbackInfo ci) {
		if (HideArmorModule.hides(state, slot)) ci.cancel();
	}
}
