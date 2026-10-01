package com.freedomclient.cosmetic.border;

import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Borde de brillos: destellos pixel que aparecen, crecen y se apagan por el marco de la tarjeta, cada vez en un
 * sitio distinto. Puede ser de brillos, estrellas, corazones, nieve (cae un poco) o pétalos (se los lleva el aire).
 */
public class SparkleBorderCosmetic extends BorderCosmetic {
	private static final String[] SPARKLE_SMALL = {".#.", "###", ".#."};
	private static final String[] SPARKLE_BIG = {"..#..", "..#..", "#####", "..#..", "..#.."};
	private static final String[] STAR = {"..#..", ".###.", "#####", ".###.", ".#.#."};
	private static final String[] HEART = {".#.#.", "#####", "#####", ".###.", "..#.."};
	private static final String[] SNOW = {"#.#.#", ".###.", "##.##", ".###.", "#.#.#"};
	private static final String[] PETAL = {".##", "###", "##."};
	private static final String[] DOT = {"#"};

	private final ModeSetting shape = add(new ModeSetting("Shape", "What twinkles around the cards.", "Sparkles",
			"Sparkles", "Stars", "Hearts", "Snow", "Petals"));
	private final ModeSetting colors = add(new ModeSetting("Colors", "Colors of the shapes: their own, the menu theme or the mod's category.",
			"Shape colors", "Shape colors", "Theme", "Category"));
	private final NumberSetting density = add(new NumberSetting("Amount", "How many shapes there are on each card.", 5, 1, 10, 1));

	public SparkleBorderCosmetic() {
		super("Sparkle Border", "Sparkles, stars, hearts, snow or petals twinkling around your enabled mod cards.");
	}

	/** Color principal y de brillo de cada forma. */
	private int[] colorsFor(int categoryColor) {
		return switch (colors.get()) {
			case "Theme" -> new int[] {ThemeManager.accent() & 0xFFFFFF, ThemeManager.highlight() & 0xFFFFFF};
			case "Category" -> new int[] {categoryColor & 0xFFFFFF, 0xFFFFFF};
			default -> switch (shape.get()) {
				case "Stars" -> new int[] {0xFFD84A, 0xFFF5AA};
				case "Hearts" -> new int[] {0xFF4F7A, 0xFFB0C4};
				case "Snow" -> new int[] {0xEAF6FF, 0x9FDCFF};
				case "Petals" -> new int[] {0xFFA8D0, 0xFFE0EE};
				default -> new int[] {0xFFF5AA, 0xFFFFFF};
			};
		};
	}

	@Override
	public void draw(GuiGraphics g, int x, int y, int w, int h, int categoryColor, float strength, float hover) {
		int[] palette = colorsFor(categoryColor);
		outline(g, x, y, w, h, alpha(palette[0], strength * 0.4F));
		long time = System.currentTimeMillis();
		int p = perimeter(w, h);
		int count = Math.max(3, Math.round(p * density.get().floatValue() / 70.0F));
		String mode = shape.get();
		for (int i = 0; i < count; i++) {
			long seed = i * 7919L + x * 31L + y * 131L;
			long period = 1300 + (long) (hash(seed) * 1300);
			long t = time + (long) (hash(seed + 1) * period);
			long cycle = t / period;
			float phase = (t % period) / (float) period;
			// Cada vez que vuelve a salir lo hace en otro sitio del borde.
			int[] pt = point((int) (hash(seed * 3 + cycle) * p), x, y, w, h);
			float life = (float) Math.sin(phase * Math.PI);
			int offset = Math.round((hash(seed * 5 + cycle) - 0.5F) * 4.0F);
			int cx = pt[0] + pt[2] * offset;
			int cy = pt[1] + pt[3] * offset;
			if (mode.equals("Snow")) cy += Math.round(phase * 6.0F);
			if (mode.equals("Petals")) {
				cx += Math.round(phase * 5.0F);
				cy += Math.round((float) Math.sin(phase * Math.PI * 2) * 2.0F + phase * 3.0F);
			}
			String[] art = switch (mode) {
				case "Stars" -> life > 0.55F ? STAR : SPARKLE_SMALL;
				case "Hearts" -> life > 0.45F ? HEART : SPARKLE_SMALL;
				case "Snow" -> life > 0.4F ? SNOW : DOT;
				case "Petals" -> PETAL;
				default -> life > 0.7F ? SPARKLE_BIG : life > 0.3F ? SPARKLE_SMALL : DOT;
			};
			int color = alpha(palette[0], strength * life);
			pattern(g, art, cx, cy, color);
			// Centro brillante en los momentos de más luz.
			if (life > 0.6F && art != PETAL) pixel(g, cx, cy, alpha(palette[1], strength * life));
		}
	}
}
