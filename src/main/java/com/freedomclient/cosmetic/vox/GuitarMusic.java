package com.freedomclient.cosmetic.vox;

import com.freedomclient.FreedomClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Música de las guitarras: con una guitarra puesta y en tercera persona suena su canción bajita (25 %), y mientras
 * la tocas, a volumen normal. Los cambios de volumen son fundidos de un segundo. Va en la categoría de sonido
 * "Jukebox/Note Blocks" (respeta sus deslizadores) y quita la música del juego mientras se oye.
 */
public final class GuitarMusic {
	/** Cuánto cambia el volumen por tick: de 0 a 1 en algo más de un segundo. */
	private static final float FADE_STEP = 0.04F;
	/** Ticks en silencio tras los que se para la canción del todo (al volver empieza desde el principio). */
	private static final int STOP_AFTER_SILENT_TICKS = 200;

	private static SongSound sound;
	private static String playingSong;
	private static float volume;
	private static float target;
	private static int silentTicks;

	private GuitarMusic() {
	}

	/** Volumen actual (0..1), para los logs y los tests. */
	public static float volume() {
		return volume;
	}

	public static String playingSong() {
		return sound == null ? null : playingSong;
	}

	/** Vuelve a empezar la canción desde el principio (al empezar a tocar). */
	public static void restart(Minecraft client) {
		stop(client);
	}

	/** Llamado cada tick: decide el volumen que toca y hace el fundido. */
	public static void tick(Minecraft client) {
		MusicGuitarCosmetic guitar = MusicGuitarCosmetic.active();
		String song = guitar == null ? null : guitar.song();
		target = 0.0F;
		if (song != null && client.player != null && client.level != null && client.player.isAlive()) {
			if (GuitarEmote.isPlaying()) {
				target = guitar.playVolume.getFloat() / 100.0F;
			} else if (guitar.music.get() && !client.options.getCameraType().isFirstPerson()) {
				target = guitar.idleVolume.getFloat() / 100.0F;
			}
		}
		// Si cambia la guitarra, se corta la canción de antes.
		if (sound != null && (song == null || !song.equals(playingSong))) {
			if (song == null) {
				target = 0.0F;
			} else {
				stop(client);
			}
		}
		if (volume < target) volume = Math.min(target, volume + FADE_STEP);
		if (volume > target) volume = Math.max(target, volume - FADE_STEP);

		if (target > 0.0F && sound == null && song != null) {
			playingSong = song;
			sound = new SongSound(SoundEvent.createVariableRangeEvent(FreedomClient.id(song)));
			client.getSoundManager().play(sound);
			FreedomClient.LOGGER.info("[guitar] playing {} (target volume {})", song, target);
		}
		if (sound != null) {
			silentTicks = volume <= 0.001F ? silentTicks + 1 : 0;
			if (silentTicks > STOP_AFTER_SILENT_TICKS) stop(client);
			// La música del juego no se mezcla con la de la guitarra.
			if (volume > 0.01F) client.getMusicManager().stopPlaying();
		}
	}

	/** Corta la canción del todo. */
	public static void stop(Minecraft client) {
		if (sound != null) {
			client.getSoundManager().stop(sound);
			sound = null;
		}
		playingSong = null;
		volume = 0.0F;
		silentTicks = 0;
	}

	/** La canción: sin posición (suena igual por los dos oídos), en bucle y con el volumen del fundido. */
	private static final class SongSound extends AbstractTickableSoundInstance {
		SongSound(SoundEvent event) {
			super(event, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
			this.looping = true;
			this.delay = 0;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
			this.volume = Math.max(0.001F, GuitarMusic.volume);
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}

		@Override
		public void tick() {
			if (GuitarMusic.sound != this) {
				stop();
				return;
			}
			this.volume = Math.max(0.0F, GuitarMusic.volume);
		}
	}
}
