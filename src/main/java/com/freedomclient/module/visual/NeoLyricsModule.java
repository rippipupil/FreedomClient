package com.freedomclient.module.visual;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.hud.MusicPlayerHud;
import com.freedomclient.music.LocalMusicPlayer;
import com.freedomclient.music.LyricsTimeline;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Vector3fc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NeoLyrics: mientras el Music Player toca una canción con letra sincronizada (la de FreedomPlayer o un .lrc), la letra
 * va saliendo en el cielo delante de ti, palabra a palabra, en letras pixel grandes con brillo y la animación «Saltito».
 * Cada línea se queda en el trozo de cielo al que mirabas cuando empezó (a 24° sobre el horizonte), como en el diseño
 * (scripts/textures/neolyrics/neolyrics.html); las cuentas están en {@link LyricsTimeline}.
 */
public class NeoLyricsModule extends Module {
	/** El diseño mide la pantalla con un campo de visión horizontal de 70° (a 480 de ancho). */
	private static final double DESIGN_FOV = 70;
	private static final double DESIGN_WIDTH = 480;
	/** El halo: la letra ocho veces alrededor (en píxeles del diseño). */
	private static final float[][] GLOW = {{-1.5F, 0}, {1.5F, 0}, {0, -1.5F}, {0, 1.5F}, {-1, -1}, {1, 1}, {-1, 1}, {1, -1}};

	private final ModeSetting color = add(new ModeSetting("Color", "Glow colour of the letters.", "White", "White", "Theme", "Cyan"));
	private final NumberSetting size = add(new NumberSetting("Size", "How big the letters are.", 1, 0.5, 2, 0.1, "x"));
	private final NumberSetting height = add(new NumberSetting("Height", "How high above the horizon the lyrics appear.",
			LyricsTimeline.ELEVATION, 5, 60, 1, "°"));

	/** Dónde quedó cada línea en el cielo: el yaw de la cámara cuando empezó. */
	private final Map<Integer, Float> anchors = new HashMap<>();
	private int song = Integer.MIN_VALUE;
	private double lastTime;

	/** Para las capturas de CI: una letra y un instante fijos en vez del Music Player. */
	private static LyricsTimeline testLyrics;
	private static double testTime;

	public NeoLyricsModule() {
		super("NeoLyrics", "Shows the lyrics of the song playing in the Music Player in the sky in front of you, "
				+ "word by word in big glowing pixel letters (synced lyrics from FreedomPlayer or a .lrc file).", Category.VISUAL, false);
	}

	/** Solo para las pruebas: fija la letra y el segundo de la canción (null para volver al Music Player). */
	public static void forceForTest(String lrc, double time) {
		testLyrics = lrc == null ? null : LyricsTimeline.parse(lrc);
		testTime = time;
	}

