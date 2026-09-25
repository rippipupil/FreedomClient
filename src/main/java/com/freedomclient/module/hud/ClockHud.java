package com.freedomclient.module.hud;

import com.freedomclient.hud.HudPosition;
import com.freedomclient.hud.TextHudModule;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import net.minecraft.client.Minecraft;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ClockHud extends TextHudModule {
	private final ModeSetting format = add(new ModeSetting("Format", "12 or 24 hour clock.", "24h", "24h", "12h"));
	private final BooleanSetting seconds = add(new BooleanSetting("Show seconds", "Show seconds.", false));

	public ClockHud() {
		super("Clock", "Shows the real-world time.", false, new HudPosition(HudPosition.Anchor.CENTER, 0, HudPosition.Anchor.START, 2));
	}

	@Override
	protected String getText(Minecraft client) {
		String pattern = format.is("24h") ? (seconds.get() ? "HH:mm:ss" : "HH:mm") : (seconds.get() ? "h:mm:ss a" : "h:mm a");
		return LocalTime.now().format(DateTimeFormatter.ofPattern(pattern, Locale.ROOT));
	}
}
