package com.freedomclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/** Botón que ejecuta una acción; no guarda ningún valor. */
public class ActionSetting extends Setting<Runnable> {
	private final String buttonLabel;

	public ActionSetting(String name, String description, String buttonLabel, Runnable action) {
		super(name, description, action);
		this.buttonLabel = buttonLabel;
	}

	public String getButtonLabel() {
		return buttonLabel;
	}

	public void run() {
		get().run();
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
