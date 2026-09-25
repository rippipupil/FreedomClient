package com.freedomclient.module.hud;

import com.freedomclient.hud.CombatTracker;
import com.freedomclient.hud.HudPosition;
import com.freedomclient.hud.TextHudModule;
import com.freedomclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

public class ComboHud extends TextHudModule {
	private final BooleanSetting hideZero = add(new BooleanSetting("Hide when 0", "Only show while you have a combo.", false));

	public ComboHud() {
		super("Combo", "Counts your hits in a row without taking damage.", false, new HudPosition(HudPosition.Anchor.START, 2, HudPosition.Anchor.START, 82));
	}

	@Override
	protected String getText(Minecraft client) {
		int combo = CombatTracker.getCombo();
		if (combo == 0 && hideZero.get()) return null;
		return "Combo: " + combo;
	}

	@Override
	protected String getPreviewText() {
		return "Combo: 3";
	}
}
