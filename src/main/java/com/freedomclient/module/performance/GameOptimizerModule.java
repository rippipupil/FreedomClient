package com.freedomclient.module.performance;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.performance.PerformanceSettings;
import com.freedomclient.setting.ActionSetting;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

/**
 * Tarjeta con el botón que aplica los ajustes de vídeo más rápidos, y el límite de FPS de los menús: con un menú
 * abierto dentro del mundo (inventario, pausa, este menú...) no hace falta dibujar a cientos de FPS, así que se
 * limita y la tarjeta gráfica y el procesador descansan.
 */
public class GameOptimizerModule extends Module {
	private static GameOptimizerModule instance;

	public final BooleanSetting limitMenuFps = add(new BooleanSetting("Limit menu FPS",
			"Cap the frame rate while a menu is open in a world (inventory, pause, this menu...). The chat is not limited.", true));
	public final NumberSetting menuFps = add(new NumberSetting("Menu FPS", "Frame rate limit while a menu is open.", 60, 30, 144, 5, " FPS"));

	public GameOptimizerModule() {
		super("FPS Optimizer", "Applies the fastest video settings without lowering your render distance, and limits the FPS in menus.",
				Category.PERFORMANCE, true);
		instance = this;
		menuFps.visibleWhen(limitMenuFps::get);
		add(new ActionSetting("Optimize video settings",
				"Turns off VSync, clouds, entity shadows, biome blend, vignette, chunk fade-in, menu blur and texture filtering, "
						+ "uses fast leaves and threaded chunk updates, uncaps the frame rate (and limits it in menus and while AFK), "
						+ "keeps the simulation distance at 8 or less, and applies the fastest Sodium and Sodium Extra options.",
				"Apply", () -> {
					limitMenuFps.set(true);
					PerformanceSettings.apply(Minecraft.getInstance());
				}));
	}

	/** Llamado desde FramerateLimitTrackerMixin: el límite de FPS que toca ahora, a partir del que calcula el juego. */
	public static int limit(int vanilla) {
		GameOptimizerModule module = instance;
		ModuleManager manager = FreedomClient.getModuleManager();
		if (module == null || manager == null || !module.limitMenuFps.get()) return vanilla;
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.screen == null || client.screen instanceof ChatScreen) return vanilla;
		return Math.min(vanilla, module.menuFps.getInt());
	}

	@Override
	public boolean canToggle() {
		return false;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
