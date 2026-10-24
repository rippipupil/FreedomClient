package com.freedomclient.music;

import org.jcodec.codecs.aac.AACDecoder;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.model.AudioBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/**
 * m4a con AAC: lee las cajas del MP4 a mano (el demuxer de jcodec no entiende los m4a fragmentados que da YouTube y
 * baja FreedomPlayer: moov vacío y luego moof+mdat) y descodifica cada trama con el AAC de jcodec (JAAD).
 * <p>
 * Sirve para los dos tipos: el normal (tablas stsz/stco/stsc del moov) y el fragmentado (tfhd/trun de cada moof).
 */
final class Mp4AacSource implements PcmSource {
	private final FileChannel channel;
	private final AACDecoder decoder;
	private long[] offsets = new long[1024];
	private int[] sizes = new int[1024];
	private int frames;
	private int next;
	private long durationMs;
	private short[] samples = new short[0];

	// Lo que se saca del moov de la pista de audio.
	private int trackId = -1;
	private long timescale;
	private long trackDuration;
	private byte[] decoderConfig;
	private int[] sampleSizes;
	private int fixedSize;
	private long[] chunkOffsets;
	private int[] stscFirst, stscCount;
	private long defaultSize, defaultDuration;
	private long fragmentTicks;
	/** Muestras de relleno del principio que se quitan, en unidades de la pista. */
	private long primingTicks;
	private boolean primed;

	Mp4AacSource(Path file) throws IOException {
		channel = FileChannel.open(file, StandardOpenOption.READ);
		try {
			index();
			if (decoderConfig == null) throw new IOException("No AAC track");
			decoder = new AACDecoder(ByteBuffer.wrap(decoderConfig));
		} catch (IOException | RuntimeException e) {
			channel.close();
			throw e;
		}
	}

	@Override
	public long durationMs() {
		return durationMs;
	}

