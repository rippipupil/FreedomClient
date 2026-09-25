package com.freedomclient.ui.menu;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.performance.BundledModModule;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.ScrollArea;
import com.freedomclient.ui.TextField;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.theme.ThemeManager;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/** Cuadrícula de tarjetas de mods (como el boceto: icono, nombre e interruptor), con filtros y búsqueda. */
public class ModGridPage implements MenuPage {
	private static final int CARD_MIN_WIDTH = 104;
	private static final int CARD_HEIGHT = 34;
	private static final int GAP = 4;
	private static final int BAR_HEIGHT = 12;

	private final FreedomMenuScreen screen;
	private final Category fixedCategory;
	private final ScrollArea scroll = new ScrollArea();
	private final TextField search = new TextField(32);
	private Category filter;

	/** @param fixedCategory categoría fija (pestaña HUD) o {@code null} para mostrar filtros de todas. */
	public ModGridPage(FreedomMenuScreen screen, Category fixedCategory) {
		this.screen = screen;
		this.fixedCategory = fixedCategory;
	}

	@Override
	public void render(Ui ui, int x, int y, int w, int h) {
		int gridY = y;
		if (fixedCategory == null) {
			renderFilterBar(ui, x, y, w);
			gridY += BAR_HEIGHT + 6;
		}
		renderGrid(ui, x, gridY, w, y + h - gridY);
	}

	private void renderFilterBar(Ui ui, int x, int y, int w) {
		int searchWidth = Math.min(90, w / 4);
		int chipX = x;
		chipX = renderChip(ui, "All", null, chipX, y);
		for (Category category : Category.values()) {
			if (category == Category.HUD) continue;
			chipX = renderChip(ui, category.getDisplayName(), category, chipX, y);
		}
		search.render(ui, x + w - searchWidth, y, searchWidth, BAR_HEIGHT, "Search...");
	}

	private int renderChip(Ui ui, String label, Category category, int x, int y) {
		int width = ui.font.width(label) + 10;
		boolean selected = filter == category;
		boolean hovered = ui.hovered(x, y, width, BAR_HEIGHT);
		int fill = selected ? ThemeManager.accent() : hovered ? ThemeManager.cardHover() : ThemeManager.card();
		int textColor = selected ? ThemeManager.shade() : ThemeManager.text();
		Draw.panel(ui.g, x, y, width, BAR_HEIGHT, fill, selected ? ThemeManager.accent() : ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.4F));
		ui.g.drawString(ui.font, label, x + 5, y + 2, textColor, false);

		ui.click(x, y, width, BAR_HEIGHT, (mx, my, button) -> {
			filter = category;
			scroll.reset();
			ui.playClick();
			return true;
		});
		return x + width + 3;
	}

	private List<Module> visibleModules() {
		String query = search.getText().toLowerCase(Locale.ROOT).trim();
		return FreedomClient.getModuleManager().getModules().stream()
				.filter(module -> fixedCategory != null ? module.getCategory() == fixedCategory
						: filter == null ? module.getCategory() != Category.HUD : module.getCategory() == filter)
				.filter(module -> query.isEmpty() || module.getName().toLowerCase(Locale.ROOT).contains(query))
				.toList();
	}

	private void renderGrid(Ui ui, int x, int y, int w, int h) {
		List<Module> modules = visibleModules();
		int innerW = w - 6;
		int columns = Math.max(1, (innerW + GAP) / (CARD_MIN_WIDTH + GAP));
		int cardWidth = (innerW - GAP * (columns - 1)) / columns;
		int rows = (modules.size() + columns - 1) / columns;
		int contentHeight = rows * (CARD_HEIGHT + GAP) - GAP;

		int offset = scroll.begin(ui, x, y, innerW, h);
		for (int i = 0; i < modules.size(); i++) {
			int cardX = x + (i % columns) * (cardWidth + GAP);
			int cardY = y + (i / columns) * (CARD_HEIGHT + GAP) - offset;
			if (cardY + CARD_HEIGHT < y || cardY > y + h) continue;
			renderCard(ui, modules.get(i), cardX, cardY, cardWidth);
		}
		if (modules.isEmpty()) {
			ui.g.drawCenteredString(ui.font, "No mods found", x + innerW / 2, y + 20, ThemeManager.textMuted());
		}
		scroll.end(ui, x, y, innerW, h, contentHeight);
	}

	private void renderCard(Ui ui, Module module, int x, int y, int w) {
		boolean hovered = ui.hovered(x, y, w, CARD_HEIGHT);
		float hover = ui.animate("hover:" + module.getId(), hovered ? 1.0F : 0.0F);
		int fill = ThemeManager.mix(ThemeManager.card(), ThemeManager.cardHover(), hover);
		int border = ThemeManager.mix(ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.45F), ThemeManager.highlight(), hover);
		Draw.bevelPanel(ui.g, x, y, w, CARD_HEIGHT, fill, border);

		Draw.iconBox(ui.g, module.getIcon(), x + 5, y + 5, 24);

		int textX = x + 34;
		int textWidth = w - 38;
		// Los nombres largos pasan a dos líneas (y se omite el ON/OFF, que ya indica el interruptor).
		boolean twoLines = ui.font.width(module.getName()) > textWidth;
		if (twoLines) {
			List<net.minecraft.util.FormattedCharSequence> lines = ui.font.split(net.minecraft.network.chat.Component.literal(module.getName()), textWidth);
			for (int i = 0; i < Math.min(2, lines.size()); i++) {
				ui.g.drawString(ui.font, lines.get(i), textX, y + 5 + i * 10, ThemeManager.text(), false);
			}
		} else {
			ui.g.drawString(ui.font, module.getName(), textX, y + 6, ThemeManager.text(), false);
		}

		int toggleX = x + w - 25;
		int toggleY = y + CARD_HEIGHT - 15;
		if (module.canToggle()) {
			boolean toggleHovered = ui.hovered(toggleX - 2, toggleY - 2, 24, 14);
			float progress = ui.animate("toggle:" + module.getId(), module.isEnabled() ? 1.0F : 0.0F);
			Draw.toggle(ui.g, toggleX, toggleY, progress, toggleHovered);
			if (!twoLines) {
				ui.g.drawString(ui.font, module.isEnabled() ? "ON" : "OFF", textX, y + 20,
						module.isEnabled() ? ThemeManager.accent() : ThemeManager.textMuted(), false);
			}
		} else if (!twoLines) {
			String label = module instanceof BundledModModule ? "Always on" : "Open >";
			ui.g.drawString(ui.font, label, textX, y + 20, ThemeManager.accent(), false);
		}

		// El interruptor se registra después de la tarjeta para tener prioridad al hacer clic.
		ui.click(x, y, w, CARD_HEIGHT, (mx, my, button) -> {
			if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && module.canToggle()) {
				module.toggle();
			} else {
				screen.openModule(module);
			}
			ui.playClick();
			return true;
		});
		if (module.canToggle()) {
			ui.click(toggleX - 2, toggleY - 2, 24, 14, (mx, my, button) -> {
				module.toggle();
				ui.playClick();
				return true;
			});
		}
	}
}
