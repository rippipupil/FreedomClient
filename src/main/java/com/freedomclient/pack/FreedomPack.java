package com.freedomclient.pack;

import com.freedomclient.FreedomClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * El único paquete de recursos de FreedomClient. En vez de un paquete por opción (Better Grass, cada color del brillo,
 * cada estilo del Gap Counter, los cristales conectados...), este junta en cada recarga solo las partes que están
 * activas. Va siempre puesto y fijo arriba, así que en la lista de paquetes solo se ven Minecraft y FreedomClient.
 * Los mods registran su parte con {@link #layer} y llaman a {@link #refresh} cuando cambian.
 */
public final class FreedomPack {
	/** Distinto del id del mod: Fabric ya usa "freedomclient" para el paquete con los recursos del mod. */
	public static final String ID = "freedomclient_pack";
	private static final PackLocationInfo INFO = new PackLocationInfo(ID, Component.literal("FreedomClient"), PackSource.BUILT_IN, Optional.empty());
	private static final PackSelectionConfig SELECTION = new PackSelectionConfig(true, Pack.Position.TOP, true);

	/** Una parte del paquete: el mod que la trae, su carpeta dentro de ese mod y cuándo va puesta. */
	private record Layer(String name, String modId, String folder, Supplier<Boolean> active) {
	}

	private static final List<Layer> LAYERS = new ArrayList<>();
	/** Partes con las que se montó el paquete en la última recarga. */
	private static String loaded = null;

	public static final RepositorySource SOURCE = consumer -> {
		Pack pack = Pack.readMetaAndCreate(INFO, new Pack.ResourcesSupplier() {
			@Override
			public PackResources openPrimary(PackLocationInfo location) {
				return base(location);
			}

			@Override
			public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
				List<PackResources> stack = new ArrayList<>();
				StringBuilder signature = new StringBuilder();
				for (Layer layer : LAYERS) {
					if (!layer.active().get()) continue;
					Path root = path(layer.modId(), layer.folder());
					if (root == null) continue;
					stack.add(new PathPackResources(location, root));
					signature.append(layer.name()).append(';');
				}
				loaded = signature.toString();
				return new CompositePackResources(base(location), stack);
			}
		}, PackType.CLIENT_RESOURCES, SELECTION);
		if (pack != null) consumer.accept(pack);
	};

	private FreedomPack() {
	}

	/**
	 * Añade una parte. Se apilan en el orden en que se registran: las últimas mandan sobre las primeras.
	 * {@code folder} es la carpeta del paquete dentro del jar del mod {@code modId}.
	 */
	public static void layer(String name, String modId, String folder, Supplier<Boolean> active) {
		LAYERS.add(new Layer(name, modId, folder, active));
	}

	private static PackResources base(PackLocationInfo location) {
		Path root = path(FreedomClient.MOD_ID, "resourcepacks/fc_resources");
		return new PathPackResources(location, root);
	}

	/** Carpetas ya buscadas (se consulta cada tick, así no se busca en el jar cada vez). */
	private static final java.util.Map<String, Optional<Path>> PATHS = new java.util.concurrent.ConcurrentHashMap<>();

	private static Path path(String modId, String folder) {
		return PATHS.computeIfAbsent(modId + "/" + folder,
				key -> FabricLoader.getInstance().getModContainer(modId).flatMap(container -> container.findPath(folder))).orElse(null);
	}

	private static String wanted() {
		StringBuilder signature = new StringBuilder();
		for (Layer layer : LAYERS) {
			if (layer.active().get() && path(layer.modId(), layer.folder()) != null) signature.append(layer.name()).append(';');
		}
		return signature.toString();
	}

	/**
	 * Paquetes que no se enseñan en las listas: los internos de los mods (Fabric, Continuity, los que pudieran quedar
	 * sueltos de FreedomClient). Así solo se ven Minecraft, FreedomClient y los paquetes del jugador.
	 */
	public static boolean isInternal(String id) {
		if (id.equals(ID)) return false;
		return id.contains("freedomclient") || id.contains("continuity") || id.equals("fabric") || id.startsWith("fabric/");
	}

	/** Si alguna parte ha cambiado desde la última recarga, recarga los recursos (no hace nada mientras se cargan). */
	public static void refresh(Minecraft client) {
		if (loaded == null || client.getOverlay() != null) return;
		if (!wanted().equals(loaded)) {
			loaded = wanted();
			client.reloadResourcePacks();
		}
	}
}
