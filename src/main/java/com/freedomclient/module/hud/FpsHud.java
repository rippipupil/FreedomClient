package com.freedomclient.module.hud;

import com.freedomclient.hud.HudPosition;
import com.freedomclient.hud.TextHudModule;
import net.minecraft.client.Minecraft;

public class FpsHud extends TextHudModule {
	public FpsHud() {
		super("FPS", "Shows your frames per second.", true, new HudPosition(HudPosition.Anchor.START, 2, HudPosition.Anchor.START, 18));
	}

	@Override
	protected String getText(Minecraft client) {
		return "FPS: " + client.getFps();
	}
}
