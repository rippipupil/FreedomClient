package com.freedomclient.module.hud;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Comparator;
import java.util.List;

public class ModuleListModule extends Module {
	public ModuleListModule() {
		super("ModuleList", "Muestra los módulos activos en la esquina superior derecha.", Category.HUD, true);
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
			int color = ColorUtil.rainbow(i * 150L);

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
