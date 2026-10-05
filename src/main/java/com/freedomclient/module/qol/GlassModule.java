package com.freedomclient.module.qol;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.pack.FreedomPack;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

/**
 * Glass: los bloques y paneles de cristal que están juntos se ven conectados, sin el marco entre ellos. Usa las
 * texturas conectadas de Continuity (incluido en el cliente), que van como una parte del paquete de FreedomClient.
 */
public class GlassModule extends Module implements com.freedomclient.module.LivePreview {
	private static GlassModule instance;

	public GlassModule() {
		super("Glass", "Glass blocks and panes next to each other look connected, without the frame between them.", Category.QOL, true);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** Al arrancar, quita el paquete suelto de Continuity si quedó puesto de antes: ahora va dentro del de FreedomClient. */
	public static void removeLegacyPack(Minecraft client) {
		PackRepository repository = client.getResourcePackRepository();
		for (String id : repository.getSelectedIds()) {
			if (id.contains("continuity") && id.endsWith("default")) {
				if (repository.removePack(id)) client.options.updateResourcePacks(repository);
				return;
			}
		}
	}

	@Override
	protected void onEnable(Minecraft client) {
		FreedomPack.refresh(client);
	}

	@Override
	protected void onDisable(Minecraft client) {
		FreedomPack.refresh(client);
	}
}
