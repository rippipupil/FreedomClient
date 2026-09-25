package com.freedomclient.module;

public enum Category {
	PVP("PvP"),
	HUD("HUD"),
	VISUAL("Visual"),
	UTILITY("Utility"),
	PERFORMANCE("Performance"),
	/** Se muestran en la pestaña Cosmetics, no en Mods. */
	COSMETICS("Cosmetics");

	private final String displayName;

	Category(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
