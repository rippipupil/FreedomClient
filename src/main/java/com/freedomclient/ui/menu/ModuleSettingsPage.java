package com.freedomclient.ui.menu;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.performance.BundledModModule;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.setting.HudPositionSetting;
import com.freedomclient.setting.KeybindSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.setting.PixelGridSetting;
import com.freedomclient.setting.Setting;
import com.freedomclient.setting.StringSetting;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.NeonStyle;
import com.freedomclient.ui.ScrollArea;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.UiText;
import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Página que se abre al hacer clic en la tarjeta de un mod: descripción, carpetas y sus ajustes por secciones. */
public class ModuleSettingsPage implements MenuPage {
	/** Secciones de ajustes, en este orden: el modo arriba, luego interruptores, valores, colores, texto, teclas y acciones. */
	private enum Section {
		MODE("Mode"),
		PREVIEW("Preview"),
		OPTIONS("Options"),
		VALUES("Values"),
		COLORS("Colors"),
		TEXT("Text"),
		KEYBINDS("Keybinds"),
		ACTIONS("Actions");

		final String title;

		Section(String title) {
			this.title = title;
		}

		static Section of(Setting<?> setting) {
			if (setting instanceof ModeSetting) return MODE;
			if (setting instanceof com.freedomclient.setting.PreviewSetting) return PREVIEW;
			if (setting instanceof BooleanSetting) return OPTIONS;
			if (setting instanceof NumberSetting || setting instanceof HudPositionSetting) return VALUES;
			if (setting instanceof ColorSetting || setting instanceof PixelGridSetting) return COLORS;
			if (setting instanceof StringSetting) return TEXT;
			if (setting instanceof KeybindSetting) return KEYBINDS;
			return ACTIONS;
		}
	}

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

	/** Carpetas del jugador: clic en una para meter o sacar este mod (las carpetas se crean en la pestaña Mods). */
	private int renderFolders(Ui ui, int x, int y, int w) {
		if (module.getCategory() == Category.COSMETICS || module.getCategory() == Category.HUD) return y;
		List<String> folders = ModFolders.names();
		ui.g.drawString(ui.font, "Folders", x + 1, y + 2, ThemeManager.textMuted(), false);
		int cx = x + ui.font.width("Folders") + 6;
		if (folders.isEmpty()) {
			ui.g.drawString(ui.font, "Create one with \"+ Folder\" in the Mods tab.", cx, y + 2, ThemeManager.textMuted(), false);
			return y + 18;
		}
		int rowY = y;
		for (String folder : folders) {
			boolean inside = ModFolders.contains(folder, module.getId());
			String label = (inside ? "- " : "+ ") + folder;
			int width = ui.font.width(label) + 10;
			if (cx + width > x + w) {
				cx = x + ui.font.width("Folders") + 6;
				rowY += 15;
			}
			boolean hovered = ui.hovered(cx, rowY, width, 12);
			int fill = inside ? ThemeManager.accent() : hovered ? ThemeManager.cardHover() : ThemeManager.card();
			Draw.panel(ui.g, cx, rowY, width, 12, fill, inside ? ThemeManager.accent() : ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.4F));
			ui.g.drawString(ui.font, label, cx + 5, rowY + 2, inside ? ThemeManager.shade() : ThemeManager.text(), false);
			ui.click(cx, rowY, width, 12, (mx, my, button) -> {
				ModFolders.toggle(folder, module.getId());
				ui.playClick();
				return true;
			});
			cx += width + 3;
		}
		return rowY + 20;
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

		Draw.iconBox(ui.g, module.getIcon(), x + 22, y, 24, module.getCategory().getColor());
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
		// Mismo margen a los dos lados: la barra de scroll va en el margen de la ventana.
		int innerW = w;
		int offset = scroll.begin(ui, x, listY, innerW, listH);
		int cursor = listY - offset;

		List<FormattedCharSequence> lines = ui.font.split(Component.literal(module.getDescription()), innerW);
		for (FormattedCharSequence line : lines) {
			ui.g.drawString(ui.font, line, x, cursor, ThemeManager.textMuted(), false);
			cursor += 10;
		}
		cursor += 4;

		cursor = renderFolders(ui, x, cursor, innerW);

		// Ajustes agrupados por tipo en secciones con título, para que cada cosa quede en su sitio.
		boolean anyVisible = false;
		for (Section section : Section.values()) {
			List<Setting<?>> settings = module.getSettings().stream()
					.filter(setting -> setting.isVisible() && Section.of(setting) == section)
					.toList();
			if (settings.isEmpty()) continue;
			anyVisible = true;
			ui.g.drawString(ui.font, section.title, x + 1, cursor + 2, ThemeManager.accent(), false);
			int lineX = x + ui.font.width(section.title) + 6;
			if (NeonStyle.on()) {
				NeonStyle.hLine(ui.g, lineX, x + innerW, cursor + 6, 1, section.ordinal() * 0.15, section.ordinal() * 0.15 + 0.5, 0.8F);
			} else {
				ui.g.fill(lineX, cursor + 6, x + innerW, cursor + 7, ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.5F));
			}
			cursor += 14;
			for (Setting<?> setting : settings) {
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
			cursor += 6;
		}
		if (!anyVisible) {
			ui.g.drawString(ui.font, "This mod has no settings.", x, cursor, ThemeManager.textMuted(), false);
			cursor += 10;
		}

		scroll.end(ui, x, listY, innerW, listH, cursor + offset - listY);
	}
}
