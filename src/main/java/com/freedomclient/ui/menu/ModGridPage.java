package com.freedomclient.ui.menu;

import com.freedomclient.FreedomClient;
import com.freedomclient.cosmetic.CosmeticModule;
import com.freedomclient.cosmetic.CosmeticPreview;
import com.freedomclient.cosmetic.CosmeticSlot;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.performance.BundledModModule;
import com.freedomclient.setting.Setting;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.NeonStyle;
import com.freedomclient.ui.ScrollArea;
import com.freedomclient.ui.TextField;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.theme.ThemeManager;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Cuadrícula de tarjetas de mods (como el boceto: icono o vista previa a la izquierda, nombre y la rueda de ajustes),
 * con filtros y búsqueda. Las tarjetas se seleccionan con un clic para activar o apagar el mod.
 */
public class ModGridPage implements MenuPage {
	private static final int CARD_MIN_WIDTH = 104;
	private static final int CARD_HEIGHT = 34;
	private static final int COSMETIC_CARD_HEIGHT = 58;
	/** Separación entre tarjetas: deja sitio a los bordes (los de brillo se salen un poco de la tarjeta). */
	private static final int GAP = 6;
	private static final int BAR_HEIGHT = 12;
	private static final int SECTION_HEADER = 14;

	private final FreedomMenuScreen screen;
	private final Category fixedCategory;
	private final ScrollArea scroll = new ScrollArea();
	private final TextField search = new TextField(32);
	private Category filter;
	/** En la pestaña Cosmetics: sección elegida en los filtros ({@code null} = todas). */
	private CosmeticSlot slotFilter;
	/** Carpeta elegida en la pestaña Mods ({@code null} = sin filtrar por carpeta). */
	private String folderFilter;
	private boolean creatingFolder;
	private final TextField folderName = new TextField(18);

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
			renderSortAndFolders(ui, x, y + BAR_HEIGHT + 5, w);
			gridY += (BAR_HEIGHT + 5) * 2 + 1;
		} else if (isCosmetics()) {
			gridY += renderSlotBar(ui, x, y, w) + 6;
		}
		if (isCosmetics()) {
			renderSections(ui, x, gridY, w, y + h - gridY);
		} else {
			renderGrid(ui, x, gridY, w, y + h - gridY);
		}
	}

	/** Chip de filtro: el elegido va relleno del color de acento. Clic derecho opcional (borrar una carpeta). */
	private int chip(Ui ui, String label, boolean selected, int x, int y, Runnable onClick, Runnable onRightClick) {
		int width = ui.font.width(label) + 10;
		boolean hovered = ui.hovered(x, y, width, BAR_HEIGHT);
		int fill = selected ? ThemeManager.accent() : hovered ? ThemeManager.cardHover() : ThemeManager.card();
		Draw.panel(ui.g, x, y, width, BAR_HEIGHT, fill, selected ? ThemeManager.accent() : ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.4F));
		if (NeonStyle.on() && !selected) {
			NeonStyle.frame(ui.g, x, y, width, BAR_HEIGHT, x / 500.0, 0.2, hovered ? 1.0F : 0.6F);
		}
		ui.g.drawString(ui.font, label, x + 5, y + 2, selected ? ThemeManager.shade() : ThemeManager.text(), false);
		ui.click(x, y, width, BAR_HEIGHT, (mx, my, button) -> {
			if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				if (onRightClick == null) return false;
				onRightClick.run();
			} else {
				onClick.run();
			}
			scroll.reset();
			ui.playClick();
			return true;
		});
		return x + width + 3;
	}

	/**
	 * Segunda fila de la pestaña Mods: el orden de la lista a la izquierda y las carpetas del jugador a la derecha
	 * (clic para ver solo esa carpeta, clic derecho para borrarla, "+" para crear una).
	 */
	private void renderSortAndFolders(Ui ui, int x, int y, int w) {
		int cx = x;
		ui.g.drawString(ui.font, "Sort", cx, y + 2, ThemeManager.textMuted(), false);
		cx += ui.font.width("Sort") + 5;
		for (String sort : ModFolders.SORTS) {
			cx = chip(ui, sort, ModFolders.sort().equals(sort), cx, y, () -> ModFolders.setSort(sort), null);
		}

		// Las carpetas van de derecha a izquierda para que queden alineadas con el buscador de arriba.
		int right = x + w;
		if (creatingFolder) {
			int fieldW = 80;
			right -= fieldW;
			folderName.render(ui, right, y, fieldW, BAR_HEIGHT, "Folder name");
			right -= 3;
		} else if (ModFolders.names().size() < ModFolders.MAX_FOLDERS) {
			int plusW = ui.font.width("+ Folder") + 10;
			right -= plusW;
			chip(ui, "+ Folder", false, right, y, () -> {
				creatingFolder = true;
				folderName.setText("");
				folderName.onSubmit(() -> {
					if (ModFolders.create(folderName.getText())) folderFilter = folderName.getText().trim();
					creatingFolder = false;
				});
				ui.focus(folderName);
			}, null);
			right -= 3;
		}
		List<String> folders = ModFolders.names();
		for (int i = folders.size() - 1; i >= 0; i--) {
			String folder = folders.get(i);
			int width = ui.font.width(folder) + 10;
			if (right - width < cx + 12) break;
			right -= width;
			chip(ui, folder, folder.equals(folderFilter), right, y,
					() -> folderFilter = folder.equals(folderFilter) ? null : folder,
					() -> {
						ModFolders.delete(folder);
						if (folder.equals(folderFilter)) folderFilter = null;
					});
			right -= 3;
		}
		if (creatingFolder && !ui.isFocused(folderName)) creatingFolder = false;
	}

	private boolean isCosmetics() {
		return fixedCategory == Category.COSMETICS;
	}

	/**
	 * Filtros de la pestaña Cosmetics: todas las secciones o solo sombreros, cuello, capas, alas, mascotas o efectos.
	 * Fichas compactas que nunca se meten debajo del buscador: si no caben, siguen en otra línea. Devuelve el alto.
	 */
	private int renderSlotBar(Ui ui, int x, int y, int w) {
		int searchWidth = Math.min(90, w / 4);
		search.render(ui, x + w - searchWidth, y, searchWidth, BAR_HEIGHT, "Search...");
		int right = x + w - searchWidth - 4;
		int chipX = x;
		int chipY = y;
		for (int i = -1; i < CosmeticSlot.values().length; i++) {
			CosmeticSlot slot = i < 0 ? null : CosmeticSlot.values()[i];
			String label = slot == null ? "All" : slot.getDisplayName();
			if (chipX > x && chipX + ui.font.width(label) + 8 > right) {
				chipX = x;
				chipY += BAR_HEIGHT + 3;
			}
			chipX = renderSlotChip(ui, label, slot, chipX, chipY);
		}
		return chipY - y + BAR_HEIGHT;
	}

	private int renderSlotChip(Ui ui, String label, CosmeticSlot slot, int x, int y) {
		int width = ui.font.width(label) + 8;
		boolean selected = slotFilter == slot;
		boolean hovered = ui.hovered(x, y, width, BAR_HEIGHT);
		int fill = selected ? ThemeManager.accent() : hovered ? ThemeManager.cardHover() : ThemeManager.card();
		Draw.panel(ui.g, x, y, width, BAR_HEIGHT, fill, selected ? ThemeManager.accent() : ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.4F));
		ui.g.drawString(ui.font, label, x + 4, y + 2, selected ? ThemeManager.shade() : ThemeManager.text(), false);
		ui.click(x, y, width, BAR_HEIGHT, (mx, my, button) -> {
			slotFilter = slot;
			scroll.reset();
			ui.playClick();
			return true;
		});
		return x + width + 2;
	}

	private static CosmeticSlot slotOf(Module module) {
		return module instanceof CosmeticModule cosmetic ? cosmetic.getSlot() : CosmeticSlot.EFFECT;
	}

	/** Cosméticos agrupados por sección, calculado solo cuando cambia la lista (no en cada fotograma). */
	private List<Module> groupedFrom;
	private java.util.Map<CosmeticSlot, List<Module>> grouped = java.util.Map.of();

	private java.util.Map<CosmeticSlot, List<Module>> sectionsBySlot(List<Module> modules) {
		if (modules != groupedFrom) {
			java.util.Map<CosmeticSlot, List<Module>> bySlot = new java.util.EnumMap<>(CosmeticSlot.class);
			for (Module module : modules) bySlot.computeIfAbsent(slotOf(module), key -> new java.util.ArrayList<>()).add(module);
			grouped = bySlot;
			groupedFrom = modules;
		}
		return grouped;
	}

	/** Cosméticos agrupados por sección, cada una con su título y su cuadrícula. */
	private void renderSections(Ui ui, int x, int y, int w, int h) {
		List<Module> modules = visibleModules();
		// Mismo margen a los dos lados: la barra de scroll va en el margen de la ventana.
		int innerW = w;
		int columns = Math.max(1, (innerW + GAP) / (cardMinWidth() + GAP));
		int cardWidth = (innerW - GAP * (columns - 1)) / columns;

		int offset = scroll.begin(ui, x, y, innerW, h);
		int cursor = 0;
		for (CosmeticSlot slot : CosmeticSlot.values()) {
			List<Module> section = sectionsBySlot(modules).getOrDefault(slot, List.of());
			if (section.isEmpty()) continue;
			cursor = sectionHeader(ui, slot.getDisplayName(), x, y, cursor - offset, innerW, h) + offset;
			for (int i = 0; i < section.size(); i++) {
				int cardX = x + (i % columns) * (cardWidth + GAP);
				int cardY = y + cursor + (i / columns) * (cardHeight() + GAP) - offset;
				if (cardY + cardHeight() < y || cardY > y + h) continue;
				renderCard(ui, section.get(i), cardX, cardY, cardWidth);
			}
			int rows = (section.size() + columns - 1) / columns;
			cursor += rows * (cardHeight() + GAP) + GAP;
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

	/** Lista de la última vez y lo que la definía: si nada cambia, se reutiliza en vez de filtrar y ordenar cada fotograma. */
	private List<Module> cachedModules;
	private String cachedKey;

	/** Mods visibles: los favoritos primero. La búsqueda mira el nombre, la descripción y todas las opciones del mod. */
	private List<Module> visibleModules() {
		String key = query() + '|' + filter + '|' + slotFilter + '|' + folderFilter + '|' + ModFolders.version() + '|' + Module.stateVersion();
		if (cachedModules == null || !key.equals(cachedKey)) {
			cachedModules = computeVisibleModules();
			cachedKey = key;
		}
		return cachedModules;
	}

	private List<Module> computeVisibleModules() {
		String query = query();
		return FreedomClient.getModuleManager().getModules().stream()
				.filter(module -> fixedCategory != null ? module.getCategory() == fixedCategory
						: filter == null ? module.getCategory() != Category.HUD && module.getCategory() != Category.COSMETICS : module.getCategory() == filter)
				.filter(module -> !isCosmetics() || slotFilter == null || slotOf(module) == slotFilter)
				.filter(module -> fixedCategory != null || folderFilter == null || ModFolders.contains(folderFilter, module.getId()))
				.filter(module -> query.isEmpty() || nameMatches(module, query) || matchingSetting(module, query) != null)
				.sorted(Comparator.comparing((Module module) -> !module.isFavorite()).thenComparing(order()))
				.toList();
	}

	/** Orden elegido en la barra de la pestaña Mods (por defecto, alfabético). Los favoritos van siempre primero. */
	private static Comparator<Module> order() {
		Comparator<Module> byName = Comparator.comparing(module -> module.getName().toLowerCase(Locale.ROOT));
		return switch (ModFolders.sort()) {
			case "Z-A" -> byName.reversed();
			case "On first" -> Comparator.comparing((Module module) -> !module.isEnabled()).thenComparing(byName);
			case "Category" -> Comparator.comparing((Module module) -> module.getCategory().ordinal()).thenComparing(byName);
			default -> byName;
		};
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

	/**
	 * Cuadrícula de mods. En "All" los de optimización no se mezclan con el resto: van al final, en su propia
	 * sección "Optimization" y en orden alfabético.
	 */
	private void renderGrid(Ui ui, int x, int y, int w, int h) {
		List<Module> all = visibleModules();
		boolean split = fixedCategory == null && filter == null && folderFilter == null;
		Comparator<Module> alphabetical = Comparator.comparing(module -> module.getName().toLowerCase(Locale.ROOT));
		List<Module> modules = split ? all.stream().filter(module -> module.getCategory() != Category.PERFORMANCE).toList() : all;
		List<Module> optimization = split
				? all.stream().filter(module -> module.getCategory() == Category.PERFORMANCE).sorted(alphabetical).toList()
				: List.of();
		if (filter == Category.PERFORMANCE) modules = all.stream().sorted(alphabetical).toList();
		// Mismo margen a los dos lados: la barra de scroll va en el margen de la ventana.
		int innerW = w;
		int columns = Math.max(1, (innerW + GAP) / (cardMinWidth() + GAP));
		int cardWidth = (innerW - GAP * (columns - 1)) / columns;

		int offset = scroll.begin(ui, x, y, innerW, h);
		int cursor = 0;
		String sort = ModFolders.sort();
		if (sort.equals("A-Z") || sort.equals("Z-A")) {
			// En orden alfabético la lista se parte por letras (A, B, C…) con una línea en cada una; los favoritos
			// van antes, en su propia sección.
			java.util.Map<String, List<Module>> sections = new java.util.LinkedHashMap<>();
			for (Module module : modules) {
				sections.computeIfAbsent(module.isFavorite() ? "Favorites" : letter(module), key -> new java.util.ArrayList<>()).add(module);
			}
			boolean first = true;
			for (java.util.Map.Entry<String, List<Module>> section : sections.entrySet()) {
				if (!first) cursor += GAP;
				first = false;
				cursor = sectionHeader(ui, section.getKey(), x, y, cursor - offset, innerW, h) + offset;
				cursor += gridRows(ui, section.getValue(), x, y, y + cursor - offset, h, columns, cardWidth);
			}
		} else {
			cursor = gridRows(ui, modules, x, y, y - offset, h, columns, cardWidth);
		}
		if (!optimization.isEmpty()) {
			if (!modules.isEmpty()) cursor += GAP;
			cursor = sectionHeader(ui, "Optimization", x, y, cursor - offset, innerW, h) + offset;
			cursor += gridRows(ui, optimization, x, y, y + cursor - offset, h, columns, cardWidth);
		}
		if (all.isEmpty()) {
			String message = folderFilter != null && query().isEmpty()
					? "This folder is empty: open a mod and add it to \"" + folderFilter + "\"."
					: "No mods found";
			ui.g.drawCenteredString(ui.font, message, x + innerW / 2, y + 20, ThemeManager.textMuted());
		}
		scroll.end(ui, x, y, innerW, h, cursor);
	}

	/** Letra de la sección alfabética de un mod ("#" si empieza por número o símbolo). */
	private static String letter(Module module) {
		String name = module.getName();
		char c = name.isEmpty() ? '#' : Character.toUpperCase(name.charAt(0));
		return Character.isLetter(c) ? String.valueOf(c) : "#";
	}

	/**
	 * Título de sección con una línea horizontal hasta el borde, en {@code y + cursor}; devuelve el cursor de
	 * debajo. Solo se dibuja si está a la vista.
	 */
	private static int sectionHeader(Ui ui, String title, int x, int y, int cursor, int innerW, int h) {
		int headerY = y + cursor;
		if (headerY + SECTION_HEADER > y && headerY < y + h) {
			ui.g.drawString(ui.font, title, x + 1, headerY + 2, ThemeManager.accent(), false);
			int lineX = x + ui.font.width(title) + 6;
			if (NeonStyle.on()) {
				NeonStyle.hLine(ui.g, lineX, x + innerW, headerY + 6, 1, 0.0, 0.5, 0.8F);
			} else {
				ui.g.fill(lineX, headerY + 6, x + innerW, headerY + 7, ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.5F));
			}
		}
		return cursor + SECTION_HEADER;
	}

	/** Dibuja las tarjetas en filas desde {@code top} y devuelve la altura ocupada. */
	private int gridRows(Ui ui, List<Module> modules, int x, int clipY, int top, int h, int columns, int cardWidth) {
		for (int i = 0; i < modules.size(); i++) {
			int cardX = x + (i % columns) * (cardWidth + GAP);
			int cardY = top + (i / columns) * (cardHeight() + GAP);
			if (cardY + cardHeight() < clipY || cardY > clipY + h) continue;
			renderCard(ui, modules.get(i), cardX, cardY, cardWidth);
		}
		int rows = (modules.size() + columns - 1) / columns;
		return Math.max(0, rows * (cardHeight() + GAP) - GAP);
	}

	/** Estrella pixel de 8x8. */
	private static final String[] STAR = {"...##...", "...##...", "########", ".######.", "..####..", ".##..##.", ".#....#.", "........"};

	private static void star(Ui ui, int x, int y, int color) {
		Draw.art(ui.g, STAR, x, y, color);
	}

	/** Ancho mínimo: las de cosméticos son más anchas para que quepan la vista previa y el texto. */
	private int cardMinWidth() {
		return isCosmetics() ? 150 : CARD_MIN_WIDTH;
	}

	/** Alto de las tarjetas: las de cosméticos son más altas para que quepa la vista previa. */
	private int cardHeight() {
		return isCosmetics() ? COSMETIC_CARD_HEIGHT : CARD_HEIGHT;
	}

	/** Rueda de ajustes pixel de 9x9. */
	private static final String[] GEAR = {
			"...#.#...",
			".#######.",
			".##...##.",
			"##.....##",
			".#..#..#.",
			"##.....##",
			".##...##.",
			".#######.",
			"...#.#...",
	};

	private static void gear(Ui ui, int x, int y, int color) {
		Draw.art(ui.g, GEAR, x, y, color);
	}

	/**
	 * Tarjeta de un mod: se selecciona entera con un clic (activa o apaga el mod). Su color sale de la categoría:
	 * apagada va oscura y, activada, se aclara y lleva el borde de Card Borders. Arriba a la derecha, la rueda
	 * pixel abre los ajustes. En los cosméticos, en vez del icono sale una vista previa del cosmético puesto.
	 */
	private void renderCard(Ui ui, Module module, int x, int y, int w) {
		int h = cardHeight();
		String id = module.getId();
		boolean alwaysOn = !module.canToggle();
		boolean hovered = ui.hovered(x, y, w, h);
		float hover = ui.animate("hover:" + id, hovered ? 1.0F : 0.0F);
		float on = ui.animate("on:" + id, module.isEnabled() || alwaysOn && module instanceof BundledModModule ? 1.0F : 0.0F);
		int category = module.getCategory().getColor();

		int offFill = ThemeManager.mix(ThemeManager.card(), category, 0.07F);
		int onFill = ThemeManager.mix(ThemeManager.mix(ThemeManager.card(), category, 0.34F), 0xFFFFFFFF, 0.06F);
		int fill = ThemeManager.mix(ThemeManager.mix(offFill, onFill, on), ThemeManager.cardHover(), hover * 0.35F);
		int border = ThemeManager.mix(ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.5F), category, 0.25F + 0.25F * hover);
		Draw.bevelPanel(ui.g, x, y, w, h, fill, border);
		if (NeonStyle.on() && on > 0.5F) {
			double phase = (x + y) / 700.0 + NeonStyle.flow() * 0.5;
			NeonStyle.frame(ui.g, x, y, w, h, phase, 0.5, 0.6F + 0.4F * hover);
		}
		// Un borde de la sección Borders de Cosmetics sustituye al de Card Borders.
		if (!com.freedomclient.cosmetic.border.BorderCosmetic.drawActive(ui.g, x, y, w, h, category, on, hover)) {
			com.freedomclient.module.visual.CardBordersModule.draw(ui.g, x, y, w, h, category, on, hover);
		}

		// Izquierda: vista previa del cosmético o el icono del mod en su caja del color de la categoría.
		int boxX = x + 5;
		int boxY = y + 5;
		int boxH = h - 10;
		int boxW = isCosmetics() ? 40 : boxH;
		boolean previewed = false;
		if (module instanceof com.freedomclient.cosmetic.border.BorderCosmetic borderCosmetic) {
			// Vista previa de un borde: una mini tarjeta con el efecto puesto.
			Draw.panel(ui.g, boxX, boxY, boxW, boxH, ThemeManager.shade(), ThemeManager.mix(category, ThemeManager.border(), 0.4F));
			int miniX = boxX + 6;
			int miniY = boxY + 10;
			int miniW = boxW - 12;
			int miniH = boxH - 20;
			Draw.bevelPanel(ui.g, miniX, miniY, miniW, miniH, ThemeManager.mix(ThemeManager.card(), category, 0.3F), ThemeManager.border());
			borderCosmetic.draw(ui.g, miniX, miniY, miniW, miniH, category, 1.0F, hover);
			previewed = true;
		}
		if (!previewed && isCosmetics() && module instanceof CosmeticModule cosmetic && CosmeticPreview.supports(cosmetic)) {
			Draw.panel(ui.g, boxX, boxY, boxW, boxH, ThemeManager.mix(ThemeManager.shade(), category, 0.14F + 0.12F * on),
					ThemeManager.mix(category, ThemeManager.border(), 0.4F));
			previewed = CosmeticPreview.render(ui.g, cosmetic, boxX + 1, boxY + 1, boxX + boxW - 1, boxY + boxH - 1);
		}
		if (!previewed) {
			int iconBox = Math.min(boxW, boxH);
			Draw.iconBox(ui.g, module.getIcon(), boxX + (boxW - iconBox) / 2, boxY + (boxH - iconBox) / 2, iconBox, category);
		}

		int textX = boxX + boxW + 5;
		// A la derecha quedan la rueda de ajustes y la estrella.
		int textWidth = x + w - textX - 24;
		int nameColor = ThemeManager.mix(ThemeManager.text(), 0xFFFFFFFF, 0.3F * on);
		boolean twoLines = ui.font.width(module.getName()) > textWidth;
		int nameY = y + (isCosmetics() ? 7 : 5);
		if (twoLines) {
			String[] lines = splitName(ui, module.getName(), textWidth, x + w - textX - 6);
			ui.g.drawString(ui.font, lines[0], textX, nameY, nameColor, false);
			ui.g.drawString(ui.font, lines[1], textX, nameY + 10, nameColor, false);
		} else {
			ui.g.drawString(ui.font, module.getName(), textX, nameY + 1, nameColor, false);
		}

		// Si el mod sale por una opción (no por su nombre), se muestra cuál.
		String query = query();
		Setting<?> matched = query.isEmpty() || nameMatches(module, query) ? null : matchingSetting(module, query);
		int statusY = y + h - 13;
		if (matched != null) {
			String label = ui.font.plainSubstrByWidth("> " + matched.getName(), x + w - textX - 4);
			ui.g.drawString(ui.font, label, textX, statusY, ThemeManager.highlight(), false);
		} else if (!twoLines || isCosmetics()) {
			// Estado con un punto de color: encendido en el color de la categoría.
			String label = module instanceof BundledModModule ? "Always on" : alwaysOn ? "Open >" : module.isEnabled() ? "ON" : "OFF";
			int dot = module.isEnabled() || alwaysOn ? category : ThemeManager.textMuted();
			ui.g.fill(textX, statusY + 2, textX + 4, statusY + 6, dot);
			ui.g.drawString(ui.font, label, textX + 7, statusY, module.isEnabled() || alwaysOn ? ThemeManager.mix(category, 0xFFFFFFFF, 0.35F)
					: ThemeManager.textMuted(), false);
		}
		if (isCosmetics() && !twoLines) {
			// Descripción corta debajo del nombre en las tarjetas de cosméticos, que tienen más sitio.
			String description = ui.font.plainSubstrByWidth(module.getDescription(), x + w - textX - 5);
			ui.g.drawString(ui.font, description, textX, nameY + 13, ThemeManager.textMuted(), false);
		}

		// Arriba a la derecha: rueda de ajustes (y la estrella de favorito a su izquierda).
		int gearX = x + w - 13;
		int gearY = y + 4;
		boolean gearHovered = ui.hovered(gearX - 2, gearY - 2, 13, 13);
		gear(ui, gearX, gearY, gearHovered ? ThemeManager.highlight() : hovered ? ThemeManager.text() : ThemeManager.textMuted());
		int starX = gearX - 11;
		int starY = y + 4;
		boolean starHovered = ui.hovered(starX - 1, starY - 1, 10, 10);
		if (module.isFavorite() || hovered) {
			star(ui, starX, starY, module.isFavorite() ? ThemeManager.accent() : starHovered ? ThemeManager.highlight() : ThemeManager.textMuted());
		}
		if (gearHovered) ui.tooltip("Settings");

		// Clic en la tarjeta: la selecciona (activa o apaga); clic derecho o la rueda: ajustes.
		ui.click(x, y, w, h, (mx, my, button) -> {
			if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT || !module.canToggle()) {
				screen.openModule(module, query);
			} else {
				module.toggle();
			}
			ui.playClick();
			return true;
		});
		ui.click(starX - 1, starY - 1, 10, 10, (mx, my, button) -> {
			module.setFavorite(!module.isFavorite());
			ui.playClick();
			return true;
		});
		ui.click(gearX - 2, gearY - 2, 13, 13, (mx, my, button) -> {
			screen.openModule(module, query);
			ui.playClick();
			return true;
		});
	}

	private static String[] splitName(Ui ui, String name, int firstWidth, int secondWidth) {
		String[] words = name.split(" ");
		StringBuilder first = new StringBuilder();
		int i = 0;
		while (i < words.length) {
			String next = first.isEmpty() ? words[i] : first + " " + words[i];
			if (ui.font.width(next) > firstWidth && !first.isEmpty()) {
				break;
			}
			first.setLength(0);
			first.append(next);
			i++;
		}
		String rest = String.join(" ", java.util.Arrays.copyOfRange(words, i, words.length));
		if (ui.font.width(rest) > secondWidth) {
			rest = ui.font.plainSubstrByWidth(rest, secondWidth - ui.font.width("..")) + "..";
		}
		String head = ui.font.width(first.toString()) > firstWidth ? ui.font.plainSubstrByWidth(first.toString(), firstWidth) : first.toString();
		return new String[] {head, rest};
	}
}
