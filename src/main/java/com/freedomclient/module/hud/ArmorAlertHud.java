package com.freedomclient.module.hud;

import com.freedomclient.hud.HudModule;
import com.freedomclient.hud.HudPosition;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Muestra una exclamación roja animada cuando alguna pieza de armadura está casi rota. */
public class ArmorAlertHud extends HudModule {
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final int WIDTH = 60;
	private static final int HEIGHT = 24;

	private final NumberSetting threshold = add(new NumberSetting("Warn below", "Durability that triggers the alert.", 15, 5, 50, 1, "%"));
	private final BooleanSetting sound = add(new BooleanSetting("Sound", "Play a sound when a piece becomes low.", true));
	private boolean wasLow;

	public ArmorAlertHud() {
		super("Armor Alert", "Warns you with a red animated exclamation mark when your armor is about to break.", true,
				new HudPosition(HudPosition.Anchor.CENTER, 0, HudPosition.Anchor.CENTER, 24));
	}

	private ItemStack lowestPiece(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) return ItemStack.EMPTY;

		ItemStack lowest = ItemStack.EMPTY;
		float lowestFraction = threshold.getFloat() / 100.0F;
		for (EquipmentSlot slot : ARMOR) {
			ItemStack stack = player.getItemBySlot(slot);
			if (!stack.isDamageableItem()) continue;
			float fraction = 1.0F - (float) stack.getDamageValue() / stack.getMaxDamage();
			if (fraction <= lowestFraction) {
				lowestFraction = fraction;
				lowest = stack;
			}
		}
		return lowest;
	}

	@Override
	public void onTick(Minecraft client) {
		boolean low = !lowestPiece(client).isEmpty();
		if (low && !wasLow && sound.get()) {
			client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 1.6F, 0.4F));
		}
		wasLow = low;
	}

	@Override
	public boolean shouldRender(Minecraft client) {
		return !lowestPiece(client).isEmpty();
	}

	@Override
	public int getWidth(Minecraft client, boolean preview) {
		return WIDTH;
	}

	@Override
	public int getHeight(Minecraft client, boolean preview) {
		return HEIGHT;
	}

	@Override
	public void render(GuiGraphics graphics, Minecraft client, boolean preview) {
		ItemStack piece = lowestPiece(client);
		if (piece.isEmpty() && preview) piece = new ItemStack(Items.DIAMOND_CHESTPLATE);
		if (piece.isEmpty()) return;

		// La exclamación rebota y parpadea entre rojo y rojo oscuro.
		double time = System.currentTimeMillis() / 1000.0;
		int bounce = (int) Math.round(Math.abs(Math.sin(time * 5.0)) * -3.0);
		int red = Math.sin(time * 10.0) > 0 ? 0xFFFF3B3B : 0xFFB0101E;

		int markX = 4;
		int markY = 3 + bounce;
		graphics.fill(markX, markY, markX + 4, markY + 11, red);
		graphics.fill(markX, markY + 13, markX + 4, markY + 17, red);
		graphics.fill(markX - 1, markY - 1, markX + 5, markY, 0xFF3A0508);

		graphics.renderItem(piece, 14, 4);
		int remaining = piece.getMaxDamage() - piece.getDamageValue();
		graphics.drawString(client.font, String.valueOf(remaining), 33, 8, red, true);
	}
}
