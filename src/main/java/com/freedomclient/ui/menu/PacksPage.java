package com.freedomclient.ui.menu;

import com.freedomclient.FreedomClient;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.NeonStyle;
import com.freedomclient.ui.ScrollArea;
import com.freedomclient.ui.TextField;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.theme.ThemeManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.Util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pestaña Packs: los paquetes de texturas como tarjetas con su icono, nombre y descripción. Clic en una tarjeta para
 * ponerla o quitarla; las puestas salen arriba en orden (la primera manda sobre las demás) y se pueden subir y bajar.
 * Los cambios no se aplican hasta pulsar "Apply", que recarga las texturas una sola vez.
 */
public class PacksPage implements MenuPage {
	private static final int BAR_HEIGHT = 14;
	private static final int CARD_HEIGHT = 40;
	private static final int GAP = 4;
	private static final Identifier UNKNOWN_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");

	private final ScrollArea scroll = new ScrollArea();
	private final TextField search = new TextField(32);
	/** Packs elegidos en el orden del juego (el primero es el de más abajo; el último manda sobre todos). */
	private final List<String> pending = new ArrayList<>();
	private final Map<String, Icon> icons = new HashMap<>();
	private boolean loaded;

	private record Icon(Identifier id, int width, int height) {
	}

	private static PackRepository repository() {
		return Minecraft.getInstance().getResourcePackRepository();
	}

	/** Vuelve a leer lo que está puesto ahora en el juego (descarta lo no aplicado). */
	private void load() {
		loaded = true;
		pending.clear();
		pending.addAll(repository().getSelectedIds());
	}

	private boolean changed() {
		return !pending.equals(new ArrayList<>(repository().getSelectedIds()));
	}

	/** Packs que el jugador puede poner o quitar (los obligatorios, como el de Minecraft, no salen). */
	private static List<Pack> choosable() {
		List<Pack> packs = new ArrayList<>();
		for (Pack pack : repository().getAvailablePacks()) {
			if (!pack.isRequired() && !pack.isFixedPosition()) packs.add(pack);
		}
		packs.sort((a, b) -> a.getTitle().getString().compareToIgnoreCase(b.getTitle().getString()));
		return packs;
	}

	@Override
	public void render(Ui ui, int x, int y, int w, int h) {
		if (!loaded) load();
		renderBar(ui, x, y, w);
		int listY = y + BAR_HEIGHT + 6;
		int listH = h - BAR_HEIGHT - 6;

		String query = search.getText().toLowerCase(Locale.ROOT).trim();
		List<Pack> all = choosable();
		// Puestos arriba, del que más manda al que menos; debajo, los demás.
		List<Pack> selected = new ArrayList<>();
		for (int i = pending.size() - 1; i >= 0; i--) {
			Pack pack = repository().getPack(pending.get(i));
			if (pack != null && all.contains(pack)) selected.add(pack);
		}
		List<Pack> available = all.stream().filter(pack -> !pending.contains(pack.getId())).toList();

		int offset = scroll.begin(ui, x, listY, w, listH);
		int cursor = 0;
		cursor = section(ui, "Active (top wins)", x, listY + cursor - offset, w, listY, listH) + cursor;
		if (selected.isEmpty()) {
			ui.g.drawString(ui.font, "No packs on: click a pack below to add it.", x + 4, listY + cursor - offset + 2, ThemeManager.textMuted(), false);
			cursor += 14;
		}
		for (int i = 0; i < selected.size(); i++) {
			Pack pack = selected.get(i);
			if (!matches(pack, query)) continue;
			renderCard(ui, pack, i + 1, i > 0, i < selected.size() - 1, x, listY + cursor - offset, w, listY, listH);
			cursor += CARD_HEIGHT + GAP;
		}
		cursor += GAP;
		cursor = section(ui, "Available", x, listY + cursor - offset, w, listY, listH) + cursor;
		int shown = 0;
		for (Pack pack : available) {
			if (!matches(pack, query)) continue;
			renderCard(ui, pack, 0, false, false, x, listY + cursor - offset, w, listY, listH);
			cursor += CARD_HEIGHT + GAP;
			shown++;
		}
		if (shown == 0) {
			String message = available.isEmpty() ? "Drop pack files in the folder (Folder button) and press Refresh." : "No packs match your search.";
			ui.g.drawString(ui.font, message, x + 4, listY + cursor - offset + 2, ThemeManager.textMuted(), false);
			cursor += 14;
		}
		scroll.end(ui, x, listY, w, listH, cursor);
	}

	private static boolean matches(Pack pack, String query) {
		return query.isEmpty() || pack.getTitle().getString().toLowerCase(Locale.ROOT).contains(query)
				|| pack.getDescription().getString().toLowerCase(Locale.ROOT).contains(query);
	}

