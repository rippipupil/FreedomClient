package com.freedomclient.module.movement;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Mantiene pulsada la tecla de correr para no tener que sujetarla. */
public class ToggleSprintModule extends Module {
	public ToggleSprintModule() {
		super("ToggleSprint", "Corre siempre sin mantener la tecla de sprint.", Category.MOVEMENT, true);
		setToggleKey(FreedomClient.registerKey("togglesprint", GLFW.GLFW_KEY_V));
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
