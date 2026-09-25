package com.freedomclient.module.hud;

import com.freedomclient.hud.HudPosition;
import com.freedomclient.hud.TextHudModule;
import com.freedomclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class CoordinatesHud extends TextHudModule {
	private final BooleanSetting showDirection = add(new BooleanSetting("Show direction", "Show the direction you are facing.", true));

	public CoordinatesHud() {
		super("Coordinates", "Shows your coordinates.", true, new HudPosition(HudPosition.Anchor.START, 2, HudPosition.Anchor.START, 50));
	}

	@Override
	protected String getText(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) return null;

		String text = "XYZ: " + player.getBlockX() + " " + player.getBlockY() + " " + player.getBlockZ();
		return showDirection.get() ? text + " (" + player.getDirection().getSerializedName() + ")" : text;
	}
}
