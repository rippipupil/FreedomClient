package com.freedomclient.mixin;

import com.freedomclient.hud.ClickTracker;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Inject(method = "onButton", at = @At("HEAD"))
	private void freedomclient$countClicks(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
		if (action == GLFW.GLFW_PRESS) {
			ClickTracker.onPress(info.button());
		}
	}
}
