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
 * Reproductor de música local: toca los MP3 y WAV de la carpeta .minecraft/freedomclient/music
 * en un hilo aparte (MP3 con JLayer, WAV con Java Sound), con pausa, siguiente, anterior y volumen.
 */
public final class LocalMusicPlayer {
	private final Path folder = FabricLoader.getInstance().getGameDir().resolve(FreedomClient.MOD_ID).resolve("music");
	private final List<Path> playlist = new ArrayList<>();
	private int index = -1;
	private Thread thread;
	private volatile boolean paused;
	private volatile boolean skip;
	private volatile boolean stopped = true;
	private volatile float volume = 0.6F;
	private volatile long positionMs;
	private volatile long durationMs;
	private volatile String title;
	private boolean shuffle;

	public Path folder() {
		return folder;
	}

	/** Vuelve a leer la carpeta de música. Devuelve cuántas canciones hay. */
	public synchronized int reload() {
		playlist.clear();
		try {
			Files.createDirectories(folder);
			try (Stream<Path> files = Files.list(folder)) {
				files.filter(file -> {
					String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
					return name.endsWith(".mp3") || name.endsWith(".wav");
				}).sorted().forEach(playlist::add);
			}
		} catch (IOException e) {
			FreedomClient.LOGGER.warn("Could not read the music folder {}", folder, e);
		}
		if (shuffle) Collections.shuffle(playlist);
		return playlist.size();
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

	/** Hilo de música: toca la canción actual y pasa a la siguiente al acabar. */
	private void run() {
		while (!stopped) {
			Path song;
			synchronized (this) {
				if (playlist.isEmpty()) break;
				index = Math.floorMod(index, playlist.size());
				song = playlist.get(index);
			}
			skip = false;
			String name = song.getFileName().toString();
			title = name.substring(0, name.lastIndexOf('.'));
			positionMs = 0;
			durationMs = 0;
			try {
				if (name.toLowerCase(Locale.ROOT).endsWith(".mp3")) playMp3(song);
				else playWav(song);
			} catch (Exception e) {
				FreedomClient.LOGGER.warn("Could not play {}", song, e);
				sleep(500);
			}
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
