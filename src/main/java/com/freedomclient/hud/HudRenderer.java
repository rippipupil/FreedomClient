package com.freedomclient.hud;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.ui.menu.FreedomMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Dibuja todos los elementos del HUD activados, cada uno en su posición y escala. */
public final class HudRenderer {
	private HudRenderer() {
	}

	public static void render(GuiGraphics graphics) {
		Minecraft client = Minecraft.getInstance();
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null || client.player == null || client.options.hideGui) return;
		// El editor dibuja los elementos por su cuenta.
		if (client.screen instanceof HudEditorScreen || client.screen instanceof FreedomMenuScreen) return;

		for (Module module : manager.getModules()) {
			if (module instanceof HudModule hud && hud.isEnabled() && hud.shouldRender(client)) {
				renderElement(graphics, client, hud, false);
			}
		}
	}

	/** Dibuja un elemento transformado a su posición; devuelve su rectángulo en pantalla {x, y, w, h}. */
	public static int[] renderElement(GuiGraphics graphics, Minecraft client, HudModule hud, boolean preview) {
		int[] bounds = bounds(graphics.guiWidth(), graphics.guiHeight(), client, hud, preview);
		float scale = hud.getPosition().getScale();

		graphics.pose().pushMatrix();
		graphics.pose().translate(bounds[0], bounds[1]);
		graphics.pose().scale(scale, scale);
		hud.render(graphics, client, preview);
		graphics.pose().popMatrix();
		return bounds;
	}

	public static int[] bounds(int screenWidth, int screenHeight, Minecraft client, HudModule hud, boolean preview) {
		float scale = hud.getPosition().getScale();
		int width = Math.max(1, Math.round(hud.getWidth(client, preview) * scale));
		int height = Math.max(1, Math.round(hud.getHeight(client, preview) * scale));
		int x = hud.getPosition().resolveX(screenWidth, width);
		int y = hud.getPosition().resolveY(screenHeight, height);
		return new int[] {x, y, width, height};
	}
}
