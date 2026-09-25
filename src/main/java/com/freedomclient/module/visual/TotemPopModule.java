package com.freedomclient.module.visual;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.particle.FeatherParticle;
import com.freedomclient.particle.GlowParticle;
import com.freedomclient.particle.PixelParticles;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * TotemPop: la animación del tótem en pantalla más pequeña (o nada) y otras partículas al gastarlo:
 * una explosión de plumas con un halo dorado que brilla, chispas doradas, o las de siempre.
 */
public class TotemPopModule extends Module {
	private static TotemPopModule instance;

	private final NumberSetting animationSize = add(new NumberSetting("Animation size", "Size of the totem that pops up on your screen. 0 hides it.",
			50, 0, 100, 5, "%"));
	private final ModeSetting particles = add(new ModeSetting("Particles", "Particles when a totem is used (yours and other players').",
			"Feathers + halo", "Vanilla", "Feathers + halo", "Golden sparks", "None"));
	private final NumberSetting amount = add(new NumberSetting("Amount", "How many particles.", 100, 25, 200, 25, "%"));

	private final RandomSource random = RandomSource.create();

	public TotemPopModule() {
		super("TotemPop", "Smaller totem animation on screen and custom totem particles: feathers with a glowing halo or golden sparks.",
				Category.VISUAL, true);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	private static TotemPopModule active() {
		return instance != null && instance.isEnabled() ? instance : null;
	}

	/** Escala de la animación del tótem en pantalla (1 = tamaño normal). */
	public static float animationScale() {
		TotemPopModule module = active();
		return module == null ? 1.0F : module.animationSize.getFloat() / 100.0F;
	}

	/** Sustituye las partículas del tótem. Devuelve true si ya se han creado las propias (y hay que cancelar las de vanilla). */
	public static boolean replaceParticles(Entity entity, ParticleOptions options) {
		TotemPopModule module = active();
		if (module == null || options.getType() != ParticleTypes.TOTEM_OF_UNDYING || module.particles.is("Vanilla")) return false;
		if (!module.particles.is("None")) module.spawn(entity);
		return true;
	}

	private void spawn(Entity entity) {
		Minecraft client = Minecraft.getInstance();
		ClientLevel level = client.level;
		if (level == null) return;
		float scale = amount.getFloat() / 100.0F;
		// Tu propio tótem en primera persona: las partículas salen alrededor de la cámara, así que se alejan
		// un poco y son más pequeñas para que no tapen la pantalla.
		boolean ownFirstPerson = entity == client.player && client.options.getCameraType().isFirstPerson();
		double spread = ownFirstPerson ? 1.3 : 0.0;
		float size = ownFirstPerson ? 0.5F : 1.0F;
		double x = entity.getX();
		double y = entity.getY() + entity.getBbHeight() * 0.6;
		double z = entity.getZ();
		double top = entity.getY() + entity.getBbHeight() + 0.35;

		if (particles.is("Golden sparks")) {
			int count = Math.round(40 * scale);
			for (int i = 0; i < count; i++) {
				double angle = random.nextDouble() * Mth.TWO_PI;
				double up = random.nextDouble() * 0.5 - 0.1;
				double speed = 0.25 + random.nextDouble() * 0.25;
				client.particleEngine.add(new GlowParticle(level, x + Math.cos(angle) * spread, y, z + Math.sin(angle) * spread,
						Math.cos(angle) * speed, up, Math.sin(angle) * speed,
						PixelParticles.sprite(random.nextBoolean() ? "spark" : "star"), 0.08F * size, 0.04F * size, 25 + random.nextInt(15)));
			}
			return;
		}

		// Explosión de plumas en todas direcciones.
		int feathers = Math.round(30 * scale);
		for (int i = 0; i < feathers; i++) {
			double angle = random.nextDouble() * Mth.TWO_PI;
			double speed = 0.2 + random.nextDouble() * 0.3;
			client.particleEngine.add(new FeatherParticle(level, x + Math.cos(angle) * spread, y, z + Math.sin(angle) * spread,
					Math.cos(angle) * speed, 0.15 + random.nextDouble() * 0.3, Math.sin(angle) * speed, PixelParticles.sprite("feather")).scale(size));
		}
		// Halo dorado que aparece sobre la cabeza, crece y se desvanece, con un anillo de chispas alrededor.
		client.particleEngine.add(new GlowParticle(level, x, top, z, 0, 0.01, 0, PixelParticles.sprite("halo_ring"), 0.25F, 0.9F, 30));
		int sparks = Math.round(16 * scale);
		for (int i = 0; i < sparks; i++) {
			double angle = i * Mth.TWO_PI / sparks;
			client.particleEngine.add(new GlowParticle(level, x + Math.cos(angle) * 0.4, top, z + Math.sin(angle) * 0.4,
					Math.cos(angle) * 0.12, 0.02, Math.sin(angle) * 0.12, PixelParticles.sprite("spark"), 0.07F, 0.03F, 22));
		}
	}
}
