package com.freedomclient.module.hud;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import net.minecraft.client.Minecraft;

/** Módulo de HUD que muestra una línea de texto en la esquina superior izquierda. */
public abstract class TextHudModule extends Module {
	protected final ColorSetting textColor = add(new ColorSetting("Text color", "Color of the text.", 0xFFF5F1E8, false));
	protected final BooleanSetting background = add(new BooleanSetting("Background", "Draw a dark box behind the text.", true));
	protected final BooleanSetting shadow = add(new BooleanSetting("Text shadow", "Draw a shadow under the text.", true));

	protected TextHudModule(String name, String description, boolean enabledByDefault) {
		super(name, description, Category.HUD, enabledByDefault);
	}

	/** Texto a mostrar, o {@code null} para no mostrar nada este frame. */
	public abstract String getText(Minecraft client);

	public int getColor() {
		return textColor.get();
	}

	public boolean hasBackground() {
		return background.get();
	}

	public boolean hasShadow() {
		return shadow.get();
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
