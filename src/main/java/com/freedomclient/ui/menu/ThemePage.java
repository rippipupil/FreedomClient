package com.freedomclient.ui.menu;

import com.freedomclient.ui.Draw;
import com.freedomclient.ui.ScrollArea;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.UiText;
import com.freedomclient.ui.theme.ThemeColor;
import com.freedomclient.ui.theme.ThemeManager;
import com.freedomclient.ui.theme.ThemePreset;

/** Pestaña Theme: temas ya hechos, cada color editable y la opacidad de la ventana. */
public class ThemePage implements MenuPage {
	private static final int PRESET_HEIGHT = 30;

	private final ScrollArea scroll = new ScrollArea();
	private final SettingRows rows = new SettingRows();

	@Override
	public void render(Ui ui, int x, int y, int w, int h) {
		int innerW = w - 6;
		int offset = scroll.begin(ui, x, y, innerW, h);
		int cursor = y - offset;

		cursor = heading(ui, "Presets", x, cursor);
		ThemePreset[] presets = ThemePreset.values();
		int columns = innerW >= 300 ? 4 : 2;
		int presetW = (innerW - 4 * (columns - 1)) / columns;
		for (int i = 0; i < presets.length; i++) {
			int px = x + (i % columns) * (presetW + 4);
			int py = cursor + (i / columns) * (PRESET_HEIGHT + 4);
			renderPreset(ui, presets[i], px, py, presetW);
		}
		cursor += ((presets.length + columns - 1) / columns) * (PRESET_HEIGHT + 4) + 4;

		cursor = heading(ui, "Colors", x, cursor);
		for (ThemeColor color : ThemeColor.values()) {
			String name = color.getDisplayName() + (ThemeManager.isCustomized(color) ? "  (custom)" : "");
			cursor += rows.renderColor(ui, color, name, color.getDescription(), x, cursor, innerW,
					() -> ThemeManager.get(color), value -> ThemeManager.setColor(color, value), false) + SettingRows.ROW_GAP;
		}

		// Opacidad de la ventana.
		int rowH = SettingRows.ROW_HEIGHT + 8;
		rows.row(ui, "Window opacity", "How see-through the menu background is.", x, cursor, innerW, rowH);
		String value = Math.round(ThemeManager.getOpacity() * 100) + "%";
		ui.g.drawString(ui.font, value, x + innerW - 6 - ui.font.width(value), cursor + 5, ThemeManager.accent(), false);
		int sliderX = x + 6;
		int sliderW = innerW - 12;
		int sliderY = cursor + 17;
		double progress = (ThemeManager.getOpacity() - 0.3) / 0.7;
		Draw.slider(ui.g, sliderX, sliderY, sliderW, progress, ui.hovered(sliderX - 2, sliderY - 3, sliderW + 4, 12));
		ui.click(sliderX - 2, sliderY - 3, sliderW + 4, 12, (mx, my, button) -> {
			ui.startDrag((dx, dy) -> ThemeManager.setOpacity(0.3F + 0.7F * (float) ((dx - sliderX) / sliderW)), mx, my);
			return true;
		});
		cursor += rowH + SettingRows.ROW_GAP + 4;

		// Botón para quitar los colores personalizados.
		String reset = "Reset colors to preset";
		int buttonW = ui.font.width(reset) + 16;
		boolean hovered = ui.hovered(x, cursor, buttonW, 16);
		Draw.bevelPanel(ui.g, x, cursor, buttonW, 16, hovered ? ThemeManager.cardHover() : ThemeManager.card(),
				hovered ? ThemeManager.highlight() : ThemeManager.border());
		ui.g.drawString(ui.font, reset, x + 8, cursor + 4, ThemeManager.text(), false);
		ui.click(x, cursor, buttonW, 16, (mx, my, button) -> {
			ThemeManager.resetColors();
			ui.playClick();
			return true;
		});
		cursor += 20;

		scroll.end(ui, x, y, innerW, h, cursor + offset - y);
	}

	private int heading(Ui ui, String text, int x, int y) {
		ui.g.drawString(ui.font, UiText.title(text), x, y, ThemeManager.accent(), false);
		return y + 16;
	}

	private void renderPreset(Ui ui, ThemePreset preset, int x, int y, int w) {
		boolean selected = ThemeManager.getPreset() == preset;
		boolean hovered = ui.hovered(x, y, w, PRESET_HEIGHT);
		int border = selected ? ThemeManager.accent() : hovered ? ThemeManager.highlight() : ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.45F);
		Draw.bevelPanel(ui.g, x, y, w, PRESET_HEIGHT, preset.get(ThemeColor.BACKGROUND), border);
		ui.g.drawString(ui.font, ui.font.plainSubstrByWidth(preset.getDisplayName(), w - 10), x + 5, y + 5, preset.get(ThemeColor.TEXT), false);

		ThemeColor[] colors = ThemeColor.values();
		for (int i = 0; i < colors.length; i++) {
			int sx = x + 5 + i * 8;
			ui.g.fill(sx, y + 18, sx + 6, y + 24, preset.get(colors[i]));
		}

		ui.click(x, y, w, PRESET_HEIGHT, (mx, my, button) -> {
			ThemeManager.setPreset(preset);
			ui.playClick();
			return true;
		});
	}
}
