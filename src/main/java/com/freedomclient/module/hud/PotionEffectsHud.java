package com.freedomclient.module.hud;

import com.freedomclient.hud.HudModule;
import com.freedomclient.hud.HudPosition;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Effect Timer Plus: lista de efectos activos con icono, nombre, nivel y tiempo restante, con varios estilos. */
public class PotionEffectsHud extends HudModule {
	private static final int ICON = 18;

	private final ModeSetting style = add(new ModeSetting("Style", "What each effect shows.", "Full", "Full", "Compact", "Icons"));
	private final ModeSetting sort = add(new ModeSetting("Sort by", "Order of the effects.", "Time", "Time", "Name"));
	private final BooleanSetting effectColors = add(new BooleanSetting("Effect colors", "Color names with the effect's own color.", true));
	private final ColorSetting textColor = add(new ColorSetting("Text color", "Name color when effect colors are off.", 0xFFF5F1E8, false));
	private final ColorSetting timeColor = add(new ColorSetting("Time color", "Color of the remaining time.", 0xFFF2C94C, false));
	private final BooleanSetting blink = add(new BooleanSetting("Blink when ending", "Blink during the last 10 seconds.", true));
	private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a box behind each effect.", true));
	private final ColorSetting backgroundColor = add(new ColorSetting("Background color", "Color of the box.", 0x803A0F1A, true));
	private final BooleanSetting hideVanilla = add(new BooleanSetting("Hide vanilla icons", "Hide Minecraft's effect icons in the top right corner.", true));

	public PotionEffectsHud() {
		super("Potion Effects", "Shows your active effects and how long they last (Effect Timer Plus).", true,
				new HudPosition(HudPosition.Anchor.END, 2, HudPosition.Anchor.CENTER, 0));
		textColor.visibleWhen(() -> !effectColors.get());
		backgroundColor.visibleWhen(background::get);
	}

	public boolean hidesVanillaEffects() {
		return isEnabled() && hideVanilla.get();
	}

	private List<MobEffectInstance> effects(Minecraft client, boolean preview) {
		LocalPlayer player = client.player;
		List<MobEffectInstance> effects = new ArrayList<>();
		if (player != null) {
			for (MobEffectInstance effect : player.getActiveEffects()) {
				if (effect.showIcon()) effects.add(effect);
			}
		}
		if (effects.isEmpty() && preview) {
			effects.add(new MobEffectInstance(MobEffects.SPEED, 20 * 95, 1));
			effects.add(new MobEffectInstance(MobEffects.STRENGTH, 20 * 8, 0));
			effects.add(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 300, 0));
		}
		if (sort.is("Name")) {
			effects.sort(Comparator.comparing((MobEffectInstance effect) -> effect.getEffect().value().getDisplayName().getString()));
		} else {
			effects.sort(Comparator.comparingInt(MobEffectInstance::getDuration));
		}
		return effects;
	}

	private int rowWidth(Minecraft client) {
		return switch (style.get()) {
			case "Full" -> ICON + 4 + client.font.width("Fire Resistance III") + 4;
			case "Compact" -> ICON + 4 + client.font.width("88:88") + 2;
			default -> ICON;
		};
	}

	@Override
	public boolean shouldRender(Minecraft client) {
		return !effects(client, false).isEmpty();
	}

	@Override
	public int getWidth(Minecraft client, boolean preview) {
		return rowWidth(client);
	}

	@Override
	public int getHeight(Minecraft client, boolean preview) {
		return Math.max(1, effects(client, preview).size()) * (ICON + 2) - 2;
	}

	@Override
	public void render(GuiGraphics graphics, Minecraft client, boolean preview) {
		int width = rowWidth(client);
		int y = 0;
		for (MobEffectInstance effect : effects(client, preview)) {
			if (background.get()) graphics.fill(0, y, width, y + ICON, backgroundColor.get());

			boolean ending = !effect.isInfiniteDuration() && effect.getDuration() < 200;
			boolean hidden = blink.get() && ending && (System.currentTimeMillis() / 250) % 2 == 0;
			if (!hidden) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(effect.getEffect()), 0, y, ICON, ICON);
			}

			String time = formatTime(effect);
			switch (style.get()) {
				case "Full" -> {
					String name = effect.getEffect().value().getDisplayName().getString() + level(effect.getAmplifier());
					int nameColor = effectColors.get() ? 0xFF000000 | effect.getEffect().value().getColor() : textColor.get();
					graphics.drawString(client.font, name, ICON + 4, y + 1, nameColor, true);
					graphics.drawString(client.font, time, ICON + 4, y + 10, timeColor.get(), true);
				}
				case "Compact" -> graphics.drawString(client.font, time, ICON + 4, y + 5, timeColor.get(), true);
				default -> {
				}
			}
			y += ICON + 2;
		}
	}

	private static String level(int amplifier) {
		return switch (amplifier) {
			case 0 -> "";
			case 1 -> " II";
			case 2 -> " III";
			case 3 -> " IV";
			case 4 -> " V";
			default -> " " + (amplifier + 1);
		};
	}

	private static String formatTime(MobEffectInstance effect) {
		if (effect.isInfiniteDuration()) return "**:**";
		int seconds = effect.getDuration() / 20;
		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}
}
