package com.freedomclient.module.hud;

import com.freedomclient.hud.HudModule;
import com.freedomclient.hud.HudPosition;
import com.freedomclient.music.LocalMusicPlayer;
import com.freedomclient.setting.ActionSetting;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.KeybindSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.setting.StringSetting;
import com.freedomclient.ui.theme.ThemeManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;

import java.util.HashSet;
import java.util.Set;

/** Music Player: reproductor de tus MP3 y WAV locales, con su HUD (canción, barra de progreso y tiempo). */
public class MusicPlayerHud extends HudModule {
	private static final int WIDTH = 150;
	private static final int HEIGHT = 26;

	private final LocalMusicPlayer player = new LocalMusicPlayer();
	private final NumberSetting volume = add(new NumberSetting("Volume", "Music volume (also follows the game's master volume).", 60, 0, 100, 5, "%"));
	private final BooleanSetting shuffle = add(new BooleanSetting("Shuffle", "Play the songs in random order.", false));
	private final BooleanSetting autoplay = add(new BooleanSetting("Autoplay", "Start playing when the game opens.", false));
	private final BooleanSetting stopGameMusic = add(new BooleanSetting("Stop game music", "Pause Minecraft's own music while yours plays.", true));
	private final KeybindSetting playKey = add(new KeybindSetting("Play / pause key", "Key to play or pause the music.", KeybindSetting.NONE));
	private final KeybindSetting nextKey = add(new KeybindSetting("Next key", "Key to skip to the next song.", KeybindSetting.NONE));
	private final KeybindSetting previousKey = add(new KeybindSetting("Previous key", "Key to go back to the previous song.", KeybindSetting.NONE));
	private final KeybindSetting volumeUpKey = add(new KeybindSetting("Volume up key", "Key to turn the music up (5% each press).", KeybindSetting.NONE));
	private final KeybindSetting volumeDownKey = add(new KeybindSetting("Volume down key", "Key to turn the music down (5% each press).", KeybindSetting.NONE));
	private final KeybindSetting stopKey = add(new KeybindSetting("Stop key", "Key to stop the music completely.", KeybindSetting.NONE));
	private final KeybindSetting shuffleKey = add(new KeybindSetting("Shuffle key", "Key to turn shuffle on or off.", KeybindSetting.NONE));
	private final StringSetting songLink = add(new StringSetting("Song link",
			"Direct link to an MP3 or WAV file (it must end in .mp3 or .wav, or be served as audio). YouTube and other streaming sites are not supported.",
			"", 512));
	private final ActionSetting downloadButton = add(new ActionSetting("Add song from link", "Download the song from the link above into your music folder.",
			"Add", this::downloadLink));
	private final ActionSetting playButton = add(new ActionSetting("Play / pause", "Play or pause the music.", "Play", player::togglePause));
	private final ActionSetting nextButton = add(new ActionSetting("Next song", "Skip to the next song.", "Next", player::next));
	private final ActionSetting previousButton = add(new ActionSetting("Previous song", "Go back to the previous song.", "Back", player::previous));
	private final ActionSetting reloadButton = add(new ActionSetting("Reload songs", "Read the music folder again after adding songs.", "Reload",
			player::reload));
	private final ActionSetting folderButton = add(new ActionSetting("Music folder", "Put your MP3 and WAV files in .minecraft/freedomclient/music.",
			"Open", () -> {
				player.reload();
				Util.getPlatform().openPath(player.folder());
			}));

	private final Set<Integer> heldKeys = new HashSet<>();
	private boolean started;
	private volatile String pendingStatus;

	public MusicPlayerHud() {
		super("Music Player", "Plays your own MP3 and WAV songs from the music folder (or from a direct link), with a HUD and hotkeys.", false,
				new HudPosition(HudPosition.Anchor.CENTER, 0, HudPosition.Anchor.START, 34));
	}

	@Override
	protected void onEnable(Minecraft client) {
		player.reload();
		if (autoplay.get()) player.play();
	}

	@Override
	protected void onDisable(Minecraft client) {
		player.stop();
	}

	@Override
	public void onShutdown(Minecraft client) {
		player.stop();
	}

