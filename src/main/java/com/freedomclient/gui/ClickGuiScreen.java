package com.freedomclient.gui;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.performance.PerformanceSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Menú para activar y desactivar módulos, agrupados por categoría. Se abre con Shift derecho. */
public class ClickGuiScreen extends Screen {
	private static final int COLUMN_GAP = 8;
	private static final int BUTTON_HEIGHT = 20;
	private static final int TOP = 40;

	private final List<Header> headers = new ArrayList<>();

	public ClickGuiScreen() {
		super(Component.literal(FreedomClient.NAME));
	}

	@Override
	protected void init() {
		headers.clear();

		Category[] categories = Category.values();
		int columnWidth = Math.min(120, (width - 20 - COLUMN_GAP * (categories.length - 1)) / categories.length);
		int totalWidth = columnWidth * categories.length + COLUMN_GAP * (categories.length - 1);
		int startX = (width - totalWidth) / 2;

		int bottom = TOP + 14;
		for (int i = 0; i < categories.length; i++) {
			Category category = categories[i];
			int x = startX + i * (columnWidth + COLUMN_GAP);
			headers.add(new Header(category, x + columnWidth / 2));

			int y = TOP + 14;
			for (Module module : FreedomClient.getModuleManager().getModules(category)) {
				addRenderableWidget(Button.builder(label(module), button -> {
							module.toggle();
							button.setMessage(label(module));
						})
						.bounds(x, y, columnWidth, BUTTON_HEIGHT)
						.tooltip(Tooltip.create(Component.literal(module.getDescription())))
						.build());
				y += BUTTON_HEIGHT + 2;
			}
			bottom = Math.max(bottom, y);
		}

		addRenderableWidget(Button.builder(Component.literal("Optimizar ajustes para FPS"), button -> {
					PerformanceSettings.apply(minecraft);
					button.setMessage(Component.literal("Ajustes aplicados").withStyle(ChatFormatting.GREEN));
				})
				.bounds(width / 2 - 100, bottom + 10, 200, BUTTON_HEIGHT)
				.tooltip(Tooltip.create(Component.literal(
						"Desactiva VSync, nubes, sombras de entidades y mezcla de biomas, "
								+ "quita el límite de FPS y pone las partículas al mínimo.")))
				.build());
	}

	private static Component label(Module module) {
		return Component.literal(module.getName() + ": ")
				.append(module.isEnabled()
						? Component.literal("ON").withStyle(ChatFormatting.GREEN)
						: Component.literal("OFF").withStyle(ChatFormatting.RED));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);

		graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFFFF);
		for (Header header : headers) {
			graphics.drawCenteredString(font, header.category().getDisplayName(), header.centerX(), TOP, header.category().getColor());
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private record Header(Category category, int centerX) {
	}
}
