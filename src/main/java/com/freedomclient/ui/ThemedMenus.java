package com.freedomclient.ui;

import com.freedomclient.hud.HudEditorScreen;
import com.freedomclient.module.visual.CustomScreensModule;
import com.freedomclient.ui.menu.FreedomMenuScreen;
import com.freedomclient.ui.scene.CrashGuardScreen;
import com.freedomclient.ui.scene.FreedomTitleScreen;
import com.freedomclient.ui.scene.PixelSky;
import com.freedomclient.ui.scene.WhatsNewScreen;
import com.freedomclient.ui.theme.ThemeColor;
import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Menús de vanilla con el tema del cliente (Client Screens → Other menus): pausa, opciones, un jugador, multijugador,
 * pantallas de carga… Fuera de una partida llevan el cielo pixel del tema; dentro, un degradado del color del tema
 * (sin desenfoque, que además gasta FPS). Los botones y deslizadores se dibujan con el estilo del menú del cliente.
 */
public final class ThemedMenus {
	private ThemedMenus() {
	}

	/** Si esta pantalla se dibuja con el tema (los inventarios, el chat y las pantallas propias no). */
	public static boolean themes(Screen screen) {
		return screen != null && CustomScreensModule.otherMenusEnabled()
				&& !(screen instanceof AbstractContainerScreen<?>) && !(screen instanceof ChatScreen) && !(screen instanceof DeathScreen)
				&& !(screen instanceof FreedomTitleScreen) && !(screen instanceof FreedomMenuScreen) && !(screen instanceof HudEditorScreen)
				&& !(screen instanceof WhatsNewScreen) && !(screen instanceof CrashGuardScreen);
	}

	/** Si los botones que se están dibujando ahora pertenecen a una pantalla con tema. */
	public static boolean active() {
		return themes(Minecraft.getInstance().screen);
	}

	public static void background(GuiGraphics g, int width, int height) {
		int tint = ThemeManager.get(ThemeColor.BACKGROUND);
		if (Minecraft.getInstance().level == null) {
			PixelSky.render(g, width, height, 1.0F);
			g.fillGradient(0, 0, width, height, ThemeManager.withAlpha(tint, 0.15F), ThemeManager.withAlpha(tint, 0.45F));
		} else {
			g.fillGradient(0, 0, width, height, ThemeManager.withAlpha(tint, 0.7F), ThemeManager.withAlpha(tint, 0.92F));
		}
	}

	/** Zonas de lista y cabeceras: un velo del color del tema en lugar de la textura oscura de vanilla. */
	public static void panel(GuiGraphics g, int x, int y, int width, int height) {
		g.fill(x, y, x + width, y + height, ThemeManager.withAlpha(ThemeManager.get(ThemeColor.BACKGROUND), 0.55F));
	}

	public static void separator(GuiGraphics g, int x, int y, int width) {
		if (NeonStyle.on()) {
			NeonStyle.hLine(g, x, x + width, y, 1, NeonStyle.flow(), NeonStyle.flow() + 1.0, 0.9F);
		} else {
			g.fill(x, y, x + width, y + 1, ThemeManager.border());
		}
	}

	public static void button(GuiGraphics g, int x, int y, int width, int height, boolean active, boolean hovered, float alpha) {
		int fill = !active ? ThemeManager.mix(ThemeManager.shade(), ThemeManager.card(), 0.5F) : hovered ? ThemeManager.cardHover() : ThemeManager.card();
		int border = !active ? ThemeManager.mix(ThemeManager.border(), 0xFF000000, 0.4F) : hovered ? ThemeManager.highlight() : ThemeManager.border();
		Draw.bevelPanel(g, x, y, width, height, ThemeManager.withAlpha(fill, alpha), ThemeManager.withAlpha(border, alpha));
		if (NeonStyle.on() && active) {
			NeonStyle.frame(g, x, y, width, height, (x + y) / 600.0 + NeonStyle.flow() * 0.5, 0.35, (hovered ? 1.0F : 0.7F) * alpha);
		}
	}

	public static void sliderHandle(GuiGraphics g, int x, int y, int width, int height, boolean hovered, float alpha) {
		int fill = hovered ? ThemeManager.highlight() : ThemeManager.accent();
		Draw.panel(g, x, y, width, height, ThemeManager.withAlpha(fill, alpha), ThemeManager.withAlpha(0xFF000000, alpha));
	}
}
