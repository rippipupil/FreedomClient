package com.freedomclient.freedomplayer;

import com.freedomclient.freedomplayer.LibraryData.Playlist;
import com.freedomclient.freedomplayer.LibraryData.Ranking;
import com.freedomclient.freedomplayer.LibraryData.Song;
import com.freedomclient.freedomplayer.LibrarySync.Library;
import com.freedomclient.freedomplayer.LibrarySync.SyncList;
import com.freedomclient.freedomplayer.LibrarySync.SyncScore;
import com.freedomclient.freedomplayer.LibrarySync.SyncSong;
import com.freedomclient.freedomplayer.LibrarySync.Tombstone;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

/**
 * Intercambiar la biblioteca por la carpeta que comparte Syncthing, como SyncExchange.kt de FreedomPlayer: se escribe la
 * de aquí («biblioteca-este dispositivo.json»), se leen las de los demás y se juntan con las reglas de
 * {@link LibrarySync}. El audio de las canciones nuevas llega por la carpeta de música; una canción cuyo audio aún no ha
 * llegado se deja para la vez siguiente.
 * <p>
 * La hora del último cambio de cada cosa se saca comparando con la exportación anterior (su huella): lo que no ha
 * cambiado conserva su hora, lo nuevo o cambiado lleva la de ahora y lo que ya no está queda como borrado a esa hora.
 */
public final class SyncExchange {
	private record Stamp(int hash, long at) {
	}

	private final String device;
	private final Path folder;
	private final Path music;
	private final Path stateFile;
	private final LongSupplier now;

	public SyncExchange(String device, Path folder, Path music, Path stateFile, LongSupplier now) {
		this.device = device;
		this.folder = folder;
		this.music = music;
		this.stateFile = stateFile;
		this.now = now;
	}

	/** Escribe la de aquí, lee las de los demás y devuelve lo juntado, o null si aún no hay nada de otro dispositivo. */
	public LibraryData exchange(LibraryData local) throws IOException {
		Files.createDirectories(folder);
		long time = now.getAsLong();
		Map<String, Stamp> previous = new HashMap<>();
		Map<String, Long> previousDeleted = new HashMap<>();
		readState(previous, previousDeleted);
		Map<String, Integer> current = fingerprints(local);
		Map<String, Stamp> stamps = new LinkedHashMap<>();
		current.forEach((key, hash) -> {
			Stamp before = previous.get(key);
			stamps.put(key, before != null && before.hash() == hash ? before : new Stamp(hash, time));
		});
		Map<String, Long> deleted = new LinkedHashMap<>(previousDeleted);
		for (String key : previous.keySet()) if (!current.containsKey(key)) deleted.put(key, time);
		deleted.keySet().removeIf(current::containsKey);
		Library mine = toSync(local, stamps, deleted, time);
		write(folder.resolve(LibrarySync.fileName(device)), LibrarySync.encode(mine));
		List<Library> others = others();
		if (others.isEmpty()) {
			writeState(stamps, deleted);
			return null;
		}
		Library merged = mine;
		for (Library other : others) merged = LibrarySync.merge(merged, other);
		LibraryData result = fromSync(merged, local);
		writeState(stampsOf(result, merged, time), tombstonesOf(merged));
		return result;
	}

	/** Lo que hay en la carpeta sin escribir nada (para enseñar la biblioteca sin Syncthing en marcha). */
	public LibraryData read() {
		List<Library> all = new ArrayList<>();
		Path own = folder.resolve(LibrarySync.fileName(device));
		try {
			if (Files.isRegularFile(own)) all.add(LibrarySync.decode(Files.readString(own, StandardCharsets.UTF_8)));
		} catch (Exception ignored) {
			// Si está a medias, se lee la vez siguiente.
		}
		all.addAll(others());
		if (all.isEmpty()) return LibraryData.EMPTY;
		Library merged = all.get(0);
		for (int i = 1; i < all.size(); i++) merged = LibrarySync.merge(merged, all.get(i));
		return fromSync(merged, LibraryData.EMPTY);
	}

	private List<Library> others() {
		List<Library> out = new ArrayList<>();
		String mine = LibrarySync.fileName(device);
		try (Stream<Path> files = Files.list(folder)) {
			for (Path file : files.sorted().toList()) {
				String name = file.getFileName().toString();
				if (!Files.isRegularFile(file) || !name.startsWith("biblioteca-") || !name.endsWith(".json") || name.equals(mine)) continue;
				try {
					out.add(LibrarySync.decode(Files.readString(file, StandardCharsets.UTF_8)));
				} catch (Exception ignored) {
					// Un JSON roto o de una versión más nueva no se usa.
				}
			}
		} catch (IOException ignored) {
			// Sin carpeta todavía.
		}
		return out;
	}

