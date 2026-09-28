package com.freedomclient.mixin;

import com.freedomclient.ui.ThemedMenus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin {
	/** Listas (mundos, servidores, opciones) sobre un velo del color del tema. */
	@Inject(method = "renderListBackground", at = @At("HEAD"), cancellable = true)
	private void freedomclient$themedList(GuiGraphics graphics, CallbackInfo ci) {
		if (!ThemedMenus.active()) return;
		AbstractSelectionList<?> self = (AbstractSelectionList<?>) (Object) this;
		ThemedMenus.panel(graphics, self.getX(), self.getY(), self.getWidth(), self.getHeight());
		ci.cancel();
	}

	/** Líneas de arriba y abajo de la lista con el color del tema (degradado en Neon). */
	@Inject(method = "renderListSeparators", at = @At("HEAD"), cancellable = true)
	private void freedomclient$themedSeparators(GuiGraphics graphics, CallbackInfo ci) {
		if (!ThemedMenus.active()) return;
		AbstractSelectionList<?> self = (AbstractSelectionList<?>) (Object) this;
		ThemedMenus.separator(graphics, self.getX(), self.getY() - 1, self.getWidth());
		ThemedMenus.separator(graphics, self.getX(), self.getBottom(), self.getWidth());
		ci.cancel();
	}
}
