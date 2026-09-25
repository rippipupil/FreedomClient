package com.freedomclient.module.utility;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;

import java.util.Arrays;
import java.util.Locale;

/** Resalta los anuncios del servidor en el chat (y opcionalmente los muestra encima de la barra rápida). */
public class AnnouncementsModule extends Module {
	private final StringSetting keywords = add(new StringSetting("Keywords", "Messages containing any of these words count as announcements (separate with commas).",
			"announcement, broadcast, anuncio, [!], alert, event", 512));
	private final ColorSetting color = add(new ColorSetting("Highlight color", "Color of the announcement tag.", 0xFFF2C94C, false));
	private final BooleanSetting actionBar = add(new BooleanSetting("Show above hotbar", "Also show the announcement above your hotbar.", true));
	private final BooleanSetting sound = add(new BooleanSetting("Sound", "Play a sound for announcements.", true));

	public AnnouncementsModule() {
		super("Chat Announcements", "Highlights server announcements in chat so you don't miss them.", Category.UTILITY, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** Mensajes del servidor: si es un anuncio, se le añade una etiqueta de color. */
	public Component modify(Component message, boolean overlay) {
		if (!isEnabled() || overlay) return message;

		String text = message.getString().toLowerCase(Locale.ROOT);
		boolean matches = Arrays.stream(keywords.get().toLowerCase(Locale.ROOT).split(","))
				.map(String::trim).anyMatch(word -> !word.isEmpty() && text.contains(word));
		if (!matches) return message;

		Minecraft client = Minecraft.getInstance();
		if (actionBar.get()) client.gui.setOverlayMessage(message.copy(), false);
		if (sound.get()) client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1.4F));

		MutableComponent tag = Component.literal("[!] ").withColor(color.get() & 0xFFFFFF);
		return tag.append(message);
	}
}
