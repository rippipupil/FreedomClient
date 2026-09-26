package com.freedomclient.ui.menu;

import com.freedomclient.FreedomClient;
import com.freedomclient.cosmetic.CosmeticModule;
import com.freedomclient.cosmetic.CosmeticSlot;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.performance.BundledModModule;
import com.freedomclient.setting.Setting;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.ScrollArea;
import com.freedomclient.ui.TextField;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.theme.ThemeManager;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Cuadrícula de tarjetas de mods (como el boceto: icono, nombre e interruptor), con filtros y búsqueda. */
public class ModGridPage implements MenuPage {
	private static final int CARD_MIN_WIDTH = 104;
	private static final int CARD_HEIGHT = 34;
	private static final int GAP = 4;
	private static final int BAR_HEIGHT = 12;
	private static final int SECTION_HEADER = 14;

	private final FreedomMenuScreen screen;
	private final Category fixedCategory;
	private final ScrollArea scroll = new ScrollArea();
	private final TextField search = new TextField(32);
	private Category filter;
	/** En la pestaña Cosmetics: sección elegida en los filtros ({@code null} = todas). */
	private CosmeticSlot slotFilter;

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
		} else if (isCosmetics()) {
			renderSlotBar(ui, x, y, w);
			gridY += BAR_HEIGHT + 6;
		}
		if (isCosmetics()) {
			renderSections(ui, x, gridY, w, y + h - gridY);
		} else {
			renderGrid(ui, x, gridY, w, y + h - gridY);
		}
	}

	private boolean isCosmetics() {
		return fixedCategory == Category.COSMETICS;
	}

	/** Filtros de la pestaña Cosmetics: todas las secciones o solo sombreros, capas, alas, mascotas o efectos. */
	private void renderSlotBar(Ui ui, int x, int y, int w) {
		int searchWidth = Math.min(90, w / 4);
		int chipX = renderSlotChip(ui, "All", null, x, y);
		for (CosmeticSlot slot : CosmeticSlot.values()) {
			chipX = renderSlotChip(ui, slot.getDisplayName(), slot, chipX, y);
		}
		search.render(ui, x + w - searchWidth, y, searchWidth, BAR_HEIGHT, "Search...");
	}

	private int renderSlotChip(Ui ui, String label, CosmeticSlot slot, int x, int y) {
		int width = ui.font.width(label) + 10;
		boolean selected = slotFilter == slot;
		boolean hovered = ui.hovered(x, y, width, BAR_HEIGHT);
		int fill = selected ? ThemeManager.accent() : hovered ? ThemeManager.cardHover() : ThemeManager.card();
		Draw.panel(ui.g, x, y, width, BAR_HEIGHT, fill, selected ? ThemeManager.accent() : ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.4F));
		ui.g.drawString(ui.font, label, x + 5, y + 2, selected ? ThemeManager.shade() : ThemeManager.text(), false);
		ui.click(x, y, width, BAR_HEIGHT, (mx, my, button) -> {
			slotFilter = slot;
			scroll.reset();
			ui.playClick();
			return true;
		});
		return x + width + 3;
	}

	private static CosmeticSlot slotOf(Module module) {
		return module instanceof CosmeticModule cosmetic ? cosmetic.getSlot() : CosmeticSlot.EFFECT;
	}

	/** Cosméticos agrupados por sección, cada una con su título y su cuadrícula. */
	private void renderSections(Ui ui, int x, int y, int w, int h) {
		List<Module> modules = visibleModules();
		int innerW = w - 6;
		int columns = Math.max(1, (innerW + GAP) / (CARD_MIN_WIDTH + GAP));
		int cardWidth = (innerW - GAP * (columns - 1)) / columns;

		int offset = scroll.begin(ui, x, y, innerW, h);
		int cursor = 0;
		for (CosmeticSlot slot : CosmeticSlot.values()) {
			List<Module> section = modules.stream().filter(module -> slotOf(module) == slot).toList();
			if (section.isEmpty()) continue;
			int headerY = y + cursor - offset;
			if (headerY + SECTION_HEADER > y && headerY < y + h) {
				String title = slot.getDisplayName();
				ui.g.drawString(ui.font, title, x + 1, headerY + 2, ThemeManager.accent(), false);
				int lineX = x + ui.font.width(title) + 6;
				ui.g.fill(lineX, headerY + 6, x + innerW, headerY + 7, ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.5F));
			}
			cursor += SECTION_HEADER;
			for (int i = 0; i < section.size(); i++) {
				int cardX = x + (i % columns) * (cardWidth + GAP);
				int cardY = y + cursor + (i / columns) * (CARD_HEIGHT + GAP) - offset;
				if (cardY + CARD_HEIGHT < y || cardY > y + h) continue;
				renderCard(ui, section.get(i), cardX, cardY, cardWidth);
			}
			int rows = (section.size() + columns - 1) / columns;
			cursor += rows * (CARD_HEIGHT + GAP) + GAP;
		}
		if (modules.isEmpty()) {
			ui.g.drawCenteredString(ui.font, "No cosmetics found", x + innerW / 2, y + 20, ThemeManager.textMuted());
		}
		scroll.end(ui, x, y, innerW, h, Math.max(0, cursor - GAP * 2));
	}

	private void renderFilterBar(Ui ui, int x, int y, int w) {
		int searchWidth = Math.min(90, w / 4);
		int chipX = x;
		chipX = renderChip(ui, "All", null, chipX, y);
		for (Category category : Category.values()) {
			if (category == Category.HUD || category == Category.COSMETICS) continue;
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

	private String query() {
		return search.getText().toLowerCase(Locale.ROOT).trim();
	}

	/** Mods visibles: los favoritos primero. La búsqueda mira el nombre, la descripción y todas las opciones del mod. */
	private List<Module> visibleModules() {
		String query = query();
		return FreedomClient.getModuleManager().getModules().stream()
				.filter(module -> fixedCategory != null ? module.getCategory() == fixedCategory
						: filter == null ? module.getCategory() != Category.HUD && module.getCategory() != Category.COSMETICS : module.getCategory() == filter)
				.filter(module -> !isCosmetics() || slotFilter == null || slotOf(module) == slotFilter)
				.filter(module -> query.isEmpty() || nameMatches(module, query) || matchingSetting(module, query) != null)
				.sorted(Comparator.comparing((Module module) -> !module.isFavorite()))
				.toList();
	}

	private static boolean nameMatches(Module module, String query) {
		return module.getName().toLowerCase(Locale.ROOT).contains(query) || module.getDescription().toLowerCase(Locale.ROOT).contains(query);
	}

	/** Primera opción del mod cuyo nombre o descripción contiene el texto buscado. */
	private static Setting<?> matchingSetting(Module module, String query) {
		for (Setting<?> setting : module.getSettings()) {
			if (settingMatches(setting, query)) return setting;
		}
		return null;
	}

	public static boolean settingMatches(Setting<?> setting, String query) {
		if (query == null || query.isEmpty()) return false;
		String lower = query.toLowerCase(Locale.ROOT);
		return setting.getName().toLowerCase(Locale.ROOT).contains(lower) || setting.getDescription().toLowerCase(Locale.ROOT).contains(lower);
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

	/** Estrella pixel de 8x8. */
	private static void star(Ui ui, int x, int y, int color) {
		String[] rows = {"...##...", "...##...", "########", ".######.", "..####..", ".##..##.", ".#....#.", "........"};
		for (int row = 0; row < rows.length; row++) {
			for (int col = 0; col < rows[row].length(); col++) {
				if (rows[row].charAt(col) == '#') ui.g.fill(x + col, y + row, x + col + 1, y + row + 1, color);
			}
		}
	}

	private void renderCard(Ui ui, Module module, int x, int y, int w) {
		boolean hovered = ui.hovered(x, y, w, CARD_HEIGHT);
		float hover = ui.animate("hover:" + module.getId(), hovered ? 1.0F : 0.0F);
		int fill = ThemeManager.mix(ThemeManager.card(), ThemeManager.cardHover(), hover);
		int border = ThemeManager.mix(ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.45F), ThemeManager.highlight(), hover);
		Draw.bevelPanel(ui.g, x, y, w, CARD_HEIGHT, fill, border);

		Draw.iconBox(ui.g, module.getIcon(), x + 5, y + 5, 24);

		int textX = x + 34;
		// Se deja sitio a la derecha para la estrella de favorito.
		int textWidth = w - 46;
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

		// Si el mod sale por una opción (no por su nombre), se muestra cuál.
		String query = query();
		Setting<?> matched = query.isEmpty() || nameMatches(module, query) ? null : matchingSetting(module, query);

		int toggleX = x + w - 25;
		int toggleY = y + CARD_HEIGHT - 15;
		if (matched != null) {
			String label = ui.font.plainSubstrByWidth("> " + matched.getName(), toggleX - textX - 3);
			ui.g.drawString(ui.font, label, textX, y + 20, ThemeManager.highlight(), false);
		}
		if (module.canToggle()) {
			boolean toggleHovered = ui.hovered(toggleX - 2, toggleY - 2, 24, 14);
			float progress = ui.animate("toggle:" + module.getId(), module.isEnabled() ? 1.0F : 0.0F);
			Draw.toggle(ui.g, toggleX, toggleY, progress, toggleHovered);
			if (!twoLines && matched == null) {
				ui.g.drawString(ui.font, module.isEnabled() ? "ON" : "OFF", textX, y + 20,
						module.isEnabled() ? ThemeManager.accent() : ThemeManager.textMuted(), false);
			}
		} else if (!twoLines && matched == null) {
			String label = module instanceof BundledModModule ? "Always on" : "Open >";
			ui.g.drawString(ui.font, label, textX, y + 20, ThemeManager.accent(), false);
		}

		// Estrella de favorito arriba a la derecha: siempre visible si es favorito, y al pasar el ratón si no.
		int starX = x + w - 11;
		int starY = y + 3;
		boolean starHovered = ui.hovered(starX - 1, starY - 1, 10, 10);
		if (module.isFavorite() || hovered) {
			star(ui, starX, starY, module.isFavorite() ? ThemeManager.accent() : starHovered ? ThemeManager.highlight() : ThemeManager.textMuted());
		}

		// El interruptor y la estrella se registran después de la tarjeta para tener prioridad al hacer clic.
		ui.click(x, y, w, CARD_HEIGHT, (mx, my, button) -> {
			if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && module.canToggle()) {
				module.toggle();
			} else {
				screen.openModule(module, query);
			}
			ui.playClick();
			return true;
		});
		ui.click(starX - 1, starY - 1, 10, 10, (mx, my, button) -> {
			module.setFavorite(!module.isFavorite());
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
