package com.freedomclient.music;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Una canción descodificada a trozos: muestras de 16 bits intercaladas por canal. La usan los formatos que no lee
 * Java Sound ni JLayer (m4a/AAC y webm/Opus, los que baja FreedomPlayer).
 */
interface PcmSource extends AutoCloseable {
	/** Un trozo de audio: {@code length} muestras intercaladas (todas las de cada canal seguidas por instante). */
	record Pcm(short[] samples, int length, int sampleRate, int channels) {
	}

	/** El siguiente trozo, o null al acabar la canción. */
	Pcm next() throws IOException;

	/** Lo que dura según el contenedor, o 0 si no lo dice. */
	long durationMs();

	@Override
	void close() throws IOException;

	static boolean supports(String name) {
		String lower = name.toLowerCase(Locale.ROOT);
		return lower.endsWith(".m4a") || lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".weba");
	}

	static PcmSource open(Path file) throws IOException {
		String lower = file.getFileName().toString().toLowerCase(Locale.ROOT);
		if (lower.endsWith(".webm") || lower.endsWith(".weba")) return new WebmOpusSource(file);
		return new Mp4AacSource(file);
	}
}
