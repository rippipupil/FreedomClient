package com.freedomclient.mixin;

import com.freedomclient.module.pvp.FreelookModule;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public class CameraMixin {
	/** Con Freelook activo, la cámara usa su propia rotación en vez de la del jugador. */
	@Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
	private float freedomclient$freelookYaw(Entity entity, float partialTick) {
		float[] rotation = FreelookModule.getCameraRotation();
		return rotation != null ? rotation[0] : entity.getViewYRot(partialTick);
	}

	@Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
	private float freedomclient$freelookPitch(Entity entity, float partialTick) {
		float[] rotation = FreelookModule.getCameraRotation();
		return rotation != null ? rotation[1] : entity.getViewXRot(partialTick);
	}
}
