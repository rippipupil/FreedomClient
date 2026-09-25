package com.freedomclient.module.hud;

import com.freedomclient.hud.ClickTracker;
import com.freedomclient.hud.HudPosition;
import com.freedomclient.hud.TextHudModule;
import com.freedomclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

public class CpsHud extends TextHudModule {
	private final BooleanSetting showRight = add(new BooleanSetting("Show right click", "Also show right clicks per second.", true));

	public CpsHud() {
		super("CPS", "Shows your clicks per second.", false, new HudPosition(HudPosition.Anchor.START, 2, HudPosition.Anchor.START, 66));
	}

	@Override
	protected String getText(Minecraft client) {
		return showRight.get()
				? "CPS: " + ClickTracker.leftCps() + " | " + ClickTracker.rightCps()
				: "CPS: " + ClickTracker.leftCps();
	}
}
