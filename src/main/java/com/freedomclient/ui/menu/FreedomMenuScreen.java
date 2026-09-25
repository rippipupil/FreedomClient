package com.freedomclient.ui.menu;

import com.freedomclient.FreedomClient;
import com.freedomclient.config.Config;
import com.freedomclient.module.Module;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.scene.PixelSky;
import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Menú principal del cliente (Shift derecho): ventana central compacta con pestañas. */
public class FreedomMenuScreen extends Screen {
	public enum Tab {
		MODS("Mods"),
		HUD("HUD"),
		COSMETICS("Cosmetics"),
		THEME("Theme");

		private final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	private static final int MAX_WIDTH = 380;
	private static final int MAX_HEIGHT = 250;
	private static final int HEADER_HEIGHT = 24;
	private static final int PADDING = 6;
	private static final long OPEN_ANIMATION_MS = 160;

	/** Recuerda la última pestaña abierta entre aperturas del menú. */
	private static Tab lastTab = Tab.MODS;

	private final Ui ui = new Ui();
	private final ModGridPage modsPage = new ModGridPage(this, null);
	private final HudPage hudPage = new HudPage(this);
	private final CosmeticsPage cosmeticsPage = new CosmeticsPage(this);
	private final ThemePage themePage = new ThemePage();
	private Tab tab = lastTab;
	private ModuleSettingsPage modulePage;
	/** Pantalla a la que volver al cerrar (por ejemplo, el editor de HUD), o null para volver al juego. */
	private Screen returnTo;
	private long openedAt;

	public FreedomMenuScreen() {
		super(Component.literal(FreedomClient.NAME));
	}

	public void setTab(Tab tab) {
		this.tab = tab;
		lastTab = tab;
		modulePage = null;
	}

	public void openModule(Module module) {
		modulePage = new ModuleSettingsPage(this, module);
	}

	/** Abre los ajustes de un mod marcando las opciones que contienen {@code highlight} (desde el buscador). */
	public void openModule(Module module, String highlight) {
		modulePage = new ModuleSettingsPage(this, module, highlight);
	}

	public void closeModule() {
		if (returnTo != null) {
			onClose();
		} else {
			modulePage = null;
		}
	}

	/** Abre directamente los ajustes de un mod y vuelve a {@code parent} al cerrarlos. */
	public static FreedomMenuScreen forModule(Module module, Screen parent) {
		FreedomMenuScreen screen = new FreedomMenuScreen();
		screen.returnTo = parent;
		screen.openModule(module);
		return screen;
	}

	@Override
	public void onClose() {
		if (returnTo != null) {
			minecraft.setScreen(returnTo);
		} else {
			super.onClose();
		}
	}

	@Override
	protected void init() {
		openedAt = System.currentTimeMillis();
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		// Sin desenfoque: el juego se sigue viendo alrededor de la ventana.
		graphics.fillGradient(0, 0, width, height, 0x20000000, 0x60200008);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		ui.begin(graphics, mouseX, mouseY);

		int w = Math.min(MAX_WIDTH, width - 16);
		int h = Math.min(MAX_HEIGHT, height - 16);
		float open = Math.min(1.0F, (System.currentTimeMillis() - openedAt) / (float) OPEN_ANIMATION_MS);
		float eased = 1.0F - (1.0F - open) * (1.0F - open);
		int x = (width - w) / 2;
		int y = (height - h) / 2 + Math.round((1.0F - eased) * 10);

		// Sombra y ventana.
		graphics.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x70000000);
		Draw.panel(graphics, x, y, w, h, ThemeManager.background(), ThemeManager.border());

		renderHeader(x, y, w);

		int contentX = x + PADDING;
		int contentY = y + HEADER_HEIGHT + PADDING;
		int contentW = w - PADDING * 2;
		int contentH = h - HEADER_HEIGHT - PADDING * 2;
		currentPage().render(ui, contentX, contentY, contentW, contentH);

		ui.end();
	}

	private MenuPage currentPage() {
		if (modulePage != null) return modulePage;
		return switch (tab) {
			case MODS -> modsPage;
			case HUD -> hudPage;
			case COSMETICS -> cosmeticsPage;
			case THEME -> themePage;
		};
	}

	private void renderHeader(int x, int y, int w) {
		GuiGraphics g = ui.g;
		int headerFill = ThemeManager.mix(ThemeManager.background(), ThemeManager.card(), 0.5F) | 0xFF000000;
		g.fill(x + 1, y + 1, x + w - 1, y + HEADER_HEIGHT, headerFill);
		g.fill(x + 1, y + HEADER_HEIGHT, x + w - 1, y + HEADER_HEIGHT + 1, ThemeManager.border());

		// Logo del cliente: las iniciales FC en pixel art con el halo encima, y el nombre en dos colores.
		int logoCenterX = x + 6 + PixelSky.logoWidth() / 2 + 1;
		int logoCenterY = y + 16;
		PixelSky.halo(g, logoCenterX, y + 6, 1, 6, 1.0F);
		PixelSky.logo(g, logoCenterX, logoCenterY, 1, 1.0F);
		int nameX = x + 6 + PixelSky.logoWidth() + 8;
		int nameY = y + 10;
		g.drawString(font, Component.literal("Freedom").withStyle(ChatFormatting.BOLD), nameX, nameY, ThemeManager.text(), true);
		int clientX = nameX + font.width(Component.literal("Freedom").withStyle(ChatFormatting.BOLD));
		g.drawString(font, Component.literal("Client").withStyle(ChatFormatting.BOLD), clientX, nameY, ThemeManager.accent(), true);

		// Pestañas alineadas a la derecha.
		int tabX = x + w - 4;
		Tab[] tabs = Tab.values();
		for (int i = tabs.length - 1; i >= 0; i--) {
			Tab current = tabs[i];
			int tabW = font.width(current.label) + 12;
			tabX -= tabW;
			renderTab(current, tabX, y + 1, tabW);
		}
	}

	private void renderTab(Tab current, int x, int y, int w) {
		boolean selected = tab == current;
		boolean hovered = ui.hovered(x, y, w, HEADER_HEIGHT - 1);
		float progress = ui.animate("tab:" + current, selected ? 1.0F : 0.0F);

		int textColor = selected ? ThemeManager.accent() : hovered ? ThemeManager.highlight() : ThemeManager.text();
		ui.g.drawString(font, current.label, x + 6, y + 8, textColor, false);

		int underline = Math.round((w - 4) * progress);
		if (underline > 0) {
			int center = x + w / 2;
			ui.g.fill(center - underline / 2, y + HEADER_HEIGHT - 4, center + (underline + 1) / 2, y + HEADER_HEIGHT - 2, ThemeManager.accent());
		}

		ui.click(x, y, w, HEADER_HEIGHT - 1, (mx, my, button) -> {
			setTab(current);
			ui.playClick();
			return true;
		});
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return ui.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		return ui.mouseDragged(event.x(), event.y()) || super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		return ui.mouseReleased() || super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		return ui.mouseScrolled(mouseX, mouseY, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (ui.hasTextFocus()) {
			ui.keyPressed(event.key(), event.hasControlDown());
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
			onClose();
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_ESCAPE && modulePage != null) {
			closeModule();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return ui.charTyped(event.codepointAsString()) || super.charTyped(event);
	}

	@Override
	public void removed() {
		Config.save();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
