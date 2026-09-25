package com.freedomclient.module.visual;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.Optional;

/** Shulker Box Tooltip: muestra el contenido de las cajas de shulker (y otros contenedores) en el tooltip. */
public class ShulkerPreviewModule extends Module {
	private static final int COLUMNS = 9;
	private static final int ROWS = 3;

	public ShulkerPreviewModule() {
		super("Shulker Preview", "Shows the items inside shulker boxes when you hover them.", Category.VISUAL, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** Datos del tooltip: las 27 casillas del contenedor. */
	public record Contents(NonNullList<ItemStack> items) implements TooltipComponent {
	}

	/** Llamado desde ItemStackMixin: devuelve la vista previa si el objeto tiene contenido. */
	public static Optional<TooltipComponent> preview(ItemStack stack) {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null || !manager.get(ShulkerPreviewModule.class).isEnabled()) return Optional.empty();

		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
		if (contents == null || contents.nonEmptyStream().findAny().isEmpty()) return Optional.empty();

		NonNullList<ItemStack> items = NonNullList.withSize(COLUMNS * ROWS, ItemStack.EMPTY);
		contents.copyInto(items);
		return Optional.of(new Contents(items));
	}

	/** Cómo se dibuja la vista previa: una cuadrícula 9x3 como la del cofre. */
	public static class PreviewComponent implements ClientTooltipComponent {
		private static final int SLOT = 18;
		private final NonNullList<ItemStack> items;

		public PreviewComponent(Contents contents) {
			this.items = contents.items();
		}

		@Override
		public int getHeight(Font font) {
			return ROWS * SLOT + 4;
		}

		@Override
		public int getWidth(Font font) {
			return COLUMNS * SLOT;
		}

		@Override
		public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics graphics) {
			graphics.fill(x, y, x + COLUMNS * SLOT, y + ROWS * SLOT, 0x60000000);
			for (int i = 0; i < Math.min(items.size(), COLUMNS * ROWS); i++) {
				int slotX = x + (i % COLUMNS) * SLOT;
				int slotY = y + (i / COLUMNS) * SLOT;
				graphics.renderOutline(slotX, slotY, SLOT, SLOT, 0x30FFFFFF);
				ItemStack stack = items.get(i);
				if (!stack.isEmpty()) {
					graphics.renderItem(stack, slotX + 1, slotY + 1);
					graphics.renderItemDecorations(font, stack, slotX + 1, slotY + 1);
				}
			}
		}
	}
}
