package com.freedomclient.mixin;

import com.freedomclient.module.visual.CustomScreensModule;
import com.freedomclient.ui.scene.FreedomTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	/**
	 * Sustituye el menú principal de Minecraft por el de FreedomClient. Se aplica justo antes de guardar la pantalla,
	 * porque vanilla crea su menú dentro de setScreen cuando se cierra una pantalla sin mundo cargado.
	 */
	@ModifyVariable(method = "setScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", opcode = Opcodes.PUTFIELD), argsOnly = true)
	private Screen freedomclient$replaceTitleScreen(Screen screen) {
		if (screen instanceof TitleScreen && !(screen instanceof FreedomTitleScreen) && CustomScreensModule.mainMenuEnabled()) {
			return new FreedomTitleScreen();
		}
		return screen;
	}
}
