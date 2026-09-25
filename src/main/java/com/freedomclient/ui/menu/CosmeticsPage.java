package com.freedomclient.ui.menu;

import com.freedomclient.FreedomClient;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.resources.Identifier;

/** Pestaña Cosmetics: alas, halo y capa de Angel Devil (llegan en la fase 4). */
public class CosmeticsPage implements MenuPage {
	private record Cosmetic(String name, String description, String icon) {
	}

	private static final Cosmetic[] COSMETICS = {
			new Cosmetic("Wings", "White angel wings", "wings"),
			new Cosmetic("Halo", "Floating golden halo", "halo"),
			new Cosmetic("Cape", "FreedomClient cape", "cape"),
	};

	@Override
	public void render(Ui ui, int x, int y, int w, int h) {
		int cardW = (w - 8) / 3;
		int cardH = 70;
		for (int i = 0; i < COSMETICS.length; i++) {
			Cosmetic cosmetic = COSMETICS[i];
			int cx = x + i * (cardW + 4);
			Draw.bevelPanel(ui.g, cx, y, cardW, cardH, ThemeManager.card(), ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.45F));

			Identifier icon = FreedomClient.id("textures/icon/" + cosmetic.icon() + ".png");
			Draw.panel(ui.g, cx + cardW / 2 - 18, y + 6, 36, 36, ThemeManager.shade(), ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.3F));
			Draw.icon(ui.g, icon, cx + cardW / 2 - 16, y + 8, 32);

			ui.g.drawCenteredString(ui.font, cosmetic.name(), cx + cardW / 2, y + 46, ThemeManager.text());
			ui.g.drawCenteredString(ui.font, "Coming soon", cx + cardW / 2, y + 57, ThemeManager.accent());
			if (ui.hovered(cx, y, cardW, cardH)) ui.tooltip(cosmetic.description());
		}

		ui.g.drawString(ui.font, "Wings, halo and cape arrive in a future update.", x, y + cardH + 8, ThemeManager.textMuted(), false);
	}
}