	public void render(GuiGraphics graphics, Minecraft client) {
		if (!isEnabled() || client.player == null || client.level == null) return;
		LyricsTimeline lyrics;
		double t;
		int number;
		if (testLyrics != null) {
			lyrics = testLyrics;
			t = testTime;
			number = -1;
		} else {
			MusicPlayerHud hud = FreedomClient.getModuleManager().get(MusicPlayerHud.class);
			if (hud == null || !hud.isEnabled()) return;
			LocalMusicPlayer player = hud.player();
			if (!player.isActive() || player.title() == null) return;
			lyrics = player.lyrics();
			t = player.playedMs() / 1000.0;
			number = player.songNumber();
		}
		if (lyrics == null) return;
		// Canción nueva o vuelta atrás: cada línea vuelve a quedarse donde mires al salir.
		if (number != song || t < lastTime - 1) anchors.clear();
		song = number;
		lastTime = t;

		Camera camera = client.gameRenderer.getMainCamera();
		Vec3 eye = camera.position();
		Vector3fc forward = camera.forwardVector();
		int width = graphics.guiWidth(), screenHeight = graphics.guiHeight();
		double focal = width / 2.0 / Math.tan(Math.toRadians(DESIGN_FOV / 2));
		double px = Math.tan(Math.toRadians(LyricsTimeline.SIZE * size.get())) * focal; // alto de letra
		float unit = (float) (width / DESIGN_WIDTH);
		int[] palette = palette();
		double pitch = Math.toRadians(-height.get());

		List<LyricsTimeline.Line> lines = lyrics.lines();
		for (int li = 0; li < lines.size(); li++) {
			LyricsTimeline.Line line = lines.get(li);
			double end = line.end();
			if (t < line.t() || t > end + LyricsTimeline.LEAVE + 1) {
				anchors.remove(li);
				continue;
			}
			float anchor = anchors.computeIfAbsent(li, k -> camera.yRot());
			double yaw = Math.toRadians(anchor);
			Vec3 dir = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
			// Lo que queda detrás de la cámara (o muy a un lado) no se dibuja.
			if (dir.x * forward.x() + dir.y * forward.y() + dir.z * forward.z() < 0.2) continue;
			Vec3 ndc = client.gameRenderer.projectPointToScreen(eye.add(dir.scale(100)));
			double cx = (ndc.x + 1) / 2 * width, cy = (1 - ndc.y) / 2 * screenHeight;

			double step = px * LyricsTimeline.SPACING;
			double x = cx - line.chars() * step / 2 + step / 2;
			int n = 0;
			for (LyricsTimeline.Word word : line.words()) {
				String text = word.text();
				for (int k = 0; k < text.length(); k++) {
					LyricsTimeline.Letter state = LyricsTimeline.letter(t, word, k, end, n++);
					char c = text.charAt(k);
					if (state != null && !Character.isLowSurrogate(c)) {
						String glyph = Character.isHighSurrogate(c) && k + 1 < text.length() ? text.substring(k, k + 2) : String.valueOf(c);
						letter(graphics, client.font, glyph, (float) x, (float) (cy - state.y() * px), (float) px, unit, state, palette);
					}
					x += step;
				}
				x += step;
			}
		}
	}

	/** {relleno, halo} en RGB. */
	private int[] palette() {
		return switch (color.get()) {
			case "Theme" -> {
				int accent = ThemeManager.accent() & 0xFFFFFF;
				yield new int[]{mix(0xFFFFFF, accent, 0.15), accent};
			}
			case "Cyan" -> new int[]{0xEAFCFF, 0x3FD7FF};
			default -> new int[]{0xFFFFFF, 0xDCEBFF};
		};
	}

	/** Una letra: el halo (la letra ocho veces alrededor, muy transparente), la sombra y la letra. */
	private static void letter(GuiGraphics graphics, Font font, String glyph, float x, float y, float px, float unit,
							   LyricsTimeline.Letter state, int[] palette) {
		Matrix3x2fStack pose = graphics.pose();
		float alpha = (float) state.alpha();
		float scale = px / 9.0F; // la fuente de Minecraft mide 9 de alto
		float half = (font.width(glyph) - 1) / 2.0F;
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale((float) state.s(), (float) state.s());
		float glow = (float) ((0.3 + 0.3 * state.flash() + (state.singing() ? 0.25 : 0)) * alpha);
		for (float[] offset : GLOW) glyph(graphics, font, glyph, offset[0] * unit, offset[1] * unit, scale, half, argb(glow, palette[1]));
		glyph(graphics, font, glyph, 1.5F * unit, 1.5F * unit, scale, half, argb(0.5F * alpha, 0x141028));
		int fill = state.flash() > 0.5 || state.singing() ? 0xFFFFFF : palette[0];
		glyph(graphics, font, glyph, 0, 0, scale, half, argb(alpha, fill));
		pose.popMatrix();
	}

	private static void glyph(GuiGraphics graphics, Font font, String glyph, float dx, float dy, float scale, float half, int color) {
		if (color >>> 24 == 0) return;
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(dx, dy);
		pose.scale(scale, scale);
		// Centrada en su sitio, como en el diseño (textAlign center y textBaseline middle).
		pose.translate(-half, -4.0F);
		graphics.drawString(font, glyph, 0, 0, color, false);
		pose.popMatrix();
	}

	private static int argb(float alpha, int rgb) {
		return Math.round(Math.max(0, Math.min(1, alpha)) * 255) << 24 | rgb;
	}

	private static int mix(int a, int b, double t) {
		int r = (int) Math.round((a >> 16 & 0xFF) * (1 - t) + (b >> 16 & 0xFF) * t);
		int g = (int) Math.round((a >> 8 & 0xFF) * (1 - t) + (b >> 8 & 0xFF) * t);
		int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return r << 16 | g << 8 | bl;
	}
}
