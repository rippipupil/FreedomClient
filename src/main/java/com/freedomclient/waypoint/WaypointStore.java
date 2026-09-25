package com.freedomclient.waypoint;

import com.freedomclient.FreedomClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Guarda los waypoints de cada mundo o servidor en {@code config/freedomclient-waypoints.json}. */
public final class WaypointStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(FreedomClient.MOD_ID + "-waypoints.json");
	private static final Map<String, List<Waypoint>> WAYPOINTS = new HashMap<>();
	private static boolean loaded;

	private WaypointStore() {
	}

	/** Identificador del mundo actual: la IP del servidor o el nombre del mundo de un jugador. */
	public static String currentWorldKey() {
		Minecraft client = Minecraft.getInstance();
		if (client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
			return "singleplayer:" + client.getSingleplayerServer().getWorldData().getLevelName();
		}
		ServerData server = client.getCurrentServer();
		return server != null ? "server:" + server.ip : "unknown";
	}

	public static List<Waypoint> current() {
		load();
		return WAYPOINTS.computeIfAbsent(currentWorldKey(), key -> new ArrayList<>());
	}

	public static void add(Waypoint waypoint) {
		current().add(waypoint);
		save();
	}

	public static void remove(Waypoint waypoint) {
		current().remove(waypoint);
		save();
	}

	private static void load() {
		if (loaded) return;
		loaded = true;
		if (!Files.exists(PATH)) return;
		try (Reader reader = Files.newBufferedReader(PATH)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			for (Map.Entry<String, JsonElement> world : root.entrySet()) {
				List<Waypoint> list = new ArrayList<>();
				for (JsonElement element : world.getValue().getAsJsonArray()) {
					list.add(Waypoint.fromJson(element.getAsJsonObject()));
				}
				WAYPOINTS.put(world.getKey(), list);
			}
		} catch (IOException | RuntimeException e) {
			FreedomClient.LOGGER.error("Could not read {}", PATH, e);
		}
	}

	public static void save() {
		JsonObject root = new JsonObject();
		WAYPOINTS.forEach((world, list) -> {
			JsonArray array = new JsonArray();
			list.forEach(waypoint -> array.add(waypoint.toJson()));
			root.add(world, array);
		});
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException e) {
			FreedomClient.LOGGER.error("Could not save {}", PATH, e);
		}
	}
}
