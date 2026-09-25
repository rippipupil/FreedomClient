package com.freedomclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/** Tecla del teclado (código GLFW); {@link #NONE} si no hay ninguna asignada. */
public class KeybindSetting extends Setting<Integer> {
	public static final int NONE = GLFW.GLFW_KEY_UNKNOWN;

	public KeybindSetting(String name, String description, int defaultKey) {
		super(name, description, defaultKey);
	}

	public boolean isBound() {
		return get() != NONE;
	}

	public String getKeyName() {
		if (!isBound()) return "None";
		return InputConstants.Type.KEYSYM.getOrCreate(get()).getDisplayName().getString();
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get());
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
			set(json.getAsInt());
		}
	}
}
