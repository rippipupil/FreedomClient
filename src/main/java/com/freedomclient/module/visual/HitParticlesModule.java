package com.freedomclient.module.visual;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.pvp.HitSoundsModule;
import com.freedomclient.particle.NeonFx;
import com.freedomclient.particle.PixelParticles;
import com.freedomclient.particle.VoxelParticle;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Hit Particles: partículas propias al golpear, distintas para golpe normal y para crítico, y se pueden mezclar:
 * plumas, copos de nieve y calabazas "pixel 3D" que giran, o descargas de Neon. El golpe normal suelta muy pocas;
 * el crítico, más, pero a los lados del objetivo y durante poco tiempo para que no molesten en PvP.
 */
public class HitParticlesModule extends Module {
	private static HitParticlesModule instance;

	private final BooleanSetting hitFeathers = add(new BooleanSetting("Hit: feathers", "Normal hits: a pixel 3D feather or two.", true));
	private final BooleanSetting hitSnow = add(new BooleanSetting("Hit: snowflakes", "Normal hits: a pixel 3D snowflake.", false));
	private final BooleanSetting hitPumpkins = add(new BooleanSetting("Hit: pumpkins", "Normal hits: a tiny pixel 3D pumpkin.", false));
	private final BooleanSetting hitNeon = add(new BooleanSetting("Hit: Neon", "Normal hits: one small electric discharge.", false));
	private final BooleanSetting critFeathers = add(new BooleanSetting("Crit: feathers", "Critical hits: a burst of pixel 3D feathers.", true));
	private final BooleanSetting critSnow = add(new BooleanSetting("Crit: snowflakes", "Critical hits: a burst of pixel 3D snowflakes.", false));
	private final BooleanSetting critPumpkins = add(new BooleanSetting("Crit: pumpkins", "Critical hits: a burst of pixel 3D pumpkins.", false));
	private final BooleanSetting critNeon = add(new BooleanSetting("Crit: Neon", "Critical hits: several electric discharges.", false));
	private final NumberSetting amount = add(new NumberSetting("Amount", "More or fewer particles for both hits and crits.", 1, 0.5, 2, 0.25, "x"));
	private final ColorSetting color = add(new ColorSetting("Feather color", "Tint of the feathers.", 0xFFFFFFFF, false));
	private final BooleanSetting hideCrits = add(new BooleanSetting("Hide vanilla crits", "Hide the vanilla critical hit particles.", true));

	private final RandomSource random = RandomSource.create();

	public HitParticlesModule() {
		super("Hit Particles", "Pixel 3D feathers, snowflakes, pumpkins or Neon discharges when you hit: pick different ones for hits and crits.",
				Category.VISUAL, true);
		instance = this;
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
		spawn(target, HitSoundsModule.isCritical(client.player, target));
	}

	/** Suelta las partículas elegidas para un golpe normal o para un crítico. */
	public void spawn(Entity target, boolean crit) {
		List<String> kinds = new ArrayList<>();
		if (crit ? critFeathers.get() : hitFeathers.get()) kinds.add("feather");
		if (crit ? critSnow.get() : hitSnow.get()) kinds.add("snow");
		if (crit ? critPumpkins.get() : hitPumpkins.get()) kinds.add("pumpkin");
		boolean neon = crit ? critNeon.get() : hitNeon.get();
		if (neon) neonHit(target, crit);
		if (kinds.isEmpty()) return;
		// Golpe normal: 1-2 partículas. Crítico: unas 5, repartidas entre los tipos elegidos.
		int count = Math.max(1, Math.round((crit ? 5 : 1.5F) * amount.getFloat()));
		if (!crit && random.nextFloat() < 0.5F) count = Math.max(1, count - 1);
		for (int i = 0; i < count; i++) {
			voxel(target, kinds.get(i % kinds.size()), crit);
		}
	}

	private void voxel(Entity target, String kind, boolean crit) {
		Minecraft client = Minecraft.getInstance();
		AABB box = target.getBoundingBox();
		double cx = (box.minX + box.maxX) / 2;
		double cz = (box.minZ + box.maxZ) / 2;
		// A los lados del objetivo (no delante de su cara) y a media altura.
		double angle = random.nextDouble() * Math.PI * 2;
		double radius = box.getXsize() * 0.5 + 0.05 + random.nextDouble() * 0.15;
		double x = cx + Math.cos(angle) * radius;
		double y = box.minY + box.getYsize() * (0.45 + random.nextDouble() * 0.4);
		double z = cz + Math.sin(angle) * radius;
		double push = crit ? 0.14 : 0.08;
		double xd = Math.cos(angle) * push * (0.5 + random.nextDouble());
		double yd = (crit ? 0.12 : 0.08) + random.nextDouble() * 0.08;
		double zd = Math.sin(angle) * push * (0.5 + random.nextDouble());
		TextureAtlasSprite[] frames = new TextureAtlasSprite[8];
		for (int f = 0; f < frames.length; f++) frames[f] = PixelParticles.sprite("hit_" + kind + "_" + f);
		float size = (crit ? 0.14F : 0.11F) * (kind.equals("pumpkin") ? 0.85F : 1.0F);
		int lifetime = (crit ? 22 : 16) + random.nextInt(8);
		VoxelParticle particle = new VoxelParticle(client.level, x, y, z, xd, yd, zd, frames, size, lifetime, !kind.equals("pumpkin"));
		if (kind.equals("feather")) {
			int rgb = color.get();
			particle.setColor((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
		}
		client.particleEngine.add(particle);
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
