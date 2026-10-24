package com.freedomclient.music;

import com.freedomclient.FreedomClient;
import org.sqlite.JDBC;
import org.sqlite.SQLiteConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * La biblioteca de FreedomPlayer en este PC: su base de datos (freedomplayer.db, SQLite) con las canciones que tienen
 * el audio bajado y las letras sincronizadas de LRCLIB que guarda para usarlas sin conexión. Solo se lee (en modo de
 * solo lectura, así que no molesta a la app si está abierta).
 */
public final class FreedomPlayerLibrary {
	/** Una canción de la biblioteca con su archivo de audio. */
	public record Song(String id, String title, String artist, Path file, long durationMs) {
	}

	private FreedomPlayerLibrary() {
	}

	/** La carpeta de datos de la app (la misma que usa FreedomPlayer: DesktopLibrary.dataDir). */
	public static Path dataDir() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String home = System.getProperty("user.home", "");
		String base;
		if (os.contains("win")) {
			String local = System.getenv("LOCALAPPDATA");
			base = local != null && !local.isBlank() ? local : home + "\\AppData\\Local";
		} else if (os.contains("mac")) {
			base = home + "/Library/Application Support";
		} else {
			String xdg = System.getenv("XDG_DATA_HOME");
			base = xdg != null && !xdg.isBlank() ? xdg : home + "/.local/share";
		}
		return Path.of(base, "FreedomPlayer");
	}

	public static Path database() {
		return dataDir().resolve("freedomplayer.db");
	}

	public static boolean exists() {
		return Files.isRegularFile(database());
	}

	/** Las canciones con audio que el Music Player sabe tocar, de la más nueva a la más antigua. */
	public static List<Song> songs() {
		List<Song> songs = new ArrayList<>();
		if (!exists()) return songs;
		try (Connection connection = open(); Statement statement = connection.createStatement();
			 ResultSet rows = statement.executeQuery("SELECT id, title, artist, audio_path, duration_sec FROM song "
					 + "WHERE audio_path IS NOT NULL ORDER BY added_at DESC")) {
			while (rows.next()) {
				String path = rows.getString(4);
				if (path == null || path.isBlank()) continue;
				Path file;
				try {
					file = Path.of(path);
				} catch (RuntimeException e) {
					continue;
				}
				if (!LocalMusicPlayer.canPlay(file.getFileName().toString()) || !Files.isRegularFile(file)) continue;
				songs.add(new Song(rows.getString(1), rows.getString(2), rows.getString(3), file, rows.getLong(5) * 1000L));
			}
		} catch (Exception e) {
			FreedomClient.LOGGER.warn("Could not read the FreedomPlayer library {}", database(), e);
		}
		return songs;
	}

	/** La letra sincronizada (LRC) que FreedomPlayer guardó para esa canción, o null si no tiene. */
	public static String syncedLyrics(String songId) {
		if (songId == null || !exists()) return null;
		try (Connection connection = open();
			 PreparedStatement statement = connection.prepareStatement("SELECT synced FROM lyrics WHERE song_id = ?")) {
			statement.setString(1, songId);
			try (ResultSet rows = statement.executeQuery()) {
				return rows.next() ? rows.getString(1) : null;
			}
		} catch (Exception e) {
			FreedomClient.LOGGER.warn("Could not read the lyrics of {} from FreedomPlayer", songId, e);
			return null;
		}
	}

	/**
	 * Abre la base de datos en solo lectura. Se usa el driver directamente y no DriverManager: en Fabric el driver no
	 * siempre se registra solo (lo carga el cargador de clases del mod).
	 */
	private static Connection open() throws Exception {
		SQLiteConfig config = new SQLiteConfig();
		config.setReadOnly(true);
		config.setBusyTimeout(2000);
		Connection connection = new JDBC().connect("jdbc:sqlite:" + database().toAbsolutePath(), config.toProperties());
		if (connection == null) throw new IllegalStateException("No SQLite connection");
		return connection;
	}
}
