package com.freedomclient.update;

import com.freedomclient.FreedomClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.util.Util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Avisador de actualizaciones: compara el commit con el que se compiló este .jar con el de la release "latest" de GitHub.
 * Si hay uno nuevo, lo descarga y sustituye el .jar (en Windows, cuando se cierra el juego, porque el archivo está en uso).
 */
public final class UpdateChecker {
	public enum State { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, READY, FAILED }

	private static final String REPO = "rippipupil/FreedomClient";
	private static final String TAG_API = "https://api.github.com/repos/" + REPO + "/git/ref/tags/latest";
	private static final String JAR_URL = "https://github.com/" + REPO + "/releases/download/latest/FreedomClient-1.21.11.jar";
	private static final String RELEASE_PAGE = "https://github.com/" + REPO + "/releases/tag/latest";

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
			.followRedirects(HttpClient.Redirect.NORMAL).build();
	private static volatile State state = State.IDLE;
	private static volatile boolean dismissed;

	private UpdateChecker() {
	}

	public static State state() {
		return state;
	}

	/** Si hay que enseñar el aviso en el menú principal. */
	public static boolean shouldShowBanner() {
		return !dismissed && (state == State.AVAILABLE || state == State.DOWNLOADING || state == State.READY || state == State.FAILED);
	}

	public static void dismiss() {
		dismissed = true;
	}

	/** Commit con el que se compiló este .jar (vacío en desarrollo). */
	public static String currentCommit() {
		return FabricLoader.getInstance().getModContainer(FreedomClient.MOD_ID)
				.map(container -> container.getMetadata().getCustomValue("freedomclient:commit"))
				.filter(value -> value != null && value.getType() == CustomValue.CvType.STRING)
				.map(CustomValue::getAsString)
				.filter(commit -> commit.matches("[0-9a-f]{40}"))
				.orElse("");
	}

	/** Con el launcher de FreedomClient el mod no se actualiza solo: lo hace el launcher antes de abrir el juego. */
	public static boolean managedByLauncher() {
		return Boolean.getBoolean("freedomclient.launcher");
	}

	public static void checkAsync() {
		String current = currentCommit();
		if (current.isEmpty() || state != State.IDLE || managedByLauncher()) return;
		state = State.CHECKING;
		HttpRequest request = HttpRequest.newBuilder(URI.create(TAG_API)).timeout(Duration.ofSeconds(10))
				.header("Accept", "application/vnd.github+json").header("User-Agent", "FreedomClient").GET().build();
		HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((response, error) -> {
			if (error != null || response.statusCode() != 200) {
				state = State.UP_TO_DATE;
				return;
			}
			try {
				JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
				String latest = json.getAsJsonObject("object").get("sha").getAsString();
				state = latest.equalsIgnoreCase(current) ? State.UP_TO_DATE : State.AVAILABLE;
				if (state == State.AVAILABLE) FreedomClient.LOGGER.info("FreedomClient update available ({} -> {})", current, latest);
			} catch (Exception e) {
				state = State.UP_TO_DATE;
			}
		});
	}

	/** Descarga el .jar nuevo y lo pone en lugar del actual. */
	public static void downloadAsync() {
		if (state != State.AVAILABLE && state != State.FAILED) return;
		Optional<Path> ownJar = ownJar();
		if (ownJar.isEmpty()) {
			// En desarrollo no hay .jar que sustituir: se abre la página de la release.
			Util.getPlatform().openUri(RELEASE_PAGE);
			return;
		}
		state = State.DOWNLOADING;
		Path target = ownJar.get();
		Path download = target.resolveSibling(target.getFileName() + ".download");
		HttpRequest request = HttpRequest.newBuilder(URI.create(JAR_URL)).timeout(Duration.ofMinutes(2))
				.header("User-Agent", "FreedomClient").GET().build();
		HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofFile(download)).whenComplete((response, error) -> {
			try {
				if (error != null || response.statusCode() != 200 || Files.size(download) < 100_000) {
					throw new IOException("Download failed: " + (error != null ? error.getMessage() : "HTTP " + response.statusCode()));
				}
				install(download, target);
				state = State.READY;
			} catch (Exception e) {
				FreedomClient.LOGGER.warn("Could not download the FreedomClient update", e);
				state = State.FAILED;
			}
		});
	}

	private static void install(Path download, Path target) throws IOException {
		boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
		if (!windows) {
			// En Linux y macOS se puede sustituir un archivo abierto: el juego sigue con el viejo hasta reiniciar.
			Files.move(download, target, StandardCopyOption.REPLACE_EXISTING);
			return;
		}
		// En Windows el .jar está bloqueado mientras el juego está abierto: se cambia justo después de cerrarlo.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				new ProcessBuilder("cmd", "/c", "timeout /t 3 /nobreak >nul & move /y \"" + download + "\" \"" + target + "\"")
						.start();
			} catch (IOException ignored) {
			}
		}, "FreedomClient updater"));
	}

	private static Optional<Path> ownJar() {
		return FabricLoader.getInstance().getModContainer(FreedomClient.MOD_ID)
				.map(ModContainer::getOrigin)
				.flatMap(origin -> {
					try {
						List<Path> paths = origin.getPaths();
						return paths.stream().filter(path -> path.toString().endsWith(".jar") && Files.isRegularFile(path)).findFirst();
					} catch (UnsupportedOperationException e) {
						return Optional.empty();
					}
				});
	}
}
