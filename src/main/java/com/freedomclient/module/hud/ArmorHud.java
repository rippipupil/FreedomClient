package com.freedomclient.module.hud;

import com.freedomclient.hud.HudModule;
import com.freedomclient.hud.HudPosition;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ArmorHud extends HudModule {
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final int SLOT = 18;
	/** Ancho de la exclamación de Armor Alert. */
	private static final int MARK = 7;

	private final ModeSetting layout = add(new ModeSetting("Layout", "Stack the items vertically or horizontally.", "Vertical", "Vertical", "Horizontal"));
	private final ModeSetting durability = add(new ModeSetting("Durability", "How to show durability.", "Number", "Number", "Percent", "None"));
	private final BooleanSetting showHeld = add(new BooleanSetting("Show held item", "Also show the item in your hand.", true));
	private final BooleanSetting alert = add(new BooleanSetting("Armor alert", "Red animated exclamation mark next to armor that is about to break.", true));
	private final NumberSetting threshold = add(new NumberSetting("Warn below", "Durability that triggers the alert.", 15, 5, 50, 1, "%"));
	private final BooleanSetting weaponAlert = add(new BooleanSetting("Weapon alert", "Also warn when your sword, axe, mace or trident is about to break.", true));
	private final BooleanSetting sound = add(new BooleanSetting("Alert sound", "Play a sound when a piece becomes low.", true));
	private boolean wasLow;

	public ArmorHud() {
		super("Armor HUD", "Shows your armor and held item with their durability, and warns you when armor is about to break.", true,
				new HudPosition(HudPosition.Anchor.END, 2, HudPosition.Anchor.END, 2));
		threshold.visibleWhen(alert::get);
		weaponAlert.visibleWhen(alert::get);
		sound.visibleWhen(alert::get);
	}

	/** Armor Alert: la pieza de armadura (o el arma en la mano) está por debajo del umbral. */
	private boolean isLow(ItemStack stack) {
		if (!alert.get() || !stack.isDamageableItem() || !(isArmor(stack) || weaponAlert.get() && isWeapon(stack))) return false;
		float fraction = 1.0F - (float) stack.getDamageValue() / stack.getMaxDamage();
		return fraction <= threshold.getFloat() / 100.0F;
	}

	private static boolean isArmor(ItemStack stack) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return true;
		for (EquipmentSlot slot : ARMOR) {
			if (player.getItemBySlot(slot) == stack) return true;
		}
		return false;
	}

	private static boolean isWeapon(ItemStack stack) {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null && player.getMainHandItem() == stack
				&& (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(Items.MACE) || stack.is(Items.TRIDENT));
	}

	@Override
	public void onTick(Minecraft client) {
		boolean low = false;
		if (client.player != null) {
			for (EquipmentSlot slot : ARMOR) {
				low |= isLow(client.player.getItemBySlot(slot));
			}
			low |= isLow(client.player.getMainHandItem());
		}
		if (low && !wasLow && sound.get()) {
			client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 1.6F, 0.4F));
		}
		wasLow = low;
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
		return (durability.is("None") ? 0 : client.font.width("100%") + 2) + (alert.get() ? MARK : 0);
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
		return vertical() ? count * SLOT : SLOT + (durability.is("None") ? 0 : 9) + (alert.get() ? 3 : 0);
	}

	@Override
	public void render(GuiGraphics graphics, Minecraft client, boolean preview) {
		List<ItemStack> items = items(client, preview);
		// La exclamación rebota y parpadea entre rojo y rojo oscuro.
		double time = System.currentTimeMillis() / 1000.0;
		int bounce = (int) Math.round(Math.abs(Math.sin(time * 5.0)) * -2.0);
		int red = Math.sin(time * 10.0) > 0 ? 0xFFFF3B3B : 0xFFB0101E;
		int top = vertical() ? 0 : (alert.get() ? 3 : 0);

		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(i);
			int x = vertical() ? 0 : i * (SLOT + 2);
			int y = vertical() ? i * SLOT : top;
			boolean low = isLow(stack);
			graphics.renderItem(stack, x + 1, y + 1);
			graphics.renderItemDecorations(client.font, stack, x + 1, y + 1);

			String label = durabilityLabel(stack);
			int color = low ? red : (label != null ? durabilityColor(stack) : 0);
			if (vertical()) {
				int labelX = x + SLOT + 2;
				if (label != null) {
					graphics.drawString(client.font, label, labelX, y + 5, color, true);
					labelX += client.font.width(label) + 2;
				}
				if (low) exclamation(graphics, labelX + 1, y + 3 + bounce, red);
			} else {
				if (label != null) {
					graphics.drawString(client.font, label, x + (SLOT - client.font.width(label)) / 2 + 1, y + SLOT + 1, color, true);
				}
				if (low) exclamation(graphics, x + SLOT - 3, y - 3 + bounce, red);
			}
		}
	}

	/** Exclamación pixel de 3x10 con borde oscuro. */
	private static void exclamation(GuiGraphics graphics, int x, int y, int red) {
		graphics.fill(x - 1, y - 1, x + 4, y + 8, 0xFF3A0508);
		graphics.fill(x - 1, y + 9, x + 4, y + 13, 0xFF3A0508);
		graphics.fill(x, y, x + 3, y + 7, red);
		graphics.fill(x, y + 10, x + 3, y + 12, red);
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
