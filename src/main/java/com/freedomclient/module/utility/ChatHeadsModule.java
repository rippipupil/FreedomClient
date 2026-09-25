package com.freedomclient.module.utility;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.world.item.component.ResolvableProfile;

/**
 * Chat Heads: la cara del jugador al lado de su mensaje. Usa los componentes de texto "object" de 1.21.9+,
 * así que la cabeza va dentro del propio mensaje. El autor se busca entre los jugadores de la lista (Tab),
 * porque muchos servidores mandan el chat como mensajes de sistema.
 */
public class ChatHeadsModule extends Module {
	private static final int SEARCH_CHARS = 48;
	private static ChatHeadsModule instance;

	private final BooleanSetting hat = add(new BooleanSetting("Show hat layer", "Draw the outer layer of the skin on the head.", true));

	public ChatHeadsModule() {
		super("Chat Heads", "Shows the face of each player next to their chat messages.", Category.UTILITY, true);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** El mensaje con la cabeza de su autor delante, o el mismo mensaje si no se sabe quién lo escribió. */
	public static Component decorate(Component message) {
		if (instance == null || !instance.isEnabled() || message == null) return message;
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection == null) return message;
		GameProfile author = findAuthor(connection, message.getString());
		if (author == null) return message;
		Component head = Component.object(new PlayerSprite(ResolvableProfile.createResolved(author), instance.hat.get()));
		return Component.empty().append(head).append(" ").append(message);
	}

	/** El jugador de la lista cuyo nombre aparece antes al principio del mensaje, como palabra completa. */
	private static GameProfile findAuthor(ClientPacketListener connection, String text) {
		String start = text.length() > SEARCH_CHARS ? text.substring(0, SEARCH_CHARS) : text;
		GameProfile best = null;
		int bestIndex = Integer.MAX_VALUE;
		for (PlayerInfo info : connection.getOnlinePlayers()) {
			GameProfile profile = info.getProfile();
			String name = profile.name();
			if (name == null || name.length() < 3) continue;
			int index = start.indexOf(name);
			while (index >= 0) {
				boolean before = index == 0 || !isNameChar(start.charAt(index - 1));
				int end = index + name.length();
				boolean after = end >= start.length() || !isNameChar(start.charAt(end));
				if (before && after) break;
				index = start.indexOf(name, index + 1);
			}
			if (index >= 0 && index < bestIndex) {
				bestIndex = index;
				best = profile;
			}
		}
		return best;
	}

	private static boolean isNameChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_';
	}
}
