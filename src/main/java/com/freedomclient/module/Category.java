package com.freedomclient.module;

public enum Category {
	PVP("PvP", 0xE0404F),
	HUD("HUD", 0x3FD7FF),
	VISUAL("Visual", 0x9B6BFF),
	UTILITY("Utility", 0x4CC38A),
	PERFORMANCE("Performance", 0xF2C94C),
	/** Se muestran en la pestaña Cosmetics, no en Mods. */
	COSMETICS("Cosmetics", 0xFF7EB6);

	private final String displayName;
	/** Color de la categoría: tiñe el marco del icono de cada mod para reconocerlo de un vistazo. */
	private final int color;

	Category(String displayName, int color) {
		this.displayName = displayName;
		this.color = color;
	}

	public int getColor() {
		return 0xFF000000 | color;
	}

	public String getDisplayName() {
		return displayName;
	}
}
