package com.freedomclient.mixin;

import com.freedomclient.module.visual.HitParticlesModule;
import com.freedomclient.module.visual.TotemPopModule;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
	/** Hit Particles: oculta las partículas de crítico de vanilla (las plumas las sustituyen). */
	@Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V",
			at = @At("HEAD"), cancellable = true)
	private void freedomclient$hideCrits(Entity entity, ParticleOptions options, CallbackInfo ci) {
		if (HitParticlesModule.hidesVanilla(options)) ci.cancel();
	}

	@Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V",
			at = @At("HEAD"), cancellable = true)
	private void freedomclient$hideCritsTimed(Entity entity, ParticleOptions options, int lifetime, CallbackInfo ci) {
		// TotemPop: el tótem usa este emisor (30 ticks de partículas TOTEM_OF_UNDYING).
		if (HitParticlesModule.hidesVanilla(options) || TotemPopModule.replaceParticles(entity, options)) ci.cancel();
	}
}
