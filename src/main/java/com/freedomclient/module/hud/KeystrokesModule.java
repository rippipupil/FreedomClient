package com.freedomclient.module.hud;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;

public class KeystrokesModule extends Module {
	private static final int KEY = 22;
	private static final int GAP = 2;

	public KeystrokesModule() {
		super("Keystrokes", "Muestra las teclas de movimiento y los clics que pulsas.", Category.HUD, true);
	}

	/** Dibuja las teclas y devuelve la altura ocupada. */
	public int render(GuiGraphics graphics, Minecraft client, int x, int y) {
		Options options = client.options;
		int row = KEY + GAP;
		int full = KEY * 3 + GAP * 2;
		int half = (full - GAP) / 2;

		drawKey(graphics, client, options.keyUp, "W", x + row, y, KEY, KEY);
		drawKey(graphics, client, options.keyLeft, "A", x, y + row, KEY, KEY);
		drawKey(graphics, client, options.keyDown, "S", x + row, y + row, KEY, KEY);
		drawKey(graphics, client, options.keyRight, "D", x + row * 2, y + row, KEY, KEY);
		drawKey(graphics, client, options.keyAttack, "LMB", x, y + row * 2, half, KEY);
		drawKey(graphics, client, options.keyUse, "RMB", x + half + GAP, y + row * 2, half, KEY);
		drawKey(graphics, client, options.keyJump, "SPACE", x, y + row * 3, full, 12);

		return row * 3 + 12;
	}

	private static void drawKey(GuiGraphics graphics, Minecraft client, KeyMapping key, String label,
			int x, int y, int width, int height) {
		boolean down = key.isDown();
		graphics.fill(x, y, x + width, y + height, down ? 0xC0FFFFFF : 0x80000000);

		int textX = x + (width - client.font.width(label)) / 2;
		int textY = y + (height - 8) / 2;
		graphics.drawString(client.font, label, textX, textY, down ? 0xFF000000 : 0xFFFFFFFF, false);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
