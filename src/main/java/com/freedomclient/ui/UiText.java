package com.freedomclient.ui;

import com.freedomclient.FreedomClient;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;

/** Textos con las fuentes propias del cliente (VT323 para logo y títulos). */
public final class UiText {
	private static final FontDescription LOGO = new FontDescription.Resource(FreedomClient.id("logo"));
	private static final FontDescription TITLE = new FontDescription.Resource(FreedomClient.id("title"));

	private UiText() {
	}

	public static Component logo(String text) {
		return Component.literal(text).withStyle(style -> style.withFont(LOGO));
	}

	public static Component title(String text) {
		return Component.literal(text).withStyle(style -> style.withFont(TITLE));
	}
}
