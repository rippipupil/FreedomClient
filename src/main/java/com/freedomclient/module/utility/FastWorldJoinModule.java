package com.freedomclient.module.utility;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;

/** Force Close / Remove Loading Screen: cierra la pantalla "Cargando terreno" en cuanto ya estás en el mundo. */
public class FastWorldJoinModule extends Module {
	public FastWorldJoinModule() {
		super("Fast World Join", "Closes the terrain loading screen as soon as you are in the world.", Category.UTILITY, true);
	}

	@Override
	public void onTick(Minecraft client) {
		if (client.screen instanceof LevelLoadingScreen && client.player != null && client.level != null) {
			client.setScreen(null);
		}
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
