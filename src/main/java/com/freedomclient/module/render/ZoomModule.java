package com.freedomclient.module.render;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Zoom estilo OptiFine: mantén la tecla (C por defecto) para acercar la cámara. */
public class ZoomModule extends Module {
	private static final int ZOOM_FOV = 30;

	private final KeyMapping zoomKey;
	private boolean zooming;
	private int savedFov;
	private boolean savedSmoothCamera;

	public ZoomModule() {
		super("Zoom", "Mantén la tecla de zoom (C) para acercar la vista.", Category.RENDER, true);
		this.zoomKey = FreedomClient.registerKey("zoom", GLFW.GLFW_KEY_C);
	}

	@Override
	public void onTick(Minecraft client) {
		boolean wantZoom = zoomKey.isDown() && client.screen == null;
		if (wantZoom && !zooming) {
			startZoom(client);
		} else if (!wantZoom && zooming) {
			stopZoom(client);
		}
	}

	private void startZoom(Minecraft client) {
		savedFov = client.options.fov().get();
		savedSmoothCamera = client.options.smoothCamera;
		client.options.fov().set(ZOOM_FOV);
		client.options.smoothCamera = true;
		zooming = true;
	}

	private void stopZoom(Minecraft client) {
		client.options.fov().set(savedFov);
		client.options.smoothCamera = savedSmoothCamera;
		zooming = false;
	}

	@Override
	protected void onDisable(Minecraft client) {
		if (zooming) stopZoom(client);
	}

	@Override
	public void onShutdown(Minecraft client) {
		// Restaura el FOV para que no se guarde el valor de zoom en options.txt.
		if (zooming) stopZoom(client);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
