package com.freedomclient.module.hud;

import com.freedomclient.hud.HudPosition;
import com.freedomclient.hud.TextHudModule;
import com.freedomclient.music.SpotifyNowPlaying;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/** Spotify: muestra en el HUD la canción que suena en la app de escritorio de Spotify. */
public class NowPlayingHud extends TextHudModule {
	private final NumberSetting maxLength = add(new NumberSetting("Max length", "Longer names are cut with \"...\".", 40, 15, 80, 5));

	public NowPlayingHud() {
		super("Spotify", "Shows the song playing in the Spotify desktop app.", false,
				new HudPosition(HudPosition.Anchor.CENTER, 0, HudPosition.Anchor.START, 18));
		textColor.set(0xFF1ED760);
	}

	@Override
	protected void onEnable(Minecraft client) {
		SpotifyNowPlaying.start();
	}

	@Override
	protected void onDisable(Minecraft client) {
		SpotifyNowPlaying.stop();
	}

	@Override
	public void onTick(Minecraft client) {
		// Por si el módulo ya venía activado de la config (onEnable no se llama al cargar).
		SpotifyNowPlaying.start();
	}

	@Override
	protected String getText(Minecraft client) {
		String song = SpotifyNowPlaying.current();
		if (song == null) return null;
		int max = maxLength.getInt();
		if (song.length() > max) song = song.substring(0, max - 3) + "...";
		return "♫ " + song;
	}

	@Override
	protected String getPreviewText() {
		return "♫ Artist - Song";
	}
}
