package com.freedomclient.mixin;

import com.freedomclient.module.pvp.FreelookModule;
import com.freedomclient.module.utility.FreecamModule;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Shadow
	protected abstract void setPosition(Vec3 position);

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

	/** Freecam: la cámara va donde la hayas llevado, mirando a donde mueves el ratón, y se ve a tu jugador. */
	@Inject(method = "setup", at = @At("TAIL"))
	private void freedomclient$freecam(Level level, Entity entity, boolean thirdPerson, boolean mirror, float partialTick, CallbackInfo ci) {
		Vec3 position = FreecamModule.cameraPosition(partialTick);
		if (position == null) return;
		detached = true;
		setRotation(FreecamModule.yaw(), FreecamModule.pitch());
		setPosition(position);
	}
}
