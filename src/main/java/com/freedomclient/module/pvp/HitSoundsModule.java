package com.freedomclient.module.pvp;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * HitSounds: un sonido a elegir cada vez que golpeas, con una variante distinta para los críticos.
 * Los sonidos propios están sintetizados por tools/make_hitsounds.py (assets/freedomclient/sounds/hit).
 */
public class HitSoundsModule extends Module {
	private static HitSoundsModule instance;

	private final ModeSetting sound = add(new ModeSetting("Sound", "Sound played when you hit something. Changing it plays a preview.",
			"Pixel pop", "Classic 1.8", "Pixel pop", "Bell", "Laser", "Coin", "Punch", "Angel harp", "Devil", "Click"));
	private final BooleanSetting critSound = add(new BooleanSetting("Crit variant", "Play a stronger version of the sound on critical hits.", true));
	private final NumberSetting volume = add(new NumberSetting("Volume", "How loud the hit sound is.", 80, 10, 100, 5, "%"));
	private final NumberSetting pitch = add(new NumberSetting("Pitch", "Higher or lower sound.", 1, 0.5, 2, 0.05, "x"));
	private final BooleanSetting playersOnly = add(new BooleanSetting("Players only", "Only play it when you hit players.", false));
	private final BooleanSetting muteVanilla = add(new BooleanSetting("Mute vanilla attack sounds",
			"Hide the 1.9+ swing sounds (strong, weak, sweep, crit, knockback).", true));

	private String lastSound;

	public HitSoundsModule() {
		super("HitSounds", "Distinct hit sounds with a special crit version: pixel pop, bell, laser, coin, punch, harp, devil, click or 1.8.",
				Category.PVP, false);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	@Override
	public void onTick(Minecraft client) {
		// Al cambiar de sonido en el menú suena una muestra (primero el normal).
		String current = sound.get();
		if (lastSound != null && !lastSound.equals(current) && client.screen != null) play(client, false);
		lastSound = current;
	}

	public void onHit(Entity target) {
		if (playersOnly.get() && !(target instanceof Player)) return;
		Minecraft client = Minecraft.getInstance();
		play(client, critSound.get() && isCritical(client.player, target));
	}

	private void play(Minecraft client, boolean crit) {
		float volumeValue = volume.getFloat() / 100.0F;
		if (sound.is("Classic 1.8")) {
			// El golpe de 1.8 es el sonido de daño; en los críticos suena además el crítico de vanilla.
			client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_HURT, pitch.getFloat(), volumeValue));
			if (crit) client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_ATTACK_CRIT, pitch.getFloat(), volumeValue));
			return;
		}
		String name = switch (sound.get()) {
			case "Bell" -> "bell";
			case "Laser" -> "laser";
			case "Coin" -> "coin";
			case "Punch" -> "punch";
			case "Angel harp" -> "angel";
			case "Devil" -> "devil";
			case "Click" -> "click";
			default -> "pop";
		};
		SoundEvent event = SoundEvent.createVariableRangeEvent(FreedomClient.id("hit." + name + (crit ? "_crit" : "")));
		client.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch.getFloat(), volumeValue));
	}

	/** Las mismas condiciones que usa el juego para un golpe crítico (cayendo, con el ataque cargado, sin correr...). */
	private static boolean isCritical(LocalPlayer player, Entity target) {
		return player != null && target instanceof LivingEntity
				&& player.getAttackStrengthScale(0.5F) > 0.9F
				&& player.fallDistance > 0.0F && !player.onGround() && !player.onClimbable() && !player.isInWater()
				&& !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger() && !player.isSprinting() && !player.isFallFlying();
	}

	/** Si hay que silenciar este sonido de ataque de vanilla. */
	public static boolean shouldMute(SoundInstance sound) {
		// Solo los que suenan en el mundo (fuente PLAYERS); los de HitSounds suenan como interfaz y no se tocan.
		return instance != null && instance.isEnabled() && instance.muteVanilla.get() && sound.getSource() == SoundSource.PLAYERS
				&& sound.getIdentifier().getPath().startsWith("entity.player.attack.");
	}
}
