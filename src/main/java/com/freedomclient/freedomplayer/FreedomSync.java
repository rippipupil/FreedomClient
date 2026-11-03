package com.freedomclient.freedomplayer;

import com.freedomclient.FreedomClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Vincular el cliente con FreedomPlayer del móvil, igual que FreedomPlayer del PC: Syncthing oficial (se descarga la
 * primera vez), el cliente enseña su id en un QR, el móvil lo escanea («Vincular con el PC» o «Vincular otro PC o
 * FreedomClient») y, mientras el QR está a la vista, se acepta al móvil que pide conectar y se comparten con él la
 * carpeta de la música y la de la biblioteca. Después, mientras el juego está abierto, se pasan las canciones y cada
 * 20 s se intercambia la biblioteca (playlists, rankings, notas y escuchas).
 * <p>
 * Todo va en un hilo aparte; la pantalla solo lee el estado.
 */
public final class FreedomSync {
	public enum Phase { OFF, DOWNLOADING, STARTING, WAITING, PAIRING, SYNCING, SYNCED, FAILED }

	private final Path root;
	private final Path home;
	private final Path music;
	private final Path library;
	private final Path linkFile;
	private final ClientLibrary clientLibrary;

	private volatile Phase phase = Phase.OFF;
	private volatile String error;
	private volatile double download;
	private volatile String myId;
	private volatile boolean[][] qr;
	private volatile boolean connected;
	private volatile long done, total;
	/** Mientras se enseña el QR se acepta al dispositivo que pida conectar. */
	private volatile boolean accepting;
	private volatile List<String> peers = List.of();

	private Thread worker;
	private Process process;
	private SyncthingApi api;

	public FreedomSync(Path root, ClientLibrary clientLibrary) {
		this.root = root;
		this.home = root.resolve("syncthing");
		this.music = root.resolve("musica");
		this.library = root.resolve("biblioteca");
		this.linkFile = root.resolve("vinculo.json");
		this.clientLibrary = clientLibrary;
		loadLink();
	}

	public Phase phase() {
		return phase;
	}

	public String error() {
		return error;
	}

	public double downloadProgress() {
		return download;
	}

	public String myId() {
		return myId;
	}

	public boolean[][] qr() {
		return qr;
	}

	public boolean connected() {
		return connected;
	}

	public long done() {
		return done;
	}

	public long total() {
		return total;
	}

	public List<String> peers() {
		return peers;
	}

	public boolean linked() {
		return !peers.isEmpty();
	}

	public boolean accepting() {
		return accepting;
	}

	/** La API del Syncthing que está en marcha (para las pruebas). */
	SyncthingApi api() {
		return api;
	}

	/** Al abrir el juego: si ya está vinculado, se arranca Syncthing para seguir sincronizando. */
	public synchronized void startIfLinked() {
		if (linked()) ensureWorker();
	}

	/** «Vincular móvil»: arranca (descargando Syncthing si hace falta), enseña el QR y espera al móvil. */
	public synchronized void startLinking() {
		accepting = true;
		error = null;
		if (phase == Phase.FAILED) phase = Phase.OFF;
		ensureWorker();
	}

	/** Deja de enseñar el QR (ya no se acepta a nadie nuevo). */
	public void stopLinking() {
		accepting = false;
		if (phase == Phase.WAITING) phase = linked() ? (connected ? Phase.SYNCED : Phase.SYNCING) : Phase.OFF;
	}

	/** Al cerrar el juego. */
	public synchronized void shutdown() {
		if (worker != null) worker.interrupt();
		worker = null;
		if (process != null) process.destroy();
		process = null;
		api = null;
	}

	private void ensureWorker() {
		if (worker != null && worker.isAlive()) return;
		worker = new Thread(this::run, "FreedomClient Sync");
		worker.setDaemon(true);
		worker.start();
	}

	private void run() {
		try {
			start();
			int tick = 0;
			while (!Thread.currentThread().isInterrupted()) {
				// Si Syncthing se ha cerrado (lo ha matado el sistema, por ejemplo), se vuelve a arrancar.
				if (process == null || !process.isAlive()) {
					api = null;
					start();
				}
				if (accepting) acceptPending();
				if (linked()) {
					watch();
					if (tick % 10 == 0) exchange();
				} else if (!accepting) {
					phase = Phase.OFF;
				}
				tick++;
				Thread.sleep(2000);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			FreedomClient.LOGGER.warn("FreedomPlayer sync failed", e);
			error = e instanceof IOException && e.getMessage() != null ? e.getMessage() : "Could not start syncing. Try again in a moment.";
			phase = Phase.FAILED;
			accepting = false;
		}
	}

	/** Syncthing en marcha (solo en 127.0.0.1, con una clave nueva cada vez) y el QR con su id. */
	private void start() throws Exception {
		if (api != null && api.ping()) return;
		phase = Phase.DOWNLOADING;
		Path exe = SyncthingBinary.ensure(home, progress -> download = progress);
		phase = Phase.STARTING;
		Files.createDirectories(home);
		Files.createDirectories(music);
		Files.createDirectories(library);
		int port;
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		}
		String key = SyncIds.newApiKey();
		ProcessBuilder builder = new ProcessBuilder(exe.toString(), "serve", "--home=" + home.toAbsolutePath(), "--gui-address=127.0.0.1:" + port,
				"--gui-apikey=" + key, "--no-browser", "--no-restart", "--no-upgrade")
				.redirectErrorStream(true).redirectOutput(home.resolve("syncthing.log").toFile());
		// Sin la carpeta «Default Folder» que Syncthing crea la primera vez, y sin actualizarse solo.
		builder.environment().putAll(Map.of("STNODEFAULTFOLDER", "1", "STNOUPGRADE", "1"));
		Process started = builder.start();
		process = started;
		Runtime.getRuntime().addShutdownHook(new Thread(started::destroy));
		SyncthingApi candidate = new SyncthingApi("http://127.0.0.1:" + port, key);
		for (int i = 0; i < 60; i++) {
			if (candidate.ping()) {
				api = candidate;
				myId = candidate.myId();
				qr = qr(myId);
				clientLibrary.setDevice(myId, library, music, home.resolve("sincronizar.json"));
				// Por si se vinculó antes con otro móvil: las carpetas siguen compartidas con todos.
				for (String peer : peers) share(peer);
				phase = accepting ? Phase.WAITING : Phase.SYNCING;
				return;
			}
			if (!started.isAlive()) throw new IOException("Syncthing closed while starting");
			Thread.sleep(500);
		}
		started.destroy();
		throw new IOException("Syncthing does not answer");
	}