	/** Título de sección; devuelve cuánto baja el cursor. */
	private static int section(Ui ui, String title, int x, int y, int w, int clipY, int clipH) {
		if (y + 12 > clipY && y < clipY + clipH) {
			ui.g.drawString(ui.font, title, x + 1, y + 2, ThemeManager.accent(), false);
			ui.g.fill(x + ui.font.width(title) + 6, y + 6, x + w, y + 7, ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.5F));
		}
		return 14;
	}

	/** Barra de arriba: buscador, abrir la carpeta, volver a buscar packs y aplicar. */
	private void renderBar(Ui ui, int x, int y, int w) {
		int right = x + w;
		boolean dirty = changed();
		right = button(ui, dirty ? "Apply" : "Applied", right, y, dirty, dirty, () -> apply(ui.minecraft));
		right = button(ui, "Undo", right - 3, y, false, dirty, this::load);
		right = button(ui, "Refresh", right - 3, y, false, true, () -> {
			repository().reload();
			pending.removeIf(id -> !repository().isAvailable(id));
		});
		right = button(ui, "Folder", right - 3, y, false, true,
				() -> Util.getPlatform().openPath(ui.minecraft.getResourcePackDirectory()));
		int searchW = Math.max(60, right - 4 - x);
		search.render(ui, x, y + 1, Math.min(searchW, 140), BAR_HEIGHT - 2, "Search packs...");
	}

	/** Botón alineado a la derecha que acaba en {@code right}; devuelve su borde izquierdo. */
	private static int button(Ui ui, String label, int right, int y, boolean primary, boolean enabled, Runnable action) {
		int width = ui.font.width(label) + 12;
		int bx = right - width;
		boolean hovered = enabled && ui.hovered(bx, y, width, BAR_HEIGHT);
		int fill = primary ? (hovered ? ThemeManager.highlight() : ThemeManager.accent()) : hovered ? ThemeManager.cardHover() : ThemeManager.card();
		Draw.bevelPanel(ui.g, bx, y, width, BAR_HEIGHT, fill, primary ? ThemeManager.accent() : ThemeManager.border());
		int text = primary ? ThemeManager.shade() : enabled ? ThemeManager.text() : ThemeManager.textMuted();
		ui.g.drawString(ui.font, label, bx + 6, y + 3, text, false);
		if (enabled) {
			ui.click(bx, y, width, BAR_HEIGHT, (mx, my, b) -> {
				action.run();
				ui.playClick();
				return true;
			});
		}
		return bx;
	}

	/** Aplica los packs elegidos: se guardan en las opciones y se recargan las texturas (solo si ha cambiado algo). */
	private void apply(Minecraft client) {
		PackRepository repository = repository();
		List<String> ids = new ArrayList<>();
		// Los obligatorios y fijos (Minecraft, los de los mods) se mantienen donde estaban.
		for (Pack pack : repository.getSelectedPacks()) {
			if (pack.isRequired() || pack.isFixedPosition()) ids.add(pack.getId());
		}
		for (String id : pending) {
			if (!ids.contains(id)) ids.add(id);
		}
		repository.setSelected(ids);
		client.options.updateResourcePacks(repository);
		load();
	}

	private void renderCard(Ui ui, Pack pack, int rank, boolean canUp, boolean canDown, int x, int y, int w, int clipY, int clipH) {
		if (y + CARD_HEIGHT < clipY || y > clipY + clipH) return;
		String id = pack.getId();
		boolean on = rank > 0;
		boolean hovered = ui.hovered(x, y, w, CARD_HEIGHT);
		float hover = ui.animate("pack:" + id, hovered ? 1.0F : 0.0F);
		int base = on ? ThemeManager.mix(ThemeManager.card(), ThemeManager.accent(), 0.22F) : ThemeManager.card();
		int fill = ThemeManager.mix(base, ThemeManager.cardHover(), hover * 0.6F);
		int border = on ? ThemeManager.accent() : ThemeManager.mix(ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.45F), ThemeManager.highlight(), hover);
		Draw.bevelPanel(ui.g, x, y, w, CARD_HEIGHT, fill, border);
		if (on) {
			// Borde doble para los que están puestos.
			ui.g.renderOutline(x + 2, y + 2, w - 4, CARD_HEIGHT - 4, ThemeManager.withAlpha(ThemeManager.accent(), 0.45F));
			if (NeonStyle.on()) NeonStyle.frame(ui.g, x, y, w, CARD_HEIGHT, (x + y) / 700.0 + NeonStyle.flow() * 0.5, 0.5, 0.9F);
		}

		Icon icon = icon(pack);
		ui.g.fill(x + 4, y + 4, x + 36, y + 36, ThemeManager.shade());
		ui.g.blit(RenderPipelines.GUI_TEXTURED, icon.id(), x + 4, y + 4, 0.0F, 0.0F, 32, 32, icon.width(), icon.height(), icon.width(), icon.height());

		int textX = x + 42;
		int rightSpace = on ? 44 : 34;
		int textW = w - (textX - x) - rightSpace;
		String title = ui.font.plainSubstrByWidth(pack.getTitle().getString(), textW);
		ui.g.drawString(ui.font, title, textX, y + 5, ThemeManager.text(), false);
		// Descripción en dos líneas como mucho.
		List<String> lines = wrap(ui, pack.getDescription().getString().replace('\n', ' '), textW, 2);
		for (int i = 0; i < lines.size(); i++) {
			ui.g.drawString(ui.font, lines.get(i), textX, y + 16 + i * 10, ThemeManager.textMuted(), false);
		}
		if (!pack.getCompatibility().isCompatible()) {
			ui.g.drawString(ui.font, "!", x + 38, y + 4, 0xFFFF5555, false);
			if (hovered) ui.tooltip("Made for another Minecraft version: it may look wrong.");
		}

		// Derecha: el puesto (#1 manda) con flechas para cambiar el orden, o "+" para ponerlo.
		int rx = x + w - rightSpace;
		if (on) {
			String label = "#" + rank;
			ui.g.drawString(ui.font, label, rx + 4, y + 16, ThemeManager.accent(), false);
			arrow(ui, rx + 26, y + 6, true, canUp, () -> move(id, 1));
			arrow(ui, rx + 26, y + 24, false, canDown, () -> move(id, -1));
		} else {
			ui.g.drawString(ui.font, "+ Add", rx - 2, y + 16, hovered ? ThemeManager.highlight() : ThemeManager.textMuted(), false);
		}

		ui.click(x, y, w, CARD_HEIGHT, (mx, my, b) -> {
			if (on) pending.remove(id);
			else pending.add(id);
			ui.playClick();
			return true;
		});
		if (on) {
			// Las flechas se registran después para tener prioridad sobre la tarjeta.
			if (canUp) registerArrow(ui, rx + 26, y + 6, () -> move(id, 1));
			if (canDown) registerArrow(ui, rx + 26, y + 24, () -> move(id, -1));
		}
	}

	/** Flecha pixel de 9x6 hacia arriba o hacia abajo. */
	private static void arrow(Ui ui, int x, int y, boolean up, boolean enabled, Runnable action) {
		int color = !enabled ? ThemeManager.withAlpha(ThemeManager.textMuted(), 0.35F)
				: ui.hovered(x - 2, y - 2, 13, 10) ? ThemeManager.highlight() : ThemeManager.text();
		for (int row = 0; row < 5; row++) {
			int half = up ? row : 4 - row;
			ui.g.fill(x + 4 - half, y + row, x + 5 + half, y + row + 1, color);
		}
	}

	private void registerArrow(Ui ui, int x, int y, Runnable action) {
		ui.click(x - 2, y - 2, 13, 10, (mx, my, b) -> {
			action.run();
			ui.playClick();
			return true;
		});
	}

	/** Sube ({@code +1}) o baja ({@code -1}) un pack puesto: subir es ir hacia el final de la lista del juego. */
	private void move(String id, int direction) {
		int index = pending.indexOf(id);
		int target = index + direction;
		if (index < 0 || target < 0 || target >= pending.size()) return;
		pending.remove(index);
		pending.add(target, id);
	}

	private static List<String> wrap(Ui ui, String text, int width, int maxLines) {
		List<String> lines = new ArrayList<>();
		String rest = text.trim();
		while (!rest.isEmpty() && lines.size() < maxLines) {
			String line = ui.font.plainSubstrByWidth(rest, width);
			if (line.length() < rest.length()) {
				int space = line.lastIndexOf(' ');
				if (space > 0) line = line.substring(0, space);
			}
			if (line.isEmpty()) break;
			rest = rest.substring(line.length()).trim();
			if (lines.size() == maxLines - 1 && !rest.isEmpty()) {
				line = ui.font.plainSubstrByWidth(line, width - ui.font.width("..")) + "..";
			}
			lines.add(line);
		}
		return lines;
	}

	/** Icono del pack (su pack.png), cargado la primera vez; si no tiene, el de "pack desconocido". */
	private Icon icon(Pack pack) {
		return icons.computeIfAbsent(pack.getId(), id -> {
			try (PackResources resources = pack.open()) {
				IoSupplier<InputStream> supplier = resources.getRootResource("pack.png");
				if (supplier != null) {
					try (InputStream input = supplier.get()) {
						NativeImage image = NativeImage.read(input);
						Identifier texture = FreedomClient.id("pack_icon/" + Integer.toHexString(id.hashCode()));
						int width = image.getWidth();
						int height = image.getHeight();
						Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(() -> "FreedomClient pack icon " + id, image));
						return new Icon(texture, width, height);
					}
				}
			} catch (Exception e) {
				FreedomClient.LOGGER.debug("No icon for pack {}", id, e);
			}
			return new Icon(UNKNOWN_ICON, 16, 16);
		});
	}

	/** Packs puestos ahora mismo (para otras pantallas). */
	public static Collection<String> activeIds() {
		return repository().getSelectedIds();
	}
}
