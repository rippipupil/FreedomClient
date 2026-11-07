package com.freedomclient.ui.menu;

import com.freedomclient.FreedomClient;
import com.freedomclient.freedomplayer.ClientLibrary;
import com.freedomclient.freedomplayer.ClientLibrary.Entry;
import com.freedomclient.freedomplayer.FreedomPlayerHub;
import com.freedomclient.freedomplayer.FreedomSync;
import com.freedomclient.freedomplayer.LibraryData.Playlist;
import com.freedomclient.freedomplayer.LibraryData.Ranking;
import com.freedomclient.freedomplayer.LibraryData.Song;
import com.freedomclient.module.hud.MusicPlayerHud;
import com.freedomclient.music.LocalMusicPlayer;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.ScrollArea;
import com.freedomclient.ui.TextField;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.theme.ThemeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pestaña Music: la biblioteca de FreedomPlayer dentro del cliente. Las canciones, playlists y rankings que llegan del
 * móvil (y las de FreedomPlayer del PC, si está instalado), con buscador, su nota en estrellas (se puede cambiar y
 * vuelve al móvil) y botón de tocar en el Music Player. «Link phone» enseña el QR para vincular el móvil.
 */
public class MusicPage implements MenuPage {
	private enum View {
		SONGS("Songs"), PLAYLISTS("Playlists"), RANKINGS("Rankings"), KEYS("Keys");

		private final String label;

		View(String label) {
			this.label = label;
		}
	}

	private static final int BAR_HEIGHT = 14;
	private static final int ROW_HEIGHT = 22;
	private static final int FOOTER_HEIGHT = 16;
	private static final String[] STAR = {"...#...", "..###..", "#######", ".#####.", "..###..", ".##.##."};
	private static final String[] STAR_LEFT = {"...#", "..##", "####", ".###", "..##", ".##."};
	private static final String[] NOTE = {"..####", "..#..#", "..#..#", "###.##", "##..##"};
	private static final String[] PLAY = {"#...", "##..", "###.", "##..", "#..."};
	private static final String[] PAUSE = {"#.#", "#.#", "#.#", "#.#", "#.#"};
	private static final String[] NEXT = {"#..#", "##.#", "####", "##.#", "#..#"};
	private static final String[] PREVIOUS = {"#..#", "#.##", "####", "#.##", "#..#"};
	private static final String[] BACK = {"..#", ".#.", "#..", ".#.", "..#"};
	private static final String[] SPEAKER = {"...#..", "..##.#", "####..", "####.#", "..##..", "...#.."};
	private static final String[] MUTED = {"...#..", "..##..", "####.#", "####..", "..##.#", "...#.."};
	private static final int SLIDER_WIDTH = 54;

	private final SettingRows rows = new SettingRows();
	/** El volumen de antes de silenciar con el altavoz. */
	private double unmuted = 60;

	private final ScrollArea scroll = new ScrollArea();
	private final TextField search = new TextField(40);
	private View view = View.SONGS;
	/** La playlist o el ranking abierto (su id), o null para la lista. */
	private String opened;
	private boolean linking;