	/** Las huellas de la biblioteca: canciones, notas, playlists y rankings (las escuchas no cambian). */
	private Map<String, Integer> fingerprints(LibraryData lib) {
		Map<String, Integer> out = new LinkedHashMap<>();
		for (Song s : lib.songs()) {
			out.put("cancion:" + s.id(), Arrays.asList(s.title(), s.artist(), s.album(), s.durationSec(), s.coverColor(), s.sourceUrl(), s.addedAt(),
					audioFile(s)).hashCode());
			out.put("nota:" + s.id(), Integer.hashCode(s.score() == null ? 0 : s.score()));
		}
		for (Playlist p : lib.playlists()) out.put("playlist:" + p.id(), Arrays.asList(p.name(), p.songIds(), p.coverColor()).hashCode());
		for (Ranking r : lib.rankings()) out.put("ranking:" + r.id(), Arrays.asList(r.name(), r.songIds()).hashCode());
		return out;
	}

	private Map<String, Stamp> stampsOf(LibraryData result, Library merged, long time) {
		Map<String, Long> songAt = new HashMap<>(), scoreAt = new HashMap<>(), playlistAt = new HashMap<>(), rankingAt = new HashMap<>();
		merged.songs().forEach(s -> songAt.put(s.id(), s.changedAt()));
		merged.scores().forEach(s -> scoreAt.put(s.songId(), s.changedAt()));
		merged.playlists().forEach(p -> playlistAt.put(p.id(), p.changedAt()));
		merged.rankings().forEach(r -> rankingAt.put(r.id(), r.changedAt()));
		Map<String, Stamp> out = new LinkedHashMap<>();
		fingerprints(result).forEach((key, hash) -> {
			String id = key.substring(key.indexOf(':') + 1);
			Long at = key.startsWith("cancion:") ? songAt.get(id) : key.startsWith("nota:") ? scoreAt.get(id)
					: key.startsWith("playlist:") ? playlistAt.get(id) : rankingAt.get(id);
			out.put(key, new Stamp(hash, at != null ? at : time));
		});
		return out;
	}

	private static Map<String, Long> tombstonesOf(Library merged) {
		Map<String, Long> out = new LinkedHashMap<>();
		merged.deletedSongs().forEach(t -> out.put("cancion:" + t.id(), t.deletedAt()));
		merged.deletedPlaylists().forEach(t -> out.put("playlist:" + t.id(), t.deletedAt()));
		merged.deletedRankings().forEach(t -> out.put("ranking:" + t.id(), t.deletedAt()));
		return out;
	}

	/** El nombre del audio si está en la carpeta de música compartida. */
	private String audioFile(Song song) {
		if (song.audioPath() == null) return null;
		Path file = Path.of(song.audioPath()).toAbsolutePath();
		String name = file.getFileName().toString();
		return file.getParent() != null && file.getParent().equals(music.toAbsolutePath()) && LibrarySync.safeFileName(name) ? name : null;
	}

	private Library toSync(LibraryData lib, Map<String, Stamp> stamps, Map<String, Long> deleted, long time) {
		List<SyncSong> songs = new ArrayList<>();
		List<SyncScore> scores = new ArrayList<>();
		for (Song s : lib.songs()) {
			songs.add(new SyncSong(s.id(), s.title(), s.artist(), s.album(), s.durationSec(), s.coverColor(), s.sourceUrl(), s.addedAt(), audioFile(s),
					at(stamps, "cancion:" + s.id(), time)));
			scores.add(new SyncScore(s.id(), s.score(), at(stamps, "nota:" + s.id(), time)));
		}
		List<SyncList> playlists = lib.playlists().stream()
				.map(p -> new SyncList(p.id(), p.name(), p.songIds(), p.coverColor(), at(stamps, "playlist:" + p.id(), time))).toList();
		List<SyncList> rankings = lib.rankings().stream()
				.map(r -> new SyncList(r.id(), r.name(), r.songIds(), null, at(stamps, "ranking:" + r.id(), time))).toList();
		return new Library(device, time, songs, scores, playlists, rankings, lib.plays(), gone(deleted, "cancion:"), gone(deleted, "playlist:"),
				gone(deleted, "ranking:"));
	}

