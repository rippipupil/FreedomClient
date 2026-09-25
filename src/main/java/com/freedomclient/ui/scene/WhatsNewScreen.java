package com.freedomclient.ui.scene;

import com.freedomclient.ui.Draw;
import com.freedomclient.ui.ScrollArea;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.UiText;
import com.freedomclient.ui.theme.ThemeManager;
import com.freedomclient.update.WhatsNew;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Pantalla de novedades que sale una vez tras actualizar FreedomClient. */
public class WhatsNewScreen extends Screen {
	private static final int PANEL_WIDTH = 340;
	private static final int PANEL_HEIGHT = 230;

	private final Screen parent;
	private final Ui ui = new Ui();
	private final ScrollArea scroll = new ScrollArea();
	private final List<WhatsNew.Entry> entries = WhatsNew.entries();

	public WhatsNewScreen(Screen parent) {
		super(Component.literal("What's new"));
		this.parent = parent;
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		PixelSky.render(graphics, width, height, 1.0F);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		ui.begin(graphics, mouseX, mouseY);
		int x = (width - PANEL_WIDTH) / 2;
		int y = (height - PANEL_HEIGHT) / 2;
		Draw.bevelPanel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, ThemeManager.withAlpha(ThemeManager.background(), 0.94F), ThemeManager.border());
		PixelSky.halo(graphics, x + 22, y + 9, 1, 6, 1.0F);
		PixelSky.logo(graphics, x + 22, y + 19, 1, 1.0F);
		graphics.drawString(font, UiText.title("What's new"), x + 40, y + 10, ThemeManager.accent(), true);

		// Lista de cambios con scroll.
		int listX = x + 12;
		int listY = y + 34;
		int listW = PANEL_WIDTH - 24;
		int listH = PANEL_HEIGHT - 34 - 34;
		int offset = scroll.begin(ui, listX, listY, listW, listH);
		int cursor = listY - offset;
		for (WhatsNew.Entry entry : entries) {
			graphics.drawString(font, entry.title(), listX, cursor, ThemeManager.highlight(), false);
			cursor += 12;
			for (String item : entry.items()) {
				List<FormattedCharSequence> lines = font.split(Component.literal(item), listW - 16);
				graphics.fill(listX + 3, cursor + 3, listX + 6, cursor + 6, ThemeManager.accent());
				for (FormattedCharSequence line : lines) {
					graphics.drawString(font, line, listX + 10, cursor, ThemeManager.text(), false);
					cursor += 10;
				}
				cursor += 2;
			}
			cursor += 6;
		}
		scroll.end(ui, listX, listY, listW, listH, cursor + offset - listY);

		// Botón para seguir.
		int buttonW = 100;
		int buttonX = x + (PANEL_WIDTH - buttonW) / 2;
		int buttonY = y + PANEL_HEIGHT - 26;
		boolean hovered = ui.hovered(buttonX, buttonY, buttonW, 18);
		Draw.bevelPanel(graphics, buttonX, buttonY, buttonW, 18, hovered ? ThemeManager.cardHover() : ThemeManager.card(),
				hovered ? ThemeManager.accent() : ThemeManager.border());
		graphics.drawCenteredString(font, "Let's go!", buttonX + buttonW / 2, buttonY + 5, hovered ? ThemeManager.accent() : ThemeManager.text());
		ui.click(buttonX, buttonY, buttonW, 18, (mx, my, button) -> {
			ui.playClick();
			onClose();
			return true;
		});
		ui.end();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return ui.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return ui.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
