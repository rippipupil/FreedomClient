package com.freedomclient.mixin;

import com.freedomclient.module.visual.CustomScreensModule;
import com.freedomclient.ui.scene.LoadingScreenRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dibuja la pantalla de carga de FreedomClient encima de la de Mojang, con los mismos fundidos. */
@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin {
	@Shadow
	@Final
	private boolean fadeIn;
	@Shadow
	private float currentProgress;
	@Shadow
	private long fadeOutStart;
	@Shadow
	private long fadeInStart;

	@Inject(method = "render", at = @At("TAIL"))
	private void freedomclient$drawLoadingScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!CustomScreensModule.loadingScreenEnabled()) return;

		long now = Util.getMillis();
		float fadeOut = fadeOutStart > -1L ? (now - fadeOutStart) / 1000.0F : -1.0F;
		float fadeInProgress = fadeInStart > -1L ? (now - fadeInStart) / 500.0F : -1.0F;
		float alpha;
		if (fadeOut >= 1.0F) {
			alpha = 1.0F - Mth.clamp(fadeOut - 1.0F, 0.0F, 1.0F);
		} else if (fadeIn) {
			alpha = Mth.clamp(fadeInProgress, 0.0F, 1.0F);
		} else {
			alpha = 1.0F;
		}
		if (alpha <= 0.0F) return;

		float barAlpha = fadeOut < 0 ? 1.0F : 1.0F - Mth.clamp(fadeOut, 0.0F, 1.0F);
		graphics.nextStratum();
		LoadingScreenRenderer.render(graphics, currentProgress, alpha, barAlpha);
	}
}
