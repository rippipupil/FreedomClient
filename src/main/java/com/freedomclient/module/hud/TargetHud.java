package com.freedomclient.module.hud;

import com.freedomclient.hud.CombatTracker;
import com.freedomclient.hud.HudModule;
import com.freedomclient.hud.HudPosition;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/** Panel con la cabeza, el nombre, la vida y la armadura del jugador al que estás pegando o apuntando. */
public class TargetHud extends HudModule {
	private static final int WIDTH = 124;
	private static final int HEIGHT = 40;
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	private final BooleanSetting showArmor = add(new BooleanSetting("Show armor", "Show the target's armor.", true));
	private final ColorSetting backgroundColor = add(new ColorSetting("Background color", "Color of the panel.", 0xC03A0F1A, true));
	private final ColorSetting barColor = add(new ColorSetting("Health bar color", "Color of the health bar.", 0xFFD7263D, false));

	private float displayedHealth = -1;

	public TargetHud() {
		super("Target HUD", "Shows the health and armor of the player you are fighting.", true,
				new HudPosition(HudPosition.Anchor.CENTER, 70, HudPosition.Anchor.CENTER, 30));
	}

	private LivingEntity target(Minecraft client) {
		LivingEntity target = CombatTracker.getTarget(5000);
		if (target == null && client.crosshairPickEntity instanceof LivingEntity aimed && aimed.isAlive()) target = aimed;
		return target != null && target.isAlive() ? target : null;
	}

	@Override
	public boolean shouldRender(Minecraft client) {
		return target(client) != null;
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
		LivingEntity target = target(client);
		if (target == null && preview) target = client.player;
		if (target == null) return;

		graphics.fill(0, 0, WIDTH, HEIGHT, backgroundColor.get());
		graphics.fill(0, 0, WIDTH, 1, barColor.get());

		// Cabeza del jugador (o el icono del mob como objeto si no es un jugador).
		if (target instanceof AbstractClientPlayer player) {
			PlayerFaceRenderer.draw(graphics, player.getSkin(), 4, 4, 24);
		} else {
			graphics.fill(4, 4, 28, 28, 0x40000000);
		}

		graphics.drawString(client.font, client.font.plainSubstrByWidth(target.getName().getString(), WIDTH - 36), 32, 5, 0xFFF5F1E8, true);

		float health = target.getHealth() + target.getAbsorptionAmount();
		float max = Math.max(1.0F, target.getMaxHealth());
		if (displayedHealth < 0 || Math.abs(displayedHealth - health) > max) displayedHealth = health;
		displayedHealth += (health - displayedHealth) * 0.2F;

		String healthText = String.format(Locale.ROOT, "%.1f", health) + " ❤";
		graphics.drawString(client.font, healthText, 32, 16, 0xFFFF5555, true);

		int barX = 4;
		int barY = 32;
		int barW = WIDTH - 8;
		graphics.fill(barX, barY, barX + barW, barY + 4, 0x80000000);
		graphics.fill(barX, barY, barX + Math.round(barW * Math.min(1.0F, displayedHealth / max)), barY + 4, barColor.get());

		if (showArmor.get()) {
			int x = WIDTH - 4 - 4 * 13;
			for (EquipmentSlot slot : ARMOR) {
				ItemStack stack = target.getItemBySlot(slot);
				if (!stack.isEmpty()) {
					graphics.pose().pushMatrix();
					graphics.pose().translate(x, 14);
					graphics.pose().scale(0.75F, 0.75F);
					graphics.renderItem(stack, 0, 0);
					graphics.pose().popMatrix();
				}
				x += 13;
			}
		}
	}
}
