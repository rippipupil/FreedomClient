package com.freedomclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

public class ModeSetting extends Setting<String> {
	private final List<String> modes;

	public ModeSetting(String name, String description, String defaultValue, String... modes) {
		super(name, description, defaultValue);
		this.modes = List.of(modes);
		if (!this.modes.contains(defaultValue)) {
			throw new IllegalArgumentException("Default mode " + defaultValue + " is not in " + this.modes);
		}
	}

	public List<String> getModes() {
		return modes;
	}

	public boolean is(String mode) {
		return get().equals(mode);
	}

	public void cycle(int direction) {
		int index = Math.floorMod(modes.indexOf(get()) + direction, modes.size());
		set(modes.get(index));
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get());
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json.isJsonPrimitive() && modes.contains(json.getAsString())) {
			set(json.getAsString());
		}
	}
}
