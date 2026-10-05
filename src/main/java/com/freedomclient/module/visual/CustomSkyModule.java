package com.freedomclient.module.visual;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.ui.theme.ThemeManager;
import com.freedomclient.ui.theme.ThemePreset;
import net.minecraft.client.Minecraft;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Custom Sky: cambia los colores del cielo del mundo normal (cielo, horizonte y niebla, nubes, amanecer y estrellas)
 * por los de los temas del cliente: atardecer de Angel Devil, noche estrellada, día, tormenta Neon, morado o menta.
 * Puede seguir el día y la noche del juego (de noche se oscurece con los colores nocturnos del tema) o quedarse fijo.
 */
public class CustomSkyModule extends Module implements com.freedomclient.module.LivePreview {
	private static CustomSkyModule instance;

	/** Colores de un cielo: de día y de noche, arriba (cielo) y en el horizonte (niebla), nubes y cuánto brillan las estrellas. */
	private enum Sky {
		SUNSET(0x7A2448, 0xF4913C, 0xFFC9A0, 0x1A0B2E, 0x5C1A35, 0x5C2A45, 0xFF8C42, 0.25F),
		STARRY_NIGHT(0x10143C, 0x3B467B, 0x3A3F6B, 0x04051A, 0x19204F, 0x2A2E55, 0x8E9BFF, 1.0F),
		DAY(0x3F8FD8, 0xBDE8FA, 0xFFFFFF, 0x060822, 0x1F2759, 0x3A3F6B, 0xFFD27A, 0.0F),
		STORM(0x0E1842, 0x2C86BF, 0x26307A, 0x04061A, 0x162358, 0x121840, 0x3FD7FF, 0.6F),
		PURPLE(0x441A66, 0xD07BA8, 0x9A63B8, 0x120726, 0x5F257D, 0x5E3680, 0xFF7AD9, 0.5F),
		MINT(0x46AAA2, 0xC4F0E8, 0xF2FFFB, 0x0B2622, 0x1B534B, 0x2F6A60, 0xB8FFE4, 0.1F);

		final int daySky;
		final int dayFog;
		final int dayClouds;
		final int nightSky;
		final int nightFog;
		final int nightClouds;
		final int glow;
		final float stars;

		Sky(int daySky, int dayFog, int dayClouds, int nightSky, int nightFog, int nightClouds, int glow, float stars) {
			this.daySky = daySky;
			this.dayFog = dayFog;
			this.dayClouds = dayClouds;
			this.nightSky = nightSky;
			this.nightFog = nightFog;
			this.nightClouds = nightClouds;
			this.glow = glow;
			this.stars = stars;
		}
	}

	private final ModeSetting sky = add(new ModeSetting("Sky", "Look of the sky. Menu theme uses the colors of the theme you picked in the Theme tab.",
			"Menu theme", "Menu theme", "Sunset", "Starry night", "Day", "Storm", "Purple", "Mint"));
	private final BooleanSetting dayCycle = add(new BooleanSetting("Day and night",
			"The sky gets dark at night with the theme's night colors. Off: the theme's look all day long.", true));
	private final BooleanSetting clouds = add(new BooleanSetting("Tint clouds", "Clouds take the colors of the sky.", true));
	private final BooleanSetting fog = add(new BooleanSetting("Tint horizon", "The fog and the horizon take the colors of the sky.", true));

	/** Cuánto es de día (0 noche, 1 día), calculado del color del cielo de vanilla en este fotograma. */
	private float daylight = 1.0F;

	public CustomSkyModule() {
		super("Custom Sky", "Themed skies: Angel Devil sunset, starry night, day, Neon storm, purple or mint. Follows day and night.",
				Category.VISUAL, false);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	private Sky current() {
		return switch (sky.get()) {
			case "Sunset" -> Sky.SUNSET;
			case "Starry night" -> Sky.STARRY_NIGHT;
			case "Day" -> Sky.DAY;
			case "Storm" -> Sky.STORM;
			case "Purple" -> Sky.PURPLE;
			case "Mint" -> Sky.MINT;
			default -> fromTheme(ThemeManager.getPreset());
		};
	}

	private static Sky fromTheme(ThemePreset preset) {
		return switch (preset) {
			case DAY_SKY -> Sky.DAY;
			case STARRY_NIGHT -> Sky.STARRY_NIGHT;
			case NEON -> Sky.STORM;
			case PURPLE -> Sky.PURPLE;
			case MINT -> Sky.MINT;
			default -> Sky.SUNSET;
		};
	}

	/**
	 * Llamado desde EnvironmentAttributeProbeMixin con cada valor de ambiente que lee la cámara. Devuelve el valor
	 * cambiado o null para dejar el de vanilla.
	 */
	public static Object override(EnvironmentAttributeProbe probe, EnvironmentAttribute<?> attribute, Object vanilla) {
		CustomSkyModule module = instance;
		if (module == null || !module.isEnabled() || !(vanilla instanceof Number)) return null;
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) return null;
		DimensionType.Skybox skybox = client.level.dimensionType().skybox();
		if (skybox == DimensionType.Skybox.NONE || skybox == DimensionType.Skybox.END) return null;
		if (probe != client.gameRenderer.getMainCamera().attributeProbe()) return null;
		Sky theme = module.current();
		if (attribute == EnvironmentAttributes.SKY_COLOR) {
			// El cielo de vanilla es lo primero que se lee: su brillo dice cuánto es de día.
			int color = ((Number) vanilla).intValue();
			module.daylight = module.dayCycle.get() ? Math.min(1.0F, luminance(color) / luminance(0x78A7FF)) : 1.0F;
			return rgb(color, module.blend(theme.nightSky, theme.daySky));
		}
		if (attribute == EnvironmentAttributes.FOG_COLOR && module.fog.get()) {
			return rgb(((Number) vanilla).intValue(), module.blend(theme.nightFog, theme.dayFog));
		}
		if (attribute == EnvironmentAttributes.CLOUD_COLOR && module.clouds.get()) {
			return rgb(((Number) vanilla).intValue(), module.blend(theme.nightClouds, theme.dayClouds));
		}
		if (attribute == EnvironmentAttributes.SUNRISE_SUNSET_COLOR) {
			// Solo se tiñe (el amanecer sigue saliendo cuando toca), con el alfa de vanilla.
			int color = ((Number) vanilla).intValue();
			if ((color >>> 24) == 0) return null;
			return rgb(color, ThemeManager.mix(0xFF000000 | theme.glow, 0xFF000000 | (color & 0xFFFFFF), 0.25F));
		}
		if (attribute == EnvironmentAttributes.STAR_BRIGHTNESS) {
			float stars = ((Number) vanilla).floatValue();
			float themed = theme.stars * (module.dayCycle.get() ? 1.0F - module.daylight * 0.8F : 1.0F);
			return Math.max(stars, themed);
		}
		return null;
	}

	/** Color del tema según la hora: el de noche mezclado con el de día. */
	private int blend(int night, int day) {
		return ThemeManager.mix(0xFF000000 | night, 0xFF000000 | day, daylight);
	}

	/** Pone el RGB del tema conservando el alfa de vanilla. */
	private static int rgb(int vanilla, int themed) {
		return (vanilla & 0xFF000000) | (themed & 0xFFFFFF);
	}

	private static float luminance(int rgb) {
		return ((rgb >> 16 & 0xFF) * 0.299F + (rgb >> 8 & 0xFF) * 0.587F + (rgb & 0xFF) * 0.114F) / 255.0F;
	}
}
