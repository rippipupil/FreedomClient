package com.freedomclient.module.hud;

import com.freedomclient.hud.HudModule;
import com.freedomclient.hud.HudPosition;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Muestra las 27 casillas del inventario principal (sin la barra rápida). */
public class InventoryHud extends HudModule {
	private static final int COLUMNS = 9;
	private static final int ROWS = 3;
	private static final int SLOT = 18;
	private static final int PADDING = 3;

	private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a box behind the inventory.", true));
	private final ColorSetting backgroundColor = add(new ColorSetting("Background color", "Color of the box.", 0x803A0F1A, true));
	private final BooleanSetting slotOutlines = add(new BooleanSetting("Slot outlines", "Draw a frame around every slot.", true));

	public InventoryHud() {
		super("Inventory HUD", "Shows the items in your main inventory.", false, new HudPosition(HudPosition.Anchor.START, 2, HudPosition.Anchor.CENTER, -40));
		backgroundColor.visibleWhen(background::get);
	}

	@Override
	public int getWidth(Minecraft client, boolean preview) {
		return COLUMNS * SLOT + PADDING * 2;
	}

	@Override
	public int getHeight(Minecraft client, boolean preview) {
		return ROWS * SLOT + PADDING * 2;
	}

	@Override
	public void render(GuiGraphics graphics, Minecraft client, boolean preview) {
		if (background.get()) {
			graphics.fill(0, 0, getWidth(client, preview), getHeight(client, preview), backgroundColor.get());
		}

		LocalPlayer player = client.player;
		List<ItemStack> items = player != null ? player.getInventory().getNonEquipmentItems() : List.of();
		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				int x = PADDING + column * SLOT;
				int y = PADDING + row * SLOT;
				if (slotOutlines.get()) graphics.renderOutline(x, y, SLOT, SLOT, 0x40FFFFFF);

				// Las casillas 0-8 son la barra rápida; el inventario principal empieza en la 9.
				int index = COLUMNS + row * COLUMNS + column;
				if (index < items.size()) {
					ItemStack stack = items.get(index);
					if (!stack.isEmpty()) {
						graphics.renderItem(stack, x + 1, y + 1);
						graphics.renderItemDecorations(client.font, stack, x + 1, y + 1);
					}
				}
			}
		}
	}
}
