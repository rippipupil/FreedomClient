package com.freedomclient.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Cliente mínimo del IPC local de Discord (el que usan todos los Rich Presence).
 * Cada mensaje es: código (int32 LE), longitud (int32 LE) y JSON en UTF-8.
 * En Windows es una tubería con nombre; en Linux y macOS, un socket Unix.
 */
public final class DiscordIpc implements Closeable {
	private static final int OP_HANDSHAKE = 0;
	private static final int OP_FRAME = 1;

	private final Transport transport;

	private DiscordIpc(Transport transport) {
		this.transport = transport;
	}

	/** Conecta con Discord y hace el saludo. Lanza IOException si Discord no está abierto. */
	public static DiscordIpc connect(String applicationId) throws IOException {
		IOException last = new IOException("Discord is not running");
		for (int i = 0; i < 10; i++) {
			Transport transport;
			try {
				transport = openTransport(i);
			} catch (IOException e) {
				last = e;
				continue;
			}
			DiscordIpc ipc = new DiscordIpc(transport);
			try {
				JsonObject hello = new JsonObject();
				hello.addProperty("v", 1);
				hello.addProperty("client_id", applicationId);
				ipc.send(OP_HANDSHAKE, hello);
				ipc.read();
				return ipc;
			} catch (IOException e) {
				ipc.close();
				last = e;
			}
		}
		throw last;
	}

	public void setActivity(JsonObject activity) throws IOException {
		JsonObject args = new JsonObject();
		args.addProperty("pid", ProcessHandle.current().pid());
		if (activity != null) args.add("activity", activity);
		JsonObject command = new JsonObject();
		command.addProperty("cmd", "SET_ACTIVITY");
		command.add("args", args);
		command.addProperty("nonce", UUID.randomUUID().toString());
		send(OP_FRAME, command);
		read();
	}

	private void send(int opcode, JsonObject json) throws IOException {
		byte[] payload = json.toString().getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.allocate(8 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(opcode).putInt(payload.length).put(payload).flip();
		transport.write(buffer);
	}

	private JsonObject read() throws IOException {
		ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
		transport.readFully(header);
		header.flip();
		int opcode = header.getInt();
		int length = header.getInt();
		if (length < 0 || length > 1 << 20) throw new IOException("Bad Discord IPC frame");
		ByteBuffer body = ByteBuffer.allocate(length);
		transport.readFully(body);
		JsonObject json = JsonParser.parseString(new String(body.array(), StandardCharsets.UTF_8)).getAsJsonObject();
		if (opcode == 2) throw new IOException("Discord closed the connection: " + json);
		return json;
	}

	@Override
	public void close() {
		try {
			transport.close();
		} catch (IOException ignored) {
		}
	}

	private static Transport openTransport(int index) throws IOException {
		String name = "discord-ipc-" + index;
		if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
			return new PipeTransport(new RandomAccessFile("\\\\.\\pipe\\" + name, "rw"));
		}
		for (Path dir : unixDirectories()) {
			Path socket = dir.resolve(name);
			if (Files.exists(socket)) {
				return new SocketTransport(SocketChannel.open(UnixDomainSocketAddress.of(socket)));
			}
		}
		throw new IOException("No Discord socket " + name);
	}

	/** Carpetas donde Discord crea su socket, incluidas las versiones Flatpak y Snap. */
	private static List<Path> unixDirectories() {
		List<Path> dirs = new ArrayList<>();
		for (String variable : new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) {
			String value = System.getenv(variable);
			if (value != null && !value.isEmpty()) {
				Path base = Path.of(value);
				dirs.add(base);
				dirs.add(base.resolve("app/com.discordapp.Discord"));
				dirs.add(base.resolve("snap.discord"));
			}
		}
		dirs.add(Path.of("/tmp"));
		return dirs;
	}

	private interface Transport extends Closeable {
		void write(ByteBuffer buffer) throws IOException;

		void readFully(ByteBuffer buffer) throws IOException;
	}

	private record SocketTransport(SocketChannel channel) implements Transport {
		@Override
		public void write(ByteBuffer buffer) throws IOException {
			while (buffer.hasRemaining()) channel.write(buffer);
		}

		@Override
		public void readFully(ByteBuffer buffer) throws IOException {
			while (buffer.hasRemaining()) {
				if (channel.read(buffer) < 0) throw new IOException("Discord closed the socket");
			}
		}

		@Override
		public void close() throws IOException {
			channel.close();
		}
	}

	private record PipeTransport(RandomAccessFile pipe) implements Transport {
		@Override
		public void write(ByteBuffer buffer) throws IOException {
			pipe.write(buffer.array(), buffer.position(), buffer.remaining());
			buffer.position(buffer.limit());
		}

		@Override
		public void readFully(ByteBuffer buffer) throws IOException {
			pipe.readFully(buffer.array(), buffer.position(), buffer.remaining());
			buffer.position(buffer.limit());
		}

		@Override
		public void close() throws IOException {
			pipe.close();
		}
	}
}
