package com.freedomclient.ui.menu;

import com.freedomclient.setting.ActionSetting;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.setting.KeybindSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.setting.Setting;
import com.freedomclient.ui.ColorPicker;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.theme.ThemeManager;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/** Dibuja las filas de ajustes (interruptor, deslizador, selector, color, tecla y botón). */
public class SettingRows {
	public static final int ROW_HEIGHT = 18;
	public static final int ROW_GAP = 3;

	private final Map<Object, ColorPicker> pickers = new HashMap<>();
	private final Map<ActionSetting, Long> actionFeedback = new HashMap<>();
	private Object expandedColor;

	/** Dibuja la fila de un ajuste y devuelve la altura que ocupa. */
	public int render(Ui ui, Setting<?> setting, int x, int y, int w) {
		if (setting instanceof NumberSetting number) return renderNumber(ui, number, x, y, w);
		if (setting instanceof ColorSetting color) {
			return renderColor(ui, color, setting.getName(), setting.getDescription(), x, y, w,
					color::get, color::set, color.allowsAlpha());
		}

		row(ui, setting.getName(), setting.getDescription(), x, y, w, ROW_HEIGHT);
		if (setting instanceof BooleanSetting bool) renderBoolean(ui, bool, x, y, w);
		else if (setting instanceof ModeSetting mode) renderMode(ui, mode, x, y, w);
		else if (setting instanceof KeybindSetting keybind) renderKeybind(ui, keybind, x, y, w);
		else if (setting instanceof ActionSetting action) renderAction(ui, action, x, y, w);
		return ROW_HEIGHT;
	}

