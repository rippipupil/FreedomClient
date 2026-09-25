package com.freedomclient.gui;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.module.hud.ArmorStatusModule;
import com.freedomclient.module.hud.KeystrokesModule;
import com.freedomclient.module.hud.ModuleListModule;
import com.freedomclient.module.hud.TextHudModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class HudRenderer {
	private static final int MARGIN = 4;
	private static final int LINE_HEIGHT = 11;

	private HudRenderer() {
	}

	public static void render(GuiGraphics graphics) {
		Minecraft client = Minecraft.getInstance();
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null || client.player == null || client.options.hideGui) return;

		int y = MARGIN;
		for (Module module : manager.getModules()) {
			if (module instanceof TextHudModule text && text.isEnabled()) {
				String line = text.getText(client);
				if (line == null) continue;

				int width = client.font.width(line);
				graphics.fill(MARGIN - 2, y - 2, MARGIN + width + 2, y + LINE_HEIGHT - 2, 0x80000000);
				graphics.drawString(client.font, line, MARGIN, y, text.getColor(), true);
				y += LINE_HEIGHT;
			}
		}

		KeystrokesModule keystrokes = manager.get(KeystrokesModule.class);
		if (keystrokes.isEnabled()) {
			y += MARGIN;
			y += keystrokes.render(graphics, client, MARGIN, y);
		}

		ArmorStatusModule armor = manager.get(ArmorStatusModule.class);
		if (armor.isEnabled()) {
			y += MARGIN;
			armor.render(graphics, client, MARGIN, y);
		}

		ModuleListModule moduleList = manager.get(ModuleListModule.class);
		if (moduleList.isEnabled()) {
			moduleList.render(graphics, client);
		}
	}
}
