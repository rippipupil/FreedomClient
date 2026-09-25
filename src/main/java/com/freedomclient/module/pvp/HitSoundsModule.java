package com.freedomclient.module.pvp;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** HitSounds: un sonido a elegir cada vez que golpeas, en vez de los sonidos de ataque de vanilla. */
public class HitSoundsModule extends Module {
	private static HitSoundsModule instance;

	private final ModeSetting sound = add(new ModeSetting("Sound", "Sound played when you hit something.", "Classic 1.8",
			"Classic 1.8", "Pixel pop", "Bell", "Ding", "Click"));
	private final NumberSetting volume = add(new NumberSetting("Volume", "How loud the hit sound is.", 80, 10, 100, 5, "%"));
	private final NumberSetting pitch = add(new NumberSetting("Pitch", "Higher or lower sound.", 1, 0.5, 2, 0.05, "x"));
	private final BooleanSetting playersOnly = add(new BooleanSetting("Players only", "Only play it when you hit players.", false));
	private final BooleanSetting muteVanilla = add(new BooleanSetting("Mute vanilla attack sounds",
			"Hide the 1.9+ swing sounds (strong, weak, sweep, crit, knockback).", true));

	public HitSoundsModule() {
		super("HitSounds", "Choose the sound of your hits: classic 1.8, pixel pop, bell, ding or click.", Category.PVP, false);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	public void onHit(Entity target) {
		if (playersOnly.get() && !(target instanceof Player)) return;
		Minecraft client = Minecraft.getInstance();
		float basePitch = switch (sound.get()) {
			case "Pixel pop" -> 1.7F;
			case "Bell" -> 1.2F;
			case "Ding" -> 1.4F;
			default -> 1.0F;
		};
		client.getSoundManager().play(SimpleSoundInstance.forUI(soundEvent(), basePitch * pitch.getFloat(), volume.getFloat() / 100.0F));
	}

	private SoundEvent soundEvent() {
		return switch (sound.get()) {
			case "Pixel pop" -> SoundEvents.ITEM_PICKUP;
			case "Bell" -> SoundEvents.NOTE_BLOCK_BELL.value();
			case "Ding" -> SoundEvents.EXPERIENCE_ORB_PICKUP;
			case "Click" -> SoundEvents.UI_BUTTON_CLICK.value();
			// El golpe de 1.8: el sonido de daño del jugador, sin los sonidos de ataque añadidos en 1.9.
			default -> SoundEvents.PLAYER_HURT;
		};
	}

	/** Si hay que silenciar este sonido de ataque de vanilla. */
	public static boolean shouldMute(SoundInstance sound) {
		return instance != null && instance.isEnabled() && instance.muteVanilla.get()
				&& sound.getIdentifier().getPath().startsWith("entity.player.attack.");
	}
}
