package com.freedomclient.waypoint;

import com.freedomclient.setting.Setting;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/** Marcador para que la página de ajustes muestre la lista de waypoints del mundo actual (se guardan aparte). */
public class WaypointListSetting extends Setting<Object> {
	public WaypointListSetting() {
		super("Waypoints in this world", "Toggle or delete your waypoints.", null);
	}

	@Override
	public boolean isSaved() {
		return false;
	}

	@Override
	public JsonElement toJson() {
		return JsonNull.INSTANCE;
	}

	@Override
	public void fromJson(JsonElement json) {
	}
}
