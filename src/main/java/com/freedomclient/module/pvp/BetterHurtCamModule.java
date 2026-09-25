package com.freedomclient.module.pvp;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/** Reduce o quita el temblor de cámara al recibir daño, usando la opción de accesibilidad de vanilla. */
public class BetterHurtCamModule extends Module {
	private final NumberSetting intensity = add(new NumberSetting("Intensity", "How much the camera shakes when you get hit.", 20, 0, 100, 5, "%"));
	private Double savedTilt;

	public BetterHurtCamModule() {
		super("Better Hurt Cam", "Reduces or removes the camera shake when you take damage.", Category.PVP, true);
	}

	@Override
	public void onTick(Minecraft client) {
		if (savedTilt == null) savedTilt = client.options.damageTiltStrength().get();
		double value = intensity.get() / 100.0;
		if (client.options.damageTiltStrength().get() != value) {
			client.options.damageTiltStrength().set(value);
		}
	}

	private void restore(Minecraft client) {
		if (savedTilt != null) {
			client.options.damageTiltStrength().set(savedTilt);
			savedTilt = null;
		}
	}

	@Override
	protected void onDisable(Minecraft client) {
		restore(client);
	}

	@Override
	public void onShutdown(Minecraft client) {
		restore(client);
	}
}
