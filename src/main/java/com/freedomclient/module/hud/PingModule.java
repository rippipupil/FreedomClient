package com.freedomclient.module.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

public class PingModule extends TextHudModule {
	public PingModule() {
		super("Ping", "Muestra tu latencia con el servidor.", true);
	}

	@Override
	public String getText(Minecraft client) {
		ClientPacketListener connection = client.getConnection();
		if (connection == null || client.player == null) return null;

		PlayerInfo info = connection.getPlayerInfo(client.player.getUUID());
		if (info == null) return null;

		return "Ping: " + info.getLatency() + " ms";
	}
}
