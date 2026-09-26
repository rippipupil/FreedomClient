package com.freedomclient.launcher;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/**
 * "Entrar al servidor al lanzar" de los perfiles del launcher cuando el juego se abre con el launcher oficial:
 * el launcher pasa el servidor en -Dfreedomclient.autoJoin y al llegar al menú principal se conecta una vez.
 */
public final class AutoJoin {
	private static final String SERVER = System.getProperty("freedomclient.autoJoin", "").trim();
	private static boolean done = SERVER.isEmpty();

	private AutoJoin() {
	}

	public static void tick(Minecraft client) {
		if (done || !(client.screen instanceof TitleScreen title)) return;
		done = true;
		if (!ServerAddress.isValidAddress(SERVER)) return;
		ServerData data = new ServerData(SERVER, SERVER, ServerData.Type.OTHER);
		ConnectScreen.startConnecting(title, client, ServerAddress.parseString(SERVER), data, false, null);
	}
}
