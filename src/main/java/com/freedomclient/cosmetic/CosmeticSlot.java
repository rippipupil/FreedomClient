package com.freedomclient.cosmetic;

/** Sección de la pestaña Cosmetics en la que sale cada cosmético (en este orden). */
public enum CosmeticSlot {
	HAT("Hats"),
	CAPE("Capes"),
	WINGS("Wings"),
	PET("Pets"),
	EFFECT("Effects");

	private final String displayName;

	CosmeticSlot(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
