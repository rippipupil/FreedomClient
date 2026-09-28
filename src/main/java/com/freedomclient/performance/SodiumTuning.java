package com.freedomclient.performance;

import com.freedomclient.FreedomClient;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;

/** Opciones de Sodium más rápidas. Va aparte para no cargar clases de Sodium si no está instalado. */
final class SodiumTuning {
	private SodiumTuning() {
	}

	static void apply() {
		try {
			SodiumOptions options = SodiumClientMod.options();
			if (options.isReadOnly()) return;
			// Hilos de construcción de chunks: 0 = automático según los núcleos del PC.
			options.performance.chunkBuilderThreads = 0;
			options.performance.animateOnlyVisibleTextures = true;
			options.performance.useEntityCulling = true;
			options.performance.useFogOcclusion = true;
			options.performance.useBlockFaceCulling = true;
			options.performance.useNoErrorGLContext = true;
			options.advanced.useAdvancedStagingBuffers = true;
			options.advanced.cpuRenderAheadLimit = 3;
			options.quality.hiddenFluidCulling = true;
			SodiumOptions.writeToDisk(options);
		} catch (Throwable e) {
			FreedomClient.LOGGER.warn("Could not apply the Sodium options", e);
		}
	}
}
