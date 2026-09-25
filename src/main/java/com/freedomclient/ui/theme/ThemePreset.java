package com.freedomclient.ui.theme;

import java.util.EnumMap;
import java.util.Map;

/** Temas ya hechos, inspirados en el cielo y en los colores de Angel Devil (pelo rojo, halo dorado, alas blancas). */
public enum ThemePreset {
	RED_SUNSET("Red Sunset", 0x3A0F1A, 0x7A1F2B, 0xFF8C42, 0xF2C94C, 0x5DADE2, 0xF5F1E8),
	DAY_SKY("Day Sky", 0x1B2A4A, 0x2E4A7A, 0xF2C94C, 0xF2C94C, 0xD7263D, 0xF5F1E8),
	STARRY_NIGHT("Starry Night", 0x0B1026, 0x1C2448, 0x8E9BFF, 0xF2C94C, 0xD7263D, 0xE8ECFF),
	RED_DEVIL("Red Devil", 0x1A0508, 0x4A0D14, 0xD7263D, 0xFF4B4B, 0xF2C94C, 0xFFE9E9);

	private final String displayName;
	private final Map<ThemeColor, Integer> colors = new EnumMap<>(ThemeColor.class);

	ThemePreset(String displayName, int background, int card, int border, int accent, int highlight, int text) {
		this.displayName = displayName;
		colors.put(ThemeColor.BACKGROUND, 0xFF000000 | background);
		colors.put(ThemeColor.CARD, 0xFF000000 | card);
		colors.put(ThemeColor.BORDER, 0xFF000000 | border);
		colors.put(ThemeColor.ACCENT, 0xFF000000 | accent);
		colors.put(ThemeColor.HIGHLIGHT, 0xFF000000 | highlight);
		colors.put(ThemeColor.TEXT, 0xFF000000 | text);
	}

	public String getDisplayName() {
		return displayName;
	}

	public int get(ThemeColor color) {
		return colors.get(color);
	}
}
