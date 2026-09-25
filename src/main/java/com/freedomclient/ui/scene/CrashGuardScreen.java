package com.freedomclient.ui.scene;

import com.freedomclient.ui.Draw;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.UiText;
import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.List;

/** Pantalla de Crash Guard: explica que el juego ha crasheado, dónde está el informe y deja volver al menú. */
public class CrashGuardScreen extends Screen {
	private static final int PANEL_WIDTH = 348;
	private static final int BUTTON_HEIGHT = 20;

	private final Ui ui = new Ui();
	private final String title;
	private final String error;
	private final Path reportFile;

	public CrashGuardScreen(String title, Throwable error, Path reportFile) {
		super(Component.literal("Crash Guard"));
		this.title = title;
		this.error = error.getClass().getSimpleName() + (error.getMessage() != null ? ": " + error.getMessage() : "");
		this.reportFile = reportFile;
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		PixelSky.render(graphics, width, height, 1.0F);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		ui.begin(graphics, mouseX, mouseY);

		List<FormattedCharSequence> errorLines = font.split(Component.literal(error), PANEL_WIDTH - 24);
		errorLines = errorLines.subList(0, Math.min(errorLines.size(), 4));
		int panelHeight = 118 + errorLines.size() * 10;
		int x = (width - PANEL_WIDTH) / 2;
		int y = (height - panelHeight) / 2;
		Draw.bevelPanel(graphics, x, y, PANEL_WIDTH, panelHeight, ThemeManager.withAlpha(ThemeManager.background(), 0.92F), ThemeManager.border());

		Component heading = UiText.title("The game crashed");
		graphics.drawString(font, heading, x + (PANEL_WIDTH - font.width(heading)) / 2, y + 10, 0xFFFF5555, true);
		graphics.drawCenteredString(font, "FreedomClient caught it and saved your world.", width / 2, y + 28, ThemeManager.text());

		int lineY = y + 46;
		graphics.drawString(font, title, x + 12, lineY, ThemeManager.accent(), true);
		lineY += 12;
		for (FormattedCharSequence line : errorLines) {
			graphics.drawString(font, line, x + 12, lineY, ThemeManager.textMuted(), true);
			lineY += 10;
		}
		graphics.drawString(font, "Report: crash-reports/" + reportFile.getFileName(), x + 12, lineY + 4, ThemeManager.textMuted(), true);

		int buttonY = y + panelHeight - BUTTON_HEIGHT - 10;
		int buttonWidth = (PANEL_WIDTH - 24 - 8) / 3;
		button("Back to menu", x + 12, buttonY, buttonWidth, () -> minecraft.setScreen(null));
		button("Open report", x + 12 + buttonWidth + 4, buttonY, buttonWidth, () -> Util.getPlatform().openPath(reportFile));
		button("Quit game", x + 12 + (buttonWidth + 4) * 2, buttonY, buttonWidth, minecraft::stop);

		ui.end();
	}

	private void button(String label, int x, int y, int w, Runnable action) {
		boolean hovered = ui.hovered(x, y, w, BUTTON_HEIGHT);
		float hover = ui.animate("crash:" + label, hovered ? 1.0F : 0.0F);
		int fill = ThemeManager.mix(ThemeManager.card(), ThemeManager.cardHover(), hover);
		int border = ThemeManager.mix(ThemeManager.border(), ThemeManager.accent(), hover);
		Draw.bevelPanel(ui.g, x, y, w, BUTTON_HEIGHT, fill, border);
		ui.g.drawCenteredString(font, label, x + w / 2, y + (BUTTON_HEIGHT - 8) / 2, ThemeManager.mix(ThemeManager.text(), ThemeManager.accent(), hover));
		ui.click(x, y, w, BUTTON_HEIGHT, (mx, my, button) -> {
			ui.playClick();
			action.run();
			return true;
		});
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return ui.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
