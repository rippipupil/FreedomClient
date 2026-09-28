package com.freedomclient.ui.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Carpetas de mods que crea el jugador (para ir directo a los que usa) y el orden elegido para la lista de mods.
 * Se guardan en config/freedomclient.json, dentro de "menu".
 */
public final class ModFolders {
	/** Órdenes posibles de la lista de mods; el primero es el de por defecto. */
	public static final List<String> SORTS = List.of("A-Z", "Z-A", "On first", "Category");
	public static final int MAX_FOLDERS = 8;

	private static final Map<String, Set<String>> FOLDERS = new LinkedHashMap<>();
	private static String sort = SORTS.get(0);

	/** Sube con cada cambio (carpetas, mods dentro u orden), para saber cuándo rehacer la lista del menú. */
	private static int version;

	private ModFolders() {
	}

	public static int version() {
		return version;
	}

	public static List<String> names() {
		return new ArrayList<>(FOLDERS.keySet());
	}

	public static boolean create(String name) {
		String clean = name.trim();
		if (clean.isEmpty() || FOLDERS.containsKey(clean) || FOLDERS.size() >= MAX_FOLDERS) return false;
		FOLDERS.put(clean, new LinkedHashSet<>());
		version++;
		return true;
	}

	public static void delete(String name) {
		FOLDERS.remove(name);
		version++;
	}

	public static boolean contains(String folder, String moduleId) {
		Set<String> ids = FOLDERS.get(folder);
		return ids != null && ids.contains(moduleId);
	}

	public static void toggle(String folder, String moduleId) {
		Set<String> ids = FOLDERS.get(folder);
		if (ids == null) return;
		if (!ids.remove(moduleId)) ids.add(moduleId);
		version++;
	}

	public static String sort() {
		return sort;
	}

	public static void setSort(String value) {
		if (SORTS.contains(value)) sort = value;
		version++;
	}

	public static JsonObject save() {
		JsonObject json = new JsonObject();
		json.addProperty("sort", sort);
		JsonObject folders = new JsonObject();
		FOLDERS.forEach((name, ids) -> {
			JsonArray array = new JsonArray();
			ids.forEach(array::add);
			folders.add(name, array);
		});
		json.add("folders", folders);
		return json;
	}

	public static void load(JsonObject json) {
		JsonElement sortValue = json.get("sort");
		if (sortValue != null && sortValue.isJsonPrimitive()) setSort(sortValue.getAsString());
		FOLDERS.clear();
		version++;
		JsonObject folders = json.getAsJsonObject("folders");
		if (folders == null) return;
		for (Map.Entry<String, JsonElement> entry : folders.entrySet()) {
			Set<String> ids = new LinkedHashSet<>();
			if (entry.getValue().isJsonArray()) {
				entry.getValue().getAsJsonArray().forEach(id -> ids.add(id.getAsString()));
			}
			FOLDERS.put(entry.getKey(), ids);
		}
	}
}
