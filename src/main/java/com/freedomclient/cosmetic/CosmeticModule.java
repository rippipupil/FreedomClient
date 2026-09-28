package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Cosmético de Angel Devil. Por ahora solo lo ve el propio jugador (en F5 y en el inventario). */
public abstract class CosmeticModule extends Module {
	private final CosmeticSlot slot;

	protected CosmeticModule(String name, String description, CosmeticSlot slot) {
		this(name, description, slot, true);
	}

	protected CosmeticModule(String name, String description, CosmeticSlot slot, boolean enabledByDefault) {
		super(name, description, Category.COSMETICS, enabledByDefault);
		this.slot = slot;
	}

	/** Sección de la pestaña Cosmetics (sombreros, capas, alas, mascotas, efectos). */
	public CosmeticSlot getSlot() {
		return slot;
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

	/**
	 * Píxeles que hay que separar de la espalda lo que va detrás: los élitros sobresalen mucho (y se abren hacia
	 * atrás) y el peto 1 px, así nada queda tapado.
	 */
	public static float backClearance(AvatarRenderState state) {
		if (state.chestEquipment.has(net.minecraft.core.component.DataComponents.GLIDER)) return 3.4F;
		return state.chestEquipment.isEmpty() ? 0.0F : 1.2F;
	}

	public static <T extends CosmeticModule> T get(Class<T> type) {
		ModuleManager manager = FreedomClient.getModuleManager();
		return manager == null ? null : manager.get(type);
	}
}
