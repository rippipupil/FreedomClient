package com.freedomclient.module.visual;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.ModuleManager;
import net.minecraft.client.Minecraft;

/** Punto único donde FOV Changer y Zoom modifican el campo de visión (llamado desde GameRendererMixin). */
public final class FovController {
	private FovController() {
	}

	public static float modifyFov(float fov) {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null) return fov;

		FovChangerModule changer = manager.get(FovChangerModule.class);
		if (changer.isEnabled()) fov = changer.apply(fov, Minecraft.getInstance().options.fov().get());

		ZoomModule zoom = manager.get(ZoomModule.class);
		return fov / zoom.getCurrentZoom();
	}

	public static boolean shouldHideHand() {
		ModuleManager manager = FreedomClient.getModuleManager();
		return manager != null && manager.get(ZoomModule.class).shouldHideHand();
	}

	public static boolean onScroll(double amount) {
		ModuleManager manager = FreedomClient.getModuleManager();
		return manager != null && manager.get(ZoomModule.class).onScroll(amount);
	}
}
