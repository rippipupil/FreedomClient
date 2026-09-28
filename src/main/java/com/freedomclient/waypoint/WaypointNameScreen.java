package com.freedomclient.waypoint;

import com.freedomclient.ui.Draw;
import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Ventanita para poner nombre a un waypoint recién creado (Enter guarda, Esc deja el nombre que tenía). */
public class WaypointNameScreen extends Screen {
	private static final int PANEL_WIDTH = 200;
	private static final int PANEL_HEIGHT = 78;

	private final Waypoint waypoint;
	private EditBox name;

	public WaypointNameScreen(Waypoint waypoint) {
		super(Component.literal("Name waypoint"));
		this.waypoint = waypoint;
	}

	@Override
	protected void init() {
		int x = (width - PANEL_WIDTH) / 2;
		int y = (height - PANEL_HEIGHT) / 2;
		name = new EditBox(font, x + 10, y + 24, PANEL_WIDTH - 20, 18, Component.literal("Name"));
		name.setMaxLength(32);
		name.setValue(waypoint.name);
		addRenderableWidget(name);
		addRenderableWidget(Button.builder(Component.literal("Save"), button -> save()).bounds(x + 10, y + 50, 88, 18).build());
		addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose()).bounds(x + PANEL_WIDTH - 98, y + 50, 88, 18).build());
		setInitialFocus(name);
	}

	private void save() {
		String value = name.getValue().trim();
		if (!value.isEmpty()) {
			waypoint.name = value;
			WaypointStore.save();
		}
		onClose();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
			save();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		int x = (width - PANEL_WIDTH) / 2;
		int y = (height - PANEL_HEIGHT) / 2;
		Draw.bevelPanel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, ThemeManager.withAlpha(ThemeManager.background(), 0.94F), ThemeManager.border());
		graphics.fill(x + 10, y + 8, x + 16, y + 14, waypoint.color);
		graphics.drawString(font, "Name this waypoint", x + 21, y + 8, ThemeManager.accent(), true);
		super.render(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
