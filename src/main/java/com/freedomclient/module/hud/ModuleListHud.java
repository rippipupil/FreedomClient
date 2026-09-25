package com.freedomclient.module.hud;

import com.freedomclient.FreedomClient;
import com.freedomclient.hud.HudModule;
import com.freedomclient.hud.HudPosition;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Comparator;
import java.util.List;

/** Lista de mods activos, ordenada de más largo a más corto. Se alinea al lado de la pantalla donde esté. */
public class ModuleListHud extends HudModule {
	private static final int LINE = 11;

	private final BooleanSetting rainbow = add(new BooleanSetting("Rainbow", "Animated rainbow colors.", true));
	private final ColorSetting color = add(new ColorSetting("Color", "Text color when rainbow is off.", 0xFFF2C94C, false));
	private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a box behind each line.", true));

	public ModuleListHud() {
		super("Module List", "Shows your active mods.", true, new HudPosition(HudPosition.Anchor.END, 2, HudPosition.Anchor.START, 2));
		color.visibleWhen(() -> !rainbow.get());
	}

	private List<String> names(Minecraft client, boolean preview) {
		List<String> names = FreedomClient.getModuleManager().getModules().stream()
				.filter(module -> module.isEnabled() && module.isVisibleInModuleList())
				.map(Module::getName)
				.sorted(Comparator.comparingInt((String name) -> client.font.width(name)).reversed())
				.toList();
		return names.isEmpty() && preview ? List.of("Toggle Sprint", "Zoom") : names;
	}

	@Override
	public boolean shouldRender(Minecraft client) {
		return !names(client, false).isEmpty();
	}

	@Override
	public int getWidth(Minecraft client, boolean preview) {
		return names(client, preview).stream().mapToInt(client.font::width).max().orElse(20) + 6;
	}

	@Override
	public int getHeight(Minecraft client, boolean preview) {
		return Math.max(1, names(client, preview).size()) * LINE;
	}

	@Override
	public void render(GuiGraphics graphics, Minecraft client, boolean preview) {
		int width = getWidth(client, preview);
		boolean alignRight = getPosition().resolveX(graphics.guiWidth(), width) + width / 2 > graphics.guiWidth() / 2;
		List<String> names = names(client, preview);

		for (int i = 0; i < names.size(); i++) {
			String name = names.get(i);
			int textWidth = client.font.width(name);
			int lineColor = rainbow.get() ? ColorUtil.rainbow(i * 150L) : color.get();
			int y = i * LINE;
			int x = alignRight ? width - textWidth - 4 : 3;

			if (background.get()) graphics.fill(x - 3, y, x + textWidth + 3, y + LINE, 0x803A0F1A);
			int barX = alignRight ? width - 1 : 0;
			graphics.fill(barX, y, barX + 1, y + LINE, lineColor);
			graphics.drawString(client.font, name, x, y + 2, lineColor, true);
		}
	}
}
