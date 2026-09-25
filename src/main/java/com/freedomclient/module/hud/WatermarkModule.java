package com.freedomclient.module.hud;

import com.freedomclient.FreedomClient;
import com.freedomclient.util.ColorUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public class WatermarkModule extends TextHudModule {
	private final String text;

	public WatermarkModule() {
		super("Watermark", "Muestra el nombre y la versión del cliente.", true);
		String version = FabricLoader.getInstance().getModContainer(FreedomClient.MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("dev");
		this.text = FreedomClient.NAME + " v" + version;
	}

	@Override
	public String getText(Minecraft client) {
		return text;
	}

	@Override
	public int getColor() {
		return ColorUtil.rainbow(0);
	}
}
