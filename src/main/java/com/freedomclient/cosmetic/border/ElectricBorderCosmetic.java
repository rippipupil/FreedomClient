package com.freedomclient.cosmetic.border;

import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Borde eléctrico: rayos en zigzag que recorren el marco de la tarjeta con una chispa en la punta, sobre una línea
 * de energía tenue, y de vez en cuando un chispazo que enciende todo el borde. Cada rayo son unos pocos tramos rectos
 * (uno cada 3 px, saltando hacia dentro o hacia fuera) en vez de un rectángulo por píxel.
 */
public class ElectricBorderCosmetic extends BorderCosmetic {
	private static final int[][] PALETTES = {
			{0xFFE14A, 0xC6F25A, 0x5EF0C8, 0x3FD7FF, 0x3A7BFF, 0x6B5BFF},
			{0xEAF6FF, 0x9FDCFF, 0x3FD7FF, 0x2F9BFF, 0x2F6BFF, 0x1E3FA8},
			{0xFFF5AA, 0xFFE14A, 0xFFC23A, 0xF29A2E, 0xD9701E, 0xA84A12},
			{0xF6EEFF, 0xE6B8FF, 0xC9A3FF, 0xFF7AD9, 0x9B5CFF, 0x5E2AB0},
	};
	/** Largo de cada tramo recto del zigzag. */
	private static final int STEP = 3;

	private final ModeSetting color = add(new ModeSetting("Color", "Color of the electricity.", "Neon", "Neon", "Blue", "Gold", "Purple"));
	private final NumberSetting speed = add(new NumberSetting("Speed", "How fast the bolts run around the card.", 5, 1, 10, 1));
	private final NumberSetting bolts = add(new NumberSetting("Bolts", "How many bolts run around each card.", 3, 1, 5, 1));

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
		long time = System.currentTimeMillis() % 100_000_000L;
		int p = perimeter(w, h);
		// Chispazo: cada ~3 s el borde entero se ilumina un momento (desfasado por tarjeta).
		long cycle = (time + (x * 31L + y * 17L)) % 3100;
		float flash = cycle < 160 ? 1.0F - cycle / 160.0F : 0.0F;
		frame(g, x, y, w, h, 0, alpha(colors[3], strength * (0.45F + 0.5F * flash)));
		// Segunda línea por dentro, más suave, para que el marco se vea cargado de energía.
		frame(g, x, y, w, h, 1, alpha(colors[4], strength * (0.18F + 0.3F * flash)));

		int count = bolts.getInt();
		int length = Math.max(12, Math.min(36, p / 6));
		double travel = time * speed.get() * 0.012;
		// El zigzag cambia de forma unas 14 veces por segundo, como la corriente.
		long jitterStep = time / 70;
		for (int b = 0; b < count; b++) {
			int head = (int) (travel + (double) b * p / count) + x + y;
			for (int i = 0; i < length; i += STEP) {
				float rnd = hash(jitterStep * 131 + (head - i) / STEP * 7L + b);
				// Cada tramo salta un píxel hacia fuera o hacia dentro: así se ve el zigzag del rayo.
				int offset = rnd < 0.33F ? 1 : rnd > 0.66F ? -1 : 0;
				float fade = 1.0F - i / (float) length;
				int c = colors[Math.min(colors.length - 1, i * colors.length / length)];
				int from = head - Math.min(length - 1, i + STEP - 1);
				int to = head - i;
				span(g, from, to, x, y, w, h, offset, 1, alpha(c, strength * fade));
				// Brillo por dentro del rayo.
				span(g, from, to, x, y, w, h, offset - 1, 1, alpha(c, strength * fade * 0.35F));
			}
			// Chispa en la punta: una crucecita blanca que parpadea.
			if (hash(jitterStep * 977 + b) > 0.35F) {
				int[] tip = point(head, x, y, w, h);
				int spark = alpha(colors[0], strength);
				g.fill(tip[0] - 1, tip[1], tip[0] + 2, tip[1] + 1, spark);
				g.fill(tip[0], tip[1] - 1, tip[0] + 1, tip[1] + 2, spark);
			}
		}
	}
}
