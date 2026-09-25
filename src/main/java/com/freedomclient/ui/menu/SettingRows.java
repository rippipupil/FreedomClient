package com.freedomclient.ui.menu;

import com.freedomclient.setting.ActionSetting;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.setting.KeybindSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.setting.PixelGridSetting;
import com.freedomclient.setting.Setting;
import com.freedomclient.setting.StringSetting;
import com.freedomclient.ui.ColorPicker;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.TextField;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.theme.ThemeManager;
import com.freedomclient.waypoint.Waypoint;
import com.freedomclient.waypoint.WaypointListSetting;
import com.freedomclient.waypoint.WaypointStore;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/** Dibuja las filas de ajustes (interruptor, deslizador, selector, color, tecla y botón). */
public class SettingRows {
	public static final int ROW_HEIGHT = 18;
	public static final int ROW_GAP = 3;

	private final Map<Object, ColorPicker> pickers = new HashMap<>();
	private final Map<ActionSetting, Long> actionFeedback = new HashMap<>();
	private final Map<StringSetting, TextField> textFields = new HashMap<>();
	private Object expandedColor;

	/** Dibuja la fila de un ajuste y devuelve la altura que ocupa. */
	public int render(Ui ui, Setting<?> setting, int x, int y, int w) {
		if (setting instanceof NumberSetting number) return renderNumber(ui, number, x, y, w);
		if (setting instanceof StringSetting text) return renderText(ui, text, x, y, w);
		if (setting instanceof PixelGridSetting grid) return renderGrid(ui, grid, x, y, w);
		if (setting instanceof WaypointListSetting) return renderWaypoints(ui, x, y, w);
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

	private int renderText(Ui ui, StringSetting setting, int x, int y, int w) {
		int height = ROW_HEIGHT + 16;
		row(ui, setting.getName(), setting.getDescription(), x, y, w, height);

		TextField field = textFields.computeIfAbsent(setting, s -> {
			TextField created = new TextField(s.getMaxLength());
			created.setText(s.get());
			return created;
		});
		field.render(ui, x + 6, y + 16, w - 12, 14, "Type here...");
		if (!field.getText().equals(setting.get())) setting.set(field.getText());
		return height;
	}

	/** Editor de la mira: haz clic o arrastra sobre la cuadrícula para pintar píxeles; clic derecho borra. */
	private int renderGrid(Ui ui, PixelGridSetting setting, int x, int y, int w) {
		int cell = 7;
		int gridSize = setting.getSize() * cell;
		int height = ROW_HEIGHT + gridSize + 8;
		row(ui, setting.getName(), setting.getDescription(), x, y, w, height);

		int gridX = x + 8;
		int gridY = y + ROW_HEIGHT;
		ui.g.fill(gridX - 1, gridY - 1, gridX + gridSize + 1, gridY + gridSize + 1, ThemeManager.border());
		for (int gy = 0; gy < setting.getSize(); gy++) {
			for (int gx = 0; gx < setting.getSize(); gx++) {
				boolean center = gx == setting.getSize() / 2 && gy == setting.getSize() / 2;
				int empty = center ? ThemeManager.mix(ThemeManager.shade(), ThemeManager.highlight(), 0.3F) : ((gx + gy) % 2 == 0 ? ThemeManager.shade() : ThemeManager.mix(ThemeManager.shade(), 0xFF000000, 0.3F));
				int color = setting.isSet(gx, gy) ? ThemeManager.accent() : empty;
				ui.g.fill(gridX + gx * cell, gridY + gy * cell, gridX + gx * cell + cell, gridY + gy * cell + cell, color);
			}
		}

		ui.click(gridX, gridY, gridSize, gridSize, (mx, my, button) -> {
			boolean paint = button != GLFW.GLFW_MOUSE_BUTTON_RIGHT;
			ui.startDrag((dx, dy) -> {
				int gx = (int) ((dx - gridX) / cell);
				int gy = (int) ((dy - gridY) / cell);
				if (gx >= 0 && gy >= 0 && gx < setting.getSize() && gy < setting.getSize()) setting.setPixel(gx, gy, paint);
			}, mx, my);
			return true;
		});

		// Botones para limpiar y volver al diseño por defecto.
		int buttonX = gridX + gridSize + 10;
		smallButton(ui, "Clear", buttonX, gridY, setting::clear);
		smallButton(ui, "Reset", buttonX, gridY + 16, setting::reset);
		ui.g.drawString(ui.font, "Left: draw", buttonX, gridY + 38, ThemeManager.textMuted(), false);
		ui.g.drawString(ui.font, "Right: erase", buttonX, gridY + 48, ThemeManager.textMuted(), false);
		return height;
	}

	/** Lista de waypoints del mundo actual: color, nombre, coordenadas, visible y borrar. */
	private int renderWaypoints(Ui ui, int x, int y, int w) {
		java.util.List<Waypoint> waypoints = ui.minecraft.level != null ? WaypointStore.current() : java.util.List.of();
		int height = ROW_HEIGHT + Math.max(1, waypoints.size()) * 16 + 4;
		row(ui, "Waypoints in this world", "Toggle or delete your waypoints.", x, y, w, height);

		int rowY = y + ROW_HEIGHT;
		if (waypoints.isEmpty()) {
			String empty = ui.minecraft.level != null ? "No waypoints yet. Press the add key in game." : "Join a world to see its waypoints.";
			ui.g.drawString(ui.font, empty, x + 8, rowY + 3, ThemeManager.textMuted(), false);
			return height;
		}

		for (Waypoint waypoint : java.util.List.copyOf(waypoints)) {
			ui.g.fill(x + 8, rowY + 2, x + 16, rowY + 10, waypoint.color);
			String text = waypoint.name + "  " + waypoint.x + ", " + waypoint.y + ", " + waypoint.z;
			ui.g.drawString(ui.font, ui.font.plainSubstrByWidth(text, w - 90), x + 20, rowY + 2, waypoint.visible ? ThemeManager.text() : ThemeManager.textMuted(), false);

			int toggleX = x + w - 52;
			Draw.toggle(ui.g, toggleX, rowY + 1, waypoint.visible ? 1.0F : 0.0F, ui.hovered(toggleX, rowY, 20, 12));
			ui.click(toggleX, rowY, 20, 12, (mx, my, button) -> {
				waypoint.visible = !waypoint.visible;
				WaypointStore.save();
				ui.playClick();
				return true;
			});

			int deleteX = x + w - 24;
			boolean hovered = ui.hovered(deleteX, rowY, 14, 12);
			Draw.panel(ui.g, deleteX, rowY, 14, 12, hovered ? 0xFFD7263D : ThemeManager.shade(), ThemeManager.border());
			ui.g.drawString(ui.font, "x", deleteX + 4, rowY + 1, ThemeManager.text(), false);
			ui.click(deleteX, rowY, 14, 12, (mx, my, button) -> {
				WaypointStore.remove(waypoint);
				ui.playClick();
				return true;
			});
			rowY += 16;
		}
		return height;
	}

	private void smallButton(Ui ui, String label, int x, int y, Runnable action) {
		int w = ui.font.width(label) + 12;
		boolean hovered = ui.hovered(x, y, w, 12);
		Draw.panel(ui.g, x, y, w, 12, hovered ? ThemeManager.cardHover() : ThemeManager.shade(), hovered ? ThemeManager.highlight() : ThemeManager.border());
		ui.g.drawString(ui.font, label, x + 6, y + 2, ThemeManager.text(), false);
		ui.click(x, y, w, 12, (mx, my, button) -> {
			action.run();
			ui.playClick();
			return true;
		});
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
