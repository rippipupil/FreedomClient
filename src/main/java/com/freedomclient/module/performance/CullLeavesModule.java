package com.freedomclient.module.performance;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import net.minecraft.client.Minecraft;

/** Cull Leaves: oculta las caras interiores de los árboles, que nunca se ven, para ganar FPS en bosques. */
public class CullLeavesModule extends Module {
	private static CullLeavesModule instance;

	public CullLeavesModule() {
		super("Cull Leaves", "Hides the inside faces of leaves you can never see. Big FPS boost in forests.", Category.PERFORMANCE, true);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	@Override
	protected void onEnable(Minecraft client) {
		reloadChunks(client);
	}

	@Override
	protected void onDisable(Minecraft client) {
		reloadChunks(client);
	}

	private static void reloadChunks(Minecraft client) {
		if (client.level != null) {
			client.levelRenderer.allChanged();
		}
	}
}
