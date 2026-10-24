package com.freedomclient.music;

import com.freedomclient.FreedomClient;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.fabricmc.loader.api.FabricLoader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Reproductor de música local: toca las canciones de la carpeta .minecraft/freedomclient/music y las de la biblioteca
 * de FreedomPlayer en un hilo aparte (MP3 con JLayer, WAV con Java Sound, m4a/AAC con jcodec y webm/Opus con
 * Concentus), con pausa, siguiente, anterior y volumen. De cada canción guarda su letra sincronizada si la tiene (la de
 * FreedomPlayer o un .lrc al lado del archivo) para NeoLyrics.
 */
public final class LocalMusicPlayer {
	/** Una canción de la lista: su archivo, el nombre que se enseña y, si es de FreedomPlayer, su id allí. */
	private record Track(Path file, String title, String songId, long durationMs) {
	}

	private final Path folder = FabricLoader.getInstance().getGameDir().resolve(FreedomClient.MOD_ID).resolve("music");
	private final List<Track> playlist = new ArrayList<>();
	private int index = -1;
	private Thread thread;
	private volatile boolean paused;
	private volatile boolean skip;
	private volatile boolean stopped = true;
	private volatile float volume = 0.6F;
	private volatile long positionMs;
	private volatile long durationMs;
	private volatile String title;
	private volatile LyricsTimeline lyrics;
	/** Sube con cada canción que empieza (NeoLyrics lo usa para saber que ha cambiado). */
	private volatile int songNumber;
	/** La salida de audio que suena ahora: su posición es lo que de verdad se ha oído (sin lo que hay en el búfer). */
	private volatile SourceDataLine line;
	private boolean shuffle;
	private boolean freedomPlayer = true;

	/** Si el Music Player sabe tocar ese archivo. */
	public static boolean canPlay(String name) {
		String lower = name.toLowerCase(Locale.ROOT);
		return lower.endsWith(".mp3") || lower.endsWith(".wav") || PcmSource.supports(lower);
	}

	public Path folder() {
		return folder;
	}

	/** Vuelve a leer la carpeta de música y la biblioteca de FreedomPlayer. Devuelve cuántas canciones hay. */
	public synchronized int reload() {
		playlist.clear();
		try {
			Files.createDirectories(folder);
			try (Stream<Path> files = Files.list(folder)) {
				files.filter(file -> canPlay(file.getFileName().toString())).sorted().forEach(file -> {
					String name = file.getFileName().toString();
					playlist.add(new Track(file, name.substring(0, name.lastIndexOf('.')), null, 0));
				});
			}
		} catch (IOException e) {
			FreedomClient.LOGGER.warn("Could not read the music folder {}", folder, e);
		}
		if (freedomPlayer) {
			for (FreedomPlayerLibrary.Song song : FreedomPlayerLibrary.songs()) {
				String name = song.artist() == null || song.artist().isBlank() ? song.title() : song.artist() + " - " + song.title();
				playlist.add(new Track(song.file(), name, song.id(), song.durationMs()));
			}
		}
		if (shuffle) Collections.shuffle(playlist);
		return playlist.size();
	}

	/** Si se tocan también las canciones bajadas en FreedomPlayer. */
	public synchronized void setFreedomPlayer(boolean freedomPlayer) {
		if (this.freedomPlayer != freedomPlayer) {
			this.freedomPlayer = freedomPlayer;
			reload();
		}
	}

	public synchronized void setShuffle(boolean shuffle) {
		if (this.shuffle != shuffle) {
			this.shuffle = shuffle;
			reload();
		}
	}

	public void setVolume(float volume) {
		this.volume = Math.max(0.0F, Math.min(1.0F, volume));
	}

	public boolean isPlaying() {
		return !stopped && !paused && title != null;
	}

	public boolean isPaused() {
		return paused;
	}

	public boolean isActive() {
		return !stopped;
	}

	public String title() {
		return title;
	}

	public long positionMs() {
		return positionMs;
	}

	public long durationMs() {
		return durationMs;
	}

	/** Lo que se ha oído de la canción, en ms: como positionMs pero sin contar lo que espera en el búfer de audio. */
	public long playedMs() {
		SourceDataLine current = line;
		if (current == null) return positionMs;
		try {
			return Math.min(positionMs, current.getMicrosecondPosition() / 1000L);
		} catch (RuntimeException e) {
			return positionMs;
		}
	}

	/** La letra sincronizada de la canción que suena, o null si no tiene. */
	public LyricsTimeline lyrics() {
		return lyrics;
	}

	public int songNumber() {
		return songNumber;
	}

	public synchronized void play() {
		if (!stopped) {
			paused = false;
			return;
		}
		if (playlist.isEmpty() && reload() == 0) return;
		stopped = false;
		paused = false;
		if (index < 0) index = 0;
		thread = new Thread(this::run, "FreedomClient Music");
		thread.setDaemon(true);
		thread.start();
	}

	public void togglePause() {
		if (stopped) play();
		else paused = !paused;
	}

	public synchronized void next() {
		if (playlist.isEmpty()) return;
		index = (index + 1) % playlist.size();
		skip = true;
		paused = false;
		if (stopped) play();
	}

