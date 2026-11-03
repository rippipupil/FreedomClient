package com.freedomclient.freedomplayer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * El programa oficial de Syncthing (el mismo que lleva FreedomPlayer, v2.1.6). No va dentro del mod: se descarga la
 * primera vez que se vincula, de las descargas oficiales de GitHub, y se comprueba con su suma SHA-256 (las de la lista
 * oficial de esa versión, escritas aquí) antes de usarlo.
 */
public final class SyncthingBinary {
	public static final String VERSION = "v2.1.6";
	private static final String BASE = "https://github.com/syncthing/syncthing/releases/download/" + VERSION + "/";
	private static final long MAX_SIZE = 80L * 1024 * 1024;

	/** Un archivo de la versión para un sistema: su nombre, su suma y el programa que hay dentro. */
	record Asset(String file, String sha256, String entry) {
	}

	private SyncthingBinary() {
	}

	/** El archivo para este sistema, o null si Syncthing no tiene versión para él. */
	static Asset asset(String os, String arch) {
		os = os.toLowerCase(Locale.ROOT);
		arch = arch.toLowerCase(Locale.ROOT);
		boolean arm = arch.contains("aarch64") || arch.contains("arm64");
		boolean x64 = arch.contains("amd64") || arch.contains("x86_64");
		if (os.contains("win")) {
			if (arm) return windows("arm64", "ddb25a3cf556157e329263bd3331d889ea7bb952b85a5f530320175105bab072");
			if (x64) return windows("amd64", "589098269cabb9a5a1703a934305b4153609d7623e978e1c94f0f49d77ae5e9f");
		} else if (os.contains("mac")) {
			return new Asset("syncthing-macos-universal-" + VERSION + ".zip", "83d2898de5607d12c25d66856b1e3aa40326814e8ded6af98ab8ba68c2a545bc",
					"syncthing-macos-universal-" + VERSION + "/syncthing");
		} else if (os.contains("linux")) {
			if (arm) return linux("arm64", "f054de02462d5d58bd126908f2f92b63aba74f162a5be85e50e96d656e7db47d");
			if (x64) return linux("amd64", "524ef4e1df1850b719e2378c8369956f4e5f2ab9a46fdf7f45f2b925012147aa");
		}
		return null;
	}

	private static Asset windows(String arch, String sha256) {
		String name = "syncthing-windows-" + arch + "-" + VERSION;
		return new Asset(name + ".zip", sha256, name + "/syncthing.exe");
	}

	private static Asset linux(String arch, String sha256) {
		String name = "syncthing-linux-" + arch + "-" + VERSION;
		return new Asset(name + ".tar.gz", sha256, name + "/syncthing");
	}

	/** Dónde queda el programa (dentro de la carpeta del cliente). */
	public static Path executable(Path home) {
		String os = System.getProperty("os.name", "");
		return home.resolve("bin").resolve(VERSION).resolve(os.toLowerCase(Locale.ROOT).contains("win") ? "syncthing.exe" : "syncthing");
	}

	/** El programa listo para usar: si no está, se descarga y se comprueba. {@code progress} va de 0 a 1. */
	public static Path ensure(Path home, DoubleConsumer progress) throws IOException {
		Path exe = executable(home);
		if (Files.isRegularFile(exe)) return exe;
		Asset asset = asset(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
		if (asset == null) throw new IOException("Syncthing is not available for this system");
		Files.createDirectories(exe.getParent());
		Path download = exe.getParent().resolve(asset.file() + ".part");
		try {
			fetch(URI.create(BASE + asset.file()), download, progress);
			String actual = sha256(download);
			if (!actual.equals(asset.sha256())) throw new IOException("Syncthing download is corrupt (SHA-256 does not match)");
			extract(download, asset, exe);
		} finally {
			Files.deleteIfExists(download);
		}
		if (!exe.toFile().setExecutable(true)) {
			// En Windows no hace falta; en otros sistemas, sin permiso de ejecutar no arrancaría.
			if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) throw new IOException("Cannot make Syncthing executable");
		}
		return exe;
	}