	@Override
	public Pcm next() throws IOException {
		while (next < frames) {
			int size = sizes[next];
			ByteBuffer frame = ByteBuffer.allocate(size);
			read(frame, offsets[next]);
			next++;
			frame.flip();
			AudioBuffer buffer;
			try {
				buffer = decoder.decodeFrame(frame, ByteBuffer.allocate(8192 * 4));
			} catch (Exception e) {
				// Una trama rota se salta (como hacen los reproductores) en vez de parar la canción.
				continue;
			}
			AudioFormat format = buffer.getFormat();
			ByteBuffer data = buffer.getData();
			int bytes = format.getSampleSizeInBits() / 8;
			if (bytes != 2) throw new IOException("Unsupported AAC output: " + format.getSampleSizeInBits() + " bits");
			int count = data.remaining() / 2;
			if (samples.length < count) samples = new short[count];
			boolean big = format.isBigEndian();
			int base = data.position();
			for (int i = 0; i < count; i++) {
				int a = data.get(base + i * 2) & 0xFF, b = data.get(base + i * 2 + 1) & 0xFF;
				samples[i] = (short) (big ? a << 8 | b : b << 8 | a);
			}
			int channels = Math.max(1, format.getChannels());
			if (!primed && primingTicks > 0 && timescale > 0) {
				int skip = (int) Math.min((long) count / channels, primingTicks * format.getSampleRate() / timescale);
				primingTicks -= skip * timescale / format.getSampleRate();
				if (primingTicks <= 0 || skip == 0) primed = true;
				System.arraycopy(samples, skip * channels, samples, 0, count - skip * channels);
				count -= skip * channels;
			}
			if (count == 0) continue;
			return new Pcm(samples, count, format.getSampleRate(), format.getChannels());
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Índice de tramas

	private void index() throws IOException {
		long size = channel.size();
		long position = 0;
		while (position + 8 <= size) {
			long[] header = header(position, size);
			long start = position, end = start + header[1];
			int type = (int) header[0];
			long body = start + header[2];
			if (type == box("moov")) moov(body, end);
			else if (type == box("moof")) moof(start, body, end);
			position = end;
		}
		if (trackId < 0) return;
		if (sampleSizes != null || fixedSize > 0) tables();
		if (timescale > 0) {
			long ticks = trackDuration > 0 ? trackDuration : fragmentTicks;
			durationMs = ticks * 1000 / timescale;
		}
	}

	/** Cabecera de caja: {tipo, tamaño total, tamaño de la cabecera}. */
	private long[] header(long position, long limit) throws IOException {
		ByteBuffer buffer = read(position, 16);
		long length = buffer.getInt() & 0xFFFFFFFFL;
		int type = buffer.getInt();
		int headerSize = 8;
		if (length == 1) {
			length = buffer.getLong();
			headerSize = 16;
		} else if (length == 0) {
			length = limit - position;
		}
		if (length < headerSize) throw new IOException("Broken MP4 box");
		return new long[]{type, Math.min(length, limit - position), headerSize};
	}

	private void moov(long position, long end) throws IOException {
		while (position + 8 <= end) {
			long[] header = header(position, end);
			int type = (int) header[0];
			long body = position + header[2], next = position + header[1];
			if (type == box("trak") && trackId < 0) trak(body, next);
			else if (type == box("mvex")) mvex(body, next);
			position = next;
		}
	}

	private void mvex(long position, long end) throws IOException {
		while (position + 8 <= end) {
			long[] header = header(position, end);
			if ((int) header[0] == box("trex")) {
				ByteBuffer trex = read(position + header[2], 24);
				trex.getInt(); // versión y banderas
				int id = trex.getInt();
				trex.getInt(); // descripción
				long duration = trex.getInt() & 0xFFFFFFFFL, size = trex.getInt() & 0xFFFFFFFFL;
				if (id == trackId || trackId < 0) {
					defaultDuration = duration;
					defaultSize = size;
				}
			}
			position += header[1];
		}
	}

	/** Una pista: se queda con ella si es de sonido con AAC. */
	private void trak(long position, long end) throws IOException {
		int id = -1;
		Track track = new Track();
		while (position + 8 <= end) {
			long[] header = header(position, end);
			int type = (int) header[0];
			long body = position + header[2], next = position + header[1];
			if (type == box("tkhd")) {
				ByteBuffer tkhd = read(body, 24);
				int version = tkhd.get() & 0xFF;
				tkhd.position(version == 1 ? 20 : 12);
				id = tkhd.getInt();
			} else if (type == box("mdia") || type == box("edts")) {
				walk(body, next, track);
			}
			position = next;
		}
		if (track.sound && track.config != null) {
			trackId = id;
			timescale = track.timescale;
			trackDuration = track.duration;
			decoderConfig = track.config;
			primingTicks = track.priming;
		}
	}

	private static final class Track {
		boolean sound;
		long timescale, duration, priming;
		byte[] config;
	}

	/** Recorre mdia/minf/stbl buscando lo que hace falta. */
	private void walk(long position, long end, Track track) throws IOException {
		while (position + 8 <= end) {
			long[] header = header(position, end);
			int type = (int) header[0];
			long body = position + header[2], next = position + header[1];
			if (type == box("minf") || type == box("stbl")) {
				walk(body, next, track);
			} else if (type == box("mdhd")) {
				ByteBuffer mdhd = read(body, 36);
				int version = mdhd.get() & 0xFF;
				if (version == 1) {
					mdhd.position(20);
					track.timescale = mdhd.getInt() & 0xFFFFFFFFL;
					track.duration = mdhd.getLong();
				} else {
					mdhd.position(12);
					track.timescale = mdhd.getInt() & 0xFFFFFFFFL;
					track.duration = mdhd.getInt() & 0xFFFFFFFFL;
				}
			} else if (type == box("elst")) {
				// El retraso del codificador: las primeras muestras (1024 con el AAC normal) son de relleno.
				ByteBuffer elst = read(body, 24);
				int version = elst.get() & 0xFF;
				elst.position(4);
				if (elst.getInt() > 0) {
					long time = version == 1 ? elst.getLong(16) : elst.getInt(12);
					if (time > 0) track.priming = time;
				}
			} else if (type == box("hdlr")) {
				ByteBuffer hdlr = read(body, 12);
				hdlr.position(8);
				track.sound = hdlr.getInt() == box("soun");
			} else if (type == box("stsd")) {
				track.config = esds(read(body, (int) Math.min(4096, next - body)));
			} else if (type == box("stsz")) {
				ByteBuffer stsz = read(body, (int) (next - body));
				stsz.getInt();
				int fixed = stsz.getInt(), count = stsz.getInt();
				if (fixed != 0) {
					fixedSize = fixed;
					sampleSizes = new int[count];
					Arrays.fill(sampleSizes, fixed);
				} else {
					sampleSizes = new int[count];
					for (int i = 0; i < count; i++) sampleSizes[i] = stsz.getInt();
				}
			} else if (type == box("stco") || type == box("co64")) {
				ByteBuffer stco = read(body, (int) (next - body));
				stco.getInt();
				int count = stco.getInt();
				chunkOffsets = new long[count];
				for (int i = 0; i < count; i++) chunkOffsets[i] = type == box("co64") ? stco.getLong() : stco.getInt() & 0xFFFFFFFFL;
			} else if (type == box("stsc")) {
				ByteBuffer stsc = read(body, (int) (next - body));
				stsc.getInt();
				int count = stsc.getInt();
				stscFirst = new int[count];
				stscCount = new int[count];
				for (int i = 0; i < count; i++) {
					stscFirst[i] = stsc.getInt();
					stscCount[i] = stsc.getInt();
					stsc.getInt();
				}
			}
			position = next;
		}
	}

	/** La configuración del descodificador (DecoderSpecificInfo, etiqueta 5) dentro de stsd/mp4a/esds. */
	private static byte[] esds(ByteBuffer stsd) {
		byte[] bytes = new byte[stsd.remaining()];
		stsd.get(bytes);
		for (int i = 0; i + 4 < bytes.length; i++) {
			if (bytes[i] != 'e' || bytes[i + 1] != 's' || bytes[i + 2] != 'd' || bytes[i + 3] != 's') continue;
			int p = i + 8; // tipo + versión y banderas
			while (p < bytes.length) {
				int tag = bytes[p++] & 0xFF;
				int length = 0;
				for (int k = 0; k < 4 && p < bytes.length; k++) {
					int b = bytes[p++] & 0xFF;
					length = length << 7 | b & 0x7F;
					if ((b & 0x80) == 0) break;
				}
				if (tag == 0x03) {
					p += 2; // ES_ID
					int flags = bytes[p++] & 0xFF;
					if ((flags & 0x80) != 0) p += 2;
					if ((flags & 0x40) != 0) p += 1 + (bytes[p] & 0xFF);
					if ((flags & 0x20) != 0) p += 2;
				} else if (tag == 0x04) {
					p += 13;
				} else if (tag == 0x05) {
					return Arrays.copyOfRange(bytes, p, Math.min(bytes.length, p + length));
				} else {
					p += length;
				}
			}
			return null;
		}
		return null;
	}

	/** Las tramas de un m4a normal: cada trozo (chunk) tiene sus tramas seguidas. */
	private void tables() {
		if (sampleSizes == null || chunkOffsets == null || stscFirst == null) return;
		int sample = 0;
		for (int chunk = 0; chunk < chunkOffsets.length && sample < sampleSizes.length; chunk++) {
			int perChunk = 0;
			for (int i = 0; i < stscFirst.length; i++) if (stscFirst[i] <= chunk + 1) perChunk = stscCount[i];
			long offset = chunkOffsets[chunk];
			for (int k = 0; k < perChunk && sample < sampleSizes.length; k++) {
				add(offset, sampleSizes[sample]);
				offset += sampleSizes[sample++];
			}
		}
	}

	/** Un fragmento (moof): sus tramas están en el mdat de después, donde digan tfhd y trun. */
	private void moof(long start, long position, long end) throws IOException {
		while (position + 8 <= end) {
			long[] header = header(position, end);
			if ((int) header[0] == box("traf")) traf(start, position + header[2], position + header[1]);
			position += header[1];
		}
	}

	private void traf(long moof, long position, long end) throws IOException {
		long base = moof;
		long size = defaultSize, duration = defaultDuration;
		boolean mine = true;
		while (position + 8 <= end) {
			long[] header = header(position, end);
			int type = (int) header[0];
			long body = position + header[2], next = position + header[1];
			if (type == box("tfhd")) {
				ByteBuffer tfhd = read(body, (int) (next - body));
				int flags = tfhd.getInt() & 0xFFFFFF;
				mine = trackId < 0 || tfhd.getInt() == trackId;
				if ((flags & 0x01) != 0) base = tfhd.getLong();
				if ((flags & 0x02) != 0) tfhd.getInt();
				if ((flags & 0x08) != 0) duration = tfhd.getInt() & 0xFFFFFFFFL;
				if ((flags & 0x10) != 0) size = tfhd.getInt() & 0xFFFFFFFFL;
			} else if (type == box("trun") && mine) {
				ByteBuffer trun = read(body, (int) (next - body));
				int flags = trun.getInt() & 0xFFFFFF;
				int count = trun.getInt();
				long offset = base + ((flags & 0x01) != 0 ? trun.getInt() : 0);
				if ((flags & 0x04) != 0) trun.getInt();
				for (int i = 0; i < count; i++) {
					long sampleDuration = (flags & 0x100) != 0 ? trun.getInt() & 0xFFFFFFFFL : duration;
					long sampleSize = (flags & 0x200) != 0 ? trun.getInt() & 0xFFFFFFFFL : size;
					if ((flags & 0x400) != 0) trun.getInt();
					if ((flags & 0x800) != 0) trun.getInt();
					add(offset, (int) sampleSize);
					offset += sampleSize;
					fragmentTicks += sampleDuration;
				}
			}
			position = next;
		}
	}

	private void add(long offset, int size) {
		if (size <= 0) return;
		if (frames == offsets.length) {
			offsets = Arrays.copyOf(offsets, frames * 2);
			sizes = Arrays.copyOf(sizes, frames * 2);
		}
		offsets[frames] = offset;
		sizes[frames++] = size;
	}

	private ByteBuffer read(long position, int length) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(Math.max(0, (int) Math.min(length, channel.size() - position)));
		read(buffer, position);
		buffer.flip();
		// Las cajas cortas se rellenan con ceros para poder leer la cabecera entera.
		if (buffer.remaining() < length) {
			ByteBuffer padded = ByteBuffer.allocate(length);
			padded.put(buffer);
			padded.flip();
			padded.limit(length);
			return padded;
		}
		return buffer;
	}

	private void read(ByteBuffer buffer, long position) throws IOException {
		while (buffer.hasRemaining()) {
			int read = channel.read(buffer, position);
			if (read < 0) break;
			position += read;
		}
	}

	private static int box(String type) {
		return type.charAt(0) << 24 | type.charAt(1) << 16 | type.charAt(2) << 8 | type.charAt(3);
	}
}
