package com.freedomclient.module.visual;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;

/**
 * Better Grass: los laterales de la hierba, la nieve, el micelio y el podzol se ven como la parte de arriba.
 * Es un paquete de recursos integrado en el cliente que este módulo activa o desactiva.
 */
public class BetterGrassModule extends Module {
	private static final String PACK = "bettergrass";

	public BetterGrassModule() {
		super("Better Grass", "Grass, snow, mycelium and podzol sides look like their top, so hills look smooth.", Category.VISUAL, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** Registra el paquete; se llama al iniciar el cliente. */
	public static void registerPack() {
		FabricLoader.getInstance().getModContainer(FreedomClient.MOD_ID).ifPresent(container ->
				ResourceLoader.registerBuiltinPack(FreedomClient.id(PACK), container,
						Component.literal("FreedomClient Better Grass"), PackActivationType.DEFAULT_ENABLED));
	}

	private static String packId(PackRepository repository) {
		for (String id : repository.getAvailableIds()) {
			if (id.endsWith(PACK)) return id;
		}
		return null;
	}

	/** Deja el estado del módulo igual que el del paquete (por si se cambió desde la pantalla de paquetes). */
	public void syncWithPacks(Minecraft client) {
		PackRepository repository = client.getResourcePackRepository();
		String id = packId(repository);
		if (id != null) {
			loadEnabled(repository.getSelectedIds().contains(id));
		}
	}

	@Override
	protected void onEnable(Minecraft client) {
		apply(client, true);
	}

	@Override
	protected void onDisable(Minecraft client) {
		apply(client, false);
	}

	private static void apply(Minecraft client, boolean enabled) {
		PackRepository repository = client.getResourcePackRepository();
		String id = packId(repository);
		if (id == null) return;
		boolean changed = enabled ? repository.addPack(id) : repository.removePack(id);
		if (changed) {
			// Guarda la lista de paquetes en options.txt y recarga los recursos.
			client.options.updateResourcePacks(repository);
		}
	}
}
