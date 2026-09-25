package com.freedomclient.module.pvp;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Custom Attack Indicator: sustituye el indicador de ataque de vanilla por uno configurable
 * (espada pixel, barra, anillo o porcentaje), bajo la mira o junto a la barra de objetos.
 */
public class AttackIndicatorModule extends Module {
	private static final long FLASH_MS = 400;
	/** Espada pixel de 12x12: o = contorno, b = hoja (se rellena con la carga), h = empuñadura. */
	private static final String[] SWORD = {
			".........ooo",
			"........obbo",
			".......obbo.",
			"......obbo..",
			".o...obbo...",
			".oo.obbo....",
			"..oobbo.....",
			"...ohoo.....",
			"..ohooo.....",
			".oho..oo....",
			"oho.........",
			"oo..........",
	};

	private final ModeSetting style = add(new ModeSetting("Style", "How the indicator looks.", "Sword", "Sword", "Bar", "Circle", "Percent"));
	private final ModeSetting position = add(new ModeSetting("Position", "Where the indicator is drawn.", "Crosshair", "Crosshair", "Hotbar"));
	private final NumberSetting scale = add(new NumberSetting("Scale", "Size of the indicator.", 1, 0.5, 3, 0.25, "x"));
	private final ModeSetting showWhen = add(new ModeSetting("Show", "When the indicator is visible.", "Charging + flash",
			"Charging", "Charging + flash", "Always"));
	private final BooleanSetting onlyWeapons = add(new BooleanSetting("Only with weapons", "Only show it while holding a sword, axe, mace or trident.", false));
	private final ColorSetting chargingColor = add(new ColorSetting("Charging color", "Color while the attack is charging.", 0xFFF5F1E8, true));
	private final ColorSetting readyColor = add(new ColorSetting("Ready color", "Color when the attack is fully charged.", 0xFFF2C94C, true));
	private final BooleanSetting targetColorEnabled = add(new BooleanSetting("Target color", "Different color when ready and aiming at a player or mob.", true));
	private final ColorSetting targetColor = add(new ColorSetting("Target ready color", "Color when ready and aiming at an entity.", 0xFFFF3B3B, true));
	private final ColorSetting backgroundColor = add(new ColorSetting("Background", "Color of the empty part.", 0x90303030, true));

	private float lastStrength = 1.0F;
	private long readyAt;

	public AttackIndicatorModule() {
		super("Custom Attack Indicator", "Replaces the vanilla attack indicator with a customizable sword, bar, ring or percent.", Category.PVP, false);
		targetColor.visibleWhen(targetColorEnabled::get);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	private boolean holdingWeapon(LocalPlayer player) {
		ItemStack stack = player.getMainHandItem();
		return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(Items.MACE) || stack.is(Items.TRIDENT);
	}

	public void render(GuiGraphics graphics, Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.options.hideGui || player.isSpectator()) return;

		float strength = player.getAttackStrengthScale(0.0F);
		long now = System.currentTimeMillis();
		if (strength >= 1.0F && lastStrength < 1.0F) readyAt = now;
		lastStrength = strength;

		if (onlyWeapons.get() && !holdingWeapon(player)) return;
		boolean ready = strength >= 1.0F;
		boolean flashing = now - readyAt < FLASH_MS;
		if (ready && !(showWhen.is("Always") || showWhen.is("Charging + flash") && flashing)) return;
		boolean crosshair = position.is("Crosshair");
		if (crosshair && !client.options.getCameraType().isFirstPerson()) return;

		boolean aiming = client.crosshairPickEntity instanceof LivingEntity living && living.isAlive();
		int fill = !ready ? chargingColor.get() : (aiming && targetColorEnabled.get() ? targetColor.get() : readyColor.get());
		if (ready && flashing && !showWhen.is("Charging")) {
			// Parpadeo breve al cargarse del todo.
			fill = ((now - readyAt) / 100) % 2 == 0 ? fill : 0xFFFFFFFF;
		}

		float x;
		float y;
		if (crosshair) {
			x = graphics.guiWidth() / 2.0F;
			y = graphics.guiHeight() / 2.0F + 12;
		} else {
			boolean right = player.getMainArm() == HumanoidArm.RIGHT;
			x = graphics.guiWidth() / 2.0F + (right ? 91 + 16 : -91 - 16);
			y = graphics.guiHeight() - 11;
		}

		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		float s = scale.getFloat();
		graphics.pose().scale(s, s);
		switch (style.get()) {
			case "Bar" -> bar(graphics, strength, fill, crosshair);
			case "Circle" -> circle(graphics, strength, fill, crosshair);
			case "Percent" -> percent(graphics, client, strength, fill);
			default -> sword(graphics, strength, fill);
		}
		graphics.pose().popMatrix();
	}

