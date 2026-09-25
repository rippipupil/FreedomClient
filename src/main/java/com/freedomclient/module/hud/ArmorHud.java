package com.freedomclient.module.hud;

import com.freedomclient.hud.HudModule;
import com.freedomclient.hud.HudPosition;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ArmorHud extends HudModule {
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final int SLOT = 18;

	private final ModeSetting layout = add(new ModeSetting("Layout", "Stack the items vertically or horizontally.", "Vertical", "Vertical", "Horizontal"));
	private final ModeSetting durability = add(new ModeSetting("Durability", "How to show durability.", "Number", "Number", "Percent", "None"));
	private final BooleanSetting showHeld = add(new BooleanSetting("Show held item", "Also show the item in your hand.", true));

	public ArmorHud() {
		super("Armor HUD", "Shows your armor and held item with their durability.", true, new HudPosition(HudPosition.Anchor.END, 2, HudPosition.Anchor.END, 2));
	}

	private List<ItemStack> items(Minecraft client, boolean preview) {
		List<ItemStack> items = new ArrayList<>();
		LocalPlayer player = client.player;
		if (player != null) {
			for (EquipmentSlot slot : ARMOR) {
				ItemStack stack = player.getItemBySlot(slot);
				if (!stack.isEmpty()) items.add(stack);
			}
			if (showHeld.get() && !player.getMainHandItem().isEmpty()) items.add(player.getMainHandItem());
		}
		if (items.isEmpty() && preview) {
			items.add(new ItemStack(Items.DIAMOND_HELMET));
			items.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
			items.add(new ItemStack(Items.DIAMOND_LEGGINGS));
			items.add(new ItemStack(Items.DIAMOND_BOOTS));
		}
		return items;
	}

	private boolean vertical() {
		return layout.is("Vertical");
	}

	private int labelWidth(Minecraft client) {
		return durability.is("None") ? 0 : client.font.width("100%") + 2;
	}

	@Override
	public boolean shouldRender(Minecraft client) {
		return !items(client, false).isEmpty();
	}

	@Override
	public int getWidth(Minecraft client, boolean preview) {
		int count = Math.max(1, items(client, preview).size());
		return vertical() ? SLOT + labelWidth(client) : count * (SLOT + 2);
	}

	@Override
	public int getHeight(Minecraft client, boolean preview) {
		int count = Math.max(1, items(client, preview).size());
		return vertical() ? count * SLOT : SLOT + (durability.is("None") ? 0 : 9);
	}

	@Override
	public void render(GuiGraphics graphics, Minecraft client, boolean preview) {
		List<ItemStack> items = items(client, preview);
		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(i);
			int x = vertical() ? 0 : i * (SLOT + 2);
			int y = vertical() ? i * SLOT : 0;
			graphics.renderItem(stack, x + 1, y + 1);
			graphics.renderItemDecorations(client.font, stack, x + 1, y + 1);

			String label = durabilityLabel(stack);
			if (label != null) {
				int color = durabilityColor(stack);
				if (vertical()) {
					graphics.drawString(client.font, label, x + SLOT + 2, y + 5, color, true);
				} else {
					graphics.drawString(client.font, label, x + (SLOT - client.font.width(label)) / 2 + 1, y + SLOT + 1, color, true);
				}
			}
		}
	}

	private String durabilityLabel(ItemStack stack) {
		if (durability.is("None") || !stack.isDamageableItem()) return null;
		int remaining = stack.getMaxDamage() - stack.getDamageValue();
		return durability.is("Percent") ? Math.round(remaining * 100.0F / stack.getMaxDamage()) + "%" : String.valueOf(remaining);
	}

	public static int durabilityColor(ItemStack stack) {
		float fraction = 1.0F - (float) stack.getDamageValue() / stack.getMaxDamage();
		if (fraction > 0.6F) return 0xFF55FF55;
		if (fraction > 0.25F) return 0xFFFFFF55;
		return 0xFFFF5555;
	}
}
