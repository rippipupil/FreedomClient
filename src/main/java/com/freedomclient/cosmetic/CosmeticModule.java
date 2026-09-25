package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Cosmético de Angel Devil. Por ahora solo lo ve el propio jugador (en F5 y en el inventario). */
public abstract class CosmeticModule extends Module {
	protected CosmeticModule(String name, String description) {
		super(name, description, Category.COSMETICS, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** Si hay que dibujar el cosmético en este jugador (solo en el tuyo y si no eres invisible). */
	public boolean shouldRender(AvatarRenderState state) {
		Minecraft client = Minecraft.getInstance();
		return isEnabled() && !state.isInvisible && client.player != null && state.id == client.player.getId();
	}

	public static <T extends CosmeticModule> T get(Class<T> type) {
		ModuleManager manager = FreedomClient.getModuleManager();
		return manager == null ? null : manager.get(type);
	}
}
