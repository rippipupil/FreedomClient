package com.freedomclient.freedomplayer;

import com.freedomclient.FreedomClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Letras sincronizadas de LRCLIB (https://lrclib.net, sin clave) para las canciones que llegan del móvil, como
 * Lrclib.kt de FreedomPlayer (la letra no viaja con la sincronización): primero la canción exacta ({@code /api/get},
 * con título, artista, álbum y duración) y, si no tiene letra con tiempos, se busca ({@code /api/search}) y se elige la
 * que mejor encaja por título, artista y duración. Se guarda en la carpeta del cliente para no volver a pedirla.
 */
public final class Lrclib {
	private static final String BASE = "https://lrclib.net";
	/** LRCLIB pide que cada app se identifique. */
	private static final String USER_AGENT = "FreedomClient (https://github.com/rippipupil/FreedomClient)";
	/** Diferencia de duración que se acepta, en segundos. */
	private static final double TOLERANCE_SEC = 3.0;
	/** Si no había letra, se vuelve a mirar pasada una semana. */
	private static final long RETRY_MS = 7L * 24 * 3600 * 1000;

	record Candidate(String track, String artist, double durationSec, String synced, boolean instrumental) {
	}

	private final Path cache;
	private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();

	public Lrclib(Path cache) {
		this.cache = cache;
	}

	/** La letra LRC de una canción (de la caché o de LRCLIB), o null si no tiene. Tarda: no llamar desde el juego. */
	public String synced(String id, String title, String artist, String album, int durationSec) {
		if (title == null || title.isBlank()) return null;
		Path file = cache.resolve(keyOf(id) + ".lrc");
		Path none = cache.resolve(keyOf(id) + ".none");
		try {
			if (Files.isRegularFile(file)) return Files.readString(file, StandardCharsets.UTF_8);
			if (Files.isRegularFile(none) && System.currentTimeMillis() - Files.getLastModifiedTime(none).toMillis() < RETRY_MS) return null;
			String lyrics = find(title, artist == null ? "" : artist, album, durationSec);
			Files.createDirectories(cache);
			if (lyrics != null) {
				Files.writeString(file, lyrics, StandardCharsets.UTF_8);
				Files.deleteIfExists(none);
			} else {
				Files.writeString(none, "", StandardCharsets.UTF_8);
			}
			return lyrics;
		} catch (Exception e) {
			FreedomClient.LOGGER.debug("No lyrics from LRCLIB for {}", title, e);
			return null;
		}
	}

	private String find(String title, String artist, String album, int durationSec) throws IOException {
		Candidate exact = get(title, artist, album, durationSec);
		if (exact != null && exact.synced() != null) return exact.synced();
		Candidate best = best(search(title, artist), title, artist, durationSec);
		return best != null ? best.synced() : null;
	}

	private Candidate get(String title, String artist, String album, int durationSec) throws IOException {
		StringBuilder query = new StringBuilder("/api/get?track_name=").append(encode(title)).append("&artist_name=").append(encode(artist));
		if (album != null && !album.isBlank()) query.append("&album_name=").append(encode(album));
		if (durationSec > 0) query.append("&duration=").append(durationSec);
		String body = call(query.toString());
		if (body == null) return null;
		JsonElement parsed = JsonParser.parseString(body);
		return parsed.isJsonObject() ? candidate(parsed.getAsJsonObject()) : null;
	}

	private List<Candidate> search(String title, String artist) throws IOException {
		String body = call("/api/search?track_name=" + encode(title) + (artist.isBlank() ? "" : "&artist_name=" + encode(artist)));
		List<Candidate> out = new ArrayList<>();
		if (body == null) return out;
		JsonElement parsed = JsonParser.parseString(body);
		if (!parsed.isJsonArray()) return out;
		for (JsonElement e : parsed.getAsJsonArray()) {
			if (!e.isJsonObject()) continue;
			Candidate c = candidate(e.getAsJsonObject());
			if (c != null) out.add(c);
		}
		return out;
	}

	/** La mejor de una búsqueda: con tiempos, mismo artista (o que lo contenga) y duración parecida si se sabe. */
	static Candidate best(List<Candidate> candidates, String title, String artist, int durationSec) {
		String a = key(artist), t = key(title);
		return candidates.stream()
				.filter(c -> c.synced() != null)
				.filter(c -> {
					String ca = key(c.artist());
					return a.isEmpty() || ca.isEmpty() || ca.contains(a) || a.contains(ca);
				})
				.filter(c -> durationSec <= 0 || c.durationSec() <= 0 || Math.abs(c.durationSec() - durationSec) <= TOLERANCE_SEC)
				.min(Comparator.<Candidate, Boolean>comparing(c -> !key(c.track()).equals(t))
						.thenComparingDouble(c -> durationSec <= 0 ? 0 : Math.abs(c.durationSec() - durationSec)))
				.orElse(null);
	}

	private String call(String path) throws IOException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + path)).header("User-Agent", USER_AGENT).timeout(Duration.ofSeconds(15)).GET().build();
		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() == 404) return null;
			if (response.statusCode() / 100 != 2) throw new IOException("LRCLIB HTTP " + response.statusCode());
			return response.body();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted", e);
		}
	}

	private static Candidate candidate(JsonObject o) {
		String track = text(o, "trackName");
		if (track == null) return null;
		String synced = text(o, "syncedLyrics");
		double duration = o.has("duration") && o.get("duration").isJsonPrimitive() ? o.get("duration").getAsDouble() : 0;
		boolean instrumental = o.has("instrumental") && o.get("instrumental").isJsonPrimitive() && o.get("instrumental").getAsBoolean();
		return new Candidate(track, text(o, "artistName") == null ? "" : text(o, "artistName"), duration,
				synced == null || synced.isBlank() ? null : synced, instrumental);
	}

	private static String text(JsonObject o, String key) {
		JsonElement e = o.get(key);
		return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
	}

	/** Para comparar: sin tildes, en minúsculas y solo letras y números. */
	static String key(String s) {
		String plain = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
		StringBuilder out = new StringBuilder();
		for (char c : plain.toCharArray()) if (Character.isLetterOrDigit(c)) out.append(c);
		return out.toString();
	}

	private static String encode(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	/** Nombre de archivo seguro para la caché a partir del id de la canción. */
	private static String keyOf(String id) {
		try {
			byte[] hash = java.security.MessageDigest.getInstance("SHA-1").digest(id.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (java.security.NoSuchAlgorithmException e) {
			return Integer.toHexString(id.hashCode());
		}
	}
}
