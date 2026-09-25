package com.freedomclient.module.hud;

import com.freedomclient.hud.HudPosition;
import com.freedomclient.hud.TextHudModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;

/** Conversor Nether ↔ Overworld: dónde caerías en la otra dimensión (el Nether es 8 veces más pequeño). */
public class NetherCoordsHud extends TextHudModule {
	public NetherCoordsHud() {
		super("Nether Coords", "Shows your coordinates in the other dimension (Overworld <-> Nether).", false,
				new HudPosition(HudPosition.Anchor.START, 2, HudPosition.Anchor.START, 70));
	}

	@Override
	protected String getText(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) return null;
		if (player.level().dimension() == Level.OVERWORLD) {
			return "Nether: " + Math.floorDiv(player.getBlockX(), 8) + ", " + Math.floorDiv(player.getBlockZ(), 8);
		}
		if (player.level().dimension() == Level.NETHER) {
			return "Overworld: " + player.getBlockX() * 8 + ", " + player.getBlockZ() * 8;
		}
		return null;
	}

	@Override
	protected String getPreviewText() {
		return "Nether: 12, -40";
	}
}
