package com.freedomclient.ui.menu;

import com.freedomclient.module.Category;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.theme.ThemeManager;

/** Pestaña Cosmetics: alas, halo y capa de Angel Devil. Por ahora solo los ves tú (F5 e inventario). */
public class CosmeticsPage implements MenuPage {
	private static final int NOTE_HEIGHT = 18;

	private final ModGridPage grid;

	public CosmeticsPage(FreedomMenuScreen screen) {
		this.grid = new ModGridPage(screen, Category.COSMETICS);
	}

	@Override
	public void render(Ui ui, int x, int y, int w, int h) {
		Draw.panel(ui.g, x, y, w - 6, NOTE_HEIGHT, ThemeManager.shade(), ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.3F));
		ui.g.drawString(ui.font, "Only you can see your cosmetics for now (press F5 to look).", x + 6, y + 5, ThemeManager.textMuted(), false);
		grid.render(ui, x, y + NOTE_HEIGHT + 6, w, h - NOTE_HEIGHT - 6);
	}
}
