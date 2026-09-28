package com.freedomclient.mixin;

import com.freedomclient.ui.ThemedMenus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin {
	/** Botones de los menús de vanilla con el estilo del cliente. */
	@Inject(method = "renderDefaultSprite", at = @At("HEAD"), cancellable = true)
	private void freedomclient$themedButton(GuiGraphics graphics, CallbackInfo ci) {
		if (!ThemedMenus.active()) return;
		AbstractButton self = (AbstractButton) (Object) this;
		ThemedMenus.button(graphics, self.getX(), self.getY(), self.getWidth(), self.getHeight(), self.active, self.isHoveredOrFocused(), self.getAlpha());
		ci.cancel();
	}
}
