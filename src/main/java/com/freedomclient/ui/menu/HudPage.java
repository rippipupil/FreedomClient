package com.freedomclient.ui.menu;

import com.freedomclient.module.Category;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.theme.ThemeManager;

/** Pestaña HUD: aviso del editor que llega en la fase 2 y las tarjetas de los elementos de HUD. */
public class HudPage implements MenuPage {
	private final ModGridPage grid;

	public HudPage(FreedomMenuScreen screen) {
		this.grid = new ModGridPage(screen, Category.HUD);
	}

	@Override
	public void render(Ui ui, int x, int y, int w, int h) {
		int bannerH = 18;
		Draw.panel(ui.g, x, y, w - 6, bannerH, ThemeManager.shade(), ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.3F));
		ui.g.drawString(ui.font, "Drag & resize HUD editor: coming in the next update", x + 6, y + 5, ThemeManager.textMuted(), false);
		grid.render(ui, x, y + bannerH + 6, w, h - bannerH - 6);
	}
}
