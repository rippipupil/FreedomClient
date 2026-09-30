package com.freedomclient.mixin;

import com.freedomclient.module.utility.FreecamModule;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
	/** Freecam: las teclas de movimiento mueven la cámara y el jugador se queda quieto. */
	@Inject(method = "tick", at = @At("TAIL"))
	private void freedomclient$freecam(CallbackInfo ci) {
		if (FreecamModule.captureInput(this.keyPresses)) {
			this.keyPresses = Input.EMPTY;
			this.moveVector = Vec2.ZERO;
		}
	}
}
