package com.freedomclient.module.hud;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Comparator;
import java.util.List;

public class ModuleListModule extends Module {
	private final BooleanSetting rainbow = add(new BooleanSetting("Rainbow", "Animated rainbow colors.", true));
	private final ColorSetting color = add(new ColorSetting("Color", "Text color when rainbow is off.", 0xFFF2C94C, false));

	public ModuleListModule() {
		super("Module List", "Shows your active mods in the top right corner.", Category.HUD, true);
		color.visibleWhen(() -> !rainbow.get());
	}

	public void render(GuiGraphics graphics, Minecraft client) {
		List<Module> active = FreedomClient.getModuleManager().getModules().stream()
				.filter(module -> module.isEnabled() && module.isVisibleInModuleList())
				.sorted(Comparator.comparingInt((Module module) -> client.font.width(module.getName())).reversed())
				.toList();

		int right = graphics.guiWidth() - 2;
		int y = 2;
		for (int i = 0; i < active.size(); i++) {
			String name = active.get(i).getName();
			int width = client.font.width(name);
			int color = rainbow.get() ? ColorUtil.rainbow(i * 150L) : this.color.get();

			graphics.fill(right - width - 4, y, right, y + 11, 0x80000000);
			graphics.fill(right, y, right + 1, y + 11, color);
			graphics.drawString(client.font, name, right - width - 2, y + 2, color, true);
			y += 11;
		}
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
