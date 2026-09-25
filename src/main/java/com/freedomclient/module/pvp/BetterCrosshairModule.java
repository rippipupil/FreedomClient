package com.freedomclient.module.pvp;

import com.freedomclient.hud.CombatTracker;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.setting.PixelGridSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Mira personalizable: formas predefinidas o dibujada píxel a píxel, con indicador de ataque listo,
 * hit marker al golpear y color distinto al apuntar a una entidad.
 */
public class BetterCrosshairModule extends Module {
	private static final int GRID = 15;
	private static final long HIT_MARKER_MS = 250;

	private final ModeSetting style = add(new ModeSetting("Style", "Shape of the crosshair.", "Cross", "Cross", "Dot", "Circle", "T-Shape", "Custom"));
	private final PixelGridSetting pixels = add(new PixelGridSetting("Custom crosshair", "Draw your own crosshair.", GRID,
			".............../.............../.......#......./.......#......./"
					+ ".......#......./.......#......./.............../..####.#.####../"
					+ ".............../.......#......./.......#......./.......#......./"
					+ ".......#......./.............../..............."));
	private final NumberSetting size = add(new NumberSetting("Size", "Length of each arm.", 4, 1, 10, 1));
	private final NumberSetting gap = add(new NumberSetting("Gap", "Space in the middle.", 2, 0, 6, 1));
	private final NumberSetting thickness = add(new NumberSetting("Thickness", "Width of the lines.", 1, 1, 3, 1));
	private final BooleanSetting centerDot = add(new BooleanSetting("Center dot", "Add a dot in the middle.", false));
	private final ColorSetting color = add(new ColorSetting("Color", "Crosshair color.", 0xFFF5F1E8, true));
	private final BooleanSetting outline = add(new BooleanSetting("Outline", "Dark outline so it is visible everywhere.", true));
	private final ColorSetting outlineColor = add(new ColorSetting("Outline color", "Color of the outline.", 0xC0000000, true));
	private final ModeSetting attackIndicator = add(new ModeSetting("Attack indicator", "Show when your attack is fully charged.", "Color", "Off", "Color", "Bar"));
	private final ColorSetting readyColor = add(new ColorSetting("Ready color", "Color when your attack is fully charged.", 0xFFF2C94C, true));
	private final BooleanSetting targetColorEnabled = add(new BooleanSetting("Target color", "Change color while aiming at a player or mob.", true));
	private final ColorSetting targetColor = add(new ColorSetting("Aiming color", "Color while aiming at an entity.", 0xFFFF3B3B, true));
	private final BooleanSetting hitMarker = add(new BooleanSetting("Hit marker", "Flash an X when you hit something.", true));
	private final ColorSetting hitMarkerColor = add(new ColorSetting("Hit marker color", "Color of the hit marker.", 0xFFFF3B3B, true));
	private final BooleanSetting thirdPerson = add(new BooleanSetting("Show in third person", "Also draw the crosshair in third person.", false));

