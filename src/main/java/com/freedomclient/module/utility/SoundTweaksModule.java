package com.freedomclient.module.utility;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.MobListSetting;
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
	private final MobListSetting mutedMobs = add(new MobListSetting("Muted mobs",
			"Click the mobs you want to silence completely (every sound they make: steps, hurt, attacks, idle...)."));
	private final StringSetting custom = add(new StringSetting("Muted sounds", "Sound names to mute, separated by commas (e.g. entity.player.burp).", "", 512));

	private String parsedCustom;
	private String[] parsedNames = new String[0];

	public SoundTweaksModule() {
		super("Sound Tweaks", "Mute annoying sounds like rain, explosions, portals, villagers or any mob you pick.", Category.UTILITY, true);
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
		// Mobs silenciados: todos los sonidos "entity.<mob>.*" (el pez globo suena como "puffer_fish").
		if (path.startsWith("entity.") && !module.mutedMobs.get().isEmpty()) {
			int end = path.indexOf('.', 7);
			if (end > 7) {
				String mob = path.substring(7, end);
				if (module.mutedMobs.contains(mob) || mob.equals("puffer_fish") && module.mutedMobs.contains("pufferfish")) return true;
			}
		}

		String lower = path.toLowerCase(Locale.ROOT);
		for (String name : module.customNames()) {
			if (lower.startsWith(name)) return true;
		}
		return false;
	}

	/** Nombres escritos a mano, ya separados (solo se vuelve a trocear el texto cuando cambia). */
	private String[] customNames() {
		String text = custom.get();
		if (!text.equals(parsedCustom)) {
			parsedCustom = text;
			parsedNames = Arrays.stream(text.toLowerCase(Locale.ROOT).split(",")).map(String::trim).filter(name -> !name.isEmpty())
					.toArray(String[]::new);
		}
		return parsedNames;
	}
}
