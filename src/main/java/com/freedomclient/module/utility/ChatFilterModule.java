package com.freedomclient.module.utility;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.StringSetting;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** ChatBlock: oculta mensajes con palabras o de jugadores que no quieres ver, y los avisos de entrar/salir. */
public class ChatFilterModule extends Module {
	private final StringSetting blockedWords = add(new StringSetting("Blocked words", "Hide messages containing any of these words (separate with commas).", "", 512));
	private final StringSetting blockedPlayers = add(new StringSetting("Blocked players", "Hide chat from these players (separate with commas).", "", 512));
	private final BooleanSetting hideJoinLeave = add(new BooleanSetting("Hide join/leave", "Hide 'joined the game' and 'left the game' messages.", false));

	public ChatFilterModule() {
		super("Chat Block", "Hide chat messages with certain words or from certain players.", Category.UTILITY, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	private static List<String> list(StringSetting setting) {
		return Arrays.stream(setting.get().toLowerCase(Locale.ROOT).split(","))
				.map(String::trim).filter(s -> !s.isEmpty()).toList();
	}

	/** Mensajes de jugadores. Devuelve false para ocultarlo. */
	public boolean allowChat(Component message, GameProfile sender) {
		if (!isEnabled()) return true;
		if (sender != null && list(blockedPlayers).contains(sender.name().toLowerCase(Locale.ROOT))) return false;
		return allowText(message);
	}

	/** Mensajes del servidor o del sistema. Devuelve false para ocultarlo. */
	public boolean allowGame(Component message) {
		if (!isEnabled()) return true;
		if (hideJoinLeave.get() && message.getContents() instanceof TranslatableContents translatable) {
			String key = translatable.getKey();
			if (key.equals("multiplayer.player.joined") || key.equals("multiplayer.player.left")
					|| key.equals("multiplayer.player.joined.renamed")) {
				return false;
			}
		}
		return allowText(message);
	}

	private boolean allowText(Component message) {
		String text = message.getString().toLowerCase(Locale.ROOT);
		for (String word : list(blockedWords)) {
			if (text.contains(word)) return false;
		}
		return true;
	}
}
