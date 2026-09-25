package com.freedomclient.setting;

import com.freedomclient.hud.HudPosition;
import com.google.gson.JsonElement;

/** Guarda la posición y escala de un elemento del HUD. No se muestra en la página de ajustes. */
public class HudPositionSetting extends Setting<HudPosition> {
	public HudPositionSetting(HudPosition position) {
		super("Position", "Where the element is drawn.", position);
		visibleWhen(() -> false);
	}

	@Override
	public void reset() {
		get().reset();
	}

	@Override
	public JsonElement toJson() {
		return get().toJson();
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json.isJsonObject()) {
			get().fromJson(json.getAsJsonObject());
		}
	}
}
