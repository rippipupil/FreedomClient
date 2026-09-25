package com.freedomclient.module.utility;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.NumberSetting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

/** Borra los logs antiguos de la carpeta logs al abrir el juego para no acumular cientos de archivos. */
public class LogCleanerModule extends Module {
	private final NumberSetting keepDays = add(new NumberSetting("Keep days", "Delete logs older than this.", 7, 1, 90, 1, " days"));

	public LogCleanerModule() {
		super("Log Cleaner", "Deletes old log files when the game starts.", Category.UTILITY, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** Llamado una vez al iniciar el cliente. */
	public void clean() {
		if (!isEnabled()) return;
		Path logs = FabricLoader.getInstance().getGameDir().resolve("logs");
		if (!Files.isDirectory(logs)) return;

		Instant limit = Instant.now().minus(keepDays.getInt(), ChronoUnit.DAYS);
		int deleted = 0;
		try (Stream<Path> files = Files.list(logs)) {
			for (Path file : files.toList()) {
				String name = file.getFileName().toString();
				if (!name.endsWith(".log.gz") && !name.endsWith(".log")) continue;
				if (name.equals("latest.log") || name.equals("debug.log")) continue;
				if (Files.getLastModifiedTime(file).toInstant().isBefore(limit)) {
					Files.deleteIfExists(file);
					deleted++;
				}
			}
		} catch (IOException e) {
			FreedomClient.LOGGER.warn("Could not clean old logs", e);
		}
		if (deleted > 0) FreedomClient.LOGGER.info("Deleted {} old log files", deleted);
	}
}
