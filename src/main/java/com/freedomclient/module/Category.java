package com.freedomclient.module;

public enum Category {
	PVP("PvP", 0xFF4A4A),
	HUD("HUD", 0x3FD7FF),
	VISUAL("Visual", 0xFFD84A),
	UTILITY("Utility", 0x3FF0E0),
	QOL("QoL", 0x7CF5A0),
	PERFORMANCE("Optimization", 0x3F7BFF),
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