	@Override
	public void render(Ui ui, int x, int y, int w, int h) {
		FreedomSync sync = FreedomPlayerHub.sync();
		// Al vincular, el QR se queda hasta que el móvil lo escanea; después se vuelve a la biblioteca.
		if (linking && !sync.accepting() && sync.phase() != FreedomSync.Phase.FAILED) linking = false;
		renderBar(ui, sync, x, y, w);
		int statusY = y + BAR_HEIGHT + 4;
		renderStatus(ui, sync, x, statusY, w);
		int listY = statusY + 13;
		int listH = h - (listY - y) - FOOTER_HEIGHT - 4;
		if (linking) {
			renderLink(ui, sync, x, listY, w, listH);
		} else if (view == View.KEYS) {
			renderKeys(ui, x, listY, w, listH);
		} else {
			renderLibrary(ui, x, listY, w, listH);
		}
		renderFooter(ui, x, y + h - FOOTER_HEIGHT, w);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Barra y estado

	private void renderBar(Ui ui, FreedomSync sync, int x, int y, int w) {
		int bx = x;
		for (View current : View.values()) {
			View target = current;
			bx = toggle(ui, current.label, bx, y, view == current && !linking, () -> {
				view = target;
				opened = null;
				linking = false;
				scroll.reset();
			}) + 3;
		}
		String linkLabel = linking ? "Close QR" : sync.linked() ? "Link another phone" : "Link phone";
		int right = button(ui, linkLabel, x + w, y, !sync.linked() && !linking, () -> {
			if (linking) {
				linking = false;
				sync.stopLinking();
			} else {
				linking = true;
				sync.startLinking();
			}
		});
		int searchW = Math.min(130, right - 4 - bx);
		if (searchW > 40 && view != View.KEYS) search.render(ui, bx + 2, y + 1, searchW, BAR_HEIGHT - 2, "Search songs...");
	}

	private void renderStatus(Ui ui, FreedomSync sync, int x, int y, int w) {
		int songs = FreedomPlayerHub.library().data().songs().size();
		String text;
		int dot;
		switch (sync.phase()) {
			case DOWNLOADING -> {
				text = "Downloading Syncthing (" + Math.round(sync.downloadProgress() * 100) + "%)...";
				dot = ThemeManager.accent();
			}
			case STARTING -> {
				text = "Starting Syncthing...";
				dot = ThemeManager.accent();
			}
			case WAITING -> {
				text = "Waiting for your phone to scan the code...";
				dot = ThemeManager.accent();
			}
			case PAIRING -> {
				text = "Linking with your phone...";
				dot = ThemeManager.accent();
			}
			case SYNCING -> {
				text = sync.connected() ? "Syncing with your phone: " + sync.done() + " of " + sync.total() + " songs"
						: "Linked. Looking for your phone (FreedomPlayer open, same Wi-Fi)...";
				dot = sync.connected() ? 0xFF3FD7A0 : 0xFFFFB43F;
			}
			case SYNCED -> {
				text = "Synced with your phone: " + songs + (songs == 1 ? " song" : " songs");
				dot = 0xFF3FD7A0;
			}
			case FAILED -> {
				text = sync.error() == null ? "Could not sync." : sync.error();
				dot = 0xFFFF5555;
			}
			default -> {
				text = sync.linked() ? "Linked with your phone (it syncs while the game is open)."
						: "Not linked: press Link phone to bring your phone's songs.";
				dot = sync.linked() ? 0xFF3FD7A0 : ThemeManager.textMuted();
			}
		}
		ui.g.fill(x + 2, y + 2, x + 6, y + 6, dot);
		ui.g.drawString(ui.font, ui.font.plainSubstrByWidth(text, w - 12), x + 10, y, ThemeManager.textMuted(), false);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Vincular el móvil

	private void renderLink(Ui ui, FreedomSync sync, int x, int y, int w, int h) {
		boolean[][] qr = sync.qr();
		int box = Math.min(h - 4, 132);
		int textX = x + box + 12;
		int textW = w - (textX - x) - 4;
		if (qr != null && (sync.phase() == FreedomSync.Phase.WAITING || sync.phase() == FreedomSync.Phase.PAIRING)) {
			// El QR en negro sobre blanco con su margen (4 cuadros), que es lo que necesita la cámara.
			int n = qr.length;
			int cell = Math.max(1, box / (n + 8));
			int size = cell * (n + 8);
			int qx = x + (box - size) / 2, qy = y + (h - size) / 2;
			ui.g.fill(qx, qy, qx + size, qy + size, 0xFFFFFFFF);
			for (int row = 0; row < n; row++) {
				for (int col = 0; col < n; col++) {
					if (qr[row][col]) ui.g.fill(qx + (col + 4) * cell, qy + (row + 4) * cell, qx + (col + 5) * cell, qy + (row + 5) * cell, 0xFF000000);
				}
			}
		} else {
			Draw.panel(ui.g, x, y + (h - box) / 2, box, box, ThemeManager.shade(), ThemeManager.border());
			String wait = switch (sync.phase()) {
				case DOWNLOADING -> Math.round(sync.downloadProgress() * 100) + "%";
				case FAILED -> "!";
				default -> "...";
			};
			ui.g.drawString(ui.font, wait, x + (box - ui.font.width(wait)) / 2, y + h / 2 - 4, ThemeManager.accent(), false);
		}

		int ty = y + 2;
		ui.g.drawString(ui.font, "Link your phone", textX, ty, ThemeManager.accent(), false);
		ty += 14;
		String[] steps = {
				"1. Open FreedomPlayer on your phone.",
				"2. Go to Settings > Vincular con el PC. If it is already linked with your PC, tap VINCULAR OTRO PC O FREEDOMCLIENT.",
				"3. Scan this code and keep this screen open until it says Linked.",
				"Your songs, playlists, rankings and scores come here while the game and FreedomPlayer are open on the same Wi-Fi."
		};
		for (String step : steps) {
			for (String line : wrap(ui, step, textW)) {
				ui.g.drawString(ui.font, line, textX, ty, step.startsWith("Your") ? ThemeManager.textMuted() : ThemeManager.text(), false);
				ty += 10;
			}
			ty += 3;
		}
		if (sync.phase() == FreedomSync.Phase.FAILED) {
			for (String line : wrap(ui, sync.error() == null ? "Something went wrong." : sync.error(), textW)) {
				ui.g.drawString(ui.font, line, textX, ty, 0xFFFF5555, false);
				ty += 10;
			}
			button(ui, "Try again", textX + ui.font.width("Try again") + 12, ty + 2, true, sync::startLinking);
		} else if (sync.myId() != null && ty + 20 < y + h) {
			// El id de este dispositivo en Syncthing (lo que lleva el QR), en dos líneas para que quepa.
			String id = sync.myId();
			int half = id.length() / 2 + 1;
			int idY = Math.max(ty, y + h - 20);
			int color = ThemeManager.withAlpha(ThemeManager.textMuted(), 0.7F);
			ui.g.drawString(ui.font, id.substring(0, Math.min(half, id.length())), textX, idY, color, false);
			ui.g.drawString(ui.font, id.substring(Math.min(half, id.length())), textX, idY + 10, color, false);
		}
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Teclas y volumen

	/** El volumen y las teclas de acceso rápido del Music Player (saltar, anterior, pausa...). */
	private void renderKeys(Ui ui, int x, int y, int w, int h) {
		MusicPlayerHud hud = hud();
		int offset = scroll.begin(ui, x, y, w, h);
		int cursor = 0;
		for (String line : wrap(ui, "Keys work while you play (not with a menu open). Click a box and press a key; right click clears it.", w - 8)) {
			ui.g.drawString(ui.font, line, x + 2, y + cursor - offset, ThemeManager.textMuted(), false);
			cursor += 10;
		}
		cursor += 4;
		cursor += rows.render(ui, hud.volume(), x, y + cursor - offset, w) + SettingRows.ROW_GAP;
		boolean bound = false;
		for (com.freedomclient.setting.KeybindSetting key : hud.keys()) {
			cursor += rows.render(ui, key, x, y + cursor - offset, w) + SettingRows.ROW_GAP;
			bound |= key.isBound();
		}
		scroll.end(ui, x, y, w, h, cursor);
		// Las teclas las lee el Music Player: si se pone una, se enciende.
		if (bound) ensurePlayerOn();
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Biblioteca

	private void renderLibrary(Ui ui, int x, int y, int w, int h) {
		ClientLibrary library = FreedomPlayerHub.library();
		int listY = y;
		int listH = h;
		List<Entry> songs;
		boolean numbered = false;
		if (view == View.SONGS) {
			songs = filter(library.songs());
		} else if (opened != null) {
			// Dentro de una playlist o un ranking: cabecera con volver, su nombre y tocar todo.
			String name = "";
			List<String> ids = List.of();
			if (view == View.PLAYLISTS) {
				for (Playlist p : library.playlists()) if (p.id().equals(opened)) {
					name = p.name();
					ids = p.songIds();
				}
			} else {
				for (Ranking r : library.rankings()) if (r.id().equals(opened)) {
					name = r.name();
					ids = r.songIds();
				}
				numbered = true;
			}
			songs = filter(resolve(library, ids));
			List<Entry> all = songs;
			int bx = x;
			boolean hover = ui.hovered(bx, listY, 40, 14);
			Draw.art(ui.g, BACK, bx + 3, listY + 4, hover ? ThemeManager.highlight() : ThemeManager.text());
			ui.g.drawString(ui.font, "Back", bx + 9, listY + 3, hover ? ThemeManager.highlight() : ThemeManager.text(), false);
			ui.click(bx, listY, 40, 14, (mx, my, b) -> {
				opened = null;
				scroll.reset();
				ui.playClick();
				return true;
			});
			int playRight = button(ui, "Play all", x + w, listY, true, () -> play(all, 0));
			ui.g.drawString(ui.font, ui.font.plainSubstrByWidth(name, playRight - bx - 50), bx + 46, listY + 3, ThemeManager.accent(), false);
			listY += 18;
			listH -= 18;
		} else {
			renderLists(ui, library, x, listY, w, listH);
			return;
		}

		int offset = scroll.begin(ui, x, listY, w, listH);
		int cursor = 0;
		for (int i = 0; i < songs.size(); i++) {
			int rowY = listY + cursor - offset;
			if (rowY + ROW_HEIGHT > listY && rowY < listY + listH) songRow(ui, songs, i, numbered ? i + 1 : 0, x, rowY, w);
			cursor += ROW_HEIGHT + 2;
		}
		if (songs.isEmpty()) {
			String message = search.getText().isBlank()
					? (view == View.SONGS ? "No songs yet. Link your phone, or install FreedomPlayer on this PC." : "This list has no songs here yet.")
					: "No songs match your search.";
			ui.g.drawString(ui.font, ui.font.plainSubstrByWidth(message, w - 8), x + 4, listY + 4, ThemeManager.textMuted(), false);
			cursor = 14;
		}
		scroll.end(ui, x, listY, w, listH, cursor);
	}

	/** Las playlists o los rankings, como filas: color, nombre y cuántas canciones. */
	private void renderLists(Ui ui, ClientLibrary library, int x, int y, int w, int h) {
		record Row(String id, String name, int count, long color) {
		}
		List<Row> rows = new ArrayList<>();
		String query = search.getText().toLowerCase(Locale.ROOT).trim();
		if (view == View.PLAYLISTS) {
			for (Playlist p : library.playlists()) rows.add(new Row(p.id(), p.name(), p.songIds().size(), p.coverColor()));
		} else {
			for (Ranking r : library.rankings()) rows.add(new Row(r.id(), r.name(), r.songIds().size(), 0xFFC2326FL));
		}
		rows.removeIf(r -> !query.isEmpty() && !r.name().toLowerCase(Locale.ROOT).contains(query));
		int offset = scroll.begin(ui, x, y, w, h);
		int cursor = 0;
		for (Row row : rows) {
			int rowY = y + cursor - offset;
			if (rowY + ROW_HEIGHT > y && rowY < y + h) {
				boolean hovered = ui.hovered(x, rowY, w, ROW_HEIGHT);
				Draw.bevelPanel(ui.g, x, rowY, w, ROW_HEIGHT, hovered ? ThemeManager.cardHover() : ThemeManager.card(), ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.5F));
				cover(ui, (int) row.color(), x + 4, rowY + 4);
				ui.g.drawString(ui.font, ui.font.plainSubstrByWidth(row.name(), w - 90), x + 24, rowY + 7, ThemeManager.text(), false);
				String count = row.count() + (row.count() == 1 ? " song" : " songs");
				ui.g.drawString(ui.font, count, x + w - 8 - ui.font.width(count), rowY + 7, ThemeManager.textMuted(), false);
				String id = row.id();
				ui.click(x, rowY, w, ROW_HEIGHT, (mx, my, b) -> {
					opened = id;
					scroll.reset();
					ui.playClick();
					return true;
				});
			}
			cursor += ROW_HEIGHT + 2;
		}
		if (rows.isEmpty()) {
			String message = view == View.PLAYLISTS ? "No playlists yet: they come from FreedomPlayer on your phone." : "No rankings yet: make one in FreedomPlayer on your phone.";
			ui.g.drawString(ui.font, ui.font.plainSubstrByWidth(message, w - 8), x + 4, y + 4, ThemeManager.textMuted(), false);
			cursor = 14;
		}
		scroll.end(ui, x, y, w, h, cursor);
	}

	/** Una canción: color de carátula, título y artista, estrellas (la nota) y tocar. */
	private void songRow(Ui ui, List<Entry> list, int index, int number, int x, int y, int w) {
		Entry entry = list.get(index);
		Song song = entry.song();
		boolean playing = song.id().equals(player().songId());
		boolean hovered = ui.hovered(x, y, w, ROW_HEIGHT);
		int fill = playing ? ThemeManager.mix(ThemeManager.card(), ThemeManager.accent(), 0.2F) : hovered ? ThemeManager.cardHover() : ThemeManager.card();
		Draw.bevelPanel(ui.g, x, y, w, ROW_HEIGHT, fill, playing ? ThemeManager.accent() : ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.5F));
		int left = x + 4;
		if (number > 0) {
			String rank = "#" + number;
			ui.g.drawString(ui.font, rank, left, y + 7, number <= 3 ? ThemeManager.accent() : ThemeManager.textMuted(), false);
			left += 20;
		}
		cover(ui, (int) song.coverColor(), left, y + 4);
		int textX = left + 20;
		int starsX = x + w - 60;
		int textW = starsX - textX - 6;
		ui.g.drawString(ui.font, ui.font.plainSubstrByWidth(song.title(), textW), textX, y + 3, playing ? ThemeManager.accent() : ThemeManager.text(), false);
		ui.g.drawString(ui.font, ui.font.plainSubstrByWidth(song.artist(), textW), textX, y + 12, ThemeManager.textMuted(), false);
		stars(ui, entry, starsX, y + 8);
		// Tocar: toda la lista que se ve, empezando por esta.
		int playColor = hovered ? ThemeManager.highlight() : ThemeManager.textMuted();
		Draw.art(ui.g, PLAY, x + w - 12, y + 8, playColor);
		ui.click(x, y, w, ROW_HEIGHT, (mx, my, b) -> {
			play(list, index);
			ui.playClick();
			return true;
		});
		if (entry.synced()) {
			// Las estrellas van después para tener prioridad sobre la fila: mitad izquierda media estrella, derecha entera.
			for (int i = 0; i < 5; i++) {
				int star = i;
				ui.click(starsX + i * 9 - 1, y + 4, 9, 14, (mx, my, b) -> {
					int value = star * 2 + (mx - (starsX + star * 9) < 4 ? 1 : 2);
					Integer current = song.score();
					FreedomPlayerHub.library().setScore(song.id(), current != null && current == value ? null : value);
					ui.playClick();
					return true;
				});
			}
			if (ui.hovered(starsX - 1, y + 4, 45, 14)) ui.tooltip("Click to score it (it goes back to your phone). Click the same again to clear.");
		} else if (ui.hovered(starsX - 1, y + 4, 45, 14)) {
			ui.tooltip("From FreedomPlayer on this PC: score it there.");
		}
	}

	private static void stars(Ui ui, Entry entry, int x, int y) {
		Integer score = entry.song().score();
		int on = entry.synced() ? 0xFFFFC84A : ThemeManager.withAlpha(0xFFFFC84A, 0.6F);
		int off = ThemeManager.withAlpha(ThemeManager.textMuted(), 0.35F);
		for (int i = 0; i < 5; i++) {
			int sx = x + i * 9;
			Draw.art(ui.g, STAR, sx, y, off);
			if (score == null) continue;
			if (score >= i * 2 + 2) Draw.art(ui.g, STAR, sx, y, on);
			else if (score == i * 2 + 1) Draw.art(ui.g, STAR_LEFT, sx, y, on);
		}
	}

	/** La carátula: su color con una nota. */
	private static void cover(Ui ui, int color, int x, int y) {
		int c = color | 0xFF000000;
		ui.g.fill(x, y, x + 14, y + 14, c);
		ui.g.fill(x, y + 11, x + 14, y + 14, ThemeManager.mix(c, 0xFF000000, 0.25F));
		Draw.art(ui.g, NOTE, x + 4, y + 4, ThemeManager.withAlpha(0xFFFFFFFF, 0.85F));
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Lo que suena

	private void renderFooter(Ui ui, int x, int y, int w) {
		LocalMusicPlayer player = player();
		Draw.panel(ui.g, x, y, w, FOOTER_HEIGHT, ThemeManager.shade(), ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.5F));
		int bx = x + 5;
		bx = icon(ui, PREVIOUS, bx, y + 5, player::previous);
		bx = icon(ui, player.isPlaying() ? PAUSE : PLAY, bx + 4, y + 5, () -> {
			ensurePlayerOn();
			player.togglePause();
		});
		bx = icon(ui, NEXT, bx + 4, y + 5, player::next);
		int volumeX = renderVolume(ui, x + w - 5 - SLIDER_WIDTH, y);
		String title = player.isActive() && player.title() != null ? player.title() : "Nothing playing: pick a song.";
		long position = player.positionMs(), duration = player.durationMs();
		String time = player.isActive() && duration > 0 ? format(position) + " / " + format(duration) : "";
		int timeW = ui.font.width(time);
		ui.g.drawString(ui.font, ui.font.plainSubstrByWidth(title, volumeX - bx - timeW - 20), bx + 8, y + 4,
				player.isActive() ? ThemeManager.text() : ThemeManager.textMuted(), false);
		if (!time.isEmpty()) ui.g.drawString(ui.font, time, volumeX - 8 - timeW, y + 4, ThemeManager.textMuted(), false);
	}

	/** Altavoz (silenciar) y deslizador del volumen; se arrastra o se mueve con la rueda. Devuelve su borde izquierdo. */
	private int renderVolume(Ui ui, int sliderX, int y) {
		com.freedomclient.setting.NumberSetting volume = hud().volume();
		boolean muted = volume.get() <= 0;
		int iconX = sliderX - 10;
		boolean iconHovered = ui.hovered(iconX - 2, y + 2, 10, 12);
		Draw.art(ui.g, muted ? MUTED : SPEAKER, iconX, y + 5, iconHovered ? ThemeManager.highlight() : ThemeManager.text());
		ui.click(iconX - 2, y + 2, 10, 12, (mx, my, b) -> {
			if (muted) {
				volume.set(unmuted > 0 ? unmuted : 60);
			} else {
				unmuted = volume.get();
				volume.set(0.0);
			}
			ui.playClick();
			return true;
		});
		boolean hovered = ui.hovered(sliderX - 2, y + 2, SLIDER_WIDTH + 4, 12);
		Draw.slider(ui.g, sliderX, y + 5, SLIDER_WIDTH, volume.getProgress(), hovered);
		ui.click(sliderX - 2, y + 2, SLIDER_WIDTH + 4, 12, (mx, my, b) -> {
			ui.startDrag((dx, dy) -> volume.setProgress((dx - sliderX) / (double) SLIDER_WIDTH), mx, my);
			return true;
		});
		ui.scroll(iconX - 2, y, SLIDER_WIDTH + 14, FOOTER_HEIGHT, amount -> volume.set(Math.max(0, Math.min(100, volume.get() + Math.signum(amount) * 5))));
		if (hovered || iconHovered) ui.tooltip("Volume " + volume.getInt() + "%" + (iconHovered ? (muted ? " (click to unmute)" : " (click to mute)") : ""));
		return iconX;
	}

	private static int icon(Ui ui, String[] art, int x, int y, Runnable action) {
		int width = art[0].length();
		boolean hovered = ui.hovered(x - 2, y - 3, width + 4, 11);
		Draw.art(ui.g, art, x, y, hovered ? ThemeManager.highlight() : ThemeManager.text());
		ui.click(x - 2, y - 3, width + 4, 11, (mx, my, b) -> {
			action.run();
			ui.playClick();
			return true;
		});
		return x + width;
	}

	// ---------------------------------------------------------------------------------------------------------------

	private static MusicPlayerHud hud() {
		return FreedomClient.getModuleManager().get(MusicPlayerHud.class);
	}

	private static LocalMusicPlayer player() {
		return hud().player();
	}

	/** Para tocar hace falta el Music Player encendido. */
	private static void ensurePlayerOn() {
		if (!hud().isEnabled()) hud().setEnabled(true);
	}

	private static void play(List<Entry> list, int index) {
		ensurePlayerOn();
		player().playSongs(list, index);
	}

	private List<Entry> filter(List<Entry> all) {
		String query = search.getText().toLowerCase(Locale.ROOT).trim();
		if (query.isEmpty()) return all;
		List<Entry> out = new ArrayList<>();
		for (Entry e : all) {
			if (e.song().title().toLowerCase(Locale.ROOT).contains(query) || e.song().artist().toLowerCase(Locale.ROOT).contains(query)) out.add(e);
		}
		return out;
	}

	private static List<Entry> resolve(ClientLibrary library, List<String> ids) {
		Map<String, Entry> byId = new HashMap<>();
		for (Entry e : library.songs()) byId.put(e.song().id(), e);
		List<Entry> out = new ArrayList<>();
		for (String id : ids) {
			Entry e = byId.get(id);
			if (e != null) out.add(e);
		}
		return out;
	}

	/** Botón de la barra pegado a la izquierda en {@code x}; devuelve su borde derecho. */
	private static int toggle(Ui ui, String label, int x, int y, boolean selected, Runnable action) {
		int width = ui.font.width(label) + 10;
		boolean hovered = ui.hovered(x, y, width, BAR_HEIGHT);
		int fill = selected ? ThemeManager.mix(ThemeManager.card(), ThemeManager.accent(), 0.3F) : hovered ? ThemeManager.cardHover() : ThemeManager.card();
		Draw.bevelPanel(ui.g, x, y, width, BAR_HEIGHT, fill, selected ? ThemeManager.accent() : ThemeManager.border());
		ui.g.drawString(ui.font, label, x + 5, y + 3, selected ? ThemeManager.accent() : ThemeManager.text(), false);
		ui.click(x, y, width, BAR_HEIGHT, (mx, my, b) -> {
			action.run();
			ui.playClick();
			return true;
		});
		return x + width;
	}

	/** Botón alineado a la derecha que acaba en {@code right}; devuelve su borde izquierdo. */
	private static int button(Ui ui, String label, int right, int y, boolean primary, Runnable action) {
		int width = ui.font.width(label) + 12;
		int bx = right - width;
		boolean hovered = ui.hovered(bx, y, width, BAR_HEIGHT);
		int fill = primary ? (hovered ? ThemeManager.highlight() : ThemeManager.accent()) : hovered ? ThemeManager.cardHover() : ThemeManager.card();
		Draw.bevelPanel(ui.g, bx, y, width, BAR_HEIGHT, fill, primary ? ThemeManager.accent() : ThemeManager.border());
		ui.g.drawString(ui.font, label, bx + 6, y + 3, primary ? ThemeManager.onAccent() : ThemeManager.text(), false);
		ui.click(bx, y, width, BAR_HEIGHT, (mx, my, b) -> {
			action.run();
			ui.playClick();
			return true;
		});
		return bx;
	}

	private static List<String> wrap(Ui ui, String text, int width) {
		List<String> lines = new ArrayList<>();
		String rest = text.trim();
		while (!rest.isEmpty()) {
			String line = ui.font.plainSubstrByWidth(rest, width);
			if (line.length() < rest.length()) {
				int space = line.lastIndexOf(' ');
				if (space > 0) line = line.substring(0, space);
			}
			if (line.isEmpty()) break;
			lines.add(line);
			rest = rest.substring(line.length()).trim();
		}
		return lines;
	}

	private static String format(long ms) {
		long seconds = ms / 1000;
		return seconds / 60 + ":" + String.format("%02d", seconds % 60);
	}

	/** Abre el QR de vincular (como el botón Link phone). */
	public void openLinking() {
		linking = true;
		FreedomPlayerHub.sync().startLinking();
	}

	/** Vuelve a la lista de canciones (para las pruebas). */
	public void showSongs() {
		linking = false;
		view = View.SONGS;
		opened = null;
	}

	/** Abre la vista de teclas y volumen (para las pruebas). */
	public void showKeys() {
		linking = false;
		view = View.KEYS;
		opened = null;
	}

	/** Abre la primera playlist (para las pruebas). */
	public void openFirstPlaylist() {
		linking = false;
		view = View.PLAYLISTS;
		List<Playlist> all = FreedomPlayerHub.library().playlists();
		opened = all.isEmpty() ? null : all.get(0).id();
	}
}
