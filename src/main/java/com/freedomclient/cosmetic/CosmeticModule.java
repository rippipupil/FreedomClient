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
		// Vista previa de una tarjeta del menú: solo ese cosmético, aunque esté apagado.
		CosmeticModule preview = CosmeticPreview.of(state);
		if (preview != null) return preview == this && !state.isInvisible;
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

	/**
	 * Fondo propio de la tarjeta del menú (fijo, no cambia con el tema). Dibuja dentro del rectángulo y devuelve true;
	 * por defecto no hay y la tarjeta usa el color de su categoría.
	 */
	public boolean drawCardBackground(net.minecraft.client.gui.GuiGraphics g, int x, int y, int w, int h) {
		return false;
	}

	/**
	 * Borde propio de la tarjeta del menú (fijo, sustituye a Card Borders y a los cosméticos de borde). {@code on} y
	 * {@code hover} van de 0 a 1. Dibuja y devuelve true; por defecto no hay.
	 */
	public boolean drawCardBorder(net.minecraft.client.gui.GuiGraphics g, int x, int y, int w, int h, float on, float hover) {
		return false;
	}

	/** Rótulo propio en la tarjeta del menú en vez del nombre en texto. Dibuja en (x, y) y devuelve true; por defecto no hay. */
	public boolean drawCardTitle(net.minecraft.client.gui.GuiGraphics g, int x, int y, float on) {
		return false;
	}

	/** Si su tarjeta enseña la línea de descripción debajo del nombre. */
	public boolean showCardDescription() {
		return true;
	}

	public static <T extends CosmeticModule> T get(Class<T> type) {
		ModuleManager manager = FreedomClient.getModuleManager();
		return manager == null ? null : manager.get(type);
	}
}
