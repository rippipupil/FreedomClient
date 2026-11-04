package com.freedomclient.module.visual;

import com.freedomclient.FreedomClient;
import com.freedomclient.freedomplayer.ClientLibrary;
import com.freedomclient.freedomplayer.FreedomPlayerHub;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.hud.MusicPlayerHud;
import com.freedomclient.music.LocalMusicPlayer;
import com.freedomclient.music.LyricsTimeline;
import com.freedomclient.music.NeoStyle;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Vector3fc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NeoLyrics: mientras el Music Player toca una canción con letra sincronizada (la de FreedomPlayer, LRCLIB o un .lrc),
 * la letra va saliendo en el cielo palabra a palabra, como en el diseño (scripts/textures/neolyrics/neolyrics_estilos.html):
 * <ul>
 * <li>Colocación: Cartel 3D (la frase puesta en el mundo a 30 bloques, que se ve en perspectiva al moverte), Flotante
 * (palabras inclinadas y de tamaños distintos, en filas), Disperso (cada palabra en un sitio del cielo) o Recta.</li>
 * <li>Fuente, color, aparición, desaparición, relieve 3D, brillo y tamaño.</li>
 * <li>Arriba, la píldora con la línea que suena y la palabra cantada; al empezar, el título y el artista.</li>
 * </ul>
 * Las cuentas están en {@link LyricsTimeline} y {@link NeoStyle}. Cada letra se dibuja con su propia transformación
 * afín (la que da proyectar tres puntos suyos con la cámara del juego), igual que en el diseño.
 */
