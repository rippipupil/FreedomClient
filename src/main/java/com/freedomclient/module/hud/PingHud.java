package com.freedomclient.module.hud;

import com.freedomclient.hud.HudPosition;
import com.freedomclient.hud.TextHudModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

public class PingHud extends TextHudModule {
	public PingHud() {
		super("Ping", "Shows your latency to the server.", true, new HudPosition(HudPosition.Anchor.START, 2, HudPosition.Anchor.START, 34));
	}

	@Override
	protected String getText(Minecraft client) {
		ClientPacketListener connection = client.getConnection();
		if (connection == null || client.player == null) return null;

		PlayerInfo info = connection.getPlayerInfo(client.player.getUUID());
		return info == null ? null : "Ping: " + info.getLatency() + " ms";
	}

	@Override
	protected String getPreviewText() {
		return "Ping: 25 ms";
	}
}