	public synchronized void previous() {
		if (playlist.isEmpty()) return;
		index = (index - 1 + playlist.size()) % playlist.size();
		skip = true;
		paused = false;
		if (stopped) play();
	}

	public synchronized void stop() {
		stopped = true;
		skip = true;
		title = null;
	}

	/**
	 * Descarga una canción desde un enlace directo a un archivo MP3 o WAV a la carpeta de música. Los enlaces de
	 * YouTube (y otras webs de streaming) no se aceptan: sacar su audio va contra sus condiciones de uso.
	 * Devuelve el mensaje para el jugador.
	 */
	public java.util.concurrent.CompletableFuture<String> download(String link) {
		return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
			java.net.URI uri;
			try {
				uri = java.net.URI.create(link);
			} catch (IllegalArgumentException e) {
				return "That is not a valid link.";
			}
			String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
			if (!scheme.equals("https") && !scheme.equals("http")) return "Paste an http(s) link to an MP3 or WAV file.";
			if (host.contains("youtube.") || host.equals("youtu.be") || host.contains("spotify.") || host.contains("soundcloud.")) {
				return "Streaming sites like YouTube are not supported. Use a direct MP3/WAV link or put the file in the music folder.";
			}
			try {
				java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
						.followRedirects(java.net.http.HttpClient.Redirect.NORMAL).connectTimeout(java.time.Duration.ofSeconds(10)).build();
				java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(uri).timeout(java.time.Duration.ofSeconds(60))
						.header("User-Agent", "FreedomClient").GET().build();
				java.net.http.HttpResponse<InputStream> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());
				try (InputStream body = response.body()) {
					if (response.statusCode() != 200) return "The link answered with error " + response.statusCode() + ".";
					String type = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
					String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
					String extension = path.endsWith(".wav") || type.contains("wav") ? ".wav"
							: path.endsWith(".mp3") || type.contains("mpeg") || type.contains("mp3") ? ".mp3" : null;
					if (extension == null) return "That link is not an MP3 or WAV file.";
					String name = path.substring(path.lastIndexOf('/') + 1).replaceAll("[^a-z0-9 ._()-]", "_");
					if (name.isBlank() || name.equals(extension) || !name.endsWith(extension)) name = "song_" + System.currentTimeMillis() + extension;
					Files.createDirectories(folder);
					Path target = folder.resolve(name);
					Path temp = folder.resolve(name + ".part");
					long limit = 60L * 1024 * 1024;
					long written = 0;
					try (java.io.OutputStream out = Files.newOutputStream(temp)) {
						byte[] buffer = new byte[16384];
						int read;
						while ((read = body.read(buffer)) > 0) {
							written += read;
							if (written > limit) {
								out.close();
								Files.deleteIfExists(temp);
								return "The song is too big (max 60 MB).";
							}
							out.write(buffer, 0, read);
						}
					}
					Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					reload();
					return "Added " + name.substring(0, name.length() - extension.length()) + ".";
				}
			} catch (Exception e) {
				FreedomClient.LOGGER.warn("Could not download {}", link, e);
				return "Could not download the song: " + e.getClass().getSimpleName() + ".";
			}
		});
	}

	/** Hilo de música: toca la canción actual y pasa a la siguiente al acabar. */
	private void run() {
		while (!stopped) {
			Track track;
			synchronized (this) {
				if (playlist.isEmpty()) break;
				index = Math.floorMod(index, playlist.size());
				track = playlist.get(index);
			}
			skip = false;
			Path song = track.file();
			String name = song.getFileName().toString().toLowerCase(Locale.ROOT);
			positionMs = 0;
			durationMs = track.durationMs();
			lyrics = loadLyrics(track);
			title = track.title();
			songNumber++;
			try {
				if (name.endsWith(".mp3")) playMp3(song);
				else if (name.endsWith(".wav")) playWav(song);
				else playPcm(song);
			} catch (Exception e) {
				FreedomClient.LOGGER.warn("Could not play {}", song, e);
				sleep(500);
			}
			line = null;
			lyrics = null;
			synchronized (this) {
				// Al acabar sola (no con siguiente/anterior), pasa a la siguiente canción.
				if (!skip && !playlist.isEmpty()) index = (index + 1) % playlist.size();
			}
		}
		title = null;
	}

	private void playMp3(Path song) throws Exception {
		try (InputStream input = new BufferedInputStream(Files.newInputStream(song))) {
			Bitstream bitstream = new Bitstream(input);
			Decoder decoder = new Decoder();
			SourceDataLine line = null;
			long size = Files.size(song);
			try {
				while (!skip && !stopped) {
					if (paused) {
						if (line != null) line.stop();
						sleep(50);
						continue;
					}
					Header header = bitstream.readFrame();
					if (header == null) break;
					SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
					if (line == null) {
						durationMs = (long) header.total_ms((int) Math.min(Integer.MAX_VALUE, size));
						line = openLine(new AudioFormat(samples.getSampleFrequency(), 16, samples.getChannelCount(), true, false));
						this.line = line;
					}
					if (!line.isRunning()) line.start();
					write(line, samples.getBuffer(), samples.getBufferLength());
					positionMs += (long) header.ms_per_frame();
					bitstream.closeFrame();
				}
				if (line != null && !skip) line.drain();
			} finally {
				if (line != null) line.close();
				bitstream.close();
			}
		}
	}

	private void playWav(Path song) throws Exception {
		try (AudioInputStream source = AudioSystem.getAudioInputStream(song.toFile())) {
			AudioFormat base = source.getFormat();
			AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, base.getSampleRate(), 16, base.getChannels(),
					base.getChannels() * 2, base.getSampleRate(), false);
			try (AudioInputStream stream = AudioSystem.getAudioInputStream(pcm, source)) {
				durationMs = (long) (source.getFrameLength() / base.getFrameRate() * 1000.0);
				SourceDataLine line = openLine(pcm);
				this.line = line;
				try {
					line.start();
					byte[] bytes = new byte[4096];
					short[] shorts = new short[2048];
					int read;
					while (!skip && !stopped && (read = stream.read(bytes)) > 0) {
						while (paused && !skip && !stopped) {
							line.stop();
							sleep(50);
						}
						if (!line.isRunning()) line.start();
						int count = read / 2;
						for (int i = 0; i < count; i++) shorts[i] = (short) (bytes[i * 2] & 0xFF | bytes[i * 2 + 1] << 8);
						write(line, shorts, count);
						positionMs += (long) (read / (double) pcm.getFrameSize() / pcm.getFrameRate() * 1000.0);
					}
					if (!skip) line.drain();
				} finally {
					line.close();
				}
			}
		}
	}

	/** m4a y webm: los descodifica {@link PcmSource} y se tocan igual que el MP3. */
	private void playPcm(Path song) throws Exception {
		try (PcmSource source = PcmSource.open(song)) {
			if (source.durationMs() > 0) durationMs = source.durationMs();
			SourceDataLine line = null;
			long frames = 0;
			try {
				while (!skip && !stopped) {
					if (paused) {
						if (line != null) line.stop();
						sleep(50);
						continue;
					}
					PcmSource.Pcm pcm = source.next();
					if (pcm == null) break;
					if (line == null) {
						line = openLine(new AudioFormat(pcm.sampleRate(), 16, pcm.channels(), true, false));
						this.line = line;
					}
					if (!line.isRunning()) line.start();
					write(line, pcm.samples(), pcm.length());
					frames += pcm.length() / pcm.channels();
					positionMs = frames * 1000L / pcm.sampleRate();
				}
				if (line != null && !skip) line.drain();
			} finally {
				if (line != null) line.close();
			}
		}
	}

	/**
	 * Descodifica entera una canción m4a o webm y dice cómo ha salido ("48000 Hz, 2 ch, 240000 frames, 99% audible"):
	 * para comprobar los descodificadores en las pruebas.
	 */
	public static String describe(Path song) throws Exception {
		try (PcmSource source = PcmSource.open(song)) {
			long samples = 0, audible = 0;
			int rate = 0, channels = 0;
			PcmSource.Pcm pcm;
			while ((pcm = source.next()) != null) {
				rate = pcm.sampleRate();
				channels = pcm.channels();
				samples += pcm.length();
				for (int i = 0; i < pcm.length(); i++) if (Math.abs(pcm.samples()[i]) > 8) audible++;
			}
			return rate + " Hz, " + channels + " ch, " + samples / Math.max(1, channels) + " frames, "
					+ (samples == 0 ? 0 : audible * 100 / samples) + "% audible, " + source.durationMs() + " ms";
		}
	}

	/** La letra de la canción: la que guardó FreedomPlayer o, si es de la carpeta, un .lrc con el mismo nombre. */
	private static LyricsTimeline loadLyrics(Track track) {
		try {
			if (track.songId() != null) return LyricsTimeline.parse(FreedomPlayerLibrary.syncedLyrics(track.songId()));
			String name = track.file().getFileName().toString();
			Path lrc = track.file().resolveSibling(name.substring(0, name.lastIndexOf('.')) + ".lrc");
			return Files.isRegularFile(lrc) ? LyricsTimeline.parse(Files.readString(lrc)) : null;
		} catch (Exception e) {
			FreedomClient.LOGGER.warn("Could not read the lyrics of {}", track.file(), e);
			return null;
		}
	}

	private static SourceDataLine openLine(AudioFormat format) throws Exception {
		SourceDataLine line = AudioSystem.getSourceDataLine(format);
		line.open(format);
		return line;
	}

	/** Escribe las muestras aplicando el volumen (curva cuadrática, que se nota más natural). */
	private void write(SourceDataLine line, short[] samples, int length) {
		float gain = volume * volume;
		byte[] bytes = new byte[length * 2];
		for (int i = 0; i < length; i++) {
			int value = Math.round(samples[i] * gain);
			bytes[i * 2] = (byte) value;
			bytes[i * 2 + 1] = (byte) (value >> 8);
		}
		line.write(bytes, 0, bytes.length);
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
