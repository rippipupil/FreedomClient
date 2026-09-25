package com.freedomclient.mixin;

import com.freedomclient.module.visual.VisualsModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelMixin {
	/** Visuals: el clima que ves (solo en el mundo del cliente, nunca en el del servidor integrado). */
	@Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
	private void freedomclient$rain(float partialTick, CallbackInfoReturnable<Float> cir) {
		if (!((Object) this instanceof ClientLevel)) return;
		float rain = VisualsModule.rainLevel();
		if (rain >= 0.0F) cir.setReturnValue(rain);
	}

	@Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
	private void freedomclient$thunder(float partialTick, CallbackInfoReturnable<Float> cir) {
		if (!((Object) this instanceof ClientLevel)) return;
		float thunder = VisualsModule.thunderLevel();
		if (thunder >= 0.0F) cir.setReturnValue(thunder);
	}
}
