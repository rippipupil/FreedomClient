package com.freedomclient.freedomplayer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * La biblioteca que se comparte con FreedomPlayer por Syncthing, tal cual su LibrarySync.kt: cada dispositivo escribe
 * su copia en «biblioteca-dispositivo.json» dentro de la carpeta compartida y lee las de los demás.
 * <ul>
 * <li>Cada canción, nota, playlist y ranking lleva la hora de su último cambio: gana el más reciente; si empatan, se
 * queda lo de este dispositivo.</li>
 * <li>Lo borrado se recuerda con su hora: no vuelve si se borró después del último cambio del otro.</li>
 * <li>Las escuchas se suman, sin repetir la misma (misma canción en el mismo momento).</li>
 * <li>El audio no va en el JSON: va en la carpeta de música compartida y la canción dice cómo se llama su archivo.</li>
 * </ul>
 */
public final class LibrarySync {
	public static final int FORMAT = 1;

	public record SyncSong(String id, String title, String artist, String album, int durationSec, long coverColor, String sourceUrl,
						   long addedAt, String audioFile, long changedAt) {
	}

	public record SyncScore(String songId, Integer score, long changedAt) {
	}

	public record SyncList(String id, String name, List<String> songIds, Long coverColor, long changedAt) {
	}

	public record Tombstone(String id, long deletedAt) {
	}

	public record PlayEvent(String songId, long atMillis) {
	}

	public record Library(String device, long exportedAt, List<SyncSong> songs, List<SyncScore> scores, List<SyncList> playlists,
						  List<SyncList> rankings, List<PlayEvent> plays, List<Tombstone> deletedSongs, List<Tombstone> deletedPlaylists,
						  List<Tombstone> deletedRankings) {
	}

	private LibrarySync() {
	}

	/** El nombre del JSON de cada dispositivo en la carpeta compartida. */
	public static String fileName(String device) {
		StringBuilder clean = new StringBuilder();
		for (char c : device.toCharArray()) {
			if ((Character.isLetterOrDigit(c) || c == '-') && clean.length() < 80) clean.append(c);
		}
		return "biblioteca-" + clean + ".json";
	}

	/** Solo un nombre de archivo, sin carpetas: lo que llega de otro dispositivo no puede salirse de la carpeta. */
	public static boolean safeFileName(String name) {
		if (name == null || name.isBlank() || name.length() > 200 || name.equals(".") || name.equals("..")) return false;
		for (char c : name.toCharArray()) {
			if (c == '/' || c == '\\' || c == ':' || c < 32) return false;
		}
		return true;
	}

	/** Junta la copia de este dispositivo con la de otro, con las reglas de arriba. */
	public static Library merge(Library local, Library remote) {
		Map<String, Tombstone> deletedSongs = newest(local.deletedSongs(), remote.deletedSongs());
		Map<String, Tombstone> deletedPlaylists = newest(local.deletedPlaylists(), remote.deletedPlaylists());
		Map<String, Tombstone> deletedRankings = newest(local.deletedRankings(), remote.deletedRankings());
		List<SyncSong> songs = mergeById(local.songs(), remote.songs(), SyncSong::id, SyncSong::changedAt).stream()
				.filter(s -> !deletedSongs.containsKey(s.id()) || deletedSongs.get(s.id()).deletedAt() < s.changedAt()).toList();
		Set<String> alive = new HashSet<>();
		for (SyncSong s : songs) alive.add(s.id());
		List<SyncScore> scores = mergeById(local.scores(), remote.scores(), SyncScore::songId, SyncScore::changedAt).stream()
				.filter(s -> alive.contains(s.songId())).toList();
		List<SyncList> playlists = lists(mergeById(local.playlists(), remote.playlists(), SyncList::id, SyncList::changedAt), deletedPlaylists, alive);
		List<SyncList> rankings = lists(mergeById(local.rankings(), remote.rankings(), SyncList::id, SyncList::changedAt), deletedRankings, alive);
		List<PlayEvent> plays = new ArrayList<>(new LinkedHashSet<>(concat(local.plays(), remote.plays())));
		plays.removeIf(p -> !alive.contains(p.songId()));
		plays.sort(Comparator.comparingLong(PlayEvent::atMillis));
		return new Library(local.device(), Math.max(local.exportedAt(), remote.exportedAt()), songs, scores, playlists, rankings, plays,
				sorted(deletedSongs.values().stream().filter(t -> !alive.contains(t.id())).toList()),
				sorted(deletedPlaylists.values().stream().filter(t -> playlists.stream().noneMatch(p -> p.id().equals(t.id()))).toList()),
				sorted(deletedRankings.values().stream().filter(t -> rankings.stream().noneMatch(r -> r.id().equals(t.id()))).toList()));
	}

