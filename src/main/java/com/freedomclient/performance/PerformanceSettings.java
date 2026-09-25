package com.freedomclient.performance;

import com.freedomclient.FreedomClient;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

/** Ajustes de vídeo de vanilla que más FPS dan sin cambiar la distancia de renderizado. */
public final class PerformanceSettings {
	/** Valor máximo del límite de FPS, que el juego trata como "ilimitado". */
	private static final int UNLIMITED_FRAMERATE = 260;

	private PerformanceSettings() {
	}

	public static void apply(Minecraft client) {
		Options options = client.options;

		options.enableVsync().set(false);
		options.framerateLimit().set(UNLIMITED_FRAMERATE);
		options.cloudStatus().set(CloudStatus.OFF);
		options.particles().set(ParticleStatus.MINIMAL);
		options.entityShadows().set(false);
		options.biomeBlendRadius().set(0);
		options.save();

		FreedomClient.LOGGER.info("Ajustes de rendimiento aplicados");
	}
}
