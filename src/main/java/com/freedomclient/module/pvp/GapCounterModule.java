package com.freedomclient.module.pvp;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.PreviewSetting;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Gap Counter: las manzanas de oro (gaps) y las de Notch llevan dibujado el número que hay en la pila, así ves
 * cuántas le quedan al rival cuando las tiene en la mano. El contorno va de claro (muchas) a oscuro (pocas).
 * Son paquetes de recursos integrados (tools/make_gap_counter.py) que se ponen por encima de los del jugador.
 */
public class GapCounterModule extends Module {
	private static final String VANILLA_PACK = "gapcounter_vanilla";
	private static final String OVERLAY_PACK = "gapcounter_pack";
	private static final int[] PREVIEW_COUNTS = {64, 48, 32, 16, 8, 3, 1};

	private final ModeSetting style = add(new ModeSetting("Style",
			"Vanilla numbers: FreedomClient's vanilla-style apple with the number, replacing your pack's apple. "
					+ "Numbers over my pack: keeps your resource pack's apple and adds the number on top.",
			"Vanilla numbers", "Vanilla numbers", "Numbers over my pack"));
	private final PreviewSetting preview = add(new PreviewSetting("Preview",
			"How golden apples (top) and Notch apples (bottom) look with each amount.", 58, this::renderPreview));
	private String applied;

	public GapCounterModule() {
		super("Gap Counter", "Numbers on golden and Notch apples so you can see how many your rival has left. Light border = many, dark = few.",
				Category.PVP, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** Registra los dos paquetes; se llama al iniciar el cliente. */
	public static void registerPacks() {
		FabricLoader.getInstance().getModContainer(FreedomClient.MOD_ID).ifPresent(container -> {
			ResourceLoader.registerBuiltinPack(FreedomClient.id(VANILLA_PACK), container,
					Component.literal("FreedomClient Gap Counter"), PackActivationType.DEFAULT_ENABLED);
			ResourceLoader.registerBuiltinPack(FreedomClient.id(OVERLAY_PACK), container,
					Component.literal("FreedomClient Gap Counter (your apple)"), PackActivationType.NORMAL);
		});
	}

	@Override
	public void onTick(Minecraft client) {
		// Al cambiar de estilo: activa el paquete elegido y lo pone por encima de los demás. Al arrancar no se toca
		// (el paquete ya está activo y sus modelos usan texturas propias, así que ganan a las manzanas de otros paquetes).
		if (client.getOverlay() != null) return;
		if (applied == null) {
			applied = style.get();
		} else if (!style.get().equals(applied)) {
			applied = style.get();
			apply(client, true);
		}
	}

	@Override
	protected void onEnable(Minecraft client) {
		applied = style.get();
		apply(client, true);
	}

	@Override
	protected void onDisable(Minecraft client) {
		applied = null;
		apply(client, false);
	}

	private static String packId(PackRepository repository, String name) {
		for (String id : repository.getAvailableIds()) {
			if (id.endsWith(name)) return id;
		}
		return null;
	}

	private void apply(Minecraft client, boolean enabled) {
		PackRepository repository = client.getResourcePackRepository();
		String vanilla = packId(repository, VANILLA_PACK);
		String overlay = packId(repository, OVERLAY_PACK);
		if (vanilla == null || overlay == null) return;
		String wanted = !enabled ? null : style.is("Vanilla numbers") ? vanilla : overlay;
		List<String> selected = new ArrayList<>(repository.getSelectedIds());
		boolean changed = false;
		for (String id : new String[] {vanilla, overlay}) {
			if (!id.equals(wanted) && selected.remove(id)) changed = true;
		}
		// El elegido va el último de la lista, que es el de más prioridad: así sustituye a la manzana de otros paquetes.
		if (wanted != null && (selected.isEmpty() || !selected.get(selected.size() - 1).equals(wanted))) {
			selected.remove(wanted);
			selected.add(wanted);
			changed = true;
		}
		if (changed) {
			repository.setSelected(selected);
			client.options.updateResourcePacks(repository);
		}
	}

	/** Dos filas de manzanas (gaps y Notch) con varias cantidades, tal y como se verán con el estilo elegido. */
	private void renderPreview(GuiGraphics graphics, int x, int y, int width, int height) {
		int size = 24;
		int gap = Math.max(4, Math.min(14, (width - PREVIEW_COUNTS.length * size) / PREVIEW_COUNTS.length));
		boolean vanilla = style.is("Vanilla numbers");
		for (int row = 0; row < 2; row++) {
			String kind = row == 0 ? "gap" : "notch";
			for (int i = 0; i < PREVIEW_COUNTS.length; i++) {
				int n = PREVIEW_COUNTS[i];
				int px = x + i * (size + gap);
				int py = y + row * (size + 6);
				if (vanilla) {
					blit(graphics, FreedomClient.id("textures/item/gapcounter/" + kind + "_" + n + ".png"), px, py, size, 32);
				} else {
					blit(graphics, Identifier.withDefaultNamespace("textures/item/golden_apple.png"), px, py, size, 16);
					blit(graphics, FreedomClient.id("textures/item/gapcounter/badge_" + kind + "_" + n + ".png"), px, py, size, 32);
				}
			}
		}
	}

	private static void blit(GuiGraphics graphics, Identifier texture, int x, int y, int size, int textureSize) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, size, size, textureSize, textureSize, textureSize, textureSize);
	}
}
