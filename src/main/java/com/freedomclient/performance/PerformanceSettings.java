package com.freedomclient.performance;

import com.freedomclient.FreedomClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.server.level.ParticleStatus;

/** Ajustes de vídeo de vanilla y de Sodium que más FPS dan sin cambiar la distancia de renderizado. */
public final class PerformanceSettings {
	/** Valor máximo del límite de FPS, que el juego trata como "ilimitado". */
	private static final int UNLIMITED_FRAMERATE = 260;

	private PerformanceSettings() {
	}

	public static void apply(Minecraft client) {
		Options options = client.options;

		// "Personalizado": si no, el preset de gráficos de 1.21.11 volvería a poner sus valores.
		options.graphicsPreset().set(GraphicsPreset.CUSTOM);
		options.enableVsync().set(false);
		options.framerateLimit().set(UNLIMITED_FRAMERATE);
		options.inactivityFpsLimit().set(InactivityFpsLimit.AFK);
		options.cloudStatus().set(CloudStatus.OFF);
		options.particles().set(ParticleStatus.MINIMAL);
		options.entityShadows().set(false);
		options.biomeBlendRadius().set(0);
		options.weatherRadius().set(5);
		options.cutoutLeaves().set(false);
		options.vignette().set(false);
		options.improvedTransparency().set(false);
		options.chunkSectionFadeInTime().set(0.0);
		options.prioritizeChunkUpdates().set(PrioritizeChunkUpdates.NONE);
		options.menuBackgroundBlurriness().set(0);
		options.textureFiltering().set(TextureFilteringMethod.NONE);
		// Distancia de simulación como mucho 8: en un jugador el servidor interno procesa menos chunks y mobs a la vez.
		options.simulationDistance().set(Math.min(options.simulationDistance().get(), 8));
		options.graphicsPreset().set(GraphicsPreset.CUSTOM);
		options.save();

		if (FabricLoader.getInstance().isModLoaded("sodium")) {
			SodiumTuning.apply();
		}
		if (FabricLoader.getInstance().isModLoaded("sodium-extra")) {
			SodiumExtraTuning.apply();
		}
		FreedomClient.LOGGER.info("Ajustes de rendimiento aplicados");
	}
}
