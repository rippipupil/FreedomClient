package com.freedomclient.module.hud;

import net.minecraft.client.Minecraft;

public class FpsModule extends TextHudModule {
	public FpsModule() {
		super("FPS", "Shows your frames per second.", true);
	}

	@Override
	public String getText(Minecraft client) {
		return "FPS: " + client.getFps();
	}
}
