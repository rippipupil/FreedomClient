package com.freedomclient.module.pvp;

import com.freedomclient.hud.CombatTracker;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

/**
 * Custom Attack Indicator: sustituye el indicador de ataque de vanilla por uno configurable
 * (barra de vanilla, barra, anillo, espada pixel o solo texto), bajo la mira o junto a la barra de objetos,
 * con el tiempo exacto que falta para que el golpe esté cargado.
 */
public class AttackIndicatorModule extends Module {
	private static final long FLASH_MS = 400;
	private static final Identifier CROSSHAIR_FULL = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_full");
	private static final Identifier CROSSHAIR_BACKGROUND = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_background");
	private static final Identifier CROSSHAIR_PROGRESS = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_progress");
	private static final Identifier HOTBAR_BACKGROUND = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_background");
	private static final Identifier HOTBAR_PROGRESS = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_progress");
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

	private final ModeSetting style = add(new ModeSetting("Style", "How the indicator looks.", "Vanilla bar",
			"Vanilla bar", "Bar", "Circle", "Sword", "Text only"));
	private final ModeSetting position = add(new ModeSetting("Position", "Where the indicator is drawn.", "Crosshair", "Crosshair", "Hotbar"));
	private final NumberSetting scale = add(new NumberSetting("Scale", "Size of the indicator.", 1, 0.5, 3, 0.25, "x"));
	private final ModeSetting cooldownText = add(new ModeSetting("Cooldown text", "Exact time left until your hit is fully charged.",
			"Seconds", "Off", "Seconds", "Ticks", "Percent"));
	private final ModeSetting showWhen = add(new ModeSetting("Show", "When the indicator is visible.", "Charging + flash",
			"After hitting", "Charging", "Charging + flash", "Always"));
	private final BooleanSetting onlyWeapons = add(new BooleanSetting("Only with weapons", "Only show it while holding a sword, axe, mace or trident.", false));
	private final ColorSetting chargingColor = add(new ColorSetting("Charging color", "Color while the attack is charging.", 0xFFF5F1E8, true));
	private final ColorSetting readyColor = add(new ColorSetting("Ready color", "Color when the attack is fully charged.", 0xFFF2C94C, true));
	private final BooleanSetting targetColorEnabled = add(new BooleanSetting("Target color", "Different color when ready and aiming at a player or mob.", true));
	private final ColorSetting targetColor = add(new ColorSetting("Target ready color", "Color when ready and aiming at an entity.", 0xFFFF3B3B, true));
	private final ColorSetting backgroundColor = add(new ColorSetting("Background", "Color of the empty part.", 0x90303030, true));

	private float lastStrength = 1.0F;
	private long readyAt;

	public AttackIndicatorModule() {
		super("Custom Attack Indicator", "Shows the exact cooldown of your hit as the vanilla bar, a bar, a ring, a sword or text.", Category.PVP, false);
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
		// "After hitting": solo mientras se recarga un golpe dado a una entidad (no al cambiar de objeto).
		if (showWhen.is("After hitting") && now - CombatTracker.getLastHitAt() > player.getCurrentItemAttackStrengthDelay() * 50 + 100) return;
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
		int textY;
		switch (style.get()) {
			case "Bar" -> {
				bar(graphics, strength, fill, crosshair);
				textY = crosshair ? 4 : -4;
			}
			case "Circle" -> {
				circle(graphics, strength, fill, crosshair);
				textY = crosshair ? 0 : -4;
			}
			case "Sword" -> {
				sword(graphics, strength, fill);
				textY = crosshair ? 8 : -4;
			}
			case "Text only" -> textY = -4;
			default -> textY = vanilla(graphics, strength, ready && aiming, crosshair);
		}
		String text = cooldownText(player, strength);
		if (text != null) {
			if (crosshair || style.is("Text only")) {
				graphics.drawString(client.font, text, -client.font.width(text) / 2, textY, fill, true);
			} else {
				// Junto a la barra de objetos el texto va al lado del indicador, hacia fuera.
				boolean right = player.getMainArm() == HumanoidArm.RIGHT;
				graphics.drawString(client.font, text, right ? 12 : -12 - client.font.width(text), textY, fill, true);
			}
		}
		graphics.pose().popMatrix();
	}

	/** Tiempo que falta para el golpe cargado, calculado con la velocidad de ataque del objeto en la mano. */
	private String cooldownText(LocalPlayer player, float strength) {
		if (cooldownText.is("Off") && !style.is("Text only")) return null;
		float delayTicks = player.getCurrentItemAttackStrengthDelay();
		float remaining = Math.max(0.0F, (1.0F - strength) * delayTicks);
		return switch (cooldownText.get()) {
			case "Ticks" -> String.valueOf(Math.round(remaining));
			case "Percent", "Off" -> Math.round(strength * 100) + "%";
			default -> String.format(Locale.ROOT, "%.2fs", remaining / 20.0F);
		};
	}

	/** Los sprites del indicador de vanilla. Devuelve la altura donde poner el texto. */
	private int vanilla(GuiGraphics graphics, float strength, boolean readyOnTarget, boolean crosshair) {
		if (crosshair) {
			if (readyOnTarget) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CROSSHAIR_FULL, -8, -8, 16, 16);
				return 9;
			}
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CROSSHAIR_BACKGROUND, -8, 0, 16, 4);
			int width = (int) (strength * 17.0F);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CROSSHAIR_PROGRESS, 16, 4, 0, 0, -8, 0, width, 4);
			return 6;
		}
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_BACKGROUND, -9, -9, 18, 18);
		int height = (int) (strength * 19.0F);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_PROGRESS, 18, 18, 0, 18 - height, -9, 9 - height, 18, height);
		return -4;
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

	private static int darker(int argb) {
		int a = argb >>> 24;
		int r = (argb >> 16 & 0xFF) * 3 / 4;
		int g = (argb >> 8 & 0xFF) * 3 / 4;
		int b = (argb & 0xFF) * 3 / 4;
		return a << 24 | r << 16 | g << 8 | b;
	}
}
