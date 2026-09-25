package com.freedomclient.mixin;

import com.freedomclient.module.visual.CustomHitboxesModule;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom Hitboxes. En showHitboxes, vanilla llama a GizmoStyle.stroke con: 0 = caja de la entidad,
 * 1 = punto del pasajero, 2 = línea de los ojos, 3 = partes del dragón; y a Gizmos.arrow con:
 * 0 = dirección de la mirada, 1 = movimiento.
 */
@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {
	@Unique
	private Entity freedomclient$entity;

	@Inject(method = "showHitboxes", at = @At("HEAD"), cancellable = true)
	private void freedomclient$filter(Entity entity, float partialTick, boolean server, CallbackInfo ci) {
		freedomclient$entity = entity;
		if (CustomHitboxesModule.shouldSkip(entity)) ci.cancel();
	}

	@ModifyArg(method = "showHitboxes", at = @At(value = "INVOKE", target = "Lnet/minecraft/gizmos/GizmoStyle;stroke(I)Lnet/minecraft/gizmos/GizmoStyle;", ordinal = 0))
	private int freedomclient$boxColor(int color) {
		return CustomHitboxesModule.boxColor(freedomclient$entity, color);
	}

	@ModifyArg(method = "showHitboxes", at = @At(value = "INVOKE", target = "Lnet/minecraft/gizmos/GizmoStyle;stroke(I)Lnet/minecraft/gizmos/GizmoStyle;", ordinal = 2))
	private int freedomclient$eyeLineColor(int color) {
		return CustomHitboxesModule.eyeLineColor(color);
	}

	@ModifyArg(method = "showHitboxes", at = @At(value = "INVOKE", target = "Lnet/minecraft/gizmos/Gizmos;arrow(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;I)Lnet/minecraft/gizmos/GizmoProperties;", ordinal = 0), index = 2)
	private int freedomclient$lookColor(int color) {
		return CustomHitboxesModule.lookColor(color);
	}

	@ModifyArg(method = "showHitboxes", at = @At(value = "INVOKE", target = "Lnet/minecraft/gizmos/Gizmos;arrow(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;I)Lnet/minecraft/gizmos/GizmoProperties;", ordinal = 1), index = 2)
	private int freedomclient$motionColor(int color) {
		return CustomHitboxesModule.motionColor(color);
	}
}
