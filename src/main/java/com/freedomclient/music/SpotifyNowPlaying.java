package com.freedomclient.music;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lee la canción que suena en la app de escritorio de Spotify, sin cuenta ni internet:
 * - Windows: el título de la ventana de Spotify ("Artista - Canción" mientras suena).
 * - macOS: AppleScript.
 * - Linux: playerctl (MPRIS).
 * Se consulta cada pocos segundos en un hilo aparte.
 */
public final class SpotifyNowPlaying {
	private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
	private static final AtomicReference<String> current = new AtomicReference<>();
	private static ScheduledExecutorService executor;

	private SpotifyNowPlaying() {
	}

	/** "Artista - Canción", o null si Spotify no está sonando. */
	public static String current() {
		return current.get();
	}

	public static synchronized void start() {
		if (executor != null) return;
		executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "FreedomClient Spotify");
			thread.setDaemon(true);
			return thread;
		});
		executor.scheduleWithFixedDelay(SpotifyNowPlaying::poll, 0, 3, TimeUnit.SECONDS);
	}

	public static synchronized void stop() {
		if (executor != null) {
			executor.shutdownNow();
			executor = null;
		}
		current.set(null);
	}

	private static void poll() {
		try {
			String song;
			if (OS.contains("win")) song = windows();
			else if (OS.contains("mac")) song = run("osascript", "-e",
					"if application \"Spotify\" is running then tell application \"Spotify\" to if player state is playing then "
							+ "return (artist of current track) & \" - \" & (name of current track)");
			else song = linux();
			current.set(song == null || song.isBlank() ? null : song.trim());
		} catch (Throwable e) {
			current.set(null);
		}
	}

	private static String linux() throws Exception {
		String status = run("playerctl", "--player=spotify", "status");
		if (status == null || !status.trim().equals("Playing")) return null;
		return run("playerctl", "--player=spotify", "metadata", "--format", "{{artist}} - {{title}}");
	}

	/** Busca la ventana de Spotify.exe: mientras suena, su título es "Artista - Canción". */
	private static String windows() {
		AtomicReference<String> found = new AtomicReference<>();
		User32.INSTANCE.EnumWindows((WinDef.HWND hwnd, com.sun.jna.Pointer data) -> {
			char[] buffer = new char[512];
			int length = User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
			if (length <= 0) return true;
			String title = Native.toString(buffer);
			if (!title.contains(" - ")) return true;
			IntByReference pid = new IntByReference();
			User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
			boolean spotify = ProcessHandle.of(pid.getValue())
					.flatMap(process -> process.info().command())
					.map(command -> command.toLowerCase(Locale.ROOT).endsWith("spotify.exe"))
					.orElse(false);
			if (spotify) {
				found.set(title);
				return false;
			}
			return true;
		}, null);
		return found.get();
	}

	private static String run(String... command) throws Exception {
		Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		if (!process.waitFor(2, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			return null;
		}
		if (process.exitValue() != 0) return null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			return reader.readLine();
		}
	}
}
