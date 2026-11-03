package com.freedomclient.freedomplayer;

import com.freedomclient.FreedomClient;
import com.freedomclient.freedomplayer.LibraryData.Playlist;
import com.freedomclient.freedomplayer.LibraryData.Ranking;
import com.freedomclient.freedomplayer.LibraryData.Song;
import com.freedomclient.freedomplayer.LibrarySync.PlayEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * La biblioteca de FreedomPlayer dentro del cliente: la que llega del móvil por Syncthing (canciones con su nota,
 * playlists, rankings y escuchas) y, si en este PC está FreedomPlayer instalado, también sus canciones y playlists
 * (solo lectura). Las notas que se ponen aquí y las canciones que se escuchan en el juego vuelven al móvil.
 */
public final class ClientLibrary {
	/** Una canción tal y como la enseña la pantalla: {@code synced} si viene del móvil (se le puede poner nota). */
	public record Entry(Song song, boolean synced) {
	}

	private volatile LibraryData data = LibraryData.EMPTY;
	private volatile List<Entry> desktopSongs = List.of();
	private volatile List<Playlist> desktopPlaylists = List.of();
	private volatile long desktopReadAt;
	private SyncExchange exchange;
	/** Sube con cada cambio (la pantalla y el reproductor lo miran para refrescarse). */
	private volatile int version;

	/** El id del cliente en Syncthing y sus carpetas: desde ahí se lee lo sincronizado. */
	public synchronized void setDevice(String device, Path folder, Path music, Path state) {
		exchange = new SyncExchange(device, folder, music, state, System::currentTimeMillis);
		if (data.songs().isEmpty()) {
			data = exchange.read();
			version++;
		}
	}

	public int version() {
		return version;
	}

	public LibraryData data() {
		return data;
	}

	/** Todas las canciones: las del móvil y, detrás, las de FreedomPlayer del PC que no estén ya. */
	public List<Entry> songs() {
		refreshDesktop();
		List<Entry> out = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		for (Song song : data.songs()) {
			out.add(new Entry(song, true));
			ids.add(song.id());
		}
		for (Entry entry : desktopSongs) {
			if (!ids.contains(entry.song().id())) out.add(entry);
		}
		return out;
	}

	public List<Playlist> playlists() {
		refreshDesktop();
		List<Playlist> out = new ArrayList<>(data.playlists());
		Set<String> ids = new HashSet<>();
		out.forEach(p -> ids.add(p.id()));
		for (Playlist p : desktopPlaylists) if (!ids.contains(p.id())) out.add(p);
		return out;
	}

	public List<Ranking> rankings() {
		return data.rankings();
	}

	public Entry find(String id) {
		for (Entry entry : songs()) {
			if (entry.song().id().equals(id)) return entry;
		}
		return null;
	}

	/** Pone (o quita, con null) la nota de una canción del móvil; va al móvil en el siguiente intercambio. */
	public synchronized void setScore(String songId, Integer score) {
		List<Song> songs = new ArrayList<>();
		for (Song song : data.songs()) songs.add(song.id().equals(songId) ? song.withScore(score) : song);
		data = new LibraryData(songs, data.playlists(), data.rankings(), data.plays());
		version++;
		exchangeLater();
	}

	/** Una escucha en el juego (se suma a las del móvil). */
	public synchronized void recordPlay(String songId) {
		if (data.song(songId) == null) return;
		List<PlayEvent> plays = new ArrayList<>(data.plays());
		plays.add(new PlayEvent(songId, System.currentTimeMillis()));
		List<Song> songs = new ArrayList<>();
		for (Song s : data.songs()) {
			songs.add(s.id().equals(songId) ? new Song(s.id(), s.title(), s.artist(), s.album(), s.durationSec(), s.coverColor(), s.sourceUrl(),
					s.addedAt(), s.audioPath(), s.score(), s.plays() + 1) : s);
		}
		data = new LibraryData(songs, data.playlists(), data.rankings(), plays);
		version++;
		exchangeLater();
	}

	/** Escribe lo de aquí y junta lo que haya llegado (lo llama la sincronización cada 20 s). */
	public void exchange() throws Exception {
		SyncExchange current;
		LibraryData local;
		synchronized (this) {
			current = exchange;
			local = data;
		}
		if (current == null) return;
		LibraryData merged = current.exchange(local);
		synchronized (this) {
			// Si mientras tanto se ha cambiado algo aquí, se deja para el intercambio siguiente.
			if (merged != null && data == local) {
				data = merged;
				version++;
			}
		}
	}

	private void exchangeLater() {
		Thread thread = new Thread(() -> {
			try {
				exchange();
			} catch (Exception e) {
				FreedomClient.LOGGER.warn("Could not save the FreedomPlayer library", e);
			}
		}, "FreedomClient Library");
		thread.setDaemon(true);
		thread.start();
	}

	/** Lo de FreedomPlayer del PC (su base de datos), como mucho una vez cada 30 s. */
	private void refreshDesktop() {
		long now = System.currentTimeMillis();
		if (now - desktopReadAt < 30_000) return;
		desktopReadAt = now;
		Thread thread = new Thread(() -> {
			List<Entry> songs = new ArrayList<>();
			for (com.freedomclient.music.FreedomPlayerLibrary.Song s : com.freedomclient.music.FreedomPlayerLibrary.songs()) {
				songs.add(new Entry(new Song(s.id(), s.title(), s.artist(), null, (int) (s.durationMs() / 1000), s.coverColor(), null, 0,
						s.file().toString(), s.score(), 0), false));
			}
			desktopSongs = List.copyOf(songs);
			desktopPlaylists = List.copyOf(com.freedomclient.music.FreedomPlayerLibrary.playlists());
			version++;
		}, "FreedomClient Desktop Library");
		thread.setDaemon(true);
		thread.start();
	}
}
