package com.freedomclient.module;

public enum Category {
	HUD("HUD", 0xFF55FFFF),
	RENDER("Render", 0xFFFFAA00),
	MOVEMENT("Movimiento", 0xFF55FF55);

	private final String displayName;
	private final int color;

	Category(String displayName, int color) {
		this.displayName = displayName;
		this.color = color;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getColor() {
		return color;
	}
}
