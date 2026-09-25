package com.freedomclient.module.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class CoordinatesModule extends TextHudModule {
	public CoordinatesModule() {
		super("Coordinates", "Shows your coordinates and facing direction.", true);
	}

	@Override
	public String getText(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) return null;

		return String.format("XYZ: %d %d %d (%s)",
				player.getBlockX(), player.getBlockY(), player.getBlockZ(),
				player.getDirection().getSerializedName());
	}
}
