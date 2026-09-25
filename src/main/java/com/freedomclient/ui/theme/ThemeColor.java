package com.freedomclient.ui.theme;

/** Colores del tema que el jugador puede editar desde la pestaña Theme. */
public enum ThemeColor {
	BACKGROUND("Background", "Main window background."),
	CARD("Cards", "Mod cards and panels."),
	BORDER("Borders", "Window and card outlines."),
	ACCENT("Accent", "Enabled switches, selected tabs and sliders."),
	HIGHLIGHT("Highlight", "Hovered elements."),
	TEXT("Text", "Main text color.");

	private final String displayName;
	private final String description;

	ThemeColor(String displayName, String description) {
		this.displayName = displayName;
		this.description = description;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDescription() {
		return description;
	}

	public String getKey() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}
}
