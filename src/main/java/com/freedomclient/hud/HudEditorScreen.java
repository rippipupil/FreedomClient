package com.freedomclient.hud;

import com.freedomclient.FreedomClient;
import com.freedomclient.config.Config;
import com.freedomclient.module.Module;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.theme.ThemeManager;
import com.freedomclient.ui.menu.FreedomMenuScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor del HUD: arrastra los elementos para moverlos, tira de la esquina inferior derecha para escalarlos
 * (o usa la rueda encima), y haz clic derecho para abrir sus opciones. Se alinean con guías magnéticas.
 */
public class HudEditorScreen extends Screen {
	private static final int SNAP_DISTANCE = 4;
	private static final int HANDLE_SIZE = 5;
	private static final int EDGE_MARGIN = 2;

	private final Screen parent;
	private final Ui ui = new Ui();
	private final List<Integer> guidesX = new ArrayList<>();
	private final List<Integer> guidesY = new ArrayList<>();

	private HudModule dragging;
	private boolean scaling;
	private double grabX;
	private double grabY;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("HUD Editor"));
		this.parent = parent;
	}

	private List<HudModule> elements() {
		List<HudModule> list = new ArrayList<>();
		for (Module module : FreedomClient.getModuleManager().getModules()) {
			if (module instanceof HudModule hud && hud.isEnabled()) list.add(hud);
		}
		return list;
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, 0x40000000);
		// Líneas del centro de la pantalla como referencia.
		int guide = ThemeManager.withAlpha(ThemeManager.border(), 0.25F);
		graphics.fill(width / 2, 0, width / 2 + 1, height, guide);
		graphics.fill(0, height / 2, width, height / 2 + 1, guide);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		ui.begin(graphics, mouseX, mouseY);

		HudModule hovered = dragging != null ? dragging : elementAt(mouseX, mouseY);
		for (HudModule hud : elements()) {
			int[] b = HudRenderer.renderElement(graphics, minecraft, hud, true);
			boolean active = hud == hovered;
			int outline = active ? ThemeManager.accent() : ThemeManager.withAlpha(ThemeManager.text(), 0.45F);
			graphics.renderOutline(b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2, outline);

			if (active) {
				graphics.fill(b[0] + b[2] - HANDLE_SIZE + 1, b[1] + b[3] - HANDLE_SIZE + 1, b[0] + b[2] + 1, b[1] + b[3] + 1, ThemeManager.accent());
				String label = hud.getName() + "  " + Math.round(hud.getPosition().getScale() * 100) + "%";
				int labelY = b[1] > 12 ? b[1] - 11 : b[1] + b[3] + 3;
				int labelW = font.width(label) + 6;
				Draw.panel(graphics, b[0], labelY, labelW, 10, ThemeManager.background() | 0xFF000000, ThemeManager.accent());
				graphics.drawString(font, label, b[0] + 3, labelY + 1, ThemeManager.text(), false);
			}
		}

		for (int x : guidesX) graphics.fill(x, 0, x + 1, height, ThemeManager.highlight());
		for (int y : guidesY) graphics.fill(0, y, width, y + 1, ThemeManager.highlight());

		renderToolbar();
		ui.end();
	}

	private void renderToolbar() {
		String hint = "Drag to move  |  Corner or scroll to resize  |  Right click for settings";
		int barW = font.width(hint) + 16;
		int barX = (width - barW) / 2;
		int barY = height - 42;
		Draw.panel(ui.g, barX, barY, barW, 14, ThemeManager.background() | 0xFF000000, ThemeManager.border());
		ui.g.drawString(font, hint, barX + 8, barY + 3, ThemeManager.textMuted(), false);

		button("Reset all", width / 2 - 84, barY + 18, 80, () -> {
			for (HudModule hud : elements()) hud.getPosition().reset();
		});
		button("Done", width / 2 + 4, barY + 18, 80, this::onClose);
	}

	private void button(String label, int x, int y, int w, Runnable action) {
		boolean hovered = ui.hovered(x, y, w, 16);
		Draw.bevelPanel(ui.g, x, y, w, 16, hovered ? ThemeManager.cardHover() : ThemeManager.card(), hovered ? ThemeManager.highlight() : ThemeManager.border());
		ui.g.drawCenteredString(font, label, x + w / 2, y + 4, ThemeManager.text());
		ui.click(x, y, w, 16, (mx, my, button) -> {
			action.run();
			ui.playClick();
			return true;
		});
	}

	private HudModule elementAt(double mouseX, double mouseY) {
		List<HudModule> list = elements();
		for (int i = list.size() - 1; i >= 0; i--) {
			int[] b = HudRenderer.bounds(width, height, minecraft, list.get(i), true);
			if (mouseX >= b[0] - 1 && mouseX <= b[0] + b[2] + 1 && mouseY >= b[1] - 1 && mouseY <= b[1] + b[3] + 1) {
				return list.get(i);
			}
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (ui.mouseClicked(event.x(), event.y(), event.button())) return true;

		HudModule hud = elementAt(event.x(), event.y());
		if (hud == null) return super.mouseClicked(event, doubleClick);

		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			ui.playClick();
			minecraft.setScreen(FreedomMenuScreen.forModule(hud, this));
			return true;
		}

		int[] b = HudRenderer.bounds(width, height, minecraft, hud, true);
		dragging = hud;
		scaling = event.x() >= b[0] + b[2] - HANDLE_SIZE - 1 && event.y() >= b[1] + b[3] - HANDLE_SIZE - 1;
		grabX = event.x() - b[0];
		grabY = event.y() - b[1];
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging == null) return super.mouseDragged(event, dragX, dragY);

		int[] b = HudRenderer.bounds(width, height, minecraft, dragging, true);
		if (scaling) {
			int baseWidth = Math.max(1, dragging.getWidth(minecraft, true));
			dragging.getPosition().setScale((float) (event.x() - b[0]) / baseWidth);
			int[] scaled = HudRenderer.bounds(width, height, minecraft, dragging, true);
			dragging.getPosition().setAbsolute(b[0], b[1], scaled[2], scaled[3], width, height);
			return true;
		}

		int x = (int) Math.round(event.x() - grabX);
		int y = (int) Math.round(event.y() - grabY);
		guidesX.clear();
		guidesY.clear();
		x = snap(x, b[2], snapLinesX(dragging), guidesX);
		y = snap(y, b[3], snapLinesY(dragging), guidesY);
		dragging.getPosition().setAbsolute(x, y, b[2], b[3], width, height);
		return true;
	}

	/** Pega el borde izquierdo, el centro o el borde derecho del elemento a la línea más cercana. */
	private static int snap(int position, int size, List<Integer> lines, List<Integer> activeGuides) {
		int best = position;
		int bestDistance = SNAP_DISTANCE + 1;
		int[] edges = {0, size / 2, size};
		for (int line : lines) {
			for (int edge : edges) {
				int distance = Math.abs(position + edge - line);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = line - edge;
				}
			}
		}
		if (bestDistance <= SNAP_DISTANCE) {
			for (int edge : edges) {
				if (lines.contains(best + edge)) activeGuides.add(best + edge);
			}
			return best;
		}
		return position;
	}

	private List<Integer> snapLinesX(HudModule ignore) {
		List<Integer> lines = new ArrayList<>(List.of(EDGE_MARGIN, width / 2, width - EDGE_MARGIN));
		for (HudModule hud : elements()) {
			if (hud == ignore) continue;
			int[] b = HudRenderer.bounds(width, height, minecraft, hud, true);
			lines.add(b[0]);
			lines.add(b[0] + b[2] / 2);
			lines.add(b[0] + b[2]);
		}
		return lines;
	}

	private List<Integer> snapLinesY(HudModule ignore) {
		List<Integer> lines = new ArrayList<>(List.of(EDGE_MARGIN, height / 2, height - EDGE_MARGIN));
		for (HudModule hud : elements()) {
			if (hud == ignore) continue;
			int[] b = HudRenderer.bounds(width, height, minecraft, hud, true);
			lines.add(b[1]);
			lines.add(b[1] + b[3] / 2);
			lines.add(b[1] + b[3]);
		}
		return lines;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		ui.mouseReleased();
		dragging = null;
		scaling = false;
		guidesX.clear();
		guidesY.clear();
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		HudModule hud = elementAt(mouseX, mouseY);
		if (hud == null) return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);

		int[] b = HudRenderer.bounds(width, height, minecraft, hud, true);
		hud.getPosition().setScale(hud.getPosition().getScale() + (float) vertical * 0.1F);
		int[] scaled = HudRenderer.bounds(width, height, minecraft, hud, true);
		hud.getPosition().setAbsolute(b[0], b[1], scaled[2], scaled[3], width, height);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	public void removed() {
		Config.save();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
