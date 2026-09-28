package com.freedomclient.mixin.compat;

import com.freedomclient.module.utility.MouseTweaksModule;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yalter.mousetweaks.MouseButton;

/**
 * Mouse Tweaks se puede apagar desde el menú (hay servidores que no lo permiten): con el mod desactivado no
 * reacciona a los clics, arrastres ni a la rueda y el inventario funciona como en vanilla.
 */
@Pseudo
@Mixin(targets = "yalter.mousetweaks.Main", remap = false)
public class MouseTweaksMainMixin {
	@Inject(method = "onMouseClicked", at = @At("HEAD"), cancellable = true)
	private static void freedomclient$clicked(Screen screen, double x, double y, MouseButton button, CallbackInfoReturnable<Boolean> cir) {
		if (MouseTweaksModule.blocked()) cir.setReturnValue(false);
	}

	@Inject(method = "onMouseReleased", at = @At("HEAD"), cancellable = true)
	private static void freedomclient$released(Screen screen, double x, double y, MouseButton button, CallbackInfoReturnable<Boolean> cir) {
		if (MouseTweaksModule.blocked()) cir.setReturnValue(false);
	}

	@Inject(method = "onMouseDrag", at = @At("HEAD"), cancellable = true)
	private static void freedomclient$dragged(Screen screen, double x, double y, MouseButton button, CallbackInfoReturnable<Boolean> cir) {
		if (MouseTweaksModule.blocked()) cir.setReturnValue(false);
	}

	@Inject(method = "onMouseScrolled", at = @At("HEAD"), cancellable = true)
	private static void freedomclient$scrolled(Screen screen, double x, double y, double amount, CallbackInfoReturnable<Boolean> cir) {
		if (MouseTweaksModule.blocked()) cir.setReturnValue(false);
	}
}
