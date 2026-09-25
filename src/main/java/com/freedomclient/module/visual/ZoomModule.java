package com.freedomclient.module.visual;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.KeybindSetting;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Zoom estilo OptiFine: mantén la tecla de zoom para acercar la cámara. */
public class ZoomModule extends Module {
	private final KeybindSetting zoomKey = add(new KeybindSetting("Zoom key", "Hold this key to zoom.", GLFW.GLFW_KEY_C));
	private final NumberSetting zoomFov = add(new NumberSetting("Zoom FOV", "Field of view while zooming (lower is closer).", 30, 30, 70, 1));
	private final BooleanSetting smoothCamera = add(new BooleanSetting("Smooth camera", "Cinematic camera while zooming.", true));

	private boolean zooming;
	private int savedFov;
	private boolean savedSmoothCamera;

	public ZoomModule() {
		super("Zoom", "Hold the zoom key to look closer.", Category.VISUAL, true);
	}

	@Override
	public void onTick(Minecraft client) {
		boolean wantZoom = zoomKey.isBound() && client.screen == null
				&& InputConstants.isKeyDown(client.getWindow(), zoomKey.get());
		if (wantZoom && !zooming) {
			startZoom(client);
		} else if (!wantZoom && zooming) {
			stopZoom(client);
		}
	}

	private void startZoom(Minecraft client) {
		savedFov = client.options.fov().get();
		savedSmoothCamera = client.options.smoothCamera;
		client.options.fov().set(zoomFov.getInt());
		client.options.smoothCamera = smoothCamera.get() || savedSmoothCamera;
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
