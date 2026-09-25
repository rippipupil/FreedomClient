package com.freedomclient.module.hud;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ArmorStatusModule extends Module {
	private static final EquipmentSlot[] SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND
	};

	public ArmorStatusModule() {
		super("ArmorStatus", "Muestra tu armadura, el objeto en mano y su durabilidad.", Category.HUD, true);
	}

	/** Dibuja la armadura y devuelve la altura ocupada. */
	public int render(GuiGraphics graphics, Minecraft client, int x, int y) {
		LocalPlayer player = client.player;
		if (player == null) return 0;

		int drawn = 0;
		for (EquipmentSlot slot : SLOTS) {
			ItemStack stack = player.getItemBySlot(slot);
			if (stack.isEmpty()) continue;

			int itemY = y + drawn * 18;
			graphics.renderItem(stack, x, itemY);
			graphics.renderItemDecorations(client.font, stack, x, itemY);

			if (stack.isDamageableItem()) {
				int remaining = stack.getMaxDamage() - stack.getDamageValue();
				graphics.drawString(client.font, String.valueOf(remaining), x + 20, itemY + 4, durabilityColor(stack), true);
			}
			drawn++;
		}
		return drawn * 18;
	}

	private static int durabilityColor(ItemStack stack) {
		float fraction = 1.0F - (float) stack.getDamageValue() / stack.getMaxDamage();
		if (fraction > 0.6F) return 0xFF55FF55;
		if (fraction > 0.25F) return 0xFFFFFF55;
		return 0xFFFF5555;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
