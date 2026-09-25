package com.freedomclient;

import com.freedomclient.config.Config;
import com.freedomclient.gui.HudRenderer;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.ui.menu.FreedomMenuScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FreedomClient implements ClientModInitializer {
	public static final String MOD_ID = "freedomclient";
	public static final String NAME = "FreedomClient";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	private static KeyMapping.Category keyCategory;
	private static ModuleManager moduleManager;

	@Override
	public void onInitializeClient() {
		keyCategory = KeyMapping.Category.register(id("general"));

		KeyMapping clickGuiKey = registerKey("clickgui", GLFW.GLFW_KEY_RIGHT_SHIFT);

		moduleManager = new ModuleManager();
		Config.load(moduleManager);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (clickGuiKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new FreedomMenuScreen());
				}
			}

			moduleManager.onTick(client);
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> moduleManager.onShutdown(client));

		HudElementRegistry.addLast(id("hud"), (graphics, deltaTracker) -> HudRenderer.render(graphics));

		LOGGER.info("{} loaded with {} mods", NAME, moduleManager.getModules().size());
	}

	public static KeyMapping registerKey(String name, int defaultKey) {
		return KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key." + MOD_ID + "." + name, InputConstants.Type.KEYSYM, defaultKey, keyCategory));
	}

	public static ModuleManager getModuleManager() {
		return moduleManager;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
