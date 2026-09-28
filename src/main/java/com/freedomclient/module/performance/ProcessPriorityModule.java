package com.freedomclient.module.performance;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.ModeSetting;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import net.minecraft.client.Minecraft;

import java.util.Locale;

/**
 * Process Priority: en Windows sube la prioridad del proceso del juego para que el sistema le dé la CPU antes que a
 * los programas en segundo plano (menos tirones cuando hay otras cosas abiertas). Vale tanto si se abre desde el
 * launcher de FreedomClient como desde el oficial, porque lo hace el propio juego.
 */
public class ProcessPriorityModule extends Module {
	private static final int NORMAL = 0x20;
	private static final int ABOVE_NORMAL = 0x8000;
	private static final int HIGH = 0x80;

	private final ModeSetting priority = add(new ModeSetting("Priority",
			"How much Windows favours the game over other programs. High gives the steadiest frames.",
			"High", "High", "Above normal", "Normal"));
	private String applied = null;

	public ProcessPriorityModule() {
		super("Process Priority", "Raises the game's priority in Windows so background programs steal less CPU.", Category.PERFORMANCE, true);
	}

	/** Solo lo que necesita de kernel32 (el JNA que ya trae Minecraft). */
	private interface Kernel32 extends Library {
		Pointer GetCurrentProcess();

		boolean SetPriorityClass(Pointer process, int priorityClass);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	@Override
	public void onTick(Minecraft client) {
		String wanted = priority.get();
		if (!wanted.equals(applied)) {
			applied = wanted;
			set(switch (wanted) {
				case "High" -> HIGH;
				case "Above normal" -> ABOVE_NORMAL;
				default -> NORMAL;
			});
		}
	}

	@Override
	protected void onDisable(Minecraft client) {
		applied = null;
		set(NORMAL);
	}

	private static void set(int priorityClass) {
		if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) return;
		try {
			Kernel32 kernel32 = Native.load("kernel32", Kernel32.class);
			boolean ok = kernel32.SetPriorityClass(kernel32.GetCurrentProcess(), priorityClass);
			FreedomClient.LOGGER.info("Process priority set to 0x{}: {}", Integer.toHexString(priorityClass), ok);
		} catch (Throwable e) {
			FreedomClient.LOGGER.warn("Could not change the process priority", e);
		}
	}
}
