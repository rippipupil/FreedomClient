package com.freedomclient.module.hud;

import net.minecraft.client.Minecraft;

public class FpsModule extends TextHudModule {
	public FpsModule() {
		super("FPS", "Muestra los fotogramas por segundo.", true);
	}

	@Override
	public String getText(Minecraft client) {
		return "FPS: " + client.getFps();
	}
}
