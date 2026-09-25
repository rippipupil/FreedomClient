package com.freedomclient.hud;

import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Elemento de HUD de una línea de texto (FPS, ping, reloj...) con estilo configurable. */
public abstract class TextHudModule extends HudModule {
	private static final int PADDING_X = 4;
	private static final int PADDING_Y = 3;

	protected final ColorSetting textColor = add(new ColorSetting("Text color", "Color of the text.", 0xFFF5F1E8, false));
	protected final BooleanSetting background = add(new BooleanSetting("Background", "Draw a box behind the text.", true));
	protected final ColorSetting backgroundColor = add(new ColorSetting("Background color", "Color of the box behind the text.", 0x803A0F1A, true));
	protected final BooleanSetting shadow = add(new BooleanSetting("Text shadow", "Draw a shadow under the text.", true));

	protected TextHudModule(String name, String description, boolean enabledByDefault, HudPosition defaultPosition) {
		super(name, description, enabledByDefault, defaultPosition);
		backgroundColor.visibleWhen(background::get);
	}

	/** Texto actual, o {@code null} si no hay nada que mostrar. */
	protected abstract String getText(Minecraft client);

	/** Texto de ejemplo para el editor cuando no hay datos reales. */
	protected String getPreviewText() {
		return getName();
	}

	protected int getTextColor() {
		return textColor.get();
	}

	private String text(Minecraft client, boolean preview) {
		String text = client.player != null ? getText(client) : null;
		return text == null && preview ? getPreviewText() : text;
	}

	@Override
	public boolean shouldRender(Minecraft client) {
		return getText(client) != null;
	}

	@Override
	public int getWidth(Minecraft client, boolean preview) {
		String text = text(client, preview);
		return (text == null ? 0 : client.font.width(text)) + PADDING_X * 2;
	}

	@Override
	public int getHeight(Minecraft client, boolean preview) {
		return 8 + PADDING_Y * 2;
	}

	@Override
	public void render(GuiGraphics graphics, Minecraft client, boolean preview) {
		String text = text(client, preview);
		if (text == null) return;

		if (background.get()) {
			graphics.fill(0, 0, getWidth(client, preview), getHeight(client, preview), backgroundColor.get());
		}
		graphics.drawString(client.font, text, PADDING_X, PADDING_Y, getTextColor(), shadow.get());
	}
}
