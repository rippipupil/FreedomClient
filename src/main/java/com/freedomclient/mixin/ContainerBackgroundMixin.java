package com.freedomclient.mixin;

import com.freedomclient.mixin.accessor.AbstractContainerScreenAccessor;
import com.freedomclient.module.visual.InvModule;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * INV: en el inventario, los cofres y las cajas de shulker, en vez de la textura del juego se dibuja la ventana
 * propia (fondo, imagen, borde y casillas). Los cofres dibujan su textura en dos trozos: la ventana se dibuja
 * entera en el primero y el segundo se salta.
 */
@Mixin({InventoryScreen.class, ContainerScreen.class, ShulkerBoxScreen.class})
public abstract class ContainerBackgroundMixin {
	@Unique
	private boolean freedomclient$windowDrawn;

	@Inject(method = "renderBg", at = @At("HEAD"))
	private void freedomclient$beginBackground(GuiGraphics graphics, float delta, int mouseX, int mouseY, CallbackInfo ci) {
		freedomclient$windowDrawn = false;
	}

	@WrapOperation(method = "renderBg", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
	private void freedomclient$customWindow(GuiGraphics graphics, RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v,
			int width, int height, int textureWidth, int textureHeight, Operation<Void> original) {
		Screen screen = (Screen) (Object) this;
		InvModule module = InvModule.active(screen);
		if (module == null) {
			original.call(graphics, pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
			return;
		}
		if (freedomclient$windowDrawn) return;
		freedomclient$windowDrawn = true;
		AbstractContainerScreenAccessor window = (AbstractContainerScreenAccessor) this;
		module.drawWindow(graphics, window.freedomclient$getLeftPos(), window.freedomclient$getTopPos(), window.freedomclient$getImageWidth(),
				window.freedomclient$getImageHeight(), ((AbstractContainerScreen<?>) screen).getMenu().slots, screen instanceof InventoryScreen);
	}
}