	/** Con el QR a la vista: el móvil que lo acaba de escanear pide conectar y se le acepta. */
	private void acceptPending() {
		try {
			Set<String> pending = api.pendingDevices();
			for (String id : pending) {
				if (id.equals(myId) || peers.contains(id)) continue;
				phase = Phase.PAIRING;
				api.addDevice(id, "FreedomPlayer");
				share(id);
				List<String> all = new ArrayList<>(peers);
				all.add(id);
				peers = List.copyOf(all);
				saveLink();
				accepting = false;
				phase = Phase.SYNCING;
				return;
			}
		} catch (IOException e) {
			FreedomClient.LOGGER.debug("Could not read pending devices", e);
		}
	}

	/** Comparte las dos carpetas con {@code id}, sin quitar a los que ya estaban. */
	private void share(String id) throws IOException {
		api.shareFolder(SyncthingApi.MUSIC_FOLDER, "FreedomPlayer · música", music.toAbsolutePath().toString(), merged(SyncthingApi.MUSIC_FOLDER, id));
		api.shareFolder(SyncthingApi.LIBRARY_FOLDER, "FreedomPlayer · biblioteca", library.toAbsolutePath().toString(), merged(SyncthingApi.LIBRARY_FOLDER, id));
	}

	private List<String> merged(String folder, String id) {
		List<String> devices = new ArrayList<>(api.folderDevices(folder));
		if (!devices.contains(id)) devices.add(id);
		return devices;
	}

	/** «12 de 20»: de la música, lo que ya está en los dos (cuenta el primer vinculado que esté conectado). */
	private void watch() {
		try {
			Set<String> online = api.connected();
			for (String peer : peers) {
				if (!online.contains(peer)) continue;
				SyncthingApi.Progress here = api.completion(SyncthingApi.MUSIC_FOLDER, null);
				SyncthingApi.Progress there = api.completion(SyncthingApi.MUSIC_FOLDER, peer);
				long all = Math.max(here.total(), there.total());
				long missing = (here.total() - here.done()) + (there.total() - there.done());
				done = Math.max(0, all - missing);
				total = all;
				connected = true;
				if (!accepting) phase = done >= total ? Phase.SYNCED : Phase.SYNCING;
				return;
			}
			connected = false;
			if (!accepting) phase = Phase.SYNCING;
		} catch (IOException e) {
			connected = false;
		}
	}

	private void exchange() {
		try {
			clientLibrary.exchange();
		} catch (Exception e) {
			FreedomClient.LOGGER.warn("Could not exchange the FreedomPlayer library", e);
		}
	}

	private static boolean[][] qr(String text) {
		try {
			BitMatrix bits = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0,
					Map.of(EncodeHintType.MARGIN, 0, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
			boolean[][] cells = new boolean[bits.getHeight()][bits.getWidth()];
			for (int y = 0; y < bits.getHeight(); y++) {
				for (int x = 0; x < bits.getWidth(); x++) cells[y][x] = bits.get(x, y);
			}
			return cells;
		} catch (Exception e) {
			FreedomClient.LOGGER.warn("Could not make the QR code", e);
			return null;
		}
	}

	private void loadLink() {
		try {
			if (!Files.isRegularFile(linkFile)) return;
			JsonObject root = JsonParser.parseString(Files.readString(linkFile, StandardCharsets.UTF_8)).getAsJsonObject();
			List<String> list = new ArrayList<>();
			if (root.has("vinculados")) root.getAsJsonArray("vinculados").forEach(e -> list.add(e.getAsString()));
			peers = List.copyOf(list);
			if (root.has("id")) {
				myId = root.get("id").getAsString();
				clientLibrary.setDevice(myId, library, music, home.resolve("sincronizar.json"));
			}
		} catch (Exception e) {
			FreedomClient.LOGGER.warn("Could not read {}", linkFile, e);
		}
	}

	private void saveLink() {
		try {
			JsonObject json = new JsonObject();
			if (myId != null) json.addProperty("id", myId);
			JsonArray list = new JsonArray();
			peers.forEach(list::add);
			json.add("vinculados", list);
			Files.createDirectories(root);
			Files.writeString(linkFile, json.toString(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			FreedomClient.LOGGER.warn("Could not save {}", linkFile, e);
		}
	}
}
