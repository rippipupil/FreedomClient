package com.freedomclient.module.pvp;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.pack.FreedomPack;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.PreviewSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Gap Counter: las manzanas de oro (gaps) y las de Notch llevan dibujado el número que hay en la pila, así ves
 * cuántas le quedan al rival cuando las tiene en la mano. El contorno va de claro (muchas) a oscuro (pocas).
 * Cada estilo es una parte del paquete de FreedomClient (tools/make_gap_counter.py), que va por encima de los del jugador.
 */
public class GapCounterModule extends Module {
	private static final String VANILLA_PACK = "gapcounter_vanilla";
	private static final String OVERLAY_PACK = "gapcounter_pack";
	private static final String PUMPKIN_PACK = "gapcounter_pumpkin";
	private static GapCounterModule instance;
	/** Solo en el hilo de render: true mientras el juego dibuja una casilla de la hotbar (lo ponen los mixins de Gui). */
	public static boolean drawingHotbarSlot;
	private static final int[] PREVIEW_COUNTS = {64, 48, 32, 16, 8, 3, 1};

	private final ModeSetting style = add(new ModeSetting("Style",
			"Vanilla numbers: FreedomClient's vanilla-style apple with the number, replacing your pack's apple. "
					+ "Pumpkin: golden pumpkins (carved jack-o'-lanterns for Notch apples) with the number. "
					+ "Numbers over my pack: keeps your resource pack's apple and adds the number on top.",
			"Vanilla numbers", "Vanilla numbers", "Pumpkin", "Numbers over my pack"));
	private final BooleanSetting hotbar = add(new BooleanSetting("Show in hotbar",
			"Also show the coloured counter on golden apples in your hotbar (instead of the plain white number).", true));
	private final PreviewSetting preview = add(new PreviewSetting("Preview",
			"How golden apples (top) and Notch apples (bottom) look with each amount.", 58, this::renderPreview));
	private String applied;

	public GapCounterModule() {
		super("Gap Counter", "Numbers on golden and Notch apples so you can see how many your rival has left. Light border = many, dark = few.",
				Category.PVP, true);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/**
	 * Carpeta (resourcepacks/...) del estilo puesto, o null si el módulo está apagado. Es una parte del paquete de
	 * FreedomClient, que va por encima de los paquetes del jugador, así su manzana sustituye a la de ellos.
	 */
	public static String activePack() {
		GapCounterModule module = instance;
		if (module == null || !module.isEnabled()) return null;
		return module.style.is("Vanilla numbers") ? VANILLA_PACK : module.style.is("Pumpkin") ? PUMPKIN_PACK : OVERLAY_PACK;
	}

	public static List<String> packs() {
		return List.of(VANILLA_PACK, OVERLAY_PACK, PUMPKIN_PACK);
	}

	@Override
	public void onTick(Minecraft client) {
		// Al cambiar de estilo se recarga el paquete con el nuevo.
		if (!style.get().equals(applied)) {
			applied = style.get();
			FreedomPack.refresh(client);
		}
	}

	@Override
	protected void onEnable(Minecraft client) {
		applied = style.get();
		FreedomPack.refresh(client);
	}

	@Override
	protected void onDisable(Minecraft client) {
		applied = null;
		FreedomPack.refresh(client);
	}

	/** Dos filas de manzanas (gaps y Notch) con varias cantidades, tal y como se verán con el estilo elegido. */
	private void renderPreview(GuiGraphics graphics, int x, int y, int width, int height) {
		int size = 24;
		int gap = Math.max(4, Math.min(14, (width - PREVIEW_COUNTS.length * size) / PREVIEW_COUNTS.length));
		boolean vanilla = style.is("Vanilla numbers");
		boolean pumpkin = style.is("Pumpkin");
		for (int row = 0; row < 2; row++) {
			String kind = row == 0 ? "gap" : "notch";
			for (int i = 0; i < PREVIEW_COUNTS.length; i++) {
				int n = PREVIEW_COUNTS[i];
				int px = x + i * (size + gap);
				int py = y + row * (size + 6);
				if (pumpkin) {
					blit(graphics, FreedomClient.id("textures/item/gapcounter/pumpkin_" + kind + "_" + n + ".png"), px, py, size, 32);
				} else if (vanilla) {
					blit(graphics, FreedomClient.id("textures/item/gapcounter/" + kind + "_" + n + ".png"), px, py, size, 32);
				} else {
					blit(graphics, Identifier.withDefaultNamespace("textures/item/golden_apple.png"), px, py, size, 16);
					blit(graphics, FreedomClient.id("textures/item/gapcounter/badge_" + kind + "_" + n + ".png"), px, py, size, 32);
				}
			}
		}
	}

	/** Si en la hotbar se cambia el número blanco de esta pila por el contador de colores. */
	public static boolean showsInHotbar(ItemStack stack) {
		GapCounterModule module = instance;
		return module != null && module.isEnabled() && module.hotbar.get() && stack.getCount() > 1
				&& (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE));
	}

	/** Dibuja el contador de colores sobre una casilla de la hotbar (16x16 en x, y). */
	public static void renderHotbarCount(GuiGraphics graphics, ItemStack stack, int x, int y) {
		int count = Math.min(64, stack.getCount());
		String kind = stack.is(Items.ENCHANTED_GOLDEN_APPLE) ? "notch" : "gap";
		graphics.blit(RenderPipelines.GUI_TEXTURED, FreedomClient.id("textures/item/gapcounter/badge_" + kind + "_" + count + ".png"),
				x + 1, y + 1, 0.0F, 0.0F, 16, 16, 32, 32, 32, 32);
	}

	private static void blit(GuiGraphics graphics, Identifier texture, int x, int y, int size, int textureSize) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, size, size, textureSize, textureSize, textureSize, textureSize);
	}
}
