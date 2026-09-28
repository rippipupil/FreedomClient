package com.freedomclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/** Fila de vista previa dentro de los ajustes de un mod (no guarda nada): la dibuja quien la crea. */
public class PreviewSetting extends Setting<PreviewSetting.Renderer> {
	private final int height;

	public interface Renderer {
		void render(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, int width, int height);
	}

	public PreviewSetting(String name, String description, int height, Renderer renderer) {
		super(name, description, renderer);
		this.height = height;
	}

	public int getHeight() {
		return height;
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
