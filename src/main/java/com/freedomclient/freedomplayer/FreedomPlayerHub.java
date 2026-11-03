package com.freedomclient.freedomplayer;

import com.freedomclient.FreedomClient;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/**
 * FreedomPlayer dentro del cliente: la biblioteca (lo que llega del móvil y lo de FreedomPlayer del PC), la
 * sincronización con el móvil y las letras de LRCLIB. Todo vive en .minecraft/freedomclient/freedomplayer.
 */
public final class FreedomPlayerHub {
	private static final Path ROOT = FabricLoader.getInstance().getGameDir().resolve(FreedomClient.MOD_ID).resolve("freedomplayer");
	private static final ClientLibrary LIBRARY = new ClientLibrary();
	private static final FreedomSync SYNC = new FreedomSync(ROOT, LIBRARY);
	private static final Lrclib LRCLIB = new Lrclib(ROOT.resolve("letras"));

	private FreedomPlayerHub() {
	}

	public static ClientLibrary library() {
		return LIBRARY;
	}

	public static FreedomSync sync() {
		return SYNC;
	}

	public static Lrclib lrclib() {
		return LRCLIB;
	}

	/** La carpeta donde llega la música del móvil. */
	public static Path music() {
		return ROOT.resolve("musica");
	}
}
