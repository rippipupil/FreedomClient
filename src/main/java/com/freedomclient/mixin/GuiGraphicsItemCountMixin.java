package com.freedomclient.mixin;

import com.freedomclient.module.pvp.GapCounterModule;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Quita el número blanco de las manzanas de oro en la hotbar cuando Gap Counter pone el suyo. */
@Mixin(GuiGraphics.class)
public class GuiGraphicsItemCountMixin {
	/** Solo en el hilo de render: true mientras Gui dibuja una casilla de la hotbar. */
	static boolean hotbarSlot;

	@Inject(method = "renderItemCount", at = @At("HEAD"), cancellable = true)
	private void freedomclient$hideGapCount(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
		if (hotbarSlot && text == null && GapCounterModule.showsInHotbar(stack)) ci.cancel();
	}
}
