package com.freedomclient.module.visual;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;

import java.util.Optional;
import java.util.UUID;

/**
 * Tag: colorea tu nombre con un degradado (Angel Devil, Neon y otros) sobre tu cabeza y en la lista de jugadores.
 * Solo cambia las letras de tu nombre de usuario: los rangos y prefijos del servidor se quedan como están.
 */
public class TagModule extends Module {
	private static TagModule instance;

	private final ModeSetting style = add(new ModeSetting("Style", "Colours of your name.", "Angel Devil",
			"Angel Devil", "Neon", "Ice", "Toxic", "Galaxy", "Gold", "Blood", "Rainbow"));
	private final BooleanSetting aboveHead = add(new BooleanSetting("Above head", "Colour your name tag above your head.", true));
	private final BooleanSetting tabList = add(new BooleanSetting("Tab list", "Colour your name in the player list.", true));
	private final BooleanSetting animated = add(new BooleanSetting("Animated", "The colours slide along your name.", true));
	private final NumberSetting speed = add(new NumberSetting("Speed", "How fast the colours move.", 1, 0.25, 3, 0.25, "x"));

	public TagModule() {
		super("Tag", "Colours your name above your head and in the tab list: Angel Devil, Neon, Ice, Toxic, Galaxy, Gold, Blood or Rainbow.",
				Category.VISUAL, true);
		instance = this;
		speed.visibleWhen(animated::get);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	private int[] palette() {
		return switch (style.get()) {
			case "Neon" -> new int[] {0xFFE14A, 0xC6F25A, 0x5EF0C8, 0x3FD7FF, 0x3A7BFF, 0x6B5BFF};
			case "Ice" -> new int[] {0xFFFFFF, 0xCFF3FF, 0x8FDBFF, 0x5AAEFF};
			case "Toxic" -> new int[] {0xEFFF6A, 0x9CFF4F, 0x3FE36B, 0x19B25A};
			case "Galaxy" -> new int[] {0xFF7AD9, 0xC77DFF, 0x8B7BFF, 0x4FA3FF};
			case "Gold" -> new int[] {0xFFF6C2, 0xF2C94C, 0xE8A926, 0xC98F1E};
			case "Blood" -> new int[] {0xFF8A8A, 0xFF4F4F, 0xD7263D, 0xA8182E};
			default -> new int[] {0xF2C94C, 0xFF8C42, 0xD7263D, 0xFF6FA8, 0x5DADE2};
		};
	}

	/** Color en {@code t} (0..1, se repite): recorre la paleta y vuelve al principio sin saltos. */
	private int color(double t) {
		double wrapped = t - Math.floor(t);
		if (style.is("Rainbow")) return Mth.hsvToRgb((float) wrapped, 0.65F, 1.0F);
		int[] colors = palette();
		double position = wrapped * colors.length;
		int index = (int) position;
		double f = position - index;
		int a = colors[index % colors.length];
		int b = colors[(index + 1) % colors.length];
		int r = (int) ((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * f);
		int g = (int) ((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * f);
		int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * f);
		return r << 16 | g << 8 | bl;
	}

	/**
	 * Recorre el texto conservando el estilo de cada trozo y cambia el color solo de las letras que forman
	 * {@code username}, con el degradado.
	 */
	private Component paint(Component name, String username) {
		String full = name.getString();
		int start = full.indexOf(username);
		if (username.isEmpty() || start < 0) return name;
		int end = start + username.length();
		double shift = animated.get() ? (System.currentTimeMillis() % 100000L) / (4000.0 / speed.get()) : 0.0;
		MutableComponent result = Component.empty();
		int[] index = {0};
		name.visit((style, text) -> {
			for (int i = 0; i < text.length(); i++) {
				int at = index[0]++;
				Style charStyle = style;
				if (at >= start && at < end) {
					double t = (at - start) / (double) Math.max(8, username.length()) + shift;
					charStyle = style.withColor(TextColor.fromRgb(color(t)));
				}
				result.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(charStyle));
			}
			return Optional.empty();
		}, Style.EMPTY);
		return result;
	}

	private static TagModule active() {
		return instance != null && instance.isEnabled() ? instance : null;
	}

	/** Nombre sobre la cabeza (solo el tuyo). */
	public static Component nametag(net.minecraft.world.entity.Entity entity, Component name) {
		TagModule module = active();
		Minecraft client = Minecraft.getInstance();
		if (module == null || !module.aboveHead.get() || entity != client.player) return name;
		return module.paint(name, client.player.getGameProfile().name());
	}

	/** Nombre en la lista de jugadores (solo el tuyo). */
	public static Component tab(UUID playerId, Component name) {
		TagModule module = active();
		Minecraft client = Minecraft.getInstance();
		if (module == null || !module.tabList.get() || client.player == null || !client.player.getUUID().equals(playerId)) return name;
		return module.paint(name, client.player.getGameProfile().name());
	}
}
