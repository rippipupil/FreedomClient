package com.freedomclient.module.pvp;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Aviso de poca vida: los bordes de la pantalla se tiñen de rojo suave cuando te quedan pocos corazones. Sin sonido. */
public class LowHealthWarningModule extends Module {
	private static final Identifier VIGNETTE = FreedomClient.id("textures/hud/vignette.png");
	private static final int TEXTURE_SIZE = 256;

	private final NumberSetting hearts = add(new NumberSetting("Hearts", "Show the red edges at this many hearts or less.", 3, 1, 10, 0.5));
	private final NumberSetting intensity = add(new NumberSetting("Intensity", "How strong the red tint is.", 35, 10, 80, 5, "%"));
	private final ColorSetting color = add(new ColorSetting("Color", "Color of the screen edges.", 0xFFD7263D, false));

	/** 0..1, sube y baja poco a poco para que el borde no aparezca de golpe. */
	private float fade;
	private long lastFrame;

	public LowHealthWarningModule() {
		super("Low Health Warning", "Tints the edges of the screen red when you are low on health.", Category.PVP, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	public void render(GuiGraphics graphics, Minecraft client) {
		LocalPlayer player = client.player;
		long now = System.currentTimeMillis();
		float delta = lastFrame == 0 ? 0.0F : Math.min(0.1F, (now - lastFrame) / 1000.0F);
		lastFrame = now;

		boolean low = player != null && !client.options.hideGui && !player.isCreative() && !player.isSpectator()
				&& player.isAlive() && player.getHealth() <= hearts.getFloat() * 2.0F;
		fade = Math.max(0.0F, Math.min(1.0F, fade + (low ? delta : -delta) * 3.0F));
		if (fade <= 0.0F) return;

		int alpha = Math.round(fade * intensity.getFloat() / 100.0F * 255.0F);
		int tint = alpha << 24 | color.get() & 0xFFFFFF;
		graphics.blit(RenderPipelines.GUI_TEXTURED, VIGNETTE, 0, 0, 0.0F, 0.0F, graphics.guiWidth(), graphics.guiHeight(),
				TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, tint);
	}
}
