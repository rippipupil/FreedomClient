package com.freedomclient.config;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
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

/** Guarda qué módulos están activados en {@code config/freedomclient.json}. */
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
			if (modules == null) return;

			for (Module module : manager.getModules()) {
				JsonElement enabled = modules.get(module.getName());
				if (enabled != null && enabled.isJsonPrimitive()) {
					module.loadEnabled(enabled.getAsBoolean());
				}
			}
		} catch (IOException | RuntimeException e) {
			FreedomClient.LOGGER.error("No se pudo leer {}", PATH, e);
		}
	}

	public static void save(ModuleManager manager) {
		JsonObject modules = new JsonObject();
		for (Module module : manager.getModules()) {
			modules.addProperty(module.getName(), module.isEnabled());
		}

		JsonObject root = new JsonObject();
		root.add("modules", modules);

		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException e) {
			FreedomClient.LOGGER.error("No se pudo guardar {}", PATH, e);
		}
	}
}
