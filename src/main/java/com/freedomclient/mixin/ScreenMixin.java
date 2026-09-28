package com.freedomclient.mixin;

import com.freedomclient.ui.ThemedMenus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
	/** Fondo con el tema en los menús de vanilla (Client Screens → Other menus). */
	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void freedomclient$themedBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		Screen self = (Screen) (Object) this;
		if (ThemedMenus.themes(self)) {
			ThemedMenus.background(graphics, self.width, self.height);
			ci.cancel();
		}
	}

	@Inject(method = "renderMenuBackground(Lnet/minecraft/client/gui/GuiGraphics;IIII)V", at = @At("HEAD"), cancellable = true)
	private void freedomclient$themedMenuBackground(GuiGraphics graphics, int x, int y, int width, int height, CallbackInfo ci) {
		if (ThemedMenus.themes((Screen) (Object) this)) {
			ThemedMenus.panel(graphics, x, y, width, height);
			ci.cancel();
		}
	}
}
