package com.freedomclient.mixin;

import com.freedomclient.module.utility.ChatHeadsModule;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
	/** Chat Heads: añade la cara del autor delante del mensaje antes de guardarlo en el chat. */
	@ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
			at = @At("HEAD"), argsOnly = true)
	private Component freedomclient$chatHead(Component message) {
		return ChatHeadsModule.decorate(message);
	}
}
