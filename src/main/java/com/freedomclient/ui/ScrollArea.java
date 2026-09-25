package com.freedomclient.ui;

import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.util.Mth;

/** Zona con desplazamiento vertical suave y barra de scroll pixel. */
public class ScrollArea {
	private static final int STEP = 20;

	private float target;
	private float offset;
	private int contentHeight;
	private int viewHeight;

	/** Empieza a dibujar el contenido: recorta la zona y devuelve el desplazamiento actual en px. */
	public int begin(Ui ui, int x, int y, int w, int h) {
		viewHeight = h;
		clampTarget();
		offset = ui.animate(this, target);
		ui.scroll(x, y, w, h, amount -> {
			target -= (float) amount * STEP;
			clampTarget();
		});
		ui.pushClip(x, y, w, h);
		return Math.round(offset);
	}

	/** Termina el contenido, indicando su altura total, y dibuja la barra si hace falta. */
	public void end(Ui ui, int x, int y, int w, int h, int contentHeight) {
		ui.popClip();
		this.contentHeight = contentHeight;
		if (contentHeight <= h) return;

		int barHeight = Math.max(12, h * h / contentHeight);
		int barY = y + Math.round((h - barHeight) * (offset / (contentHeight - h)));
		ui.g.fill(x + w + 2, y, x + w + 4, y + h, ThemeManager.shade());
		ui.g.fill(x + w + 2, barY, x + w + 4, barY + barHeight, ThemeManager.accent());
	}

	public void reset() {
		target = 0;
		offset = 0;
	}

	private void clampTarget() {
		target = Mth.clamp(target, 0, Math.max(0, contentHeight - viewHeight));
	}
}