	private static List<SyncList> lists(List<SyncList> merged, Map<String, Tombstone> deleted, Set<String> alive) {
		List<SyncList> out = new ArrayList<>();
		for (SyncList l : merged) {
			Tombstone t = deleted.get(l.id());
			if (t != null && t.deletedAt() >= l.changedAt()) continue;
			out.add(new SyncList(l.id(), l.name(), l.songIds().stream().filter(alive::contains).toList(), l.coverColor(), l.changedAt()));
		}
		return out;
	}

	/** Por id: el más reciente; si empatan, el de este dispositivo. En el orden de aquí y luego lo nuevo. */
	private static <T> List<T> mergeById(List<T> local, List<T> remote, Function<T, String> id, ToLongFunction<T> at) {
		Map<String, T> theirs = new HashMap<>();
		for (T t : remote) theirs.putIfAbsent(id.apply(t), t);
		Set<String> known = new HashSet<>();
		List<T> merged = new ArrayList<>();
		for (T mine : local) {
			T other = theirs.get(id.apply(mine));
			merged.add(other != null && at.applyAsLong(other) > at.applyAsLong(mine) ? other : mine);
			known.add(id.apply(mine));
		}
		for (T t : remote) {
			if (!known.contains(id.apply(t))) merged.add(t);
		}
		return merged;
	}

	private static Map<String, Tombstone> newest(List<Tombstone> a, List<Tombstone> b) {
		Map<String, Tombstone> out = new HashMap<>();
		for (Tombstone t : concat(a, b)) out.merge(t.id(), t, (x, y) -> y.deletedAt() > x.deletedAt() ? y : x);
		return out;
	}

	private static List<Tombstone> sorted(List<Tombstone> list) {
		List<Tombstone> out = new ArrayList<>(list);
		out.sort(Comparator.comparing(Tombstone::id));
		return out;
	}

	private static <T> List<T> concat(List<T> a, List<T> b) {
		List<T> out = new ArrayList<>(a);
		out.addAll(b);
		return out;
	}

	// ---------------------------------------------------------------------------------------------------------------
	// JSON (las mismas claves que FreedomPlayer)

	public static String encode(Library lib) {
		JsonObject root = new JsonObject();
		root.addProperty("formato", FORMAT);
		root.addProperty("dispositivo", lib.device());
		root.addProperty("exportado", lib.exportedAt());
		JsonArray songs = new JsonArray();
		for (SyncSong s : lib.songs()) {
			JsonObject o = new JsonObject();
			o.addProperty("id", s.id());
			o.addProperty("titulo", s.title());
			o.addProperty("artista", s.artist());
			if (s.album() != null) o.addProperty("album", s.album());
			o.addProperty("duracion", s.durationSec());
			o.addProperty("color", s.coverColor());
			if (s.sourceUrl() != null) o.addProperty("origen", s.sourceUrl());
			o.addProperty("anadida", s.addedAt());
			if (s.audioFile() != null) o.addProperty("audio", s.audioFile());
			o.addProperty("cambio", s.changedAt());
			songs.add(o);
		}
		root.add("canciones", songs);
		JsonArray scores = new JsonArray();
		for (SyncScore n : lib.scores()) {
			JsonObject o = new JsonObject();
			o.addProperty("cancion", n.songId());
			o.add("nota", n.score() == null ? JsonNull.INSTANCE : new JsonPrimitive(n.score()));
			o.addProperty("cambio", n.changedAt());
			scores.add(o);
		}
		root.add("notas", scores);
		root.add("playlists", encodeLists(lib.playlists()));
		root.add("rankings", encodeLists(lib.rankings()));
		JsonArray plays = new JsonArray();
		for (PlayEvent p : lib.plays()) {
			JsonObject o = new JsonObject();
			o.addProperty("cancion", p.songId());
			o.addProperty("momento", p.atMillis());
			plays.add(o);
		}
		root.add("escuchas", plays);
		root.add("borradas", encodeTombstones(lib.deletedSongs()));
		root.add("playlistsBorradas", encodeTombstones(lib.deletedPlaylists()));
		root.add("rankingsBorrados", encodeTombstones(lib.deletedRankings()));
		return root.toString();
	}

	private static JsonArray encodeLists(List<SyncList> all) {
		JsonArray out = new JsonArray();
		for (SyncList l : all) {
			JsonObject o = new JsonObject();
			o.addProperty("id", l.id());
			o.addProperty("nombre", l.name());
			JsonArray ids = new JsonArray();
			l.songIds().forEach(ids::add);
			o.add("canciones", ids);
			if (l.coverColor() != null) o.addProperty("color", l.coverColor());
			o.addProperty("cambio", l.changedAt());
			out.add(o);
		}
		return out;
	}

