package com.freedomclient.module.pvp;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import net.minecraft.client.Minecraft;

/** Mantiene pulsada la tecla de correr para no tener que sujetarla. */
public class ToggleSprintModule extends Module {
	public ToggleSprintModule() {
		super("Toggle Sprint", "Always sprint without holding the sprint key.", Category.PVP, true);
	}

	@Override
	public void onTick(Minecraft client) {
		if (client.player != null) {
			client.options.keySprint.setDown(true);
		}
	}

	@Override
	protected void onDisable(Minecraft client) {
		client.options.keySprint.setDown(false);
	}
}
