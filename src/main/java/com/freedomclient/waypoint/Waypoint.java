package com.freedomclient.waypoint;

import com.google.gson.JsonObject;

/** Punto guardado en el mundo. */
public class Waypoint {
	public String name;
	public final int x;
	public final int y;
	public final int z;
	public final String dimension;
	public final int color;
	public final boolean death;
	public boolean visible = true;

	public Waypoint(String name, int x, int y, int z, String dimension, int color, boolean death) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
		this.color = color;
		this.death = death;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("name", name);
		json.addProperty("x", x);
		json.addProperty("y", y);
		json.addProperty("z", z);
		json.addProperty("dimension", dimension);
		json.addProperty("color", color);
		json.addProperty("death", death);
		json.addProperty("visible", visible);
		return json;
	}

	public static Waypoint fromJson(JsonObject json) {
		Waypoint waypoint = new Waypoint(json.get("name").getAsString(), json.get("x").getAsInt(), json.get("y").getAsInt(),
				json.get("z").getAsInt(), json.get("dimension").getAsString(), json.get("color").getAsInt(),
				json.has("death") && json.get("death").getAsBoolean());
		if (json.has("visible")) waypoint.visible = json.get("visible").getAsBoolean();
		return waypoint;
	}
}