	private static JsonArray encodeTombstones(List<Tombstone> all) {
		JsonArray out = new JsonArray();
		for (Tombstone t : all) {
			JsonObject o = new JsonObject();
			o.addProperty("id", t.id());
			o.addProperty("borrado", t.deletedAt());
			out.add(o);
		}
		return out;
	}

	/** Lee el JSON de otro dispositivo. Falla si no es de FreedomPlayer o es de un formato más nuevo. */
	public static Library decode(String text) {
		JsonElement parsed = JsonParser.parseString(text);
		if (!parsed.isJsonObject()) throw new IllegalArgumentException("Not a FreedomPlayer library");
		JsonObject root = parsed.getAsJsonObject();
		Long format = number(root, "formato");
		if (format == null) throw new IllegalArgumentException("Not a FreedomPlayer library");
		if (format > FORMAT) throw new IllegalArgumentException("Library from a newer FreedomPlayer (format " + format + ")");
		String device = text(root, "dispositivo");
		if (device == null) throw new IllegalArgumentException("Missing device");
		List<SyncSong> songs = new ArrayList<>();
		for (JsonObject o : objects(root, "canciones")) {
			String id = text(o, "id"), title = text(o, "titulo");
			if (id == null || title == null) continue;
			String audio = text(o, "audio");
			songs.add(new SyncSong(id, title, orEmpty(text(o, "artista")), text(o, "album"), (int) (long) or(number(o, "duracion"), 0L),
					or(number(o, "color"), 0xFF3B2F8FL), text(o, "origen"), or(number(o, "anadida"), 0L), safeFileName(audio) ? audio : null,
					or(number(o, "cambio"), 0L)));
		}
		List<SyncScore> scores = new ArrayList<>();
		for (JsonObject o : objects(root, "notas")) {
			String song = text(o, "cancion");
			if (song == null) continue;
			Long score = number(o, "nota");
			scores.add(new SyncScore(song, score != null && score >= 1 && score <= 10 ? score.intValue() : null, or(number(o, "cambio"), 0L)));
		}
		List<PlayEvent> plays = new ArrayList<>();
		for (JsonObject o : objects(root, "escuchas")) {
			String song = text(o, "cancion");
			Long at = number(o, "momento");
			if (song != null && at != null) plays.add(new PlayEvent(song, at));
		}
		return new Library(device, or(number(root, "exportado"), 0L), songs, scores, decodeLists(root, "playlists"), decodeLists(root, "rankings"),
				plays, decodeTombstones(root, "borradas"), decodeTombstones(root, "playlistsBorradas"), decodeTombstones(root, "rankingsBorrados"));
	}

	private static List<SyncList> decodeLists(JsonObject root, String key) {
		List<SyncList> out = new ArrayList<>();
		for (JsonObject o : objects(root, key)) {
			String id = text(o, "id"), name = text(o, "nombre");
			if (id == null || name == null) continue;
			List<String> ids = new ArrayList<>();
			JsonElement list = o.get("canciones");
			if (list != null && list.isJsonArray()) {
				for (JsonElement e : list.getAsJsonArray()) {
					if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) ids.add(e.getAsString());
				}
			}
			out.add(new SyncList(id, name, ids, number(o, "color"), or(number(o, "cambio"), 0L)));
		}
		return out;
	}

	private static List<Tombstone> decodeTombstones(JsonObject root, String key) {
		List<Tombstone> out = new ArrayList<>();
		for (JsonObject o : objects(root, key)) {
			String id = text(o, "id");
			Long at = number(o, "borrado");
			if (id != null && at != null) out.add(new Tombstone(id, at));
		}
		return out;
	}

	private static List<JsonObject> objects(JsonObject root, String key) {
		List<JsonObject> out = new ArrayList<>();
		JsonElement list = root.get(key);
		if (list != null && list.isJsonArray()) {
			for (JsonElement e : list.getAsJsonArray()) if (e.isJsonObject()) out.add(e.getAsJsonObject());
		}
		return out;
	}

	private static String text(JsonObject o, String key) {
		JsonElement e = o.get(key);
		return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
	}

	private static Long number(JsonObject o, String key) {
		JsonElement e = o.get(key);
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return null;
		try {
			return e.getAsJsonPrimitive().getAsBigDecimal().longValueExact();
		} catch (ArithmeticException ex) {
			return null;
		}
	}

	private static String orEmpty(String s) {
		return s == null ? "" : s;
	}

	private static <T> T or(T value, T fallback) {
		return value != null ? value : fallback;
	}
}
