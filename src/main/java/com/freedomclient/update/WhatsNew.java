package com.freedomclient.update;

import com.freedomclient.FreedomClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Novedades: tras actualizar el cliente, enseña una vez lo nuevo de la versión (assets/freedomclient/changelog.json). */
public final class WhatsNew {
	private static final Path STATE = FabricLoader.getInstance().getConfigDir().resolve("freedomclient-state.json");
	private static boolean checked;

	public record Entry(String title, List<String> items) {
	}

	private WhatsNew() {
	}

	/** Si hay que enseñar las novedades ahora (solo la primera vez que se abre esta versión). */
	public static boolean shouldShow() {
		if (checked) return false;
		checked = true;
		String current = UpdateChecker.currentCommit();
		if (current.isEmpty()) return false;
		String seen = "";
		try {
			if (Files.exists(STATE)) {
				JsonObject json = JsonParser.parseString(Files.readString(STATE)).getAsJsonObject();
				if (json.has("lastSeenCommit")) seen = json.get("lastSeenCommit").getAsString();
			}
		} catch (Exception ignored) {
		}
		if (current.equals(seen)) return false;
		try {
			JsonObject json = new JsonObject();
			json.addProperty("lastSeenCommit", current);
			Files.writeString(STATE, json.toString());
		} catch (Exception e) {
			FreedomClient.LOGGER.warn("Could not save {}", STATE, e);
		}
		return true;
	}

	/** Las entradas del registro de cambios, de la más nueva a la más vieja. */
	public static List<Entry> entries() {
		List<Entry> entries = new ArrayList<>();
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(FreedomClient.id("changelog.json"));
		if (resource.isEmpty()) return entries;
		try (Reader reader = resource.get().openAsReader()) {
			for (JsonElement element : JsonParser.parseReader(reader).getAsJsonArray()) {
				JsonObject object = element.getAsJsonObject();
				List<String> items = new ArrayList<>();
				JsonArray array = object.getAsJsonArray("items");
				for (JsonElement item : array) items.add(item.getAsString());
				entries.add(new Entry(object.get("title").getAsString(), items));
			}
		} catch (Exception e) {
			FreedomClient.LOGGER.warn("Could not read the changelog", e);
		}
		return entries;
	}
}
