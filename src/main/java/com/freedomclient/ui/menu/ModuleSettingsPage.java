package com.freedomclient.ui.menu;

import com.freedomclient.module.Module;
import com.freedomclient.module.performance.BundledModModule;
import com.freedomclient.setting.Setting;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.ScrollArea;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.UiText;
import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Página que se abre al hacer clic en la tarjeta de un mod: descripción y todos sus ajustes. */
public class ModuleSettingsPage implements MenuPage {
	private final FreedomMenuScreen screen;
	private final Module module;
	private final ScrollArea scroll = new ScrollArea();
	private final SettingRows rows = new SettingRows();
	/** Texto buscado: las opciones que lo contienen se marcan (vacío si no se viene de una búsqueda). */
	private final String highlight;

	public ModuleSettingsPage(FreedomMenuScreen screen, Module module) {
		this(screen, module, "");
	}

	public ModuleSettingsPage(FreedomMenuScreen screen, Module module, String highlight) {
		this.screen = screen;
		this.module = module;
		this.highlight = highlight;
	}

	@Override
	public void render(Ui ui, int x, int y, int w, int h) {
		// Cabecera: volver, icono, nombre e interruptor.
		boolean backHovered = ui.hovered(x, y, 16, 24);
		Draw.panel(ui.g, x, y + 4, 16, 16, backHovered ? ThemeManager.cardHover() : ThemeManager.card(),
				backHovered ? ThemeManager.highlight() : ThemeManager.border());
		ui.g.drawString(ui.font, "<", x + 6, y + 8, ThemeManager.text(), false);
		ui.click(x, y, 16, 24, (mx, my, button) -> {
			screen.closeModule();
			ui.playClick();
			return true;
		});

		Draw.iconBox(ui.g, module.getIcon(), x + 22, y, 24);
		ui.g.drawString(ui.font, UiText.title(module.getName()), x + 52, y + 2, ThemeManager.text(), false);
		ui.g.drawString(ui.font, module.getCategory().getDisplayName(), x + 52, y + 15, ThemeManager.textMuted(), false);

		if (module.canToggle()) {
			int toggleX = x + w - 26;
			boolean hovered = ui.hovered(toggleX - 2, y + 5, 24, 14);
			Draw.toggle(ui.g, toggleX, y + 7, ui.animate("toggle:" + module.getId(), module.isEnabled() ? 1.0F : 0.0F), hovered);
			ui.click(toggleX - 2, y + 5, 24, 14, (mx, my, button) -> {
				module.toggle();
				ui.playClick();
				return true;
			});
		} else if (module instanceof BundledModModule) {
			String label = "Always on";
			ui.g.drawString(ui.font, label, x + w - ui.font.width(label) - 2, y + 9, ThemeManager.accent(), false);
		}

		// Descripción y ajustes, con scroll.
		int listY = y + 30;
		int listH = h - 30;
		int innerW = w - 6;
		int offset = scroll.begin(ui, x, listY, innerW, listH);
		int cursor = listY - offset;

		List<FormattedCharSequence> lines = ui.font.split(Component.literal(module.getDescription()), innerW);
		for (FormattedCharSequence line : lines) {
			ui.g.drawString(ui.font, line, x, cursor, ThemeManager.textMuted(), false);
			cursor += 10;
		}
		cursor += 4;

		boolean anyVisible = false;
		for (Setting<?> setting : module.getSettings()) {
			if (!setting.isVisible()) continue;
			anyVisible = true;
			int rowHeight = rows.render(ui, setting, x, cursor, innerW);
			if (ModGridPage.settingMatches(setting, highlight)) {
				// Opción encontrada con el buscador: marco con el color de acento.
				int accent = ThemeManager.accent();
				ui.g.fill(x, cursor, x + innerW, cursor + 1, accent);
				ui.g.fill(x, cursor + rowHeight - 1, x + innerW, cursor + rowHeight, accent);
				ui.g.fill(x, cursor, x + 1, cursor + rowHeight, accent);
				ui.g.fill(x + innerW - 1, cursor, x + innerW, cursor + rowHeight, accent);
			}
			cursor += rowHeight + SettingRows.ROW_GAP;
		}
		if (!anyVisible) {
			ui.g.drawString(ui.font, "This mod has no settings.", x, cursor, ThemeManager.textMuted(), false);
			cursor += 10;
		}

		scroll.end(ui, x, listY, innerW, listH, cursor + offset - listY);
	}
}
