package com.freedomclient.cosmetic.border;

import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;

/** Borde arcoíris: un degradado de todos los colores que gira alrededor de la tarjeta, con un brillo suave por fuera. */
public class RainbowBorderCosmetic extends BorderCosmetic {
	private final NumberSetting speed = add(new NumberSetting("Speed", "How fast the colors go around the card.", 4, 1, 10, 1));
	private final NumberSetting thickness = add(new NumberSetting("Thickness", "How thick the rainbow is.", 2, 1, 3, 1, " px"));
	private final NumberSetting saturation = add(new NumberSetting("Saturation", "How strong the colors are (low = pastel).", 80, 30, 100, 5, "%"));
	private final BooleanSetting glow = add(new BooleanSetting("Glow", "A soft glow outside the border.", true));

	public RainbowBorderCosmetic() {
		super("Rainbow Border", "A rainbow that flows around your enabled mod cards, with a soft glow.");
	}

	@Override
	public void draw(GuiGraphics g, int x, int y, int w, int h, int categoryColor, float strength, float hover) {
		int p = perimeter(w, h);
		int t = thickness.getInt();
		float sat = saturation.getFloat() / 100.0F;
		double shift = System.currentTimeMillis() * speed.get() * 0.00006;
		for (int s = 0; s < p; s++) {
			int[] pt = point(s, x, y, w, h);
			float hue = (float) ((double) s / p - shift);
			int rgb = java.awt.Color.HSBtoRGB(hue - (float) Math.floor(hue), sat, 1.0F) & 0xFFFFFF;
			int c = alpha(rgb, strength);
			// Grosor hacia dentro de la tarjeta (contra la normal).
			int x1 = pt[0];
			int y1 = pt[1];
			int x2 = pt[0] + 1;
			int y2 = pt[1] + 1;
			if (pt[2] > 0) x1 -= t - 1;
			if (pt[2] < 0) x2 += t - 1;
			if (pt[3] > 0) y1 -= t - 1;
			if (pt[3] < 0) y2 += t - 1;
			g.fill(x1, y1, x2, y2, c);
			if (glow.get()) pixel(g, pt[0] + pt[2], pt[1] + pt[3], alpha(rgb, strength * 0.3F));
		}
	}
}
