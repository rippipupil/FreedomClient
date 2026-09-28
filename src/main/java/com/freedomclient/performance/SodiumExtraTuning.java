package com.freedomclient.performance;

import com.freedomclient.FreedomClient;
import me.flashyreese.mods.sodiumextra.client.SodiumExtraClientMod;
import me.flashyreese.mods.sodiumextra.client.config.SodiumExtraGameOptions;

/**
 * Opciones de Sodium Extra que ahorran trabajo sin quitar nada que importe al jugar (las animaciones, el cielo y las
 * partículas de golpes se dejan). Va aparte para no cargar sus clases si no está.
 */
final class SodiumExtraTuning {
	private SodiumExtraTuning() {
	}

	static void apply() {
		try {
			SodiumExtraGameOptions options = SodiumExtraClientMod.options();
			// Salpicaduras de lluvia: muchas partículas que no aportan nada.
			options.particleSettings.rainSplash = false;
			// Rayos de faro limitados a la altura del mundo visible.
			options.renderSettings.limitBeaconBeamHeight = true;
			// Menos avisos que dibujar (recetas y tutorial); los logros se siguen viendo.
			options.extraSettings.recipeToast = false;
			options.extraSettings.tutorialToast = false;
			// La pantalla F3 se refresca a intervalos en vez de en cada fotograma.
			options.extraSettings.steadyDebugHud = true;
			options.extraSettings.reduceResolutionOnMac = true;
			options.writeChanges();
		} catch (Throwable e) {
			FreedomClient.LOGGER.warn("Could not apply the Sodium Extra options", e);
		}
	}
}
