package com.freedomclient.module.utility;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.ActionSetting;
import com.freedomclient.setting.KeybindSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Quick Pack: abre la pantalla de packs de recursos con una tecla, desde cualquier sitio. */
public class QuickPackModule extends Module {
	private final KeybindSetting key = add(new KeybindSetting("Open key", "Key that opens the resource pack screen.", GLFW.GLFW_KEY_F8));
	private boolean wasDown;

	public QuickPackModule() {
		super("Quick Pack", "Open the resource pack screen with a key to switch packs quickly.", Category.UTILITY, true);
		add(new ActionSetting("Resource packs", "Open the resource pack screen now.", "Open", () -> open(Minecraft.getInstance())));
	}

	@Override
	public void onTick(Minecraft client) {
		boolean down = key.isBound() && client.screen == null && InputConstants.isKeyDown(client.getWindow(), key.get());
		if (down && !wasDown) open(client);
		wasDown = down;
	}

	private static void open(Minecraft client) {
		client.setScreen(new PackSelectionScreen(client.getResourcePackRepository(), repository -> {
			client.options.updateResourcePacks(repository);
			client.setScreen(null);
		}, client.getResourcePackDirectory(), Component.translatable("resourcePack.title")));
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
