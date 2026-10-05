package com.freedomclient.module.visual;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shulker Box Tooltip: mientras mantienes Shift sobre una caja de shulker (u otro contenedor), muestra su contenido en
 * el tooltip, como una ventanita de cofre con una línea de resumen (casillas usadas y objetos). Si el mod INV está activo, la ventanita
 * usa su aspecto (fondo, imagen, borde y casillas), igual que tu inventario.
 */
public class ShulkerPreviewModule extends Module {
	private static final int COLUMNS = 9;
	private static final int ROWS = 3;
	private static ShulkerPreviewModule instance;

	private final BooleanSetting summary = add(new BooleanSetting("Summary", "A line with the used slots and how many items there are.", true));
	private final BooleanSetting holdShift = add(new BooleanSetting("Hold Shift", "Only show the contents while you hold Shift over the shulker box.", true));
	private final BooleanSetting invStyle = add(new BooleanSetting("Use INV look", "Use the look of your inventory from the INV mod when it is on.", true));

	public ShulkerPreviewModule() {
		super("Shulker Preview", "Hold Shift over a shulker box to see the items inside, in a small window like your inventory.",
				Category.VISUAL, true);
		instance = this;
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
		if (manager == null) return Optional.empty();
		ShulkerPreviewModule module = manager.get(ShulkerPreviewModule.class);
		if (!module.isEnabled() || module.holdShift.get() && !shiftDown()) return Optional.empty();

		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
		if (contents == null || contents.nonEmptyStream().findAny().isEmpty()) return Optional.empty();

		NonNullList<ItemStack> items = NonNullList.withSize(COLUMNS * ROWS, ItemStack.EMPTY);
		contents.copyInto(items);
		return Optional.of(new Contents(items));
	}

	private static boolean shiftDown() {
		com.mojang.blaze3d.platform.Window window = net.minecraft.client.Minecraft.getInstance().getWindow();
		return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
				|| com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	/** Cómo se dibuja la vista previa: resumen arriba y una ventanita 9x3 como la del cofre. */
	public static class PreviewComponent implements ClientTooltipComponent {
		private static final int SLOT = 18;
		private static final int PAD = 4;
		private static final int HEADER = 11;
		private final NonNullList<ItemStack> items;
		private final String info;

		public PreviewComponent(Contents contents) {
			this.items = contents.items();
			int used = 0;
			int total = 0;
			for (ItemStack stack : items) {
				if (stack.isEmpty()) continue;
				used++;
				total += stack.getCount();
			}
			this.info = used + "/" + COLUMNS * ROWS + " slots  -  " + total + (total == 1 ? " item" : " items");
		}

		private static boolean showSummary() {
			return instance == null || instance.summary.get();
		}

		private int headerHeight() {
			return showSummary() ? HEADER : 0;
		}

		@Override
		public int getHeight(Font font) {
			return headerHeight() + ROWS * SLOT + PAD * 2 + 2;
		}

		@Override
		public int getWidth(Font font) {
			return Math.max(COLUMNS * SLOT + PAD * 2, showSummary() ? font.width(info) + 2 : 0);
		}

		@Override
		public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics graphics) {
			if (showSummary()) graphics.drawString(font, info, x + 1, y + 1, 0xFFB8B8C8, false);
			int windowY = y + headerHeight();
			int windowW = COLUMNS * SLOT + PAD * 2;
			int windowH = ROWS * SLOT + PAD * 2;
			InvModule inv = instance != null && instance.invStyle.get() ? InvModule.styled() : null;
			if (inv != null) {
				inv.drawWindow(graphics, x, windowY, windowW, windowH, slots(), false);
			} else {
				// Aspecto limpio con los colores del tema: panel oscuro, borde fino y casillas suaves.
				graphics.fill(x, windowY, x + windowW, windowY + windowH, 0xC0101018);
				graphics.renderOutline(x, windowY, windowW, windowH, ThemeManager.withAlpha(ThemeManager.border(), 0.8F));
				for (int i = 0; i < COLUMNS * ROWS; i++) {
					int sx = x + PAD + (i % COLUMNS) * SLOT;
					int sy = windowY + PAD + (i / COLUMNS) * SLOT;
					graphics.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, items.get(i).isEmpty() ? 0x28FFFFFF : 0x40FFFFFF);
				}
			}
			for (int i = 0; i < Math.min(items.size(), COLUMNS * ROWS); i++) {
				ItemStack stack = items.get(i);
				if (stack.isEmpty()) continue;
				int sx = x + PAD + (i % COLUMNS) * SLOT + 1;
				int sy = windowY + PAD + (i / COLUMNS) * SLOT + 1;
				graphics.renderItem(stack, sx, sy);
				graphics.renderItemDecorations(font, stack, sx, sy);
			}
		}

		/** Casillas falsas para dibujar la ventana con el aspecto del mod INV (van 1 px dentro de su marco de 18). */
		private static List<Slot> slots() {
			List<Slot> slots = new ArrayList<>();
			SimpleContainer container = new SimpleContainer(COLUMNS * ROWS);
			for (int i = 0; i < COLUMNS * ROWS; i++) {
				slots.add(new Slot(container, i, PAD + (i % COLUMNS) * SLOT + 1, PAD + (i / COLUMNS) * SLOT + 1));
			}
			return slots;
		}
	}
}
