package com.freedomclient.module.qol;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

/**
 * Glass: los bloques y paneles de cristal que están juntos se ven conectados, sin el marco entre ellos. Usa las
 * texturas conectadas de Continuity (incluido en el cliente): este módulo activa o quita su paquete integrado.
 */
public class GlassModule extends Module {
	public GlassModule() {
		super("Glass", "Glass blocks and panes next to each other look connected, without the frame between them.", Category.QOL, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** Id del paquete de texturas conectadas por defecto de Continuity, o null si no está. */
	private static String packId(PackRepository repository) {
		for (String id : repository.getAvailableIds()) {
			if (id.contains("continuity") && id.endsWith("default")) return id;
		}
		return null;
	}

	/** Al arrancar, deja el paquete como diga el módulo (Continuity lo trae apagado). */
	public void applyOnStart(Minecraft client) {
		apply(client, isEnabled());
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
