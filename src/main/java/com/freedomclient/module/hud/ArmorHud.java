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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.freedomclient.ui.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class ArmorHud extends HudModule {
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final int SLOT = 18;
	/** Ancho de las dos exclamaciones de Armor Alert (con su borde). */
	private static final int MARK = 13;

	private final ModeSetting layout = add(new ModeSetting("Layout", "Stack the items vertically or horizontally.", "Vertical", "Vertical", "Horizontal"));
	private final ModeSetting durability = add(new ModeSetting("Durability", "How to show durability.", "Number", "Number", "Percent", "None"));
	private final BooleanSetting showHeld = add(new BooleanSetting("Show held item", "Also show the item in your hand.", true));
	private final BooleanSetting showOffhand = add(new BooleanSetting("Show offhand", "Also show the item in your other hand (shield, totem...).", false));
	private final BooleanSetting alert = add(new BooleanSetting("Armor alert", "Two red exclamation marks that bob and pulse next to armor that is about to break.", true));
	private final NumberSetting threshold = add(new NumberSetting("Warn below", "Durability that triggers the alert.", 15, 5, 50, 1, "%"));
	private final BooleanSetting weaponAlert = add(new BooleanSetting("Tool alert",
			"Also warn when the tool or weapon in your hand (pickaxe, sword, axe, shovel, bow, shield...) is about to break.", true));
	private final BooleanSetting sound = add(new BooleanSetting("Alert sound", "Play a sound when a piece becomes low.", true));
	private boolean wasLow;

	public ArmorHud() {
		super("Armor HUD", "Shows your armor and held item with their durability, and warns you when armor is about to break.", true,
				new HudPosition(HudPosition.Anchor.END, 2, HudPosition.Anchor.END, 2));
		threshold.visibleWhen(alert::get);
		weaponAlert.visibleWhen(alert::get);
		showOffhand.visibleWhen(showHeld::get);
		sound.visibleWhen(alert::get);
	}

	/** Armor Alert: la pieza de armadura (o la herramienta en la mano) está por debajo del umbral. */
	private boolean isLow(ItemStack stack) {
		if (!alert.get() || !stack.isDamageableItem() || !(isArmor(stack) || weaponAlert.get() && isHeld(stack))) return false;
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

	/** Cualquier cosa con durabilidad en una mano: picos, palas, azadas, espadas, hachas, arcos, escudos, tijeras... */
	private static boolean isHeld(ItemStack stack) {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null && (player.getMainHandItem() == stack || player.getOffhandItem() == stack);
	}

	@Override
	public void onTick(Minecraft client) {
		boolean low = false;
		if (client.player != null) {
			for (EquipmentSlot slot : ARMOR) {
				low |= isLow(client.player.getItemBySlot(slot));
			}
			low |= isLow(client.player.getMainHandItem());
			low |= isLow(client.player.getOffhandItem());
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
			if (showHeld.get() && showOffhand.get() && !player.getOffhandItem().isEmpty()) items.add(player.getOffhandItem());
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
		return vertical() ? SLOT + labelWidth(client) : count * (SLOT + 2) + (alert.get() ? 4 : 0);
	}

	@Override
	public int getHeight(Minecraft client, boolean preview) {
		int count = Math.max(1, items(client, preview).size());
		return vertical() ? count * SLOT : SLOT + (durability.is("None") ? 0 : 9) + (alert.get() ? 6 : 0);
	}

	@Override
	public void render(GuiGraphics graphics, Minecraft client, boolean preview) {
		List<ItemStack> items = items(client, preview);
		float time = (System.currentTimeMillis() % 100_000L) / 1000.0F;
		// El texto del aviso late entre rojo claro y rojo oscuro.
		int red = mix(0xFFB0101E, 0xFFFF3B3B, 0.5F + 0.5F * (float) Math.sin(time * 6.0));
		int top = vertical() ? 0 : (alert.get() ? 6 : 0);

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
				if (low) exclamations(graphics, labelX + 1 + MARK / 2.0F, y + SLOT / 2.0F, time + i * 0.4F);
			} else {
				if (label != null) {
					graphics.drawString(client.font, label, x + (SLOT - client.font.width(label)) / 2 + 1, y + SLOT + 1, color, true);
				}
				if (low) exclamations(graphics, x + SLOT - 2, y - 1, time + i * 0.4F);
			}
		}
	}

	/**
	 * Dos exclamaciones centradas en (cx, cy) que suben y bajan despacio (cada una un poco desfasada, como una ola)
	 * y laten: crecen un poco y se encienden a la vez.
	 */
	private static void exclamations(GuiGraphics graphics, float cx, float cy, float time) {
		float beat = 0.5F + 0.5F * (float) Math.sin(time * 6.0);
		float scale = 1.0F + 0.14F * beat;
		for (int k = 0; k < 2; k++) {
			float bob = (float) Math.sin(time * 3.2 + k * 0.9) * 1.6F;
			int red = mix(0xFFC0101E, 0xFFFF4545, (0.5F + 0.5F * (float) Math.sin(time * 6.0 + k * 0.5)));
			graphics.pose().pushMatrix();
			graphics.pose().translate(cx + (k == 0 ? -3.0F : 3.0F), cy + bob);
			graphics.pose().scale(scale, scale);
			exclamation(graphics, -2, -6, red, beat);
			graphics.pose().popMatrix();
		}
	}

	/** Exclamación pixel de 3x12 con borde oscuro y un brillo arriba cuando late. */
	private static void exclamation(GuiGraphics graphics, int x, int y, int red, float beat) {
		graphics.fill(x - 1, y - 1, x + 4, y + 8, 0xFF3A0508);
		graphics.fill(x - 1, y + 9, x + 4, y + 13, 0xFF3A0508);
		graphics.fill(x, y, x + 3, y + 7, red);
		graphics.fill(x, y + 10, x + 3, y + 12, red);
		graphics.fill(x, y, x + 1, y + 3, mix(red, 0xFFFFD0D0, 0.3F + 0.4F * beat));
	}

	private static int mix(int a, int b, float t) {
		return ThemeManager.mix(a, b, Math.max(0.0F, Math.min(1.0F, t)));
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
