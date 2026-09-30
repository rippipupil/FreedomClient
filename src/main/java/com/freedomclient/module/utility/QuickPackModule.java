package com.freedomclient.module.utility;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.ActionSetting;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.KeybindSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Quick Pack: abre la pantalla de packs de recursos con una tecla, desde cualquier sitio. */
public class QuickPackModule extends Module {
	private final KeybindSetting key = add(new KeybindSetting("Open key", "Key that opens the resource pack screen.", GLFW.GLFW_KEY_F8));
	private final BooleanSetting clientMenu = add(new BooleanSetting("FreedomClient menu",
			"Open the Packs tab of the client menu (cards with icons, order and Apply) instead of Minecraft's pack screen.", true));
	private boolean wasDown;

	public QuickPackModule() {
		super("Quick Pack", "Open the resource packs with a key (F8) to switch packs quickly, in the client's own pack menu.", Category.UTILITY, true);
		add(new ActionSetting("Resource packs", "Open the resource pack screen now.", "Open", () -> open(Minecraft.getInstance())));
		instance = this;
	}

	@Override
	public void onTick(Minecraft client) {
		boolean down = key.isBound() && client.screen == null && InputConstants.isKeyDown(client.getWindow(), key.get());
		if (down && !wasDown) open(client);
		wasDown = down;
	}

	private static QuickPackModule instance;

	private static void open(Minecraft client) {
		if (instance == null || instance.clientMenu.get()) {
			client.setScreen(com.freedomclient.ui.menu.FreedomMenuScreen.forTab(com.freedomclient.ui.menu.FreedomMenuScreen.Tab.PACKS));
			return;
		}
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
