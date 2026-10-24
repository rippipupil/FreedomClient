package com.freedomclient.music;

import io.github.jaredmdobson.concentus.OpusDecoder;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * webm con Opus (lo que baja FreedomPlayer de YouTube cuando no hay m4a): lee el Matroska/EBML a mano, de principio a
 * fin, y descodifica cada bloque con Concentus a 48 kHz, saltando las muestras de «pre-skip» del OpusHead.
 */
final class WebmOpusSource implements PcmSource {
	private static final int SEGMENT = 0x18538067, INFO = 0x1549A966, TIMECODE_SCALE = 0x2AD7B1, DURATION = 0x4489;
	private static final int TRACKS = 0x1654AE6B, TRACK_ENTRY = 0xAE, TRACK_NUMBER = 0xD7, CODEC_ID = 0x86, CODEC_PRIVATE = 0x63A2;
	private static final int AUDIO = 0xE1, CHANNELS = 0x9F, CLUSTER = 0x1F43B675, SIMPLE_BLOCK = 0xA3, BLOCK_GROUP = 0xA0, BLOCK = 0xA1;
	private static final int RATE = 48000;
	/** Lo más largo que puede durar un paquete Opus: 120 ms. */
	private static final int MAX_FRAME = RATE * 120 / 1000;

	private final InputStream in;
	private long position;
	private final long size;
	private OpusDecoder decoder;
	private int track = -1;
	private int channels = 2;
	private int preSkip;
	private long durationMs;
	private short[] samples = new short[MAX_FRAME * 2];
	/** Paquetes de un bloque con varios (lacing) que quedan por descodificar. */
	private byte[][] pending = new byte[0][];
	private int pendingIndex;

	WebmOpusSource(Path file) throws IOException {
		size = Files.size(file);
		in = new BufferedInputStream(Files.newInputStream(file), 1 << 16);
		try {
			// Cabecera: hasta llegar al primer Cluster se conocen la pista, los canales y la duración.
			while (decoder == null) {
				if (!element(true)) throw new IOException("No Opus track");
			}
		} catch (IOException | RuntimeException e) {
			in.close();
			throw e;
		}
	}

	@Override
	public long durationMs() {
		return durationMs;
	}