	private static void fetch(URI uri, Path target, DoubleConsumer progress) throws IOException {
		HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15)).build();
		HttpRequest request = HttpRequest.newBuilder(uri).header("User-Agent", "FreedomClient").timeout(Duration.ofMinutes(5)).GET().build();
		try {
			HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
			try (InputStream body = response.body(); OutputStream out = Files.newOutputStream(target)) {
				if (response.statusCode() != 200) throw new IOException("Syncthing download answered HTTP " + response.statusCode());
				long total = response.headers().firstValueAsLong("Content-Length").orElse(-1);
				byte[] buffer = new byte[65536];
				long done = 0;
				int read;
				while ((read = body.read(buffer)) > 0) {
					done += read;
					if (done > MAX_SIZE) throw new IOException("Syncthing download is too big");
					out.write(buffer, 0, read);
					if (total > 0) progress.accept(Math.min(1.0, done / (double) total));
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted", e);
		}
	}

	static String sha256(Path file) throws IOException {
		try (InputStream in = Files.newInputStream(file)) {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] buffer = new byte[65536];
			int read;
			while ((read = in.read(buffer)) > 0) digest.update(buffer, 0, read);
			return HexFormat.of().formatHex(digest.digest());
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}

	/** Saca solo el programa del zip o del tar.gz. */
	static void extract(Path archive, Asset asset, Path exe) throws IOException {
		Path temp = exe.resolveSibling(exe.getFileName() + ".tmp");
		boolean found = false;
		try (InputStream in = new BufferedInputStream(Files.newInputStream(archive))) {
			if (asset.file().endsWith(".zip")) {
				try (ZipInputStream zip = new ZipInputStream(in)) {
					ZipEntry entry;
					while ((entry = zip.getNextEntry()) != null) {
						if (entry.getName().equals(asset.entry())) {
							Files.copy(zip, temp, StandardCopyOption.REPLACE_EXISTING);
							found = true;
							break;
						}
					}
				}
			} else {
				found = untar(new GZIPInputStream(in), asset.entry(), temp);
			}
		}
		if (!found) throw new IOException("Syncthing archive has no " + asset.entry());
		Files.move(temp, exe, StandardCopyOption.REPLACE_EXISTING);
	}

	/** Lee un tar (cabeceras de 512 bytes) y copia el archivo {@code name}. */
	private static boolean untar(InputStream in, String name, Path target) throws IOException {
		byte[] header = new byte[512];
		String longName = null;
		while (in.readNBytes(header, 0, 512) == 512) {
			if (header[0] == 0) return false;
			String entry = longName != null ? longName : text(header, 0, 100);
			longName = null;
			String prefix = text(header, 345, 155);
			if (!prefix.isEmpty() && entry.equals(text(header, 0, 100))) entry = prefix + "/" + entry;
			long size = Long.parseLong(text(header, 124, 12).trim().isEmpty() ? "0" : text(header, 124, 12).trim(), 8);
			char type = (char) header[156];
			long padded = (size + 511) / 512 * 512;
			if (type == 'L') {
				// Nombre largo de GNU: el siguiente archivo se llama así.
				longName = new String(in.readNBytes((int) size), java.nio.charset.StandardCharsets.UTF_8).replace("\0", "");
				in.skipNBytes(padded - size);
				continue;
			}
			if ((type == '0' || type == 0) && entry.equals(name)) {
				try (OutputStream out = Files.newOutputStream(target)) {
					long left = size;
					byte[] buffer = new byte[65536];
					while (left > 0) {
						int read = in.read(buffer, 0, (int) Math.min(buffer.length, left));
						if (read < 0) throw new IOException("Truncated tar");
						out.write(buffer, 0, read);
						left -= read;
					}
				}
				return true;
			}
			in.skipNBytes(padded);
		}
		return false;
	}

	private static String text(byte[] bytes, int offset, int length) {
		int end = offset;
		while (end < offset + length && bytes[end] != 0) end++;
		return new String(bytes, offset, end - offset, java.nio.charset.StandardCharsets.UTF_8);
	}
}
