package com.freedomclient.module.performance;

import com.freedomclient.mixin.accessor.ParticleEngineAccessor;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Particle Limiter: pone un tope a las partículas que hay a la vez (Minecraft deja hasta 16384) y permite quitar
 * las que más cuestan, como las de romper bloques o las explosiones. En peleas con pociones y críticos es donde más
 * caen los FPS.
 */
public class ParticleLimiterModule extends Module {
	private static ParticleLimiterModule instance;

	private final NumberSetting max = add(new NumberSetting("Max particles",
			"Particles allowed at the same time. New ones are skipped while the limit is reached.", 2000, 200, 8000, 100));
	private final BooleanSetting blockBreak = add(new BooleanSetting("Hide block break",
			"No fragments when blocks are broken or being mined.", false));
	private final BooleanSetting explosions = add(new BooleanSetting("Hide explosions",
			"No explosion smoke and flashes (TNT, creepers, end crystals).", false));
	private final BooleanSetting rain = add(new BooleanSetting("Hide rain splashes",
			"No splashes where the rain hits the ground.", true));
	private final BooleanSetting potions = add(new BooleanSetting("Hide potion swirls",
			"No swirls around players and mobs with potion effects.", false));
	private final BooleanSetting ambient = add(new BooleanSetting("Hide ambient particles",
			"No falling spores, ash, dripping and other decoration particles.", true));

	/** Partículas que hay ahora (se cuenta una vez por tick y se suman las que se van añadiendo). */
	private int count;

	public ParticleLimiterModule() {
		super("Particle Limiter", "Caps how many particles exist at once and hides the heaviest kinds for more FPS in fights.",
				Category.PERFORMANCE, true);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	@Override
	public void onTick(Minecraft client) {
		count = 0;
		for (ParticleGroup<?> group : ((ParticleEngineAccessor) client.particleEngine).freedomclient$getParticles().values()) {
			count += group.size();
		}
	}

	/** Si hay que saltarse este tipo de partícula antes de crearla. */
	public static boolean blocksType(ParticleOptions options) {
		if (instance == null || !instance.isEnabled()) return false;
		ParticleType<?> type = options.getType();
		if (instance.explosions.get() && (type == ParticleTypes.EXPLOSION || type == ParticleTypes.EXPLOSION_EMITTER
				|| type == ParticleTypes.GUST || type == ParticleTypes.GUST_EMITTER_LARGE || type == ParticleTypes.GUST_EMITTER_SMALL)) {
			return true;
		}
		if (instance.rain.get() && type == ParticleTypes.RAIN) return true;
		if (instance.potions.get() && type == ParticleTypes.ENTITY_EFFECT) return true;
		if (instance.ambient.get() && (type == ParticleTypes.ASH || type == ParticleTypes.WHITE_ASH || type == ParticleTypes.SPORE_BLOSSOM_AIR
				|| type == ParticleTypes.FALLING_SPORE_BLOSSOM || type == ParticleTypes.CRIMSON_SPORE || type == ParticleTypes.WARPED_SPORE
				|| type == ParticleTypes.DRIPPING_WATER || type == ParticleTypes.FALLING_WATER || type == ParticleTypes.DRIPPING_LAVA
				|| type == ParticleTypes.FALLING_LAVA || type == ParticleTypes.MYCELIUM || type == ParticleTypes.UNDERWATER)) {
			return true;
		}
		return false;
	}

	/** Si hay que descartar esta partícula al añadirla (por el tope o por ser de romper bloques). */
	public static boolean blocksParticle(Particle particle) {
		if (instance == null || !instance.isEnabled()) return false;
		if (instance.blockBreak.get() && particle instanceof TerrainParticle) return true;
		if (instance.count >= instance.max.get()) return true;
		instance.count++;
		return false;
	}
}