	@Override
	public void onTick(Minecraft client) {
		if (!started) {
			started = true;
			player.reload();
			if (autoplay.get()) player.play();
		}
		player.setShuffle(shuffle.get());
		float master = client.options.getSoundSourceVolume(SoundSource.MASTER);
		player.setVolume(volume.getFloat() / 100.0F * master);
		if (stopGameMusic.get() && player.isPlaying()) client.getMusicManager().stopPlaying();

		if (client.screen == null) {
			key(client, playKey, () -> {
				player.togglePause();
				status(client, player.isPaused() ? "Paused" : "Playing");
			});
			key(client, nextKey, () -> {
				player.next();
				status(client, "Next song");
			});
			key(client, previousKey, () -> {
				player.previous();
				status(client, "Previous song");
			});
			key(client, volumeUpKey, () -> changeVolume(client, 5));
			key(client, volumeDownKey, () -> changeVolume(client, -5));
			key(client, stopKey, () -> {
				player.stop();
				status(client, "Stopped");
			});
			key(client, shuffleKey, () -> {
				shuffle.toggle();
				status(client, "Shuffle " + (shuffle.get() ? "on" : "off"));
			});
		}
		String message = pendingStatus;
		if (message != null) {
			pendingStatus = null;
			status(client, message);
		}
	}

	private void changeVolume(Minecraft client, int step) {
		volume.set(Math.max(0.0, Math.min(100.0, volume.get() + step)));
		status(client, "Volume " + volume.getInt() + "%");
	}

	/** Aviso corto encima de la hotbar con lo que ha hecho la tecla. */
	private static void status(Minecraft client, String text) {
		if (client.player != null) client.player.displayClientMessage(Component.literal("§6♪ Music:§r " + text), true);
	}

	/** Descarga la canción del enlace en un hilo aparte y avisa al acabar. */
	private void downloadLink() {
		String link = songLink.get().trim();
		pendingStatus = "Downloading...";
		player.download(link).thenAccept(result -> pendingStatus = result);
	}

	private void key(Minecraft client, KeybindSetting setting, Runnable action) {
		if (!setting.isBound()) return;
		int key = setting.get();
		boolean down = InputConstants.isKeyDown(client.getWindow(), key);
		if (down && heldKeys.add(key)) action.run();
		else if (!down) heldKeys.remove(key);
	}

	@Override
	public boolean shouldRender(Minecraft client) {
		return player.isActive() && player.title() != null;
	}

	@Override
	public int getWidth(Minecraft client, boolean preview) {
		return WIDTH;
	}

	@Override
	public int getHeight(Minecraft client, boolean preview) {
		return HEIGHT;
	}

	@Override
	public void render(GuiGraphics graphics, Minecraft client, boolean preview) {
		String title = player.title();
		long position = player.positionMs();
		long duration = player.durationMs();
		boolean paused = player.isPaused();
		if (title == null) {
			title = "Artist - Song";
			position = 83_000;
			duration = 215_000;
		}

		graphics.fill(0, 0, WIDTH, HEIGHT, 0xC0200810);
		graphics.fill(0, 0, WIDTH, 1, ThemeManager.accent());
		// Icono: nota musical o pausa.
		int accent = ThemeManager.accent();
		if (paused) {
			graphics.fill(5, 5, 7, 12, accent);
			graphics.fill(9, 5, 11, 12, accent);
		} else {
			graphics.fill(8, 4, 9, 11, accent);
			graphics.fill(9, 4, 12, 5, accent);
			graphics.fill(11, 4, 12, 7, accent);
			graphics.fill(5, 9, 9, 12, accent);
		}

		// Título: si no cabe, se desplaza despacio.
		int textX = 16;
		int textWidth = WIDTH - textX - 4;
		int titleWidth = client.font.width(title);
		graphics.enableScissor(textX, 0, textX + textWidth, 14);
		int scroll = titleWidth > textWidth ? (int) ((System.currentTimeMillis() / 40) % (titleWidth + 30)) : 0;
		graphics.drawString(client.font, title, textX - scroll, 4, 0xFFF5F1E8, true);
		if (scroll > 0) graphics.drawString(client.font, title, textX - scroll + titleWidth + 30, 4, 0xFFF5F1E8, true);
		graphics.disableScissor();

		// Barra de progreso y tiempo.
		String time = format(position) + " / " + format(duration);
		int timeWidth = client.font.width(time);
		int barX = 5;
		int barWidth = WIDTH - 10 - timeWidth - 4;
		float progress = duration > 0 ? Math.min(1.0F, position / (float) duration) : 0.0F;
		graphics.fill(barX, 18, barX + barWidth, 20, 0x80FFFFFF);
		graphics.fill(barX, 18, barX + Math.round(barWidth * progress), 20, accent);
		graphics.drawString(client.font, time, WIDTH - 5 - timeWidth, 15, 0xFFB8B0A4, false);
	}

	private static String format(long ms) {
		long seconds = ms / 1000;
		return seconds / 60 + ":" + String.format("%02d", seconds % 60);
	}
}
