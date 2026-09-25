package com.freedomclient.particle;

import com.freedomclient.FreedomClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;

/** Acceso a los sprites de partículas de FreedomClient (textures/particle/*.png, en el atlas de partículas). */
public final class PixelParticles {
	private PixelParticles() {
	}

	public static TextureAtlasSprite sprite(String name) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(FreedomClient.id(name));
	}
}