	@Override
	public Pcm next() throws IOException {
		while (true) {
			while (pendingIndex < pending.length) {
				byte[] packet = pending[pendingIndex++];
				int decoded;
				try {
					decoded = decoder.decode(packet, 0, packet.length, samples, 0, MAX_FRAME, false);
				} catch (Exception e) {
					continue;
				}
				int skip = Math.min(preSkip, decoded);
				preSkip -= skip;
				if (decoded - skip <= 0) continue;
				if (skip > 0) System.arraycopy(samples, skip * channels, samples, 0, (decoded - skip) * channels);
				return new Pcm(samples, (decoded - skip) * channels, RATE, channels);
			}
			if (!element(false)) return null;
		}
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

	// ---------------------------------------------------------------------------------------------------------------
	// EBML

	private String codec;
	private byte[] codecPrivate;
	private int entryNumber, entryChannels;
	private long timecodeScale = 1_000_000;
	private double duration;

	/**
	 * Lee un elemento. Los contenedores que interesan (Segment, Cluster, Tracks…) no se saltan: se entra en ellos y se
	 * siguen leyendo sus hijos uno a uno (así da igual que tengan tamaño desconocido). Devuelve false al acabar el archivo.
	 */
	private boolean element(boolean header) throws IOException {
		if (position >= size) {
			finishEntry();
			return false;
		}
		int id;
		long length;
		try {
			id = readId();
			length = readSize();
		} catch (EOFException e) {
			finishEntry();
			return false;
		}
		switch (id) {
			case SEGMENT, INFO, TRACKS, CLUSTER, BLOCK_GROUP, AUDIO -> {
				if (id == CLUSTER) finishEntry();
				return true;
			}
			case TRACK_ENTRY -> {
				finishEntry();
				codec = null;
				codecPrivate = null;
				entryNumber = 0;
				entryChannels = 0;
				return true;
			}
			case TIMECODE_SCALE -> timecodeScale = readUnsigned(length);
			case DURATION -> {
				byte[] bytes = readBytes(length);
				long bits = 0;
				for (byte b : bytes) bits = bits << 8 | b & 0xFF;
				duration = length == 4 ? Float.intBitsToFloat((int) bits) : Double.longBitsToDouble(bits);
				durationMs = (long) (duration * timecodeScale / 1_000_000.0);
			}
			case TRACK_NUMBER -> entryNumber = (int) readUnsigned(length);
			case CODEC_ID -> codec = new String(readBytes(length)).trim();
			case CODEC_PRIVATE -> codecPrivate = readBytes(length);
			case CHANNELS -> entryChannels = (int) readUnsigned(length);
			case SIMPLE_BLOCK, BLOCK -> {
				byte[] block = readBytes(length);
				if (!header) block(block);
			}
			default -> skip(length);
		}
		return true;
	}

	/** Al acabar una pista: si es la primera de Opus, se prepara el descodificador. */
	private void finishEntry() {
		if (decoder != null || codec == null || !codec.equals("A_OPUS")) {
			codec = null;
			return;
		}
		int count = entryChannels > 0 ? entryChannels : 2;
		if (codecPrivate != null && codecPrivate.length >= 19) {
			count = codecPrivate[9] & 0xFF;
			preSkip = codecPrivate[10] & 0xFF | (codecPrivate[11] & 0xFF) << 8;
		}
		channels = Math.max(1, Math.min(2, count));
		try {
			decoder = new OpusDecoder(RATE, channels);
		} catch (Exception e) {
			throw new IllegalStateException("Could not start the Opus decoder", e);
		}
		track = entryNumber;
		codec = null;
	}

	/** Un bloque: número de pista, tiempo, banderas y uno o varios paquetes (según el «lacing»). */
	private void block(byte[] block) {
		int[] p = {0};
		long number = vint(block, p, true);
		if (number != track || p[0] + 3 > block.length) return;
		p[0] += 2; // tiempo relativo al Cluster
		int flags = block[p[0]++] & 0xFF;
		int lacing = flags >> 1 & 3;
		if (lacing == 0) {
			pending = new byte[][]{slice(block, p[0], block.length)};
		} else {
			int count = (block[p[0]++] & 0xFF) + 1;
			int[] lengths = new int[count];
			int known = 0;
			if (lacing == 1) { // Xiph
				for (int i = 0; i < count - 1; i++) {
					int value, total = 0;
					do {
						value = block[p[0]++] & 0xFF;
						total += value;
					} while (value == 255);
					lengths[i] = total;
					known += total;
				}
			} else if (lacing == 3) { // EBML
				lengths[0] = (int) vint(block, p, true);
				known = lengths[0];
				for (int i = 1; i < count - 1; i++) {
					int start = p[0];
					long raw = vint(block, p, true);
					int bytes = p[0] - start;
					long bias = (1L << (7 * bytes - 1)) - 1;
					lengths[i] = (int) (lengths[i - 1] + raw - bias);
					known += lengths[i];
				}
			} else { // tamaño fijo
				int each = (block.length - p[0]) / count;
				for (int i = 0; i < count - 1; i++) lengths[i] = each;
				known = each * (count - 1);
			}
			lengths[count - 1] = block.length - p[0] - known;
			pending = new byte[count][];
			for (int i = 0; i < count; i++) {
				pending[i] = slice(block, p[0], p[0] + Math.max(0, lengths[i]));
				p[0] += lengths[i];
			}
		}
		pendingIndex = 0;
	}

	private static byte[] slice(byte[] bytes, int from, int to) {
		to = Math.min(to, bytes.length);
		byte[] out = new byte[Math.max(0, to - from)];
		System.arraycopy(bytes, from, out, 0, out.length);
		return out;
	}

	/** Entero de longitud variable de EBML; {@code strip} quita el bit que marca la longitud (tamaños y números). */
	private static long vint(byte[] bytes, int[] p, boolean strip) {
		int first = bytes[p[0]++] & 0xFF;
		int length = Integer.numberOfLeadingZeros(first) - 23;
		long value = strip ? first & (0xFF >> length) : first;
		for (int i = 1; i < length; i++) value = value << 8 | bytes[p[0]++] & 0xFF;
		return value;
	}

	private int readId() throws IOException {
		int first = read();
		int length = Integer.numberOfLeadingZeros(first) - 23;
		if (length < 1 || length > 4) throw new IOException("Broken EBML id");
		int value = first;
		for (int i = 1; i < length; i++) value = value << 8 | read();
		return value;
	}

	/** Tamaño de un elemento; -1 si es «desconocido» (todos los bits a 1). */
	private long readSize() throws IOException {
		int first = read();
		int length = Integer.numberOfLeadingZeros(first) - 23;
		if (length < 1 || length > 8) throw new IOException("Broken EBML size");
		long value = first & (0xFF >> length);
		boolean unknown = value == (0xFF >> length);
		for (int i = 1; i < length; i++) {
			int b = read();
			value = value << 8 | b;
			if (b != 0xFF) unknown = false;
		}
		return unknown ? -1 : value;
	}

	private long readUnsigned(long length) throws IOException {
		long value = 0;
		for (long i = 0; i < length; i++) value = value << 8 | read();
		return value;
	}

	private byte[] readBytes(long length) throws IOException {
		if (length < 0 || length > 64 << 20) throw new IOException("Broken EBML element");
		byte[] bytes = new byte[(int) length];
		int done = 0;
		while (done < bytes.length) {
			int read = in.read(bytes, done, bytes.length - done);
			if (read < 0) throw new EOFException();
			done += read;
		}
		position += length;
		return bytes;
	}

	private void skip(long length) throws IOException {
		if (length < 0) return;
		long left = length;
		while (left > 0) {
			long skipped = in.skip(left);
			if (skipped <= 0) {
				if (in.read() < 0) throw new EOFException();
				skipped = 1;
			}
			left -= skipped;
		}
		position += length;
	}

	private int read() throws IOException {
		int b = in.read();
		if (b < 0) throw new EOFException();
		position++;
		return b;
	}
}
