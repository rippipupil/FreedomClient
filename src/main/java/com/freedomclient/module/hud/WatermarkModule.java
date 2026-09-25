package com.freedomclient.module.hud;

import com.freedomclient.FreedomClient;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.util.ColorUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public class WatermarkModule extends TextHudModule {
	private final BooleanSetting rainbow = add(new BooleanSetting("Rainbow", "Animated rainbow text instead of the text color.", false));
	private final String text;

	public WatermarkModule() {
		super("Watermark", "Shows the client name and version.", true);
		textColor.set(0xFFF2C94C);
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
		return rainbow.get() ? ColorUtil.rainbow(0) : super.getColor();
	}
}
