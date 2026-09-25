package com.freedomclient.module.render;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lwjgl.glfw.GLFW;

/** Visión completa en la oscuridad mediante un efecto de visión nocturna solo en el cliente. */
public class FullbrightModule extends Module {
	public FullbrightModule() {
		super("Fullbright", "Ilumina todo como si fuera de día.", Category.RENDER, false);
		setToggleKey(FreedomClient.registerKey("fullbright", GLFW.GLFW_KEY_G));
	}

	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player != null && !player.hasEffect(MobEffects.NIGHT_VISION)) {
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false));
		}
	}

	@Override
	protected void onDisable(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) return;

		MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
		// Solo quitamos nuestro efecto infinito, no una poción real de visión nocturna.
		if (effect != null && effect.isInfiniteDuration()) {
			player.removeEffect(MobEffects.NIGHT_VISION);
		}
	}
}
