package com.freedomclient.ui;

import com.freedomclient.setting.ColorSetting;
import com.freedomclient.ui.theme.ThemeManager;
import com.freedomclient.util.ColorUtil;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Selector de color HSV: cuadro de saturación/brillo, barra de tono y, si se permite, de transparencia. */
public class ColorPicker {
	public static final int HEIGHT = 54;
	private static final int BOX_WIDTH = 72;
	private static final int BAR_WIDTH = 8;
	private static final int GAP = 6;

	private final IntSupplier getter;
	private final IntConsumer setter;
	private final boolean allowAlpha;
	private float hue;
	private float saturation;
	private float value;
	private float alpha;
	private int lastColor;

	public ColorPicker(IntSupplier getter, IntConsumer setter, boolean allowAlpha) {
		this.getter = getter;
		this.setter = setter;
		this.allowAlpha = allowAlpha;
		syncFrom(getter.getAsInt());
	}

	private void syncFrom(int argb) {
		float[] hsv = ColorUtil.rgbToHsv(argb & 0xFFFFFF);
		// Si el color es gris, conservamos el tono elegido para que no salte al mover el brillo.
		if (hsv[1] > 0.0F && hsv[2] > 0.0F) hue = hsv[0];
		saturation = hsv[1];
		value = hsv[2];
		alpha = (argb >>> 24) / 255.0F;
		lastColor = argb;
	}

	private void apply() {
		int rgb = ColorUtil.hsvToRgb(hue, saturation, value);
		int argb = (allowAlpha ? (int) (alpha * 255.0F) << 24 : 0xFF000000) | rgb;
		lastColor = argb;
		setter.accept(argb);
	}

	public void render(Ui ui, int x, int y) {
		int current = getter.getAsInt();
		if (current != lastColor) syncFrom(current);

		int boxH = HEIGHT - 4;
		renderSaturationValueBox(ui, x, y + 2, BOX_WIDTH, boxH);

		int hueX = x + BOX_WIDTH + GAP;
		renderHueBar(ui, hueX, y + 2, BAR_WIDTH, boxH);

		int nextX = hueX + BAR_WIDTH + GAP;
		if (allowAlpha) {
			renderAlphaBar(ui, nextX, y + 2, BAR_WIDTH, boxH);
			nextX += BAR_WIDTH + GAP;
		}

		// Vista previa y código hexadecimal.
		Draw.panel(ui.g, nextX, y + 2, 28, 16, current | 0xFF000000, ThemeManager.border());
		String hex = ColorSetting.toHex(current).substring(allowAlpha ? 1 : 3);
		ui.g.drawString(ui.font, "#" + hex, nextX, y + 22, ThemeManager.text(), false);
	}

	private void renderSaturationValueBox(Ui ui, int x, int y, int w, int h) {
		for (int column = 0; column < w; column++) {
			int top = 0xFF000000 | ColorUtil.hsvToRgb(hue, column / (float) (w - 1), 1.0F);
			ui.g.fillGradient(x + column, y, x + column + 1, y + h, top, 0xFF000000);
		}
		ui.g.renderOutline(x - 1, y - 1, w + 2, h + 2, ThemeManager.border());

		int markerX = x + Math.round(saturation * (w - 1));
		int markerY = y + Math.round((1.0F - value) * (h - 1));
		ui.g.renderOutline(markerX - 2, markerY - 2, 5, 5, 0xFFFFFFFF);
		ui.g.renderOutline(markerX - 1, markerY - 1, 3, 3, 0xFF000000);

		ui.click(x, y, w, h, (mx, my, button) -> {
			ui.startDrag((dx, dy) -> {
				saturation = Mth.clamp((float) (dx - x) / (w - 1), 0.0F, 1.0F);
				value = 1.0F - Mth.clamp((float) (dy - y) / (h - 1), 0.0F, 1.0F);
				apply();
			}, mx, my);
			return true;
		});
	}

	private void renderHueBar(Ui ui, int x, int y, int w, int h) {
		for (int i = 0; i < 6; i++) {
			int top = 0xFF000000 | ColorUtil.hsvToRgb(i / 6.0F, 1.0F, 1.0F);
			int bottom = 0xFF000000 | ColorUtil.hsvToRgb((i + 1) / 6.0F, 1.0F, 1.0F);
			ui.g.fillGradient(x, y + i * h / 6, x + w, y + (i + 1) * h / 6, top, bottom);
		}
		ui.g.renderOutline(x - 1, y - 1, w + 2, h + 2, ThemeManager.border());

		int markerY = y + Math.round(hue * (h - 1));
		ui.g.fill(x - 2, markerY - 1, x + w + 2, markerY + 2, 0xFF000000);
		ui.g.fill(x - 1, markerY, x + w + 1, markerY + 1, 0xFFFFFFFF);

		ui.click(x - 2, y, w + 4, h, (mx, my, button) -> {
			ui.startDrag((dx, dy) -> {
				hue = Mth.clamp((float) (dy - y) / (h - 1), 0.0F, 0.999F);
				apply();
			}, mx, my);
			return true;
		});
	}

	private void renderAlphaBar(Ui ui, int x, int y, int w, int h) {
		for (int cy = 0; cy < h; cy += 4) {
			for (int cx = 0; cx < w; cx += 4) {
				int checker = ((cx + cy) / 4) % 2 == 0 ? 0xFFCCCCCC : 0xFF888888;
				ui.g.fill(x + cx, y + cy, x + Math.min(cx + 4, w), y + Math.min(cy + 4, h), checker);
			}
		}
		int rgb = ColorUtil.hsvToRgb(hue, saturation, value);
		ui.g.fillGradient(x, y, x + w, y + h, 0xFF000000 | rgb, rgb);
		ui.g.renderOutline(x - 1, y - 1, w + 2, h + 2, ThemeManager.border());

		int markerY = y + Math.round((1.0F - alpha) * (h - 1));
		ui.g.fill(x - 2, markerY - 1, x + w + 2, markerY + 2, 0xFF000000);
		ui.g.fill(x - 1, markerY, x + w + 1, markerY + 1, 0xFFFFFFFF);

		ui.click(x - 2, y, w + 4, h, (mx, my, button) -> {
			ui.startDrag((dx, dy) -> {
				alpha = 1.0F - Mth.clamp((float) (dy - y) / (h - 1), 0.0F, 1.0F);
				apply();
			}, mx, my);
			return true;
		});
	}
}
