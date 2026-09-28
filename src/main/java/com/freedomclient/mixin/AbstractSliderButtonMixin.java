package com.freedomclient.mixin;

import com.freedomclient.ui.ThemedMenus;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonMixin {
	/** Barra del deslizador con el estilo del cliente. */
	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
	private void freedomclient$track(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color) {
		AbstractSliderButton self = (AbstractSliderButton) (Object) this;
		if (ThemedMenus.active()) {
			ThemedMenus.button(graphics, x, y, width, height, self.active, false, self.getAlpha());
		} else {
			graphics.blitSprite(pipeline, sprite, x, y, width, height, color);
		}
	}

	/** Tirador del deslizador con el color de acento del tema. */
	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", ordinal = 1,
			target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
	private void freedomclient$handle(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color) {
		AbstractSliderButton self = (AbstractSliderButton) (Object) this;
		if (ThemedMenus.active()) {
			ThemedMenus.sliderHandle(graphics, x, y, width, height, self.isHoveredOrFocused(), self.getAlpha());
		} else {
			graphics.blitSprite(pipeline, sprite, x, y, width, height, color);
		}
	}
}
