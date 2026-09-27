package com.freedomclient.module.visual;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.pvp.HitSoundsModule;
import com.freedomclient.particle.FeatherParticle;
import com.freedomclient.particle.NeonFx;
import com.freedomclient.particle.PixelParticles;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/** Hit Particles: plumas pixel o descargas de Neon al golpear, en lugar de las partículas de crítico de vanilla. */
public class HitParticlesModule extends Module {
	private static HitParticlesModule instance;

	private final ModeSetting style = add(new ModeSetting("Style", "Feathers: pixel angel feathers. Neon: a small electric discharge, several on crits.",
			"Feathers", "Feathers", "Neon"));
	private final NumberSetting amount = add(new NumberSetting("Amount", "Feathers per hit.", 6, 1, 20, 1));
	private final ColorSetting color = add(new ColorSetting("Color", "Tint of the feathers.", 0xFFFFFFFF, false));
	private final BooleanSetting hideCrits = add(new BooleanSetting("Hide vanilla crits", "Hide the vanilla critical hit particles.", true));

	private final RandomSource random = RandomSource.create();

	public HitParticlesModule() {
		super("Hit Particles", "Pixel feathers or Neon discharges when you hit something.", Category.VISUAL, true);
		instance = this;
		amount.visibleWhen(() -> style.is("Feathers"));
		color.visibleWhen(() -> style.is("Feathers"));
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** Si hay que ocultar estas partículas de golpe de vanilla. */
	public static boolean hidesVanilla(ParticleOptions options) {
		return instance != null && instance.isEnabled() && instance.hideCrits.get()
				&& (options.getType() == ParticleTypes.CRIT || options.getType() == ParticleTypes.ENCHANTED_HIT);
	}

	public void onHit(Entity target) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) return;
		if (style.is("Neon")) {
			neonHit(target, HitSoundsModule.isCritical(client.player, target));
			return;
		}
		TextureAtlasSprite sprite = PixelParticles.sprite("feather");
		AABB box = target.getBoundingBox();
		int rgb = color.get();
		for (int i = 0; i < amount.getInt(); i++) {
			double x = box.minX + random.nextDouble() * box.getXsize();
			double y = box.minY + box.getYsize() * (0.4 + random.nextDouble() * 0.5);
			double z = box.minZ + random.nextDouble() * box.getZsize();
			double xd = (random.nextDouble() - 0.5) * 0.3;
			double yd = 0.1 + random.nextDouble() * 0.15;
			double zd = (random.nextDouble() - 0.5) * 0.3;
			FeatherParticle feather = new FeatherParticle(client.level, x, y, z, xd, yd, zd, sprite);
			feather.setColor((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
			client.particleEngine.add(feather);
		}
	}

	/**
	 * Estilo Neon: un golpe normal suelta una descarga pequeña; un crítico, varias alrededor. Duran unos pocos ticks y
	 * salen a los lados del objetivo, no en el centro, para no tapar la vista en PvP.
	 */
	public void neonHit(Entity target, boolean crit) {
		AABB box = target.getBoundingBox();
		double cx = (box.minX + box.maxX) / 2;
		double cz = (box.minZ + box.maxZ) / 2;
		double top = box.minY + box.getYsize() * 0.75;
		int count = crit ? 4 : 1;
		for (int i = 0; i < count; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double radius = box.getXsize() * 0.55 + random.nextDouble() * 0.15;
			double x = cx + Math.cos(angle) * radius;
			double y = top + (random.nextDouble() - 0.5) * box.getYsize() * 0.4;
			double z = cz + Math.sin(angle) * radius;
			NeonFx.discharge(x, y, z, crit ? 0.16F : 0.13F, crit ? 6 : 5);
			NeonFx.spark(x, y, z, 0.12, 0.05F, 5);
		}
	}
}
