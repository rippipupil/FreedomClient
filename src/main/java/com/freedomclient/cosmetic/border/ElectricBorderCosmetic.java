package com.freedomclient.cosmetic.border;

import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Borde eléctrico: rayos en zigzag que recorren el marco de la tarjeta con chispas en la punta, sobre una línea
 * de energía tenue, y de vez en cuando un chispazo que enciende todo el borde.
 */
public class ElectricBorderCosmetic extends BorderCosmetic {
	private static final int[][] PALETTES = {
			{0xFFE14A, 0xC6F25A, 0x5EF0C8, 0x3FD7FF, 0x3A7BFF, 0x6B5BFF},
			{0xEAF6FF, 0x9FDCFF, 0x3FD7FF, 0x2F9BFF, 0x2F6BFF, 0x1E3FA8},
			{0xFFF5AA, 0xFFE14A, 0xFFC23A, 0xF29A2E, 0xD9701E, 0xA84A12},
			{0xF6EEFF, 0xE6B8FF, 0xC9A3FF, 0xFF7AD9, 0x9B5CFF, 0x5E2AB0},
	};

	private final ModeSetting color = add(new ModeSetting("Color", "Color of the electricity.", "Neon", "Neon", "Blue", "Gold", "Purple"));
	private final NumberSetting speed = add(new NumberSetting("Speed", "How fast the bolts run around the card.", 5, 1, 10, 1));
	private final NumberSetting bolts = add(new NumberSetting("Bolts", "How many bolts run around each card.", 2, 1, 4, 1));

	public ElectricBorderCosmetic() {
		super("Electric Border", "Zigzag lightning running around your enabled mod cards, with sparks and a flash now and then.");
	}

	private int[] palette() {
		return switch (color.get()) {
			case "Blue" -> PALETTES[1];
			case "Gold" -> PALETTES[2];
			case "Purple" -> PALETTES[3];
			default -> PALETTES[0];
		};
	}

	@Override
	public void draw(GuiGraphics g, int x, int y, int w, int h, int categoryColor, float strength, float hover) {
		int[] colors = palette();
		long time = System.currentTimeMillis();
		int p = perimeter(w, h);
		// Chispazo: cada ~3 s el borde entero se ilumina un momento (desfasado por tarjeta).
		long cycle = (time + (x * 31L + y * 17L)) % 3100;
		float flash = cycle < 160 ? 1.0F - cycle / 160.0F : 0.0F;
		outline(g, x, y, w, h, alpha(colors[3], strength * (0.28F + 0.6F * flash)));

		int count = bolts.getInt();
		int length = Math.max(10, Math.min(28, p / 8));
		double travel = time * speed.get() * 0.012;
		// El zigzag cambia de forma unas 14 veces por segundo, como la corriente.
		long jitterStep = time / 70;
		for (int b = 0; b < count; b++) {
			int head = (int) (travel + (double) b * p / count) + x + y;
			for (int i = 0; i < length; i++) {
				int[] pt = point(head - i, x, y, w, h);
				float rnd = hash(jitterStep * 131 + (head - i) * 7L + b);
				int offset = rnd < 0.3F ? -1 : rnd > 0.7F ? 1 : 0;
				float fade = 1.0F - i / (float) length;
				int c = colors[Math.min(colors.length - 1, i * colors.length / length)];
				int px = pt[0] + pt[2] * offset;
				int py = pt[1] + pt[3] * offset;
				pixel(g, px, py, alpha(c, strength * fade));
				// Brillo a los dos lados del rayo.
				pixel(g, px + pt[2], py + pt[3], alpha(c, strength * fade * 0.35F));
				pixel(g, px - pt[2], py - pt[3], alpha(c, strength * fade * 0.2F));
			}
			// Chispas sueltas alrededor de la punta.
			int[] tip = point(head, x, y, w, h);
			for (int k = 0; k < 3; k++) {
				float a = hash(jitterStep * 977 + k * 13L + b);
				float d = hash(jitterStep * 389 + k * 29L + b);
				int sx = tip[0] + Math.round((a - 0.5F) * 6.0F) + tip[2] * Math.round(d * 3.0F);
				int sy = tip[1] + Math.round((d - 0.5F) * 6.0F) + tip[3] * Math.round(a * 3.0F);
				pixel(g, sx, sy, alpha(colors[k % 2 == 0 ? 0 : 2], strength * (0.5F + 0.5F * a)));
			}
		}
	}
}