	/** Espada que se llena en diagonal desde la empuñadura hasta la punta. */
	private void sword(GuiGraphics graphics, float strength, int fill) {
		int size = SWORD.length;
		int left = -size / 2;
		int top = -size / 2;
		for (int row = 0; row < size; row++) {
			for (int col = 0; col < size; col++) {
				char c = SWORD[row].charAt(col);
				if (c == '.') continue;
				int color;
				if (c == 'o') {
					color = 0xE0100508;
				} else if (c == 'h') {
					color = 0xFFC98F1E;
				} else {
					float along = (col + (size - 1 - row)) / (float) (2 * (size - 1));
					color = along <= strength ? fill : backgroundColor.get();
				}
				graphics.fill(left + col, top + row, left + col + 1, top + row + 1, color);
			}
		}
	}

	private void bar(GuiGraphics graphics, float strength, int fill, boolean crosshair) {
		if (crosshair) {
			int width = 16;
			graphics.fill(-width / 2 - 1, -2, width / 2 + 1, 2, 0xE0100508);
			graphics.fill(-width / 2, -1, width / 2, 1, backgroundColor.get());
			graphics.fill(-width / 2, -1, -width / 2 + Math.round(width * strength), 1, fill);
		} else {
			int height = 16;
			graphics.fill(-3, -height / 2 - 1, 3, height / 2 + 1, 0xE0100508);
			graphics.fill(-2, -height / 2, 2, height / 2, backgroundColor.get());
			graphics.fill(-2, height / 2 - Math.round(height * strength), 2, height / 2, fill);
		}
	}

	/** Anillo pixel que se rellena en el sentido de las agujas del reloj desde arriba. */
	private void circle(GuiGraphics graphics, float strength, int fill, boolean crosshair) {
		int radius = 6;
		// Bajo la mira el anillo rodea el centro de la pantalla en vez de quedar debajo.
		int centerY = crosshair ? -12 : 0;
		int outer = crosshair ? radius + 4 : radius;
		for (int y = -outer - 1; y <= outer; y++) {
			for (int x = -outer - 1; x <= outer; x++) {
				double dx = x + 0.5;
				double dy = y + 0.5;
				double distance = Math.sqrt(dx * dx + dy * dy);
				if (distance < outer - 1.5 || distance >= outer + 0.5) continue;
				boolean edge = distance >= outer - 0.5;
				double angle = Math.atan2(dx, -dy);
				if (angle < 0) angle += Math.PI * 2;
				int color = angle / (Math.PI * 2) <= strength ? fill : backgroundColor.get();
				graphics.fill(x, centerY + y, x + 1, centerY + y + 1, edge ? color : darker(color));
			}
		}
	}

	private void percent(GuiGraphics graphics, Minecraft client, float strength, int fill) {
		String text = Math.round(strength * 100) + "%";
		graphics.drawString(client.font, text, -client.font.width(text) / 2, -4, fill, true);
	}

	private static int darker(int argb) {
		int a = argb >>> 24;
		int r = (argb >> 16 & 0xFF) * 3 / 4;
		int g = (argb >> 8 & 0xFF) * 3 / 4;
		int b = (argb & 0xFF) * 3 / 4;
		return a << 24 | r << 16 | g << 8 | b;
	}
}
