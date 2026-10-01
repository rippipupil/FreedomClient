package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.ClientAsset;

/**
 * Capa del cliente (sección Capes). Solo se puede llevar una a la vez: al ponerte una se quitan las demás. La textura
 * usa el formato de las capas de Minecraft (también pinta los élitros), y puede cambiar con el tiempo para animarse.
 */
public abstract class ClientCapeCosmetic extends CosmeticModule {
	protected ClientCapeCosmetic(String name, String description, boolean enabledByDefault) {
		super(name, description, CosmeticSlot.CAPE, enabledByDefault);
	}

	/** Textura de la capa ahora mismo. */
	public abstract ClientAsset.Texture texture();

	@Override
	protected void onEnable(Minecraft client) {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null) return;
		for (ClientCapeCosmetic other : manager.ofType(ClientCapeCosmetic.class)) {
			if (other != this) other.setEnabled(false);
		}
	}

	/** La capa que llevas puesta, o null. */
	public static ClientCapeCosmetic active() {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null) return null;
		for (ClientCapeCosmetic cape : manager.ofType(ClientCapeCosmetic.class)) {
			if (cape.isEnabled()) return cape;
		}
		return null;
	}
}