	/** Fondo de la fila y nombre a la izquierda (con la descripción como tooltip). */
	public void row(Ui ui, String name, String description, int x, int y, int w, int h) {
		boolean hovered = ui.hovered(x, y, w, h);
		Draw.panel(ui.g, x, y, w, h, hovered ? ThemeManager.cardHover() : ThemeManager.card(),
				ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.55F));
		ui.g.drawString(ui.font, name, x + 6, y + 5, ThemeManager.text(), false);
		if (hovered && description != null && !description.isEmpty() && ui.mouseX < x + 6 + ui.font.width(name) + 4) {
			ui.tooltip(description);
		}
	}

	private void renderBoolean(Ui ui, BooleanSetting setting, int x, int y, int w) {
		int toggleX = x + w - 26;
		boolean hovered = ui.hovered(x, y, w, ROW_HEIGHT);
		Draw.toggle(ui.g, toggleX, y + 4, ui.animate(setting, setting.get() ? 1.0F : 0.0F), hovered);
		ui.click(x, y, w, ROW_HEIGHT, (mx, my, button) -> {
			setting.toggle();
			ui.playClick();
			return true;
		});
	}

	private int renderNumber(Ui ui, NumberSetting setting, int x, int y, int w) {
		int height = ROW_HEIGHT + 8;
		row(ui, setting.getName(), setting.getDescription(), x, y, w, height);

		String value = setting.format();
		ui.g.drawString(ui.font, value, x + w - 6 - ui.font.width(value), y + 5, ThemeManager.accent(), false);

		int sliderX = x + 6;
		int sliderW = w - 12;
		int sliderY = y + 17;
		boolean hovered = ui.hovered(sliderX - 2, sliderY - 3, sliderW + 4, 12);
		Draw.slider(ui.g, sliderX, sliderY, sliderW, setting.getProgress(), hovered);
		ui.click(sliderX - 2, sliderY - 3, sliderW + 4, 12, (mx, my, button) -> {
			ui.startDrag((dx, dy) -> setting.setProgress((dx - sliderX) / (double) sliderW), mx, my);
			return true;
		});
		return height;
	}

	private void renderMode(Ui ui, ModeSetting setting, int x, int y, int w) {
		int boxW = Math.max(70, ui.font.width(setting.get()) + 26);
		int boxX = x + w - boxW - 4;
		int boxY = y + 3;
		boolean hovered = ui.hovered(boxX, boxY, boxW, 12);
		Draw.panel(ui.g, boxX, boxY, boxW, 12, ThemeManager.shade(), hovered ? ThemeManager.highlight() : ThemeManager.mix(ThemeManager.border(), 0xFF000000, 0.35F));
		ui.g.drawString(ui.font, "<", boxX + 3, boxY + 2, ThemeManager.accent(), false);
		ui.g.drawString(ui.font, ">", boxX + boxW - 8, boxY + 2, ThemeManager.accent(), false);
		ui.g.drawCenteredString(ui.font, setting.get(), boxX + boxW / 2, boxY + 2, ThemeManager.text());

		ui.click(boxX, boxY, boxW, 12, (mx, my, button) -> {
			boolean left = mx < boxX + boxW / 2.0 || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
			setting.cycle(left ? -1 : 1);
			ui.playClick();
			return true;
		});
	}

	private void renderKeybind(Ui ui, KeybindSetting setting, int x, int y, int w) {
		boolean listening = ui.isListening(setting);
		String label = listening ? "Press a key..." : setting.getKeyName();
		int boxW = Math.max(60, ui.font.width(label) + 12);
		int boxX = x + w - boxW - 4;
		int boxY = y + 3;
		boolean hovered = ui.hovered(boxX, boxY, boxW, 12);
		int border = listening ? ThemeManager.accent() : hovered ? ThemeManager.highlight() : ThemeManager.mix(ThemeManager.border(), 0xFF000000, 0.35F);
		Draw.panel(ui.g, boxX, boxY, boxW, 12, ThemeManager.shade(), border);
		ui.g.drawCenteredString(ui.font, label, boxX + boxW / 2, boxY + 2, listening ? ThemeManager.accent() : ThemeManager.text());
		if (hovered) ui.tooltip("Click, then press a key. Backspace or right click clears it.");

		ui.click(boxX, boxY, boxW, 12, (mx, my, button) -> {
			if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				setting.set(KeybindSetting.NONE);
			} else {
				ui.listenForKey(setting);
			}
			ui.playClick();
			return true;
		});
	}

	private void renderAction(Ui ui, ActionSetting setting, int x, int y, int w) {
		Long doneAt = actionFeedback.get(setting);
		boolean done = doneAt != null && System.currentTimeMillis() - doneAt < 1500;
		String label = done ? "Done!" : setting.getButtonLabel();
		int boxW = Math.max(50, ui.font.width(label) + 14);
		int boxX = x + w - boxW - 4;
		int boxY = y + 3;
		boolean hovered = ui.hovered(boxX, boxY, boxW, 12);
		int fill = done ? ThemeManager.accent() : hovered ? ThemeManager.highlight() : ThemeManager.mix(ThemeManager.accent(), ThemeManager.card(), 0.35F);
		Draw.panel(ui.g, boxX, boxY, boxW, 12, fill, ThemeManager.mix(fill, 0xFF000000, 0.4F));
		ui.g.drawCenteredString(ui.font, label, boxX + boxW / 2, boxY + 2, ThemeManager.shade());

		ui.click(boxX, boxY, boxW, 12, (mx, my, button) -> {
			setting.run();
			actionFeedback.put(setting, System.currentTimeMillis());
			ui.playClick();
			return true;
		});
	}

	/** Fila de color con muestra; al hacer clic se despliega el selector debajo. */
	public int renderColor(Ui ui, Object key, String name, String description, int x, int y, int w,
			java.util.function.IntSupplier getter, java.util.function.IntConsumer setter, boolean allowAlpha) {
		boolean expanded = expandedColor == key;
		int height = expanded ? ROW_HEIGHT + ColorPicker.HEIGHT + 4 : ROW_HEIGHT;
		row(ui, name, description, x, y, w, height);

		int swatchX = x + w - 30;
		Draw.panel(ui.g, swatchX, y + 4, 24, 10, getter.getAsInt() | 0xFF000000, expanded ? ThemeManager.accent() : ThemeManager.border());
		ui.click(x, y, w, ROW_HEIGHT, (mx, my, button) -> {
			expandedColor = expanded ? null : key;
			ui.playClick();
			return true;
		});

		if (expanded) {
			ColorPicker picker = pickers.computeIfAbsent(key, k -> new ColorPicker(getter, setter, allowAlpha));
			picker.render(ui, x + 8, y + ROW_HEIGHT);
		}
		return height;
	}
}
