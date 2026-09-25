package com.freedomclient.module.hud;

import com.freedomclient.hud.CombatTracker;
import com.freedomclient.hud.HudPosition;
import com.freedomclient.hud.TextHudModule;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

import java.util.Locale;

public class ReachHud extends TextHudModule {
	private final NumberSetting keepSeconds = add(new NumberSetting("Keep for", "How long the last reach stays on screen.", 3, 1, 10, 1, "s"));

	public ReachHud() {
		super("Reach", "Shows the distance of your last hit.", false, new HudPosition(HudPosition.Anchor.START, 2, HudPosition.Anchor.START, 98));
	}

	@Override
	protected String getText(Minecraft client) {
		double reach = CombatTracker.getReach(keepSeconds.getInt() * 1000L);
		return reach < 0 ? "Reach: -" : String.format(Locale.ROOT, "Reach: %.2f", reach);
	}

	@Override
	protected String getPreviewText() {
		return "Reach: 2.87";
	}
}
