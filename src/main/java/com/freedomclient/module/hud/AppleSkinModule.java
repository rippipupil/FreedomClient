package com.freedomclient.module.hud;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * AppleSkin: muestra la saturación sobre la barra de hambre, lo que recuperarías con la comida que llevas en la mano
 * y los valores de comida en el tooltip de los alimentos.
 */
public class AppleSkinModule extends Module {
	private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
	private static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");
	private static final int SATURATION_COLOR = 0xFFF2C94C;

	private final BooleanSetting saturationOverlay = add(new BooleanSetting("Saturation overlay", "Show your saturation over the hunger bar.", true));
	private final BooleanSetting foodPreview = add(new BooleanSetting("Food preview", "Flash the hunger you would restore with the food in your hand.", true));
	private final BooleanSetting tooltips = add(new BooleanSetting("Tooltip values", "Show hunger and saturation in food tooltips.", true));

	public AppleSkinModule() {
		super("AppleSkin", "Shows saturation, food previews and food values.", Category.HUD, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** Se dibuja justo después de la barra de hambre de vanilla. */
	public void renderOverlay(GuiGraphics graphics, Minecraft client) {
		if (!isEnabled() || client.options.hideGui) return;
		LocalPlayer player = client.player;
		if (player == null || player.isCreative() || player.isSpectator()) return;
		if (player.getVehicle() instanceof LivingEntity) return;

		FoodData food = player.getFoodData();
		int right = graphics.guiWidth() / 2 + 91;
		int top = graphics.guiHeight() - 39;

		FoodProperties held = heldFood(player);
		float pulse = (float) (0.35 + 0.35 * Math.sin(System.currentTimeMillis() / 150.0));

		if (foodPreview.get() && held != null) {
			int newFood = Math.min(20, food.getFoodLevel() + held.nutrition());
			for (int i = 0; i < 10; i++) {
				int value = i * 2 + 1;
				if (value <= food.getFoodLevel() || value > newFood + 1) continue;
				Identifier sprite = value < newFood ? FOOD_FULL : value == newFood ? FOOD_HALF : null;
				if (sprite != null) {
					graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, right - i * 8 - 9, top, 9, 9, pulse + 0.25F);
				}
			}
		}

		if (saturationOverlay.get()) {
			renderSaturation(graphics, right, top, food.getSaturationLevel(), SATURATION_COLOR);
			if (foodPreview.get() && held != null) {
				int newFood = Math.min(20, food.getFoodLevel() + held.nutrition());
				float newSaturation = Math.min(newFood, food.getSaturationLevel() + held.saturation());
				int alpha = (int) (pulse * 255.0F) << 24;
				renderSaturation(graphics, right, top, newSaturation, alpha | (SATURATION_COLOR & 0xFFFFFF));
			}
		}
	}

	/** Contorno dorado sobre cada icono de comida según la saturación (2 puntos por icono). */
	private static void renderSaturation(GuiGraphics graphics, int right, int top, float saturation, int color) {
		for (int i = 0; i < 10; i++) {
			float amount = saturation - i * 2;
			if (amount <= 0) break;
			int x = right - i * 8 - 9;
			boolean half = amount < 2;
			int left = half ? x + 5 : x;
			graphics.fill(left, top, x + 9, top + 1, color);
			graphics.fill(left, top + 8, x + 9, top + 9, color);
			graphics.fill(x + 8, top + 1, x + 9, top + 8, color);
			if (!half) graphics.fill(x, top + 1, x + 1, top + 8, color);
		}
	}

	private static FoodProperties heldFood(LocalPlayer player) {
		FoodProperties food = player.getMainHandItem().get(DataComponents.FOOD);
		return food != null ? food : player.getOffhandItem().get(DataComponents.FOOD);
	}

	/** Añade los valores de comida al tooltip de un alimento. */
	public void appendTooltip(ItemStack stack, List<Component> lines) {
		if (!isEnabled() || !tooltips.get()) return;
		FoodProperties food = stack.get(DataComponents.FOOD);
		if (food == null) return;

		lines.add(Component.literal("Hunger: +" + food.nutrition() + "  Saturation: +"
				+ String.format(Locale.ROOT, "%.1f", food.saturation())).withStyle(ChatFormatting.GOLD));
	}
}
