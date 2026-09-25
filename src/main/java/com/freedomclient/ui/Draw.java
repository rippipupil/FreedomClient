package com.freedomclient.ui;

import com.freedomclient.ui.theme.ThemeManager;
import com.freedomclient.util.ModIcons;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Primitivas de dibujo con estilo pixel: esquinas recortadas, bordes de 1 px y relieve. */
public final class Draw {
	private Draw() {
	}

	/** Rectángulo con borde de 1 px y las esquinas recortadas (aspecto pixel art). */
	public static void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
		g.fill(x + 1, y, x + w - 1, y + 1, border);
		g.fill(x + 1, y + h - 1, x + w - 1, y + h, border);
		g.fill(x, y + 1, x + 1, y + h - 1, border);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
	}

	/** Panel con relieve: línea clara arriba y oscura abajo por dentro del borde. */
	public static void bevelPanel(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
		panel(g, x, y, w, h, fill, border);
		g.fill(x + 1, y + 1, x + w - 1, y + 2, ThemeManager.mix(fill, 0xFFFFFFFF, 0.14F));
		g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, ThemeManager.mix(fill, 0xFF000000, 0.3F));
	}

	/** Interruptor pixel de 20x10; {@code progress} va de 0 (apagado) a 1 (encendido) para animarlo. */
	public static void toggle(GuiGraphics g, int x, int y, float progress, boolean hovered) {
		int track = ThemeManager.mix(ThemeManager.shade(), ThemeManager.accent(), progress);
		int border = hovered ? ThemeManager.highlight() : ThemeManager.mix(ThemeManager.border(), 0xFF000000, 0.35F);
		panel(g, x, y, 20, 10, track, border);

		int knobX = x + 2 + Math.round(progress * 10);
		g.fill(knobX, y + 2, knobX + 6, y + 8, ThemeManager.text());
		g.fill(knobX, y + 7, knobX + 6, y + 8, ThemeManager.mix(ThemeManager.text(), 0xFF000000, 0.3F));
	}

	/** Barra de un deslizador con su relleno y el tirador. */
	public static void slider(GuiGraphics g, int x, int y, int w, double progress, boolean hovered) {
		int filled = (int) Math.round(progress * (w - 2));
		panel(g, x, y, w, 6, ThemeManager.shade(), ThemeManager.mix(ThemeManager.border(), 0xFF000000, 0.35F));
		g.fill(x + 1, y + 1, x + 1 + filled, y + 5, ThemeManager.accent());

		int knobX = x + filled - 1;
		panel(g, knobX, y - 2, 4, 10, hovered ? ThemeManager.highlight() : ThemeManager.text(), 0xFF000000);
	}

	/** Dibuja un icono (propio de 16x16 o el original de un mod) escalado a {@code size} px. */
	public static void icon(GuiGraphics g, Identifier icon, int x, int y, int size) {
		int textureSize = ModIcons.textureSize(icon);
		g.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0.0F, 0.0F, size, size, textureSize, textureSize, textureSize, textureSize);
	}

	/** Recuadro con el icono dentro, como en el boceto de las tarjetas. */
	public static void iconBox(GuiGraphics g, Identifier icon, int x, int y, int boxSize) {
		panel(g, x, y, boxSize, boxSize, ThemeManager.shade(), ThemeManager.mix(ThemeManager.border(), ThemeManager.card(), 0.3F));
		int offset = (boxSize - 16) / 2;
		icon(g, icon, x + offset, y + offset, 16);
	}
}
