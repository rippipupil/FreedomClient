package com.freedomclient.mixin;

import com.freedomclient.module.pvp.GapCounterModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Casillas de la hotbar: marca que se está dibujando la hotbar y, al acabar, pone el contador de Gap Counter. */
@Mixin(Gui.class)
public class GuiHotbarMixin {
	@Inject(method = "renderSlot", at = @At("HEAD"))
	private void freedomclient$beginSlot(GuiGraphics graphics, int x, int y, DeltaTracker delta, Player player, ItemStack stack, int seed, CallbackInfo ci) {
		GuiGraphicsItemCountMixin.hotbarSlot = true;
	}

	@Inject(method = "renderSlot", at = @At("RETURN"))
	private void freedomclient$endSlot(GuiGraphics graphics, int x, int y, DeltaTracker delta, Player player, ItemStack stack, int seed, CallbackInfo ci) {
		GuiGraphicsItemCountMixin.hotbarSlot = false;
		if (GapCounterModule.showsInHotbar(stack)) GapCounterModule.renderHotbarCount(graphics, stack, x, y);
	}
}
