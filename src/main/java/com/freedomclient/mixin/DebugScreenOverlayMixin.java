package com.freedomclient.mixin;

import com.freedomclient.module.utility.CustomF3Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {
	/** Custom F3: con F3 abierto se dibuja la pantalla propia en lugar de la de vanilla. */
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void freedomclient$customF3(GuiGraphics graphics, CallbackInfo ci) {
		if (CustomF3Module.replacesVanilla()) {
			CustomF3Module.render(graphics);
			ci.cancel();
		}
	}
}
