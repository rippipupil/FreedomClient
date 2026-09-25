package com.freedomclient.module.visual;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.ui.UiText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/** Pone la insignia FC delante de tu nombre (sobre tu cabeza y en la lista de jugadores). */
public class FcNametagModule extends Module {
	private final BooleanSetting nametag = add(new BooleanSetting("Above head", "Show the FC badge in your name tag (F5).", true));
	private final BooleanSetting tabList = add(new BooleanSetting("Tab list", "Show the FC badge in the player list.", true));
	private final BooleanSetting gradient = add(new BooleanSetting("Gradient name", "Your name above your head gets an animated sunset gradient.", true));
	private final BooleanSetting showOwn = add(new BooleanSetting("Show own name", "See your own name tag in third person (F5).", true));

	/** Colores del degradado del nombre: dorado, naranja atardecer, rojo, rosa y azul cielo. */
	private static final int[] GRADIENT = {0xF2C94C, 0xFF8C42, 0xD7263D, 0xFF6FA8, 0x5DADE2, 0xF2C94C};

	public FcNametagModule() {
		super("FC Nametag", "FC badge next to your name and an animated gradient name above your head.", Category.VISUAL, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	private static FcNametagModule active() {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null) return null;
		FcNametagModule module = manager.get(FcNametagModule.class);
		return module.isEnabled() ? module : null;
	}

	private static Component badge(Component name) {
		return Component.empty().append(UiText.fcBadge()).append(" ").append(name);
	}

	public static Component decorateNametag(Entity entity, Component name) {
		FcNametagModule module = active();
		if (module == null || !module.nametag.get() || entity != Minecraft.getInstance().player) return name;
		return badge(name);
	}

	/** El nombre del jugador con un degradado que se desplaza con el tiempo. */
	public static Component gradientName(Entity entity, Component name) {
		FcNametagModule module = active();
		if (module == null || !module.gradient.get() || entity != Minecraft.getInstance().player) return name;
		String text = name.getString();
		if (text.isEmpty()) return name;
		double shift = (System.currentTimeMillis() % 4000L) / 4000.0;
		MutableComponent result = Component.empty();
		for (int i = 0; i < text.length(); i++) {
			double t = (i / (double) Math.max(8, text.length()) + shift) % 1.0;
			result.append(Component.literal(String.valueOf(text.charAt(i)))
					.withStyle(style -> style.withColor(TextColor.fromRgb(sample(t)))));
		}
		return result;
	}

	private static int sample(double t) {
		double position = t * (GRADIENT.length - 1);
		int index = (int) position;
		double f = position - index;
		int a = GRADIENT[index];
		int b = GRADIENT[Math.min(index + 1, GRADIENT.length - 1)];
		int r = (int) ((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * f);
		int g = (int) ((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * f);
		int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * f);
		return r << 16 | g << 8 | bl;
	}

	/** Si hay que enseñar tu propio nombre en tercera persona. */
	public static boolean showOwnName(Entity entity) {
		FcNametagModule module = active();
		Minecraft client = Minecraft.getInstance();
		return module != null && module.showOwn.get() && entity == client.player && !client.options.hideGui
				&& !client.options.getCameraType().isFirstPerson();
	}

	public static Component decorateTabName(UUID playerId, Component name) {
		FcNametagModule module = active();
		Minecraft client = Minecraft.getInstance();
		if (module == null || !module.tabList.get() || client.player == null || !client.player.getUUID().equals(playerId)) return name;
		return badge(name);
	}
}
