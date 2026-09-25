package com.freedomclient.module.visual;

import com.freedomclient.mixin.accessor.OverlayTextureAccessor;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.ColorSetting;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;

/** Cambia el color con el que parpadean las entidades al recibir daño (rojo por defecto en vanilla). */
public class HitColorModule extends Module {
	/** Color de vanilla: rojo con 70 % de opacidad. */
	private static final int VANILLA_COLOR = 0xB2FF0000;

	private final ColorSetting color = add(new ColorSetting("Hit color", "Color of entities when they take damage.", 0x99F2C94C, true));
	private int appliedColor = VANILLA_COLOR;

	public HitColorModule() {
		super("Hit Color", "Changes the color entities flash when they get hit.", Category.PVP, false);
	}

	@Override
	public void onTick(Minecraft client) {
		if (appliedColor != color.get()) apply(client, color.get());
	}

	@Override
	protected void onDisable(Minecraft client) {
		apply(client, VANILLA_COLOR);
	}

	/** Las 8 primeras filas de la textura de superposición son el tinte de daño. */
	private void apply(Minecraft client, int argb) {
		if (client.gameRenderer == null) return;
		DynamicTexture texture = ((OverlayTextureAccessor) client.gameRenderer.overlayTexture()).freedomclient$getTexture();
		NativeImage pixels = texture.getPixels();
		if (pixels == null) return;

		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 16; x++) {
				pixels.setPixel(x, y, argb);
			}
		}
		texture.upload();
		appliedColor = argb;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
