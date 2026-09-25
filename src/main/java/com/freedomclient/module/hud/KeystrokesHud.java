package com.freedomclient.module.hud;

import com.freedomclient.hud.ClickTracker;
import com.freedomclient.hud.HudModule;
import com.freedomclient.hud.HudPosition;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;

public class KeystrokesHud extends HudModule {
	private static final int KEY = 22;
	private static final int GAP = 2;
	private static final int ROW = KEY + GAP;
	private static final int FULL = KEY * 3 + GAP * 2;

	private final BooleanSetting showMouse = add(new BooleanSetting("Show mouse", "Show left and right mouse buttons.", true));
	private final BooleanSetting showCps = add(new BooleanSetting("Show CPS", "Show clicks per second under the mouse buttons.", true));
	private final BooleanSetting showSpace = add(new BooleanSetting("Show space", "Show the jump key.", true));
	private final ColorSetting keyColor = add(new ColorSetting("Key color", "Background of released keys.", 0x803A0F1A, true));
	private final ColorSetting pressedColor = add(new ColorSetting("Pressed color", "Background of pressed keys.", 0xE0F2C94C, true));
	private final ColorSetting textColor = add(new ColorSetting("Text color", "Color of the key labels.", 0xFFF5F1E8, false));
	private final ColorSetting pressedTextColor = add(new ColorSetting("Pressed text color", "Label color while pressed.", 0xFF3A0F1A, false));

	public KeystrokesHud() {
		super("Keystrokes", "Shows your movement keys, clicks and CPS.", true, new HudPosition(HudPosition.Anchor.START, 2, HudPosition.Anchor.END, 2));
		showCps.visibleWhen(showMouse::get);
	}

	@Override
	public int getWidth(Minecraft client, boolean preview) {
		return FULL;
	}

	@Override
	public int getHeight(Minecraft client, boolean preview) {
		int height = ROW * 2 - GAP;
		if (showMouse.get()) height += ROW;
		if (showSpace.get()) height += 12 + GAP;
		return height;
	}

	@Override
	public void render(GuiGraphics graphics, Minecraft client, boolean preview) {
		Options options = client.options;
		int half = (FULL - GAP) / 2;

		drawKey(graphics, client, options.keyUp, "W", ROW, 0, KEY, KEY);
		drawKey(graphics, client, options.keyLeft, "A", 0, ROW, KEY, KEY);
		drawKey(graphics, client, options.keyDown, "S", ROW, ROW, KEY, KEY);
		drawKey(graphics, client, options.keyRight, "D", ROW * 2, ROW, KEY, KEY);

		int y = ROW * 2;
		if (showMouse.get()) {
			boolean cps = showCps.get();
			drawKey(graphics, client, options.keyAttack, cps ? ClickTracker.leftCps() + " CPS" : "LMB", 0, y, half, KEY);
			drawKey(graphics, client, options.keyUse, cps ? ClickTracker.rightCps() + " CPS" : "RMB", half + GAP, y, half, KEY);
			y += ROW;
		}
		if (showSpace.get()) {
			boolean down = options.keyJump.isDown();
			graphics.fill(0, y, FULL, y + 12, down ? pressedColor.get() : keyColor.get());
			int barColor = down ? pressedTextColor.get() : textColor.get();
			graphics.fill(FULL / 2 - 10, y + 5, FULL / 2 + 10, y + 7, barColor);
		}
	}

	private void drawKey(GuiGraphics graphics, Minecraft client, KeyMapping key, String label, int x, int y, int width, int height) {
		boolean down = key.isDown();
		graphics.fill(x, y, x + width, y + height, down ? pressedColor.get() : keyColor.get());

		int textX = x + (width - client.font.width(label)) / 2;
		int textY = y + (height - 8) / 2;
		graphics.drawString(client.font, label, textX, textY, down ? pressedTextColor.get() : textColor.get(), false);
	}
}
