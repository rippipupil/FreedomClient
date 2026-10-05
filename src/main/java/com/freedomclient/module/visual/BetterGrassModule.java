package com.freedomclient.module.visual;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.pack.FreedomPack;
import net.minecraft.client.Minecraft;

/**
 * Better Grass: los laterales de la hierba, la nieve, el micelio y el podzol se ven como la parte de arriba.
 * Es una parte del paquete de FreedomClient ({@link FreedomPack}) que este módulo pone o quita.
 */
public class BetterGrassModule extends Module implements com.freedomclient.module.LivePreview {
	public BetterGrassModule() {
		super("Better Grass", "Grass, snow, mycelium and podzol sides look like their top, so hills look smooth.", Category.VISUAL, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	@Override
	protected void onEnable(Minecraft client) {
		FreedomPack.refresh(client);
	}

	@Override
	protected void onDisable(Minecraft client) {
		FreedomPack.refresh(client);
	}
}
