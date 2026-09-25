package com.freedomclient.config;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.Setting;
import com.freedomclient.ui.theme.ThemeManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Guarda los módulos, sus ajustes y el tema en {@code config/freedomclient.json}. */
public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(FreedomClient.MOD_ID + ".json");

	private Config() {
	}

	public static void load(ModuleManager manager) {
		if (!Files.exists(PATH)) {
			save(manager);
			return;
		}

		try (Reader reader = Files.newBufferedReader(PATH)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

			JsonObject modules = root.getAsJsonObject("modules");
			if (modules != null) {
				for (Module module : manager.getModules()) {
					JsonElement data = modules.get(module.getId());
					if (data != null && data.isJsonObject()) {
						loadModule(module, data.getAsJsonObject());
					}
				}
			}

			JsonObject theme = root.getAsJsonObject("theme");
			if (theme != null) {
				ThemeManager.load(theme);
			}
		} catch (IOException | RuntimeException e) {
			FreedomClient.LOGGER.error("Could not read {}", PATH, e);
		}
	}

	private static void loadModule(Module module, JsonObject data) {
		JsonElement favorite = data.get("favorite");
		if (favorite != null && favorite.isJsonPrimitive()) {
			module.loadFavorite(favorite.getAsBoolean());
		}
		JsonElement enabled = data.get("enabled");
		if (enabled != null && enabled.isJsonPrimitive()) {
			module.loadEnabled(enabled.getAsBoolean());
		}

		JsonObject settings = data.getAsJsonObject("settings");
		if (settings == null) return;

		for (Setting<?> setting : module.getSettings()) {
			JsonElement value = settings.get(setting.getName());
			if (value != null && setting.isSaved()) {
				setting.fromJson(value);
			}
		}
	}

	public static void save(ModuleManager manager) {
		if (manager == null) return;

		JsonObject modules = new JsonObject();
		for (Module module : manager.getModules()) {
			JsonObject settings = new JsonObject();
			for (Setting<?> setting : module.getSettings()) {
				if (setting.isSaved()) {
					settings.add(setting.getName(), setting.toJson());
				}
			}

			JsonObject data = new JsonObject();
			data.addProperty("enabled", module.isEnabled());
			if (module.isFavorite()) data.addProperty("favorite", true);
			data.add("settings", settings);
			modules.add(module.getId(), data);
		}

		JsonObject root = new JsonObject();
		root.add("modules", modules);
		root.add("theme", ThemeManager.save());

		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException e) {
			FreedomClient.LOGGER.error("Could not save {}", PATH, e);
		}
	}

	/** Guarda la config actual (atajo para cambios hechos desde el menú). */
	public static void save() {
		save(FreedomClient.getModuleManager());
	}
}
