package com.freedomclient.ui.menu;

import com.freedomclient.FreedomClient;
import com.freedomclient.config.Config;
import com.freedomclient.module.Module;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.NeonStyle;
import com.freedomclient.ui.ThemeDecor;
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
		THEME("Theme"),
		PACKS("Packs");

		private final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	private static final int MAX_WIDTH = 380;
	private static final int MAX_HEIGHT = 250;
	private static final int HEADER_HEIGHT = 28;
	private static final int PADDING = 6;
	private static final long OPEN_ANIMATION_MS = 160;

	/** Recuerda la última pestaña abierta entre aperturas del menú. */
	private static Tab lastTab = Tab.MODS;

	private final Ui ui = new Ui();
	private final ModGridPage modsPage = new ModGridPage(this, null);
	private final HudPage hudPage = new HudPage(this);
	private final CosmeticsPage cosmeticsPage = new CosmeticsPage(this);
	private final ThemePage themePage = new ThemePage();
	private final PacksPage packsPage = new PacksPage();
	private Tab tab = lastTab;
	private ModuleSettingsPage modulePage;
	/** Pantalla a la que volver al cerrar (por ejemplo, el editor de HUD), o null para volver al juego. */
	private Screen returnTo;
	private long openedAt;
	/** Cámara que había antes de abrir los ajustes de un mod con vista previa (para dejarla igual al salir). */
	private net.minecraft.client.CameraType savedCamera;

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

	/** Busca en la pestaña Cosmetics (lo usan los tests para enseñar una tarjeta concreta). */
	public void searchCosmetics(String text) {
		cosmeticsPage.setSearch(text);
	}

	/** Abre el menú directamente en una pestaña (por ejemplo, Packs con la tecla de Quick Pack). */
	public static FreedomMenuScreen forTab(Tab tab) {
		FreedomMenuScreen screen = new FreedomMenuScreen();
		screen.setTab(tab);
		return screen;
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

	/** Mod abierto cuyos cambios se ven en el juego, si cabe la ventana a un lado; si no, null. */
	private com.freedomclient.module.LivePreview livePreview() {
		if (modulePage == null || width < 420) return null;
		return modulePage.module() instanceof com.freedomclient.module.LivePreview preview ? preview : null;
	}

	/** Pone la cámara del mod con vista previa al abrir sus ajustes y deja la de antes al salir. */
	private void syncPreviewCamera() {
		com.freedomclient.module.LivePreview preview = livePreview();
		net.minecraft.client.CameraType wanted = preview == null ? null : preview.previewCamera();
		if (wanted != null) {
			if (savedCamera == null) savedCamera = minecraft.options.getCameraType();
			if (minecraft.options.getCameraType() != wanted) minecraft.options.setCameraType(wanted);
		} else {
			restoreCamera();
		}
	}

	private void restoreCamera() {
		if (savedCamera == null) return;
		minecraft.options.setCameraType(savedCamera);
		savedCamera = null;
	}

	/** Ancho de la ventana: con vista previa ocupa la parte izquierda y deja el juego a la vista. */
	private int windowWidth() {
		if (livePreview() != null) return Math.max(230, Math.min(MAX_WIDTH, Math.round(width * 0.46F)));
		return Math.min(MAX_WIDTH, width - 16);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		// Sin desenfoque: el juego se sigue viendo alrededor de la ventana.
		// Oscurece el fondo hacia abajo con un tono del tema (rojizo en Angel Devil, azul noche en Neon).
		int tint = ThemeManager.get(com.freedomclient.ui.theme.ThemeColor.BACKGROUND) & 0xFFFFFF;
		// Con vista previa solo se oscurece el lado de la ventana, para ver bien el juego.
		int right = livePreview() != null ? 8 + windowWidth() + 8 : width;
		graphics.fillGradient(0, 0, right, height, 0x20000000, 0x60000000 | tint);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		ui.begin(graphics, mouseX, mouseY);
		syncPreviewCamera();
		boolean previewing = livePreview() != null;

		int w = windowWidth();
		int h = Math.min(MAX_HEIGHT, height - 16);
		float open = Math.min(1.0F, (System.currentTimeMillis() - openedAt) / (float) OPEN_ANIMATION_MS);
		float eased = 1.0F - (1.0F - open) * (1.0F - open);
		int x = previewing ? 8 : (width - w) / 2;
		int y = (height - h) / 2 + Math.round((1.0F - eased) * 10);

		// Sombra y ventana.
		graphics.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x70000000);
		Draw.panel(graphics, x, y, w, h, ThemeManager.background(), ThemeManager.border());
		ThemeDecor.background(graphics, x, y, w, h, HEADER_HEIGHT);

		renderHeader(x, y, w);
		if (NeonStyle.on()) {
			// Marco con la energía de Neon dando la vuelta y chispas que lo recorren de vez en cuando.
			NeonStyle.frame(graphics, x, y, w, h, NeonStyle.flow(), 2.0, 1.0F);
			NeonStyle.spark(graphics, x, y, w, 4200, 0);
			NeonStyle.spark(graphics, x, y + HEADER_HEIGHT, w, 6100, 2300);
		}

		int contentX = x + PADDING;
		int contentY = y + HEADER_HEIGHT + PADDING;
		int contentW = w - PADDING * 2;
		int contentH = h - HEADER_HEIGHT - PADDING * 2;
		currentPage().render(ui, contentX, contentY, contentW, contentH);
		ThemeDecor.foreground(graphics, x, y, w, h, HEADER_HEIGHT);
		if (previewing) renderPreviewLabel(graphics, x + w + 8, y);

		ui.end();
	}

	/** Etiqueta "Live preview" encima del juego, a la derecha de la ventana. */
	private void renderPreviewLabel(GuiGraphics graphics, int left, int y) {
		String label = "Live preview";
		int labelW = font.width(label) + 16;
		int lx = left + (width - left - labelW) / 2;
		Draw.panel(graphics, lx, y, labelW, 14, ThemeManager.withAlpha(ThemeManager.background(), 0.85F), ThemeManager.accent());
		// Punto que late, como el de "en directo".
		float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() % 100_000L / 250.0);
		graphics.fill(lx + 4, y + 5, lx + 8, y + 9, ThemeManager.withAlpha(0xFFFF4B4B, 0.5F + 0.5F * pulse));
		graphics.drawString(font, label, lx + 11, y + 3, ThemeManager.text(), false);
	}

	private MenuPage currentPage() {
		if (modulePage != null) return modulePage;
		return switch (tab) {
			case MODS -> modsPage;
			case HUD -> hudPage;
			case COSMETICS -> cosmeticsPage;
			case THEME -> themePage;
			case PACKS -> packsPage;
		};
	}

	private void renderHeader(int x, int y, int w) {
		GuiGraphics g = ui.g;
		int headerFill = ThemeManager.mix(ThemeManager.background(), ThemeManager.card(), 0.5F) | 0xFF000000;
		g.fill(x + 1, y + 1, x + w - 1, y + HEADER_HEIGHT, headerFill);
		ThemeDecor.header(g, x, y, w, HEADER_HEIGHT);
		if (NeonStyle.on()) {
			NeonStyle.hLine(g, x + 1, x + w - 1, y + HEADER_HEIGHT, 1, NeonStyle.flow() + 0.5, NeonStyle.flow() + 1.5, 1.0F);
		} else {
			g.fill(x + 1, y + HEADER_HEIGHT, x + w - 1, y + HEADER_HEIGHT + 1, ThemeManager.border());
		}

		// Logo del cliente: las iniciales FC de circuito con el halo encima, y el nombre en dos colores. En la ventana
		// estrecha (vista previa en directo) lo que no cabe junto a las pestañas no se dibuja.
		int tabsWidth = 0;
		for (Tab current : Tab.values()) tabsWidth += font.width(current.label) + 12;
		int tabsLeft = x + w - 4 - tabsWidth;
		if (x + 6 + PixelSky.logoWidth() + 4 <= tabsLeft) {
			int logoCenterX = x + 6 + PixelSky.logoWidth() / 2 + 1;
			int logoCenterY = y + 17;
			PixelSky.logo(g, logoCenterX, logoCenterY - 2, 1, 1.0F);
		}
		int nameX = x + 6 + PixelSky.logoWidth() + 8;
		int nameY = y + 11;
		int freedomWidth = font.width(Component.literal("Freedom").withStyle(ChatFormatting.BOLD));
		int nameWidth = freedomWidth + font.width(Component.literal("Client").withStyle(ChatFormatting.BOLD));
		if (nameX + nameWidth + 6 <= tabsLeft) {
			g.drawString(font, Component.literal("Freedom").withStyle(ChatFormatting.BOLD), nameX, nameY, ThemeManager.text(), true);
			int clientX = nameX + freedomWidth;
			if (NeonStyle.on()) {
				NeonStyle.gradientText(g, font, "Client", clientX, nameY, NeonStyle.flow(), 0.5, true);
			} else {
				g.drawString(font, Component.literal("Client").withStyle(ChatFormatting.BOLD), clientX, nameY, ThemeManager.accent(), true);
			}
		}

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
		ui.g.drawString(font, current.label, x + 6, y + (HEADER_HEIGHT - 8) / 2, textColor, false);

		int underline = Math.round((w - 4) * progress);
		if (underline > 0) {
			int center = x + w / 2;
			if (NeonStyle.on()) {
				NeonStyle.hLine(ui.g, center - underline / 2, center + (underline + 1) / 2, y + HEADER_HEIGHT - 4, 2, 0.0, 0.35, 1.0F);
			} else {
				ui.g.fill(center - underline / 2, y + HEADER_HEIGHT - 4, center + (underline + 1) / 2, y + HEADER_HEIGHT - 2, ThemeManager.accent());
			}
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
		restoreCamera();
		Config.save();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
