package com.freedomclient.ui;

import com.freedomclient.FreedomClient;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;

/** Textos con las fuentes propias del cliente (VT323 para logo y títulos). */
public final class UiText {
	private static final FontDescription LOGO = new FontDescription.Resource(FreedomClient.id("logo"));
	private static final FontDescription TITLE = new FontDescription.Resource(FreedomClient.id("title"));
	private static final FontDescription ICONS = new FontDescription.Resource(FreedomClient.id("icons"));

	private UiText() {
	}

	public static Component logo(String text) {
		return Component.literal(text).withStyle(style -> style.withFont(LOGO));
	}

	/** Insignia "FC" de 8x8 para poner delante de los nombres. */
	public static Component fcBadge() {
		return Component.literal("\uE000").withStyle(style -> style.withFont(ICONS));
	}

	public static Component title(String text) {
		return Component.literal(text).withStyle(style -> style.withFont(TITLE));
	}
}
