package com.freedomclient.module.visual;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.particle.FeatherParticle;
import com.freedomclient.particle.PixelParticles;
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

/** Hit Particles: plumas pixel al golpear, en lugar de las partículas de crítico de vanilla. */
public class HitParticlesModule extends Module {
	private static HitParticlesModule instance;

	private final NumberSetting amount = add(new NumberSetting("Amount", "Feathers per hit.", 6, 1, 20, 1));
	private final ColorSetting color = add(new ColorSetting("Color", "Tint of the feathers.", 0xFFFFFFFF, false));
	private final BooleanSetting hideCrits = add(new BooleanSetting("Hide vanilla crits", "Hide the vanilla critical hit particles.", true));

	private final RandomSource random = RandomSource.create();

	public HitParticlesModule() {
		super("Hit Particles", "Pixel feathers burst out when you hit something.", Category.VISUAL, true);
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
}
