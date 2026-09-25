package com.freedomclient.module.utility;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.ui.scene.CrashGuardScreen;
import net.minecraft.CrashReport;
import net.minecraft.ReportType;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Crash Guard: cuando el juego crashea mientras juegas, guarda el informe del crasheo, sale del mundo
 * guardándolo y te devuelve al menú en vez de cerrar Minecraft. Si vuelve a crashear enseguida,
 * deja que el juego se cierre como siempre para no quedarse en un bucle.
 */
public class CrashGuardModule extends Module {
	private static final long REPEAT_WINDOW_MS = 10_000;
	private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
	private static CrashGuardModule instance;

	private long lastRecoveryAt;
	private boolean recovering;

	public CrashGuardModule() {
		super("Crash Guard", "When the game crashes, saves the crash report and takes you back to the menu instead of closing Minecraft.",
				Category.UTILITY, true);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** Intenta recuperarse del crasheo. Devuelve false si hay que dejar que el juego se cierre. */
	public static boolean tryRecover(Minecraft client, CrashReport report) {
		if (instance == null || !instance.isEnabled() || instance.recovering) return false;
		Throwable error = report.getException();
		if (error instanceof OutOfMemoryError || error instanceof StackOverflowError) return false;
		long now = System.currentTimeMillis();
		if (now - instance.lastRecoveryAt < REPEAT_WINDOW_MS) return false;

		instance.recovering = true;
		try {
			client.fillReport(report);
			Path file = client.gameDirectory.toPath().resolve("crash-reports")
					.resolve("crash-" + LocalDateTime.now().format(FILE_DATE) + "-client.txt");
			report.saveToFile(file, ReportType.CRASH);
			FreedomClient.LOGGER.error("Crash Guard caught a crash, report saved to {}", file, error);

			CrashGuardScreen screen = new CrashGuardScreen(report.getTitle(), error, file);
			if (client.level != null) {
				client.disconnect(screen, false);
			} else {
				client.setScreen(screen);
			}
			instance.lastRecoveryAt = now;
			return true;
		} catch (Throwable recoveryError) {
			FreedomClient.LOGGER.error("Crash Guard could not recover, letting the game crash", recoveryError);
			return false;
		} finally {
			instance.recovering = false;
		}
	}
}
