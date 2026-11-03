package com.freedomclient.freedomplayer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Syncthing por su API REST local (http://127.0.0.1:puerto), con la clave que se genera al arrancarlo. Lo mismo que usa
 * FreedomPlayer (Syncthing.kt): el id de este dispositivo, añadir el del móvil, compartir las dos carpetas con id fijo
 * (música y biblioteca) y saber cuánto falta.
 */
public final class SyncthingApi {
	public static final String MUSIC_FOLDER = "freedomplayer-musica";
	public static final String LIBRARY_FOLDER = "freedomplayer-biblioteca";

	/** Cuánto lleva sincronizado: «12 de 20». */
	public record Progress(long done, long total) {
		public boolean finished() {
			return done >= total;
		}
	}

	private final String base;
	private final String apiKey;
	private final HttpClient client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).connectTimeout(Duration.ofSeconds(5)).build();

	public SyncthingApi(String base, String apiKey) {
		this.base = base;
		this.apiKey = apiKey;
	}

	/** Responde (Syncthing ya ha arrancado). */
	public boolean ping() {
		try {
			JsonElement pong = get("/rest/system/ping").get("ping");
			return pong != null && "pong".equals(pong.getAsString());
		} catch (IOException e) {
			return false;
		}
	}

	/** El id de este dispositivo. */
	public String myId() throws IOException {
		JsonElement id = get("/rest/system/status").get("myID");
		if (id == null) throw new IOException("Syncthing: no device id");
		return id.getAsString();
	}

	/** Añade (o actualiza) el otro dispositivo. */
	public void addDevice(String id, String name) throws IOException {
		JsonObject device = new JsonObject();
		device.addProperty("deviceID", id);
		device.addProperty("name", name);
		JsonArray addresses = new JsonArray();
		addresses.add("dynamic");
		device.add("addresses", addresses);
		device.addProperty("autoAcceptFolders", false);
		put("/rest/config/devices/" + id, device);
	}

	/** Con qué dispositivos se comparte ya {@code folderId} (vacío si la carpeta aún no existe). */
	public List<String> folderDevices(String folderId) {
		List<String> devices = new ArrayList<>();
		try {
			JsonElement list = get("/rest/config/folders/" + folderId).get("devices");
			if (list != null && list.isJsonArray()) {
				for (JsonElement entry : list.getAsJsonArray()) {
					if (entry.isJsonObject() && entry.getAsJsonObject().has("deviceID")) devices.add(entry.getAsJsonObject().get("deviceID").getAsString());
				}
			}
		} catch (IOException ignored) {
			// La carpeta todavía no existe.
		}
		return devices;
	}

	/** Comparte una carpeta (con id fijo, el mismo en todos los dispositivos) con los dispositivos dados. */
	public void shareFolder(String folderId, String label, String path, List<String> devices) throws IOException {
		JsonObject folder = new JsonObject();
		folder.addProperty("id", folderId);
		folder.addProperty("label", label);
		folder.addProperty("path", path);
		folder.addProperty("type", "sendreceive");
		folder.addProperty("fsWatcherEnabled", true);
		folder.addProperty("rescanIntervalS", 3600);
		JsonArray list = new JsonArray();
		for (String id : devices) {
			JsonObject device = new JsonObject();
			device.addProperty("deviceID", id);
			list.add(device);
		}
		folder.add("devices", list);
		put("/rest/config/folders/" + folderId, folder);
	}

	/** Qué dispositivos están conectados ahora. */
	public Set<String> connected() throws IOException {
		Set<String> ids = new LinkedHashSet<>();
		JsonElement all = get("/rest/system/connections").get("connections");
		if (all == null || !all.isJsonObject()) return ids;
		for (Map.Entry<String, JsonElement> entry : all.getAsJsonObject().entrySet()) {
			JsonElement connected = entry.getValue().isJsonObject() ? entry.getValue().getAsJsonObject().get("connected") : null;
			if (connected != null && connected.getAsBoolean()) ids.add(entry.getKey());
		}
		return ids;
	}

	/** Cuántos archivos de {@code folderId} tiene ya {@code device} (null: este dispositivo) de los que debería tener. */
	public Progress completion(String folderId, String device) throws IOException {
		JsonObject o = get("/rest/db/completion?folder=" + folderId + (device != null ? "&device=" + device : ""));
		long total = o.has("globalItems") ? o.get("globalItems").getAsLong() : 0;
		long need = o.has("needItems") ? o.get("needItems").getAsLong() : 0;
		return new Progress(Math.max(0, total - need), total);
	}

	/** Los dispositivos sin vincular que han intentado conectar: el móvil que acaba de escanear el QR. */
	public Set<String> pendingDevices() throws IOException {
		Set<String> ids = new LinkedHashSet<>();
		for (String key : get("/rest/cluster/pending/devices").keySet()) {
			String id = SyncIds.device(key);
			if (id != null) ids.add(id);
		}
		return ids;
	}

	private JsonObject get(String path) throws IOException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(base + path)).header("X-API-Key", apiKey).timeout(Duration.ofSeconds(30)).GET().build();
		String body = send(request, path);
		JsonElement parsed = body.isBlank() ? null : JsonParser.parseString(body);
		return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
	}

	private void put(String path, JsonObject body) throws IOException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(base + path)).header("X-API-Key", apiKey).header("Content-Type", "application/json")
				.timeout(Duration.ofSeconds(30)).PUT(HttpRequest.BodyPublishers.ofString(body.toString())).build();
		send(request, path);
	}

	private String send(HttpRequest request, String path) throws IOException {
		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() / 100 != 2) throw new IOException("Syncthing: HTTP " + response.statusCode() + " on " + path);
			return response.body();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted", e);
		} catch (RuntimeException e) {
			throw new IOException("Syncthing: " + e.getMessage(), e);
		}
	}
}
