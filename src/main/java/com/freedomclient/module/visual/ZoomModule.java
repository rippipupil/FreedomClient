package com.freedomclient.module.visual;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.KeybindSetting;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** WI Zoom: zoom suave con la rueda para acercar más, cámara suave y menos sensibilidad mientras dura. */
public class ZoomModule extends Module {
	private static final float MIN_ZOOM = 1.5F;
	private static final float MAX_ZOOM = 50.0F;

	private final KeybindSetting zoomKey = add(new KeybindSetting("Zoom key", "Hold this key to zoom.", GLFW.GLFW_KEY_C));
	private final NumberSetting defaultZoom = add(new NumberSetting("Zoom level", "How much it zooms when you press the key.", 3, 1.5, 10, 0.5, "x"));
	private final BooleanSetting scrollZoom = add(new BooleanSetting("Scroll to zoom", "Use the mouse wheel to zoom further while zooming.", true));
	private final BooleanSetting smoothZoom = add(new BooleanSetting("Smooth animation", "Animate the zoom in and out.", true));
	private final BooleanSetting smoothCamera = add(new BooleanSetting("Smooth camera", "Cinematic camera while zooming.", false));
	private final BooleanSetting lowerSensitivity = add(new BooleanSetting("Lower sensitivity", "Lower mouse sensitivity the more you zoom.", true));
	private final BooleanSetting hideHand = add(new BooleanSetting("Hide hand", "Hide your hand while zooming.", true));

	private boolean zooming;
	private float targetZoom = 1.0F;
	private float currentZoom = 1.0F;
	private long lastFrameNanos;
	private boolean savedSmoothCamera;
	private double savedSensitivity = -1;

	public ZoomModule() {
		super("Zoom", "Hold the zoom key to zoom in, and scroll to zoom further (WI Zoom).", Category.VISUAL, true);
	}

	@Override
	public void onTick(Minecraft client) {
		boolean wantZoom = zoomKey.isBound() && client.screen == null && client.player != null
				&& InputConstants.isKeyDown(client.getWindow(), zoomKey.get());
		if (wantZoom && !zooming) startZoom(client);
		else if (!wantZoom && zooming) stopZoom(client);

		if (zooming && lowerSensitivity.get() && savedSensitivity >= 0) {
			client.options.sensitivity().set(savedSensitivity / Math.sqrt(targetZoom));
		}
	}

	private void startZoom(Minecraft client) {
		zooming = true;
		targetZoom = defaultZoom.getFloat();
		savedSmoothCamera = client.options.smoothCamera;
		if (smoothCamera.get()) client.options.smoothCamera = true;
		if (lowerSensitivity.get()) savedSensitivity = client.options.sensitivity().get();
	}

	private void stopZoom(Minecraft client) {
		zooming = false;
		targetZoom = 1.0F;
		client.options.smoothCamera = savedSmoothCamera;
		if (savedSensitivity >= 0) {
			client.options.sensitivity().set(savedSensitivity);
			savedSensitivity = -1;
		}
	}

	/** Zoom actual, animado frame a frame. 1 = sin zoom. */
	public float getCurrentZoom() {
		long now = System.nanoTime();
		float seconds = lastFrameNanos == 0 ? 0 : Math.min((now - lastFrameNanos) / 1.0E9F, 0.1F);
		lastFrameNanos = now;

		float target = isEnabled() ? targetZoom : 1.0F;
		if (!smoothZoom.get()) {
			currentZoom = target;
		} else {
			currentZoom += (target - currentZoom) * Math.min(1.0F, seconds * 14.0F);
			if (Math.abs(target - currentZoom) < 0.01F) currentZoom = target;
		}
		return currentZoom;
	}

	public boolean shouldHideHand() {
		return zooming && hideHand.get();
	}

	/** Devuelve true si la rueda se ha usado para el zoom. */
	public boolean onScroll(double amount) {
		if (!zooming || !scrollZoom.get() || amount == 0) return false;
		targetZoom = Mth.clamp(targetZoom * (amount > 0 ? 1.25F : 0.8F), MIN_ZOOM, MAX_ZOOM);
		return true;
	}

	@Override
	protected void onDisable(Minecraft client) {
		if (zooming) stopZoom(client);
	}

	@Override
	public void onShutdown(Minecraft client) {
		// Restaura la sensibilidad para que no se guarde la del zoom en options.txt.
		if (zooming) stopZoom(client);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
