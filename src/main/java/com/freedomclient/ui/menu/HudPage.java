package com.freedomclient.ui.menu;

import com.freedomclient.hud.HudEditorScreen;
import com.freedomclient.module.Category;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.theme.ThemeManager;

/** Pestaña HUD: botón para abrir el editor y las tarjetas de todos los elementos del HUD. */
public class HudPage implements MenuPage {
	private static final int BUTTON_HEIGHT = 18;

	private final FreedomMenuScreen screen;
	private final ModGridPage grid;

	public HudPage(FreedomMenuScreen screen) {
		this.screen = screen;
		this.grid = new ModGridPage(screen, Category.HUD);
	}

	@Override
	public void render(Ui ui, int x, int y, int w, int h) {
		String label = "Edit HUD layout";
		int buttonW = w - 6;
		boolean hovered = ui.hovered(x, y, buttonW, BUTTON_HEIGHT);
		int fill = hovered ? ThemeManager.highlight() : ThemeManager.accent();
		Draw.bevelPanel(ui.g, x, y, buttonW, BUTTON_HEIGHT, fill, ThemeManager.mix(fill, 0xFF000000, 0.4F));
		ui.g.drawCenteredString(ui.font, label, x + buttonW / 2, y + 5, ThemeManager.shade());
		ui.click(x, y, buttonW, BUTTON_HEIGHT, (mx, my, button) -> {
			ui.playClick();
			ui.minecraft.setScreen(new HudEditorScreen(screen));
			return true;
		});

		grid.render(ui, x, y + BUTTON_HEIGHT + 6, w, h - BUTTON_HEIGHT - 6);
	}
}
