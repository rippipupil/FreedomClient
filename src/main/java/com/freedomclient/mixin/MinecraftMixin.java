package com.freedomclient.mixin;

import com.freedomclient.module.visual.CustomScreensModule;
import com.freedomclient.ui.scene.FreedomTitleScreen;
import com.freedomclient.module.utility.CrashGuardModule;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	/**
	 * Sustituye el menú principal de Minecraft por el de FreedomClient. Se aplica cada vez que setScreen lee la
	 * pantalla, porque vanilla crea su menú dentro del propio setScreen cuando se cierra una pantalla sin mundo
	 * cargado; así tanto la pantalla guardada como la que se inicializa son la de FreedomClient.
	 */
	@ModifyVariable(method = "setScreen", at = @At("LOAD"), argsOnly = true)
	private Screen freedomclient$replaceTitleScreen(Screen screen) {
		if (screen instanceof TitleScreen && !(screen instanceof FreedomTitleScreen) && CustomScreensModule.mainMenuEnabled()) {
			return new FreedomTitleScreen();
		}
		return screen;
	}

	/** Crash Guard: intenta volver al menú en vez de cerrar el juego. */
	@Inject(method = "emergencySaveAndCrash", at = @At("HEAD"), cancellable = true)
	private void freedomclient$crashGuard(CrashReport report, CallbackInfo ci) {
		if (CrashGuardModule.tryRecover((Minecraft) (Object) this, report)) {
			ci.cancel();
		}
	}
}
