package com.freedomclient.mixin;

import com.freedomclient.module.visual.FcNametagModule;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
	/** Insignia FC delante de tu nombre en la lista de jugadores (Tab). */
	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
	private void freedomclient$fcBadge(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
		Component name = cir.getReturnValue();
		Component decorated = FcNametagModule.decorateTabName(info.getProfile().id(), name);
		if (decorated != name) cir.setReturnValue(decorated);
	}
}