	public BetterCrosshairModule() {
		super("Better Crosshair", "Custom crosshair with attack indicator, hit marker and target color.", Category.PVP, true);
		pixels.visibleWhen(() -> style.is("Custom"));
		size.visibleWhen(() -> !style.is("Custom") && !style.is("Dot"));
		gap.visibleWhen(() -> !style.is("Custom") && !style.is("Dot"));
		thickness.visibleWhen(() -> !style.is("Custom"));
		centerDot.visibleWhen(() -> !style.is("Custom") && !style.is("Dot"));
		outlineColor.visibleWhen(outline::get);
		readyColor.visibleWhen(() -> !attackIndicator.is("Off"));
		targetColor.visibleWhen(targetColorEnabled::get);
		hitMarkerColor.visibleWhen(hitMarker::get);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	public boolean shouldRender(Minecraft client) {
		if (client.player == null || client.options.hideGui) return false;
		return client.options.getCameraType().isFirstPerson() || thirdPerson.get();
	}

	public void render(GuiGraphics graphics, Minecraft client) {
		LocalPlayer player = client.player;
		float attack = player.getAttackStrengthScale(0.0F);
		boolean aiming = client.crosshairPickEntity instanceof LivingEntity living && living.isAlive();

		int drawColor = color.get();
		if (targetColorEnabled.get() && aiming) drawColor = targetColor.get();
		else if (attackIndicator.is("Color") && attack >= 1.0F) drawColor = readyColor.get();

		graphics.pose().pushMatrix();
		// Centro exacto de la pantalla (puede caer en medio píxel de la interfaz).
		graphics.pose().translate(graphics.guiWidth() / 2.0F, graphics.guiHeight() / 2.0F);

		switch (style.get()) {
			case "Dot" -> dot(graphics, drawColor);
			case "Circle" -> circle(graphics, drawColor);
			case "Custom" -> custom(graphics, drawColor);
			default -> cross(graphics, drawColor, style.is("T-Shape"));
		}
		if (centerDot.get() && !style.is("Dot") && !style.is("Custom")) dot(graphics, drawColor);

		if (attackIndicator.is("Bar") && attack < 1.0F) {
			int barWidth = 16;
			int barY = gap.getInt() + size.getInt() + 4;
			rect(graphics, -barWidth / 2, barY, barWidth / 2, barY + 2, 0x80000000);
			rect(graphics, -barWidth / 2, barY, -barWidth / 2 + Math.round(barWidth * attack), barY + 2, readyColor.get());
		}

		if (hitMarker.get() && System.currentTimeMillis() - CombatTracker.getLastHitAt() < HIT_MARKER_MS) {
			hitMarker(graphics);
		}
		graphics.pose().popMatrix();
	}

	/** Rectángulo con contorno opcional; coordenadas relativas al centro. */
	private void rect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int fill) {
		if (outline.get()) graphics.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, outlineColor.get());
		graphics.fill(x1, y1, x2, y2, fill);
	}

	private void cross(GuiGraphics graphics, int fill, boolean tShape) {
		int t = thickness.getInt();
		int a = -t / 2;
		int b = a + t;
		int g = gap.getInt();
		int l = size.getInt();
		rect(graphics, a - g - l, a, a - g, b, fill);
		rect(graphics, b + g, a, b + g + l, b, fill);
		if (!tShape) rect(graphics, a, a - g - l, b, a - g, fill);
		rect(graphics, a, b + g, b, b + g + l, fill);
	}

	private void dot(GuiGraphics graphics, int fill) {
		int t = thickness.getInt() + 1;
		int a = -t / 2;
		rect(graphics, a, a, a + t, a + t, fill);
	}

	private void circle(GuiGraphics graphics, int fill) {
		int radius = gap.getInt() + size.getInt();
		int t = thickness.getInt();
		if (outline.get()) {
			circlePixels(graphics, radius, t + 2, -1, outlineColor.get());
		}
		circlePixels(graphics, radius, t, 0, fill);
	}

	private static void circlePixels(GuiGraphics graphics, int radius, int thickness, int grow, int color) {
		for (int y = -radius - thickness; y <= radius + thickness; y++) {
			for (int x = -radius - thickness; x <= radius + thickness; x++) {
				double distance = Math.sqrt((x + 0.5) * (x + 0.5) + (y + 0.5) * (y + 0.5));
				if (distance >= radius + grow - 0.5 && distance < radius + grow + thickness - 0.5) {
					graphics.fill(x, y, x + 1, y + 1, color);
				}
			}
		}
	}

	private void custom(GuiGraphics graphics, int fill) {
		int half = GRID / 2;
		if (outline.get()) {
			for (int y = 0; y < GRID; y++) {
				for (int x = 0; x < GRID; x++) {
					if (pixels.isSet(x, y)) graphics.fill(x - half - 1, y - half - 1, x - half + 2, y - half + 2, outlineColor.get());
				}
			}
		}
		for (int y = 0; y < GRID; y++) {
			for (int x = 0; x < GRID; x++) {
				if (pixels.isSet(x, y)) graphics.fill(x - half, y - half, x - half + 1, y - half + 1, fill);
			}
		}
	}

	private void hitMarker(GuiGraphics graphics) {
		int start = gap.getInt() + 2;
		for (int i = 0; i < 3; i++) {
			int d = start + i;
			int c = hitMarkerColor.get();
			graphics.fill(-d - 1, -d - 1, -d, -d, c);
			graphics.fill(d, -d - 1, d + 1, -d, c);
			graphics.fill(-d - 1, d, -d, d + 1, c);
			graphics.fill(d, d, d + 1, d + 1, c);
		}
	}
}
