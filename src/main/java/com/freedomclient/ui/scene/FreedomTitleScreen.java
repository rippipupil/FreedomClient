package com.freedomclient.ui.scene;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.visual.CustomScreensModule;
import com.freedomclient.module.utility.UpdatesModule;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.Ui;
import com.freedomclient.ui.UiText;
import com.freedomclient.ui.menu.FreedomMenuScreen;
import com.freedomclient.ui.theme.ThemeManager;
import com.freedomclient.update.UpdateChecker;
import com.freedomclient.update.WhatsNew;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Menú principal de FreedomClient: cielo pixel animado, logo con halo y botones con el tema del cliente.
 * Hereda de TitleScreen para que el juego y otros mods lo sigan reconociendo como el menú principal,
 * pero no usa nada de su lógica (botones, Realms, splash).
 */
public class FreedomTitleScreen extends TitleScreen {
	private static final int BUTTON_WIDTH = 180;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 4;

	private final Ui ui = new Ui();
	private final String version;
	private final String minecraftVersion;
	private long openedAt;

	public FreedomTitleScreen() {
		super(false);
		version = FabricLoader.getInstance().getModContainer(FreedomClient.MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("dev");
		minecraftVersion = FabricLoader.getInstance().getModContainer("minecraft")
				.map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("");
	}

	@Override
	protected void init() {
		openedAt = System.currentTimeMillis();
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		// Paralaje: las capas del cielo se desplazan un poco según dónde esté el ratón.
		float parallaxX = CustomScreensModule.parallaxEnabled() ? Math.max(-1.0F, Math.min(1.0F, (mouseX - width / 2.0F) / (width / 2.0F))) : 0.0F;
		float parallaxY = CustomScreensModule.parallaxEnabled() ? Math.max(-1.0F, Math.min(1.0F, (mouseY - height / 2.0F) / (height / 2.0F))) : 0.0F;
		PixelSky.render(graphics, width, height, 1.0F, parallaxX, parallaxY);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		ui.begin(graphics, mouseX, mouseY);
		float appear = Math.min(1.0F, (System.currentTimeMillis() - openedAt) / 400.0F);

		// Logo: halo, "FC" y el nombre en la fuente VT323.
		int p = Math.max(2, height / 90);
		int centerX = width / 2;
		int logoY = height / 5 + p * 4;
		PixelSky.halo(graphics, centerX, logoY - p * 10, p, appear);
		PixelSky.logo(graphics, centerX, logoY, p, appear);
		Component name = UiText.logo(FreedomClient.NAME);
		int nameY = logoY + p * 9;
		graphics.drawString(font, name, centerX - font.width(name) / 2 + 1, nameY + 1, 0xFF1A0508, false);
		graphics.drawString(font, name, centerX - font.width(name) / 2, nameY, 0xFFF5F1E8, false);

		// Botones.
		int y = Math.max(nameY + 34, height / 2);
		int x = centerX - BUTTON_WIDTH / 2;
		y = button("Singleplayer", x, y, () -> minecraft.setScreen(new SelectWorldScreen(this)));
		y = button("Multiplayer", x, y, () -> minecraft.setScreen(minecraft.options.skipMultiplayerWarning
				? new JoinMultiplayerScreen(this) : new SafetyScreen(this)));
		y = button("FreedomClient", x, y, () -> minecraft.setScreen(new FreedomMenuScreen()));
		int half = (BUTTON_WIDTH - BUTTON_GAP) / 2;
		button("Options", x, y, half, () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)));
		button("Quit", x + half + BUTTON_GAP, y, half, minecraft::stop);

		// Textos de las esquinas.
		graphics.drawString(font, FreedomClient.NAME + " v" + version + " | Minecraft " + minecraftVersion,
				4, height - 12, 0xCCF5F1E8, true);
		String copyright = "Copyright Mojang AB. Do not distribute!";
		graphics.drawString(font, copyright, width - font.width(copyright) - 4, height - 12, 0xCCF5F1E8, true);

		renderUpdateBanner(graphics);
		ui.end();
	}

	private int button(String label, int x, int y, Runnable action) {
		button(label, x, y, BUTTON_WIDTH, action);
		return y + BUTTON_HEIGHT + BUTTON_GAP;
	}

	private void button(String label, int x, int y, int w, Runnable action) {
		button(label, x, y, w, BUTTON_HEIGHT, action);
	}

	private void button(String label, int x, int y, int w, int h, Runnable action) {
		boolean hovered = ui.hovered(x, y, w, h);
		float hover = ui.animate("title:" + label, hovered ? 1.0F : 0.0F);
		int fill = ThemeManager.mix(ThemeManager.withAlpha(ThemeManager.card(), 0.85F), ThemeManager.withAlpha(ThemeManager.cardHover(), 0.95F), hover);
		int border = ThemeManager.mix(ThemeManager.border(), ThemeManager.accent(), hover);
		Draw.bevelPanel(ui.g, x, y, w, h, fill, border);
		int textColor = ThemeManager.mix(ThemeManager.text(), ThemeManager.accent(), hover);
		ui.g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, textColor);
		ui.click(x, y, w, h, (mx, my, button) -> {
			ui.playClick();
			action.run();
			return true;
		});
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return ui.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void tick() {
		// Novedades: una vez tras actualizar.
		UpdatesModule updates = FreedomClient.getModuleManager().get(UpdatesModule.class);
		if (updates.isEnabled() && updates.whatsNew.get() && WhatsNew.shouldShow()) {
			minecraft.setScreen(new WhatsNewScreen(this));
		}
	}

	/** Aviso de actualización arriba del todo: descargar, o el progreso de la descarga. */
	private void renderUpdateBanner(GuiGraphics graphics) {
		if (!UpdateChecker.shouldShowBanner()) return;
		UpdateChecker.State state = UpdateChecker.state();
		String text = switch (state) {
			case DOWNLOADING -> "Downloading the update...";
			case READY -> "Update installed! Restart Minecraft to use it.";
			case FAILED -> "The download failed. Try again?";
			default -> "A new FreedomClient version is available!";
		};
		int bannerW = font.width(text) + (state == UpdateChecker.State.AVAILABLE || state == UpdateChecker.State.FAILED ? 100 : 30);
		int x = (width - bannerW) / 2;
		int y = 6;
		Draw.bevelPanel(ui.g, x, y, bannerW, 20, ThemeManager.withAlpha(ThemeManager.background(), 0.92F), ThemeManager.accent());
		graphics.drawString(font, text, x + 8, y + 6, ThemeManager.text(), false);
		if (state == UpdateChecker.State.AVAILABLE || state == UpdateChecker.State.FAILED) {
			int buttonX = x + bannerW - 88;
			button(state == UpdateChecker.State.FAILED ? "Retry" : "Update", buttonX, y + 3, 60, 14, UpdateChecker::downloadAsync);
		}
		int closeX = x + bannerW - 18;
		boolean closeHovered = ui.hovered(closeX, y + 4, 12, 12);
		graphics.drawString(font, "x", closeX + 3, y + 6, closeHovered ? ThemeManager.accent() : ThemeManager.textMuted(), false);
		ui.click(closeX, y + 4, 12, 12, (mx, my, b) -> {
			UpdateChecker.dismiss();
			return true;
		});
	}

	@Override
	public void added() {
	}

	@Override
	public void removed() {
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
