package com.freedomclient.module.visual;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Card Borders: el borde de las tarjetas de mods y cosméticos que están activados. Esquinas gruesas en L (como en
 * el boceto), borde entero, doble, discontinuo o con brillo; del color de la categoría del mod, del tema o uno propio.
 */
public class CardBordersModule extends Module {
	private static CardBordersModule instance;

	private final ModeSetting style = add(new ModeSetting("Style", "Shape of the border of enabled cards.",
			"Corners", "Corners", "Full", "Double", "Dashed", "Glow"));
	private final ModeSetting colorMode = add(new ModeSetting("Color", "Color of the border: the mod's category, the menu theme or your own.",
			"Category", "Category", "Theme", "Custom"));
	private final ColorSetting color = add(new ColorSetting("Custom color", "Border color when Color is Custom.", 0xFFFFD84A, false));
	private final NumberSetting thickness = add(new NumberSetting("Thickness", "How thick the border is.", 2, 1, 3, 1, " px"));
	private final BooleanSetting animated = add(new BooleanSetting("Animated", "The border softly pulses.", true));
	private final BooleanSetting onDisabled = add(new BooleanSetting("On disabled cards", "Also draw a faint border on cards that are off.", false));

	public CardBordersModule() {
		super("Card Borders", "Custom borders for the mod and cosmetic cards that are on: corners, full, double, dashed or glow.",
				Category.VISUAL, true);
		instance = this;
		color.visibleWhen(() -> colorMode.is("Custom"));
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/**
	 * Dibuja el borde de una tarjeta. {@code on} (0..1) es cuánto está activada (animado) y {@code hover} (0..1) si
	 * el ratón está encima. Sin este mod, las activadas llevan un borde simple del color de su categoría.
	 */
	public static void draw(GuiGraphics g, int x, int y, int w, int h, int categoryColor, float on, float hover) {
		CardBordersModule module = instance;
		if (module == null || !module.isEnabled()) {
			if (on > 0.01F) outline(g, x, y, w, h, 1, ThemeManager.withAlpha(categoryColor, on));
			return;
		}
		float strength = on + (module.onDisabled.get() ? (1.0F - on) * 0.35F : 0.0F);
		if (strength <= 0.01F) return;
		int base = switch (module.colorMode.get()) {
			case "Theme" -> ThemeManager.accent();
			case "Custom" -> module.color.get();
			default -> categoryColor;
		};
		// Al pasar el ratón se aclara un poco; animado, late despacio.
		float pulse = module.animated.get() ? 0.82F + 0.18F * (float) Math.sin(System.currentTimeMillis() / 380.0 + (x + y) * 0.02) : 1.0F;
		int c = ThemeManager.withAlpha(ThemeManager.mix(base, 0xFFFFFFFF, 0.25F * hover), strength * pulse);
		int t = module.thickness.getInt();
		switch (module.style.get()) {
			case "Full" -> outline(g, x, y, w, h, t, c);
			case "Double" -> {
				outline(g, x, y, w, h, 1, c);
				outline(g, x + 1 + t, y + 1 + t, w - 2 - t * 2, h - 2 - t * 2, 1, c);
			}
			case "Dashed" -> dashed(g, x, y, w, h, t, c, module.animated.get());
			case "Glow" -> {
				outline(g, x, y, w, h, 1, c);
				for (int i = 1; i <= t + 1; i++) {
					outline(g, x - i, y - i, w + i * 2, h + i * 2, 1, ThemeManager.withAlpha(c, strength * pulse * 0.35F / i));
				}
			}
			default -> corners(g, x, y, w, h, t, c);
		}
	}

	private static void outline(GuiGraphics g, int x, int y, int w, int h, int t, int c) {
		if (w <= 0 || h <= 0) return;
		g.fill(x, y, x + w, y + t, c);
		g.fill(x, y + h - t, x + w, y + h, c);
		g.fill(x, y + t, x + t, y + h - t, c);
		g.fill(x + w - t, y + t, x + w, y + h - t, c);
	}

	/** Esquinas gruesas en L con un píxel suelto dentro, como en el boceto. */
	private static void corners(GuiGraphics g, int x, int y, int w, int h, int t, int c) {
		int len = Math.max(5, Math.min(w, h) / 3);
		int thick = t + 1;
		int[][] origins = {{x, y, 1, 1}, {x + w, y, -1, 1}, {x, y + h, 1, -1}, {x + w, y + h, -1, -1}};
		for (int[] o : origins) {
			int ox = o[0];
			int oy = o[1];
			int dx = o[2];
			int dy = o[3];
			fillBox(g, ox, oy, ox + dx * len, oy + dy * thick, c);
			fillBox(g, ox, oy, ox + dx * thick, oy + dy * len, c);
			// Punto suelto en diagonal, dentro de la esquina.
			int px = ox + dx * (thick + 1);
			int py = oy + dy * (thick + 1);
			fillBox(g, px, py, px + dx * t, py + dy * t, c);
		}
		// Línea fina entre esquinas para cerrar el marco.
		int thin = ThemeManager.withAlpha(c, ((c >>> 24) / 255.0F) * 0.35F);
		g.fill(x + len, y, x + w - len, y + 1, thin);
		g.fill(x + len, y + h - 1, x + w - len, y + h, thin);
		g.fill(x, y + len, x + 1, y + h - len, thin);
		g.fill(x + w - 1, y + len, x + w, y + h - len, thin);
	}

	private static void fillBox(GuiGraphics g, int x1, int y1, int x2, int y2, int c) {
		g.fill(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), c);
	}

	/** Borde de trazos que, animado, avanza alrededor de la tarjeta. */
	private static void dashed(GuiGraphics g, int x, int y, int w, int h, int t, int c, boolean moving) {
		int shift = moving ? (int) (System.currentTimeMillis() / 90 % 6) : 0;
		for (int i = -shift; i < w; i += 6) {
			int a = Math.max(0, i);
			int b = Math.min(w, i + 3);
			if (b <= a) continue;
			g.fill(x + a, y, x + b, y + t, c);
			g.fill(x + w - b, y + h - t, x + w - a, y + h, c);
		}
		for (int i = -shift; i < h; i += 6) {
			int a = Math.max(0, i);
			int b = Math.min(h, i + 3);
			if (b <= a) continue;
			g.fill(x + w - t, y + a, x + w, y + b, c);
			g.fill(x, y + h - b, x + t, y + h - a, c);
		}
	}
}
