package com.freedomclient.module.hud;

import com.freedomclient.FreedomClient;
import com.freedomclient.hud.HudPosition;
import com.freedomclient.hud.TextHudModule;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.util.ColorUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public class WatermarkHud extends TextHudModule {
	private final BooleanSetting showVersion = add(new BooleanSetting("Show version", "Show the client version next to the name.", true));
	private final BooleanSetting rainbow = add(new BooleanSetting("Rainbow", "Animated rainbow text instead of the text color.", false));
	private final String version;

	public WatermarkHud() {
		super("Watermark", "Shows the client name.", true, new HudPosition(HudPosition.Anchor.START, 2, HudPosition.Anchor.START, 2));
		textColor.set(0xFFF2C94C);
		version = FabricLoader.getInstance().getModContainer(FreedomClient.MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("dev");
	}

	@Override
	protected String getText(Minecraft client) {
		return showVersion.get() ? FreedomClient.NAME + " v" + version : FreedomClient.NAME;
	}

	@Override
	protected int getTextColor() {
		return rainbow.get() ? ColorUtil.rainbow(0) : super.getTextColor();
	}
}
