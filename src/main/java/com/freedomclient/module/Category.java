package com.freedomclient.module;

public enum Category {
	PVP("PvP"),
	HUD("HUD"),
	VISUAL("Visual"),
	UTILITY("Utility"),
	PERFORMANCE("Performance");

	private final String displayName;

	Category(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