public class NeoLyricsModule extends Module {
	/** El halo: dos anillos de ocho copias (radio en unidades de fuente; el diseño usa 5 y 10 en letras de 100). */
	private static final float[][] RING = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {0.7F, 0.7F}, {-0.7F, 0.7F}, {0.7F, -0.7F}, {-0.7F, -0.7F}};
	/** La fuente de Minecraft mide 9 de alto: un alto de letra (em) son 9 unidades. */
	private static final float EM = 9.0F;

	private final ModeSetting layout = add(new ModeSetting("Layout", "Where the lyrics go: a 3D sign in the world, floating words, words scattered over the sky or one straight line.",
			"3D sign", "3D sign", "Floating", "Scattered", "Straight"));
	private final ModeSetting font = add(new ModeSetting("Font", "Font of the letters.", "Rounded", "Minecraft", "Rounded", "Handwritten", "Script", "Futuristic"));
	private final ModeSetting color = add(new ModeSetting("Color", "Colour of the letters and their glow.", "White",
			"White", "Lilac", "Pink", "Cyan", "Gold", "Rainbow", "Karaoke"));
	private final ModeSetting appear = add(new ModeSetting("Appear", "How each letter comes in.", "Bounce", "Bounce", "Fade", "Typewriter", "Zoom"));
	private final ModeSetting leave = add(new ModeSetting("Disappear", "How the letters go when the line ends.", "Rise",
			"Rise", "Fade", "Fall", "Wind", "Shatter", "Shrink"));
	private final BooleanSetting depth = add(new BooleanSetting("3D depth", "Give the letters a raised 3D edge.", true));
	private final BooleanSetting glow = add(new BooleanSetting("Glow", "A soft glow around the letters.", true));
	private final NumberSetting size = add(new NumberSetting("Size", "How big the letters are.", 1, 0.5, 2, 0.05, "x"));
	private final NumberSetting height = add(new NumberSetting("Height", "How high above the horizon the lyrics appear.",
			LyricsTimeline.ELEVATION, 0, 50, 1, "°"));
	private final BooleanSetting pill = add(new BooleanSetting("Lyrics pill", "A pill at the top with the line that is playing and the word being sung.", true));
	private final BooleanSetting title = add(new BooleanSetting("Song title", "Show the song title and artist in the sky when a song starts.", true));

	/** Dónde quedó cada línea: el yaw de la cámara al empezar y, para el cartel, su sitio en el mundo. */
	private record Anchor(float yaw, Vec3 center, Vec3 right, Vec3 up, Vec3 normal) {
	}

	/** Lleva un punto de la letra (lx, ly, lz en em) a la pantalla, o null si queda detrás. */
	private interface Frame {
		float[] map(double lx, double ly, double lz);
	}

	private final Map<Object, Anchor> anchors = new HashMap<>();
	private final Map<String, Float> widths = new HashMap<>();
	private String widthsFont = "";
	private int song = Integer.MIN_VALUE;
	private double lastTime;

	/** Para las capturas de CI: una letra, un instante y un título fijos en vez del Music Player. */
	private static LyricsTimeline testLyrics;
	private static double testTime;
	private static String testTitle, testArtist;

	// Lo que hace falta mientras se dibuja un fotograma.
	private Minecraft client;
	private Vec3 eye;
	private Vector3fc forward;
	private float focal;
	private GuiGraphics graphics;
	private Font mcFont;
	private FontDescription fontDescription;

	public NeoLyricsModule() {
		super("NeoLyrics", "Shows the lyrics of the song playing in the Music Player in the sky, word by word, as a 3D sign, floating "
				+ "or scattered words, with fonts, colours and animations (synced lyrics from FreedomPlayer, LRCLIB or a .lrc file).",
				Category.VISUAL, false);
	}

	/** Solo para las pruebas: fija la letra, el segundo de la canción y el título (null para volver al Music Player). */
	public static void forceForTest(String lrc, double time, String songTitle, String artist) {
		testLyrics = lrc == null ? null : LyricsTimeline.parse(lrc);
		testTime = time;
		testTitle = songTitle;
		testArtist = artist;
	}

	public static void forceForTest(String lrc, double time) {
		forceForTest(lrc, time, null, null);
	}

	public void render(GuiGraphics graphics, Minecraft client) {
		if (!isEnabled() || client.player == null || client.level == null) return;
		LyricsTimeline lyrics;
		double t;
		int number;
		String songTitle, artist;
		long cover = 0xFF3FD7FFL;
		if (testLyrics != null) {
			lyrics = testLyrics;
			t = testTime;
			number = -1;
			songTitle = testTitle;
			artist = testArtist;
		} else {
			MusicPlayerHud hud = FreedomClient.getModuleManager().get(MusicPlayerHud.class);
			if (hud == null || !hud.isEnabled()) return;
			LocalMusicPlayer player = hud.player();
			if (!player.isActive() || player.title() == null) return;
			lyrics = player.lyrics();
			t = player.playedMs() / 1000.0;
			number = player.songNumber();
			songTitle = player.songName();
			artist = player.songArtist();
			String id = player.songId();
			ClientLibrary.Entry entry = id == null ? null : FreedomPlayerHub.library().find(id);
			if (entry != null) cover = entry.song().coverColor();
		}
		// Canción nueva o vuelta atrás: cada línea vuelve a quedarse donde mires al salir.
		if (number != song || t < lastTime - 1) anchors.clear();
		song = number;
		lastTime = t;

		this.client = client;
		this.graphics = graphics;
		this.mcFont = client.font;
		Camera camera = client.gameRenderer.getMainCamera();
		eye = camera.position();
		forward = camera.forwardVector();
		focal = focal(camera);
		fontDescription = switch (font.get()) {
			case "Rounded" -> new FontDescription.Resource(FreedomClient.id("neolyrics_rounded"));
			case "Handwritten" -> new FontDescription.Resource(FreedomClient.id("neolyrics_handwritten"));
			case "Script" -> new FontDescription.Resource(FreedomClient.id("neolyrics_script"));
			case "Futuristic" -> new FontDescription.Resource(FreedomClient.id("neolyrics_futuristic"));
			default -> null;
		};
		if (!font.get().equals(widthsFont)) {
			widths.clear();
			widthsFont = font.get();
		}

		if (title.get() && songTitle != null && lyrics != null) intro(t, lyrics, camera, songTitle, artist);
		if (lyrics != null) lyrics(t, lyrics, camera);
		if (pill.get()) pill(t, lyrics, songTitle != null ? songTitle : "", cover);
	}

	/** Píxeles de pantalla (GUI) por unidad de tangente: lo que da la proyección del juego en el centro. */
	private float focal(Camera camera) {
		Vector3fc up = camera.upVector();
		float[] a = project(eye.add(forward.x() * 10, forward.y() * 10, forward.z() * 10));
		float[] b = project(eye.add(forward.x() * 10 + up.x(), forward.y() * 10 + up.y(), forward.z() * 10 + up.z()));
		if (a == null || b == null) return graphics.guiHeight() / 2.0F / (float) Math.tan(Math.toRadians(35));
		return (a[1] - b[1]) / 0.1F;
	}

	/** Un punto del mundo en la pantalla (GUI), o null si queda detrás de la cámara. */
	private float[] project(Vec3 point) {
		Vec3 d = point.subtract(eye);
		if (d.x * forward.x() + d.y * forward.y() + d.z * forward.z() < 0.3) return null;
		Vec3 ndc = client.gameRenderer.projectPointToScreen(point);
		return new float[]{(float) ((ndc.x + 1) / 2 * graphics.guiWidth()), (float) ((1 - ndc.y) / 2 * graphics.guiHeight())};
	}

	/** La dirección de un yaw (el del juego: sube hacia la derecha) a {@code elev} grados sobre el horizonte. */
	private static Vec3 dir(double yaw, double elev) {
		double y = Math.toRadians(yaw), e = Math.toRadians(elev);
		return new Vec3(-Math.sin(y) * Math.cos(e), Math.sin(e), Math.cos(y) * Math.cos(e));
	}

	/** En el cielo, en la dirección dada: las letras van en el plano de la pantalla. */
	private Frame skyFrame(double yaw, double elev, double scale) {
		Vec3 d = dir(yaw, elev);
		if (d.x * forward.x() + d.y * forward.y() + d.z * forward.z() < 0.2) return null;
		float[] a = project(eye.add(d.scale(1000)));
		if (a == null) return null;
		double px = Math.tan(Math.toRadians(LyricsTimeline.SIZE * size.get() * scale)) * focal;
		return (lx, ly, lz) -> new float[]{(float) (a[0] + (lx - lz * 0.35) * px), (float) (a[1] - (ly + lz * 0.55) * px)};
	}

	/** En el mundo: un cartel a 30 bloques, mirando a donde estabas al empezar la línea. */
	private Frame worldFrame(Anchor an) {
		double hgt = Math.tan(Math.toRadians(LyricsTimeline.SIZE * size.get())) * LyricsTimeline.DISTANCE;
		return (lx, ly, lz) -> project(an.center().add(an.right().scale(lx * hgt)).add(an.up().scale(ly * hgt)).add(an.normal().scale(lz * hgt)));
	}

	private Anchor anchor(Object key, Camera camera) {
		return anchors.computeIfAbsent(key, k -> {
			float yaw = camera.yRot();
			double y = Math.toRadians(yaw), e = Math.toRadians(height.get());
			Vec3 d = dir(yaw, height.get());
			return new Anchor(yaw, camera.position().add(d.scale(LyricsTimeline.DISTANCE)), new Vec3(-Math.cos(y), 0, -Math.sin(y)),
					new Vec3(Math.sin(y) * Math.sin(e), Math.cos(e), -Math.cos(y) * Math.sin(e)), d.scale(-1));
		});
	}

	// ---------------------------------------------------------------------------------------------------------------
	// La letra

	private void lyrics(double t, LyricsTimeline lyrics, Camera camera) {
		NeoStyle.Layout mode = switch (layout.get()) {
			case "Floating" -> NeoStyle.Layout.FLOTANTE;
			case "Scattered" -> NeoStyle.Layout.DISPERSO;
			case "Straight" -> NeoStyle.Layout.RECTA;
			default -> NeoStyle.Layout.CARTEL;
		};
		LyricsTimeline.Appear ap = switch (appear.get()) {
			case "Fade" -> LyricsTimeline.Appear.FUNDIDO;
			case "Typewriter" -> LyricsTimeline.Appear.MAQUINA;
			case "Zoom" -> LyricsTimeline.Appear.ZOOM;
			default -> LyricsTimeline.Appear.SALTO;
		};
		LyricsTimeline.Leave lv = switch (leave.get()) {
			case "Fade" -> LyricsTimeline.Leave.DESVANECER;
			case "Fall" -> LyricsTimeline.Leave.CAER;
			case "Wind" -> LyricsTimeline.Leave.VIENTO;
			case "Shatter" -> LyricsTimeline.Leave.ROMPER;
			case "Shrink" -> LyricsTimeline.Leave.ENCOGER;
			default -> LyricsTimeline.Leave.SUBIR;
		};
		NeoStyle.Color colors = NeoStyle.Color.values()[Math.max(0, color.getModes().indexOf(color.get()))];
		List<LyricsTimeline.Line> lines = lyrics.lines();
		for (int li = 0; li < lines.size(); li++) {
			LyricsTimeline.Line line = lines.get(li);
			double end = line.end();
			if (t < line.t() || t > end + LyricsTimeline.LEAVE + line.text().length() * 0.03 + 0.3) {
				anchors.remove(li);
				continue;
			}
			Anchor an = anchor(li, camera);
			List<NeoStyle.Item> items = NeoStyle.layout(line, li, mode, this::width);
			Map<Integer, Frame> frames = new HashMap<>();
			for (int n = 0; n < items.size(); n++) {
				NeoStyle.Item it = items.get(n);
				LyricsTimeline.Word word = line.words().get(it.wi());
				LyricsTimeline.Letter st = LyricsTimeline.letter(ap, lv, t, word, it.k(), end, n);
				if (st == null) continue;
				int lineIndex = li;
				Frame frame = frames.computeIfAbsent(it.group(), group -> {
					if (mode == NeoStyle.Layout.CARTEL) return worldFrame(an);
					if (group < 0) return skyFrame(an.yaw(), height.get(), 1);
					double[] slot = NeoStyle.slot(lineIndex, group);
					return skyFrame(an.yaw() + slot[0], height.get() + slot[1], 1);
				});
				if (frame != null) letter(frame, it, st, NeoStyle.letterColor(colors, t, word, n, st), n);
			}
		}
	}

	/** Ancho de un carácter en em con la fuente elegida. */
	private double width(String ch) {
		return widths.computeIfAbsent(ch, c -> mcFont.width(text(c)) / EM);
	}

	private Component text(String s) {
		return fontDescription == null ? Component.literal(s) : Component.literal(s).withStyle(style -> style.withFont(fontDescription));
	}

	/** Una letra: el relieve (o una sombra), el brillo, la letra y, al romperse, sus trocitos. */
	private void letter(Frame frame, NeoStyle.Item it, LyricsTimeline.Letter st, int[] color, int n) {
		double gx = it.x() + st.x() * it.scale(), gy = it.y() + st.y() * it.scale(), rot = it.rot() + st.rot(), sc = it.scale() * st.s();
		float face = (float) (st.alpha() * (1 - LyricsTimeline.ease(st.burst() * 3)));
		Component glyph = text(it.ch());
		float half = mcFont.width(glyph) / 2.0F;
		if (face > 0.01F) {
			if (depth.get()) {
				int side = NeoStyle.mix(color[0], 0x3A3550, 0.55);
				for (int k = NeoStyle.LAYERS; k >= 1; k--) {
					int layer = NeoStyle.mix(side, 0x000000, 0.25 * k / NeoStyle.LAYERS);
					draw(frame, gx, gy, -NeoStyle.DEPTH * k / NeoStyle.LAYERS, rot, sc, glyph, half, 0, 0, argb(face, layer));
				}
			} else {
				draw(frame, gx, gy, 0, rot, sc, glyph, half, 0.36F, 0.36F, argb(0.45F * face, 0x141028));
			}
			if (glow.get()) {
				float ga = (float) ((0.22 + 0.25 * st.flash() + (st.singing() ? 0.2 : 0)) * face);
				for (float[] ring : new float[][]{{0.45F, 1}, {0.9F, 0.5F}}) {
					for (float[] d : RING) draw(frame, gx, gy, 0, rot, sc, glyph, half, d[0] * ring[0], d[1] * ring[0], argb(ga * ring[1], color[1]));
				}
			}
			draw(frame, gx, gy, 0, rot, sc, glyph, half, 0, 0, argb(face, color[0]));
		}
		if (st.burst() > 0) shards(frame, gx, gy, rot, sc, st, color[0], n);
	}

	/**
	 * Pone la transformación de la letra (de unidades de fuente a la pantalla, centrada en su sitio) y la dibuja
	 * desplazada (dx, dy) unidades.
	 */
	private void draw(Frame frame, double gx, double gy, double gz, double rot, double sc, Component glyph, float half, float dx, float dy, int argb) {
		if (argb >>> 24 == 0 || !begin(frame, gx, gy, gz, rot, sc)) return;
		Matrix3x2fStack pose = graphics.pose();
		pose.translate(dx - half, dy - 4.5F);
		graphics.drawString(mcFont, glyph, 0, 0, argb, false);
		pose.popMatrix();
	}

	/** Empuja la transformación afín de la letra: las columnas son lo que se mueve en pantalla una unidad de fuente. */
	private boolean begin(Frame frame, double gx, double gy, double gz, double rot, double sc) {
		double c = Math.cos(rot) * sc / EM, s = Math.sin(rot) * sc / EM;
		float[] o = frame.map(gx, gy, gz), a = frame.map(gx + c, gy + s, gz), b = frame.map(gx + s, gy - c, gz);
		if (o == null || a == null || b == null) return false;
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.mul(new Matrix3x2f(a[0] - o[0], a[1] - o[1], b[0] - o[0], b[1] - o[1], o[0], o[1]));
		return true;
	}

	/** Romperse: seis trocitos cuadrados salen de la letra y caen. */
	private void shards(Frame frame, double gx, double gy, double rot, double sc, LyricsTimeline.Letter st, int fill, int n) {
		if (!begin(frame, gx, gy, 0, rot, sc)) return;
		int argb = argb((float) st.alpha(), fill);
		if (argb >>> 24 != 0) {
			Matrix3x2fStack pose = graphics.pose();
			for (int j = 0; j < 6; j++) {
				double ang = LyricsTimeline.hash(n * 8 + j) * Math.PI * 2;
				double d = LyricsTimeline.ease(st.burst()) * (0.6 + LyricsTimeline.hash(n * 8 + j + 100) * 1.2);
				double sx = Math.cos(ang) * d, sy = Math.sin(ang) * d - st.burst() * st.burst() * 1.2;
				float size = (float) (0.14 * EM * (1 - st.burst() * 0.5));
				pose.pushMatrix();
				pose.translate((float) (sx * EM) - size / 2, (float) (-sy * EM) - size / 2);
				pose.scale(size / 4, size / 4);
				graphics.fill(0, 0, 4, 4, argb);
				pose.popMatrix();
			}
		}
		graphics.pose().popMatrix();
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Título y píldora

	/** Título y artista al empezar la canción, con una estela de luz entre los dos. */
	private void intro(double t, LyricsTimeline lyrics, Camera camera, String songTitle, String artist) {
		double stop = Math.min(LyricsTimeline.INTRO, lyrics.lines().get(0).t() - 0.2);
		if (t > stop) return;
		float a = (float) (LyricsTimeline.ease(t / 0.6) * (1 - LyricsTimeline.ease((t - (stop - 0.8)) / 0.8)));
		if (a <= 0.01F) return;
		Anchor an = anchor("intro", camera);
		Frame frame = skyFrame(an.yaw(), height.get() + 6, 1);
		if (frame == null) return;
		int[] colors = NeoStyle.titleColor(NeoStyle.Color.values()[Math.max(0, color.getModes().indexOf(color.get()))]);
		float[] o = frame.map(0, 0, 0), u = frame.map(1, 0, 0);
		float em = u[0] - o[0];
		// Estela: un óvalo de luz muy alargado (dos degradados de los lados al centro).
		int cx = Math.round(o[0]), cy = Math.round(o[1] + em * 0.15F), half = Math.round(em * 4.2F);
		for (int[] band : new int[][]{{2, 0}, {1, 1}}) {
			int glowColor = band[1] == 0 ? argb(0.35F * a, colors[1]) : argb(0.9F * a, 0xFFFFFF);
			int clear = glowColor & 0xFFFFFF;
			Draw.hGradient(graphics, cx - half, cy - band[0], cx, cy + band[0], clear, glowColor);
			Draw.hGradient(graphics, cx, cy - band[0], cx + half, cy + band[0], glowColor, clear);
		}
		String[] texts = {songTitle, artist};
		double[][] place = {{1.4 + (1 - LyricsTimeline.ease(t / 0.8)) * -0.4, 2.2}, {-1.0, 1.2}};
		for (int i = 0; i < 2; i++) {
			if (texts[i] == null || texts[i].isBlank()) continue;
			float[] p = frame.map(0, place[i][0], 0);
			Component text = text(texts[i]);
			float scale = (float) (em * place[i][1] / EM);
			float w = mcFont.width(text);
			Matrix3x2fStack pose = graphics.pose();
			pose.pushMatrix();
			pose.translate(p[0], p[1]);
			pose.scale(scale, scale);
			if (glow.get()) {
				for (float[] d : RING) {
					pose.pushMatrix();
					pose.translate(d[0] * 0.72F - w / 2, d[1] * 0.72F - 4.5F);
					graphics.drawString(mcFont, text, 0, 0, argb(0.25F * a, colors[1]), false);
					pose.popMatrix();
				}
			}
			pose.translate(-w / 2, -4.5F);
			graphics.drawString(mcFont, text, 0, 0, argb(a, colors[0]), false);
			pose.popMatrix();
		}
	}

	/** La píldora de arriba: carátula, tiempo, la línea que suena con la palabra cantada resaltada y unas barritas. */
	private void pill(double t, LyricsTimeline lyrics, String songTitle, long cover) {
		LyricsTimeline.Line current = null;
		if (lyrics != null) {
			for (LyricsTimeline.Line line : lyrics.lines()) if (t >= line.t() && t < line.end() + 0.5) current = line;
		}
		String text = current != null ? current.text() : "♪ " + songTitle;
		int textW = Math.min(240, mcFont.width(text));
		int w = textW + 64, h = 16, x = (graphics.guiWidth() - w) / 2, y = 4;
		int accent = 0xFFC9A6FF;
		graphics.fill(x + 3, y, x + w - 3, y + h, 0xD9161128);
		graphics.fill(x + 1, y + 2, x + w - 1, y + h - 2, 0xD9161128);
		graphics.fill(x + 3, y, x + w - 3, y + 1, ThemeManager.withAlpha(accent, 0.45F));
		graphics.fill(x + 3, y + h - 1, x + w - 3, y + h, ThemeManager.withAlpha(accent, 0.45F));
		// Carátula: un disco de su color.
		int c = (int) cover | 0xFF000000;
		graphics.fill(x + 5, y + 4, x + 13, y + 12, c);
		graphics.fill(x + 6, y + 3, x + 12, y + 13, c);
		graphics.fill(x + 4, y + 5, x + 14, y + 11, c);
		String time = (int) t / 60 + ":" + String.format("%02d", (int) t % 60);
		graphics.drawString(mcFont, time, x + 17, y + 4, 0xFFB9B3D6, false);
		int tx = x + 44;
		graphics.enableScissor(tx - 3, y, tx + textW + 3, y + h);
		if (current != null) {
			LyricsTimeline.Word sung = null;
			for (LyricsTimeline.Word word : current.words()) if (t >= word.start()) sung = word;
			int cx = tx;
			for (LyricsTimeline.Word word : current.words()) {
				int ww = mcFont.width(word.text());
				if (word == sung) graphics.fill(cx - 2, y + 3, cx + ww + 2, y + h - 3, 0xFF9B6BFF);
				graphics.drawString(mcFont, word.text(), cx, y + 4, t >= word.start() ? 0xFFFFFFFF : 0x8CFFFFFF, false);
				cx += ww + mcFont.width(" ");
			}
		} else {
			graphics.drawString(mcFont, text, tx, y + 4, 0xFFFFFFFF, false);
		}
		graphics.disableScissor();
		for (int i = 0; i < 4; i++) {
			int bh = 3 + (int) Math.round(Math.abs(Math.sin(t * 7 + i * 1.3)) * 6);
			graphics.fill(x + w - 17 + i * 3, y + 8 - bh / 2, x + w - 15 + i * 3, y + 8 - bh / 2 + bh, accent);
		}
	}

	private static int argb(float alpha, int rgb) {
		int a = Math.round(Math.max(0, Math.min(1, alpha)) * 255);
		// La fuente pinta opaco el texto con alfa de 1 a 3: tan transparente ya no se dibuja (si no, parpadea al irse).
		return a < 4 ? 0 : a << 24 | (rgb & 0xFFFFFF);
	}
}