	private static long at(Map<String, Stamp> stamps, String key, long time) {
		Stamp stamp = stamps.get(key);
		return stamp != null ? stamp.at() : time;
	}

	private static List<Tombstone> gone(Map<String, Long> deleted, String prefix) {
		List<Tombstone> out = new ArrayList<>();
		deleted.forEach((key, at) -> {
			if (key.startsWith(prefix)) out.add(new Tombstone(key.substring(prefix.length()), at));
		});
		out.sort(Comparator.comparing(Tombstone::id));
		return out;
	}

	private LibraryData fromSync(Library merged, LibraryData local) {
		Map<String, Song> mine = new HashMap<>();
		local.songs().forEach(s -> mine.put(s.id(), s));
		Map<String, Integer> scores = new HashMap<>();
		merged.scores().forEach(s -> scores.put(s.songId(), s.score()));
		List<Song> songs = new ArrayList<>();
		for (SyncSong s : merged.songs()) {
			Song here = mine.get(s.id());
			Path arrived = s.audioFile() != null ? music.resolve(s.audioFile()) : null;
			String arrivedPath = arrived != null && Files.isRegularFile(arrived) ? arrived.toAbsolutePath().toString() : null;
			if (here == null && arrivedPath == null) continue;
			int plays = here != null ? here.plays() : (int) merged.plays().stream().filter(p -> p.songId().equals(s.id())).count();
			String audio = here != null ? here.audioPath() : arrivedPath;
			long color = here != null ? here.coverColor() : s.coverColor();
			int duration = here != null ? here.durationSec() : s.durationSec();
			String source = here != null ? here.sourceUrl() : s.sourceUrl();
			long added = here != null ? here.addedAt() : s.addedAt();
			songs.add(new Song(s.id(), s.title(), s.artist(), s.album(), duration, color, source, added, audio, scores.get(s.id()), plays));
		}
		List<String> ids = songs.stream().map(Song::id).toList();
		Map<String, Long> colors = new HashMap<>();
		local.playlists().forEach(p -> colors.put(p.id(), p.coverColor()));
		List<Playlist> playlists = merged.playlists().stream().map(p -> new Playlist(p.id(), p.name(), p.songIds().stream().filter(ids::contains).toList(),
				p.coverColor() != null ? p.coverColor() : colors.getOrDefault(p.id(), 0xFF7A1F55L))).toList();
		List<Ranking> rankings = merged.rankings().stream().map(r -> new Ranking(r.id(), r.name(), r.songIds().stream().filter(ids::contains).toList())).toList();
		return new LibraryData(songs, playlists, rankings, merged.plays().stream().filter(p -> ids.contains(p.songId())).toList());
	}

	/** Se escribe entero y luego se cambia de nombre: Syncthing nunca ve un JSON a medias. */
	private static void write(Path file, String text) throws IOException {
		Path tmp = file.resolveSibling("." + file.getFileName() + ".tmp");
		Files.writeString(tmp, text, StandardCharsets.UTF_8);
		try {
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (java.nio.file.AtomicMoveNotSupportedException e) {
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void readState(Map<String, Stamp> stamps, Map<String, Long> deleted) {
		try {
			JsonObject root = JsonParser.parseString(Files.readString(stateFile, StandardCharsets.UTF_8)).getAsJsonObject();
			if (root.has("huellas")) {
				for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("huellas").entrySet()) {
					JsonArray pair = e.getValue().getAsJsonArray();
					stamps.put(e.getKey(), new Stamp(pair.get(0).getAsInt(), pair.get(1).getAsLong()));
				}
			}
			if (root.has("borrados")) {
				for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("borrados").entrySet()) deleted.put(e.getKey(), e.getValue().getAsLong());
			}
		} catch (Exception ignored) {
			// Sin estado anterior (o roto): todo cuenta como nuevo.
		}
	}

	private void writeState(Map<String, Stamp> stamps, Map<String, Long> deleted) throws IOException {
		JsonObject root = new JsonObject();
		JsonObject marks = new JsonObject();
		stamps.forEach((k, v) -> {
			JsonArray pair = new JsonArray();
			pair.add(v.hash());
			pair.add(v.at());
			marks.add(k, pair);
		});
		root.add("huellas", marks);
		JsonObject gone = new JsonObject();
		deleted.forEach(gone::addProperty);
		root.add("borrados", gone);
		if (stateFile.getParent() != null) Files.createDirectories(stateFile.getParent());
		write(stateFile, root.toString());
	}
}
