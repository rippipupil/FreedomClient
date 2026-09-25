package com.freedomclient.ui;

import com.freedomclient.ui.theme.ThemeManager;
import org.lwjgl.glfw.GLFW;

/** Campo de texto de una línea para el menú (búsqueda). */
public class TextField {
	private final int maxLength;
	private String text = "";

	public TextField(int maxLength) {
		this.maxLength = maxLength;
	}

	public String getText() {
		return text;
	}

	public void render(Ui ui, int x, int y, int w, int h, String placeholder) {
		boolean focused = ui.isFocused(this);
		boolean hovered = ui.hovered(x, y, w, h);
		int border = focused ? ThemeManager.accent() : hovered ? ThemeManager.highlight() : ThemeManager.mix(ThemeManager.border(), 0xFF000000, 0.35F);
		Draw.panel(ui.g, x, y, w, h, ThemeManager.shade(), border);

		int textY = y + (h - 8) / 2;
		String shown = text.isEmpty() && !focused ? placeholder : text;
		int color = text.isEmpty() && !focused ? ThemeManager.textMuted() : ThemeManager.text();
		String visible = ui.font.plainSubstrByWidth(shown, w - 10, true);
		ui.g.drawString(ui.font, visible, x + 4, textY, color, false);

		if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
			int caretX = x + 4 + ui.font.width(visible);
			ui.g.fill(caretX, textY - 1, caretX + 1, textY + 9, ThemeManager.accent());
		}

		ui.click(x, y, w, h, (mx, my, button) -> {
			if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) text = "";
			ui.focus(this);
			return true;
		});
	}

	public boolean keyPressed(int key, boolean control) {
		switch (key) {
			case GLFW.GLFW_KEY_BACKSPACE -> {
				if (control) {
					text = "";
				} else if (!text.isEmpty()) {
					text = text.substring(0, text.offsetByCodePoints(text.length(), -1));
				}
				return true;
			}
			case GLFW.GLFW_KEY_DELETE -> {
				text = "";
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	public void charTyped(String chars) {
		if (text.length() + chars.length() <= maxLength) {
			text += chars;
		}
	}
}
