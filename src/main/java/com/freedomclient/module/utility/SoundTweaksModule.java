package com.freedomclient.module.utility;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.StringSetting;
import net.minecraft.client.resources.sounds.SoundInstance;

import java.util.Arrays;
import java.util.Locale;

/** Sound Tweaks: silencia sonidos molestos, por categoría o por nombre. */
public class SoundTweaksModule extends Module {
	private final BooleanSetting rain = add(new BooleanSetting("Mute rain", "Silence rain and thunder.", false));
	private final BooleanSetting explosions = add(new BooleanSetting("Mute explosions", "Silence explosions (TNT, creepers, crystals).", false));
	private final BooleanSetting portals = add(new BooleanSetting("Mute portals", "Silence nether portal hum.", true));
	private final BooleanSetting villagers = add(new BooleanSetting("Mute villagers", "Silence villager and trader sounds.", false));
	private final BooleanSetting mobAmbient = add(new BooleanSetting("Mute mob ambience", "Silence idle sounds of animals and mobs.", false));
	private final StringSetting custom = add(new StringSetting("Muted sounds", "Sound names to mute, separated by commas (e.g. entity.player.burp).", "", 512));

	public SoundTweaksModule() {
		super("Sound Tweaks", "Mute annoying sounds like rain, explosions, portals or villagers.", Category.UTILITY, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** Llamado desde SoundManagerMixin: true si el sonido debe silenciarse. */
	public static boolean shouldMute(SoundInstance sound) {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null) return false;
		SoundTweaksModule module = manager.get(SoundTweaksModule.class);
		if (!module.isEnabled()) return false;

		String path = sound.getIdentifier().getPath();
		if (module.rain.get() && path.startsWith("weather.")) return true;
		if (module.explosions.get() && (path.contains("explode") || path.contains("explosion"))) return true;
		if (module.portals.get() && path.startsWith("block.portal")) return true;
		if (module.villagers.get() && (path.startsWith("entity.villager") || path.startsWith("entity.wandering_trader"))) return true;
		if (module.mobAmbient.get() && path.startsWith("entity.") && path.endsWith(".ambient")) return true;

		String lower = path.toLowerCase(Locale.ROOT);
		return Arrays.stream(module.custom.get().toLowerCase(Locale.ROOT).split(","))
				.map(String::trim).anyMatch(name -> !name.isEmpty() && lower.startsWith(name));
	}
}
