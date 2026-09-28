package com.freedomclient.mixin;

import com.freedomclient.module.visual.InvModule;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** INV: color de los títulos (nombre del cofre, "Inventario", "Fabricación"). */
@Mixin({AbstractContainerScreen.class, InventoryScreen.class})
public class ContainerLabelsMixin {
	@ModifyArg(method = "renderLabels", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"),
			index = 4)
	private int freedomclient$labelColor(int color) {
		InvModule module = InvModule.active((Screen) (Object) this);
		return module == null ? color : module.textColor();
	}
}
