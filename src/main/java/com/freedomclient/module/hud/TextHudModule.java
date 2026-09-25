package com.freedomclient.module.hud;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import net.minecraft.client.Minecraft;

/** Módulo de HUD que muestra una línea de texto en la esquina superior izquierda. */
public abstract class TextHudModule extends Module {
	protected TextHudModule(String name, String description, boolean enabledByDefault) {
		super(name, description, Category.HUD, enabledByDefault);
	}

	/** Texto a mostrar, o {@code null} para no mostrar nada este frame. */
	public abstract String getText(Minecraft client);

	public int getColor() {
		return 0xFFFFFFFF;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
