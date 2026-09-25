package com.freedomclient.module.hud;

import com.freedomclient.hud.HudModule;
import com.freedomclient.hud.HudPosition;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Cuenta los objetos importantes para PvP que llevas: tótems, manzanas, perlas, flechas y pociones arrojadizas. */
public class ItemCounterHud extends HudModule {
	private record Counter(BooleanSetting setting, ItemStack icon, Set<Item> items) {
	}

	private static final int SLOT = 18;

	private final ModeSetting layout = add(new ModeSetting("Layout", "Stack vertically or horizontally.", "Horizontal", "Horizontal", "Vertical"));
	private final BooleanSetting hideZero = add(new BooleanSetting("Hide empty", "Hide items you don't have.", true));
	private final List<Counter> counters = new ArrayList<>();

	public ItemCounterHud() {
		super("Item Counter", "Counts your totems, golden apples, pearls, arrows and splash potions.", false,
				new HudPosition(HudPosition.Anchor.END, 2, HudPosition.Anchor.START, 40));
		counter("Totems", Items.TOTEM_OF_UNDYING, Set.of(Items.TOTEM_OF_UNDYING));
		counter("Golden apples", Items.GOLDEN_APPLE, Set.of(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE));
		counter("Ender pearls", Items.ENDER_PEARL, Set.of(Items.ENDER_PEARL));
		counter("Arrows", Items.ARROW, Set.of(Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW));
		counter("Splash potions", Items.SPLASH_POTION, Set.of(Items.SPLASH_POTION));
		counter("Experience bottles", Items.EXPERIENCE_BOTTLE, Set.of(Items.EXPERIENCE_BOTTLE));
	}

	private void counter(String name, Item icon, Set<Item> items) {
		BooleanSetting setting = add(new BooleanSetting(name, "Count " + name.toLowerCase(java.util.Locale.ROOT) + ".", true));
		counters.add(new Counter(setting, new ItemStack(icon), items));
	}

	private record Entry(ItemStack icon, int count) {
	}

	private List<Entry> entries(Minecraft client, boolean preview) {
		List<Entry> entries = new ArrayList<>();
		LocalPlayer player = client.player;
		for (Counter counter : counters) {
			if (!counter.setting().get()) continue;
			int count = 0;
			if (player != null) {
				for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
					if (counter.items().contains(stack.getItem())) count += stack.getCount();
				}
				ItemStack offhand = player.getOffhandItem();
				if (counter.items().contains(offhand.getItem())) count += offhand.getCount();
			}
			if (preview && count == 0) count = 8;
			if (count > 0 || !hideZero.get()) entries.add(new Entry(counter.icon(), count));
		}
		return entries;
	}

	private boolean vertical() {
		return layout.is("Vertical");
	}

	@Override
	public boolean shouldRender(Minecraft client) {
		return !entries(client, false).isEmpty();
	}

	@Override
	public int getWidth(Minecraft client, boolean preview) {
		int count = Math.max(1, entries(client, preview).size());
		return vertical() ? SLOT + client.font.width("999") + 2 : count * (SLOT + 4);
	}

	@Override
	public int getHeight(Minecraft client, boolean preview) {
		int count = Math.max(1, entries(client, preview).size());
		return vertical() ? count * SLOT : SLOT;
	}

	@Override
	public void render(GuiGraphics graphics, Minecraft client, boolean preview) {
		List<Entry> entries = entries(client, preview);
		for (int i = 0; i < entries.size(); i++) {
			Entry entry = entries.get(i);
			int x = vertical() ? 0 : i * (SLOT + 4);
			int y = vertical() ? i * SLOT : 0;
			graphics.renderItem(entry.icon(), x + 1, y + 1);

			String count = String.valueOf(entry.count());
			int color = entry.count() == 0 ? 0xFFFF5555 : 0xFFFFFFFF;
			if (vertical()) {
				graphics.drawString(client.font, count, x + SLOT + 2, y + 5, color, true);
			} else {
				graphics.drawString(client.font, count, x + SLOT + 2 - client.font.width(count), y + 10, color, true);
			}
		}
	}
}
