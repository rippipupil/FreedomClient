package com.freedomclient.cosmetic.vox;

import com.freedomclient.FreedomClient;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.ui.theme.ThemeManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Juliet (Música): la guitarra diseñada en Claude Design. Cuerpo blanco con puntas de mechón, una flor morada en relieve
 * en la boca de la que cae sangre, hojas verdes y cintas moradas; la pala con flequillo, gotas azules y una mini flor, y
 * el diapasón con flores incrustadas. Alrededor caen despacio flores de vóxeles moradas, blancas y rojas que giran.
 * <p>
 * El modelo (assets/freedomclient/vox/juliet.vox) sale del código del propio diseño con
 * scripts/textures/juliet_guitar: es vóxel a vóxel el mismo. Las flores usan los mismos números que el diseño.
 */
public class JulietGuitarCosmetic extends MusicGuitarCosmetic {
	/** Colores del diseño (PAL, en el mismo orden): w d g l b r p v u m n e y c s h k. */
	private static final Vox.Palette PALETTE = new Vox.Palette("guitar_juliet",
			'w', 0xFFF7F7F9, 'd', 0xFF3A3A42, 'g', 0xFFB9BAC2, 'l', 0xFFD6D7DD, 'b', 0xFFC3CBEA, 'r', 0xFFB1101C,
			'p', 0xFFECC4C8, 'v', 0xFF5A3EA8, 'u', 0xFF6D50C4, 'm', 0xFF4B3196, 'n', 0xFF35206E, 'e', 0xFF3F7D2C,
			'y', 0xFF8CB43C, 'c', 0xFF6A3FA3, 's', 0xFFDFE1EA, 'h', 0xFFE4E5EA, 'k', 0xFFCDCED6);
	private static final String MODEL = "/assets/freedomclient/vox/juliet.vox";
	/** Sonido de su canción (sounds.json). */
	public static final String SONG = "guitar.juliet";
	/** Ritmo del rasgueo: la canción va a 140 pulsos (medido sobre el audio); un rasgueo cada dos, tranquilo. */
	private static final float BPM = 70.0F;
	/** Inclinación de la guitarra en el diseño (rotation.z = -0,38 rad, con y hacia arriba). */
	private static final float TILT = 0.38F;
	/** Centro de la flor de la boca en el .vox: queda en el centro de la espalda. */
	private static final float CENTER_Y = -31.0F;
	/** Origen del diseño (donde giran la guitarra y las flores) en el .vox: y = 60 del diseño, menos su base 107,5. */
	private static final float PIVOT_Y = -47.5F;

	/** Tarjeta del menú (scripts/textures/juliet_guitar/card.py): campo lejano, neblina, flores cercanas, flor morada. */
	private static final Identifier CARD_FAR = FreedomClient.id("textures/cosmetic/juliet_card_far.png");
	private static final Identifier CARD_HAZE = FreedomClient.id("textures/cosmetic/juliet_card_haze.png");
	private static final Identifier CARD_NEAR = FreedomClient.id("textures/cosmetic/juliet_card_near.png");
	private static final Identifier CARD_FLOWER = FreedomClient.id("textures/cosmetic/juliet_card_flower.png");
	private static final Identifier TITLE = FreedomClient.id("textures/cosmetic/juliet_title.png");
	private static final Identifier ROOT_TOP = FreedomClient.id("textures/cosmetic/juliet_root_top.png");
	private static final Identifier ROOT_BOTTOM = FreedomClient.id("textures/cosmetic/juliet_root_bottom.png");
	private static final Identifier ROOT_LEFT = FreedomClient.id("textures/cosmetic/juliet_root_left.png");
	private static final Identifier ROOT_RIGHT = FreedomClient.id("textures/cosmetic/juliet_root_right.png");
	private static final Identifier CORNER_PURPLE = FreedomClient.id("textures/cosmetic/juliet_corner_purple.png");
	private static final Identifier CORNER_RED = FreedomClient.id("textures/cosmetic/juliet_corner_red.png");
	private static final int CARD_WIDTH = 320;
	private static final int CARD_HEIGHT = 64;
	private static final int FLOWER_WIDTH = 17;
	private static final int FLOWER_HEIGHT = 26;
	private static final int TITLE_WIDTH = 45;
	private static final int TITLE_HEIGHT = 12;
	/** Grosor de las tiras de raíces del borde. */
	private static final int ROOT = 6;
	private static final int CORNER = 11;
	/** Pétalos que lleva el viento por la tarjeta: rojo, blanco (con su sombra gris) y morado. */
	private static final int[] PETAL_COLORS = {0xFFB1101C, 0xFFB9BAC2, 0xFF6D50C4, 0xFFE0566A};

	/** Las 21 piezas de cada flor del diseño: x, y, z (y hacia arriba) y lado del cubo, en múltiplos de su tamaño. */
	private static final float[][] FLOWER = flower();
	/** Colores de las 13 flores del diseño (FC). */
	private static final int[] FLOWER_COLORS = {0x5A3EA8, 0xF7F7F9, 0xB1101C, 0x6D50C4, 0xE9E4F5, 0xD0202E, 0x8A6FD8, 0xF3D6DA,
			0x8A0D17, 0x4B3196, 0xC9B8EE, 0xE0566A, 0xA24BB5};

	public final NumberSetting size = add(new NumberSetting("Size", "How big the guitar is.", 0.75, 0.5, 1.1, 0.05, "x"));
	/** Por defecto, hacia el mismo lado que en el diseño. */
	public final ModeSetting side = add(new ModeSetting("Neck side", "Which shoulder the neck sticks out over.", "Right", "Left", "Right"));
	public final BooleanSetting flowers = add(new BooleanSetting("Flowers",
			"Little voxel flowers in purple, white and red slowly falling and spinning around the guitar, like in its design.", true));

	private Vox.Shape guitar;
	private Vox.Shape[] flowerShapes;
	/** Datos de cada flor (los mismos números aleatorios que el diseño): r, y, v, w, ph, sz, eje x, y, z, rs. */
	private float[][] petals;

	public JulietGuitarCosmetic() {
		super("Juliet", "Music: a white guitar with a purple flower in relief, falling blood, green leaves and purple ribbons. "
				+ "Little voxel flowers fall around it.");
	}

	@Override
	public String song() {
		return SONG;
	}

	@Override
	public String songTitle() {
		return "Hello Juliet — Clarion";
	}

	@Override
	public float bpm() {
		return BPM;
	}

	@Override
	public float motion() {
		return 0.6F;
	}

	/** Juliet no suelta notas musicales: su ambiente son las flores del diseño. */
	@Override
	public boolean beatNotes() {
		return false;
	}

	private static float[][] flower() {
		float[][] list = new float[21][];
		for (int j = 0; j < 8; j++) {
			double a = j * Math.PI / 4.0;
			list[j] = new float[] {(float) (Math.cos(a) * 1.75), (float) (Math.sin(a) * 1.75), 0.0F, 0.85F};
		}
		for (int j = 0; j < 8; j++) {
			double a = j * Math.PI / 4.0 + Math.PI / 8.0;
			list[8 + j] = new float[] {(float) (Math.cos(a) * 0.95), (float) (Math.sin(a) * 0.95), 0.55F, 0.75F};
		}
		list[16] = new float[] {0.0F, 0.0F, 0.0F, 1.5F};
		list[17] = new float[] {0.0F, 0.0F, 0.95F, 0.9F};
		list[18] = new float[] {0.0F, 0.0F, 1.5F, 0.5F};
		list[19] = new float[] {0.0F, 0.0F, -0.7F, 1.1F};
		list[20] = new float[] {0.0F, 0.0F, -1.5F, 0.45F};
		return list;
	}

	/** Color de la pieza {@code j} de una flor de color {@code c}, como en el diseño. */
	static int flowerPieceColor(int c, int j) {
		if (j == 18) return 0x35206E;
		if (j == 19) return 0x3F7D2C;
		if (j == 20) return 0x28531C;
		float[] rgb = {(c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F};
		for (int i = 0; i < 3; i++) {
			if (j < 8) {
				if (j % 2 == 1) rgb[i] *= 0.86F;
			} else if (j < 16) {
				rgb[i] += (1.0F - rgb[i]) * 0.28F;
			} else if (j == 16) {
				rgb[i] *= 0.78F;
			} else {
				rgb[i] *= 0.62F;
			}
		}
		return Math.round(rgb[0] * 255.0F) << 16 | Math.round(rgb[1] * 255.0F) << 8 | Math.round(rgb[2] * 255.0F);
	}

	private void build() {
		guitar = new Vox.Shape(PALETTE);
		InputStream stream = JulietGuitarCosmetic.class.getResourceAsStream(MODEL);
		if (stream == null) {
			FreedomClient.LOGGER.warn("Missing guitar model {}", MODEL);
		} else {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.isBlank() || line.startsWith("#")) continue;
					String[] p = line.trim().split("\\s+");
					guitar.box(p[0].charAt(0), Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3]),
							Float.parseFloat(p[4]), Float.parseFloat(p[5]), Float.parseFloat(p[6]));
				}
			} catch (Exception e) {
				FreedomClient.LOGGER.warn("Could not read guitar model {}", MODEL, e);
			}
		}
		// Una paleta con los colores distintos de todas las flores (una franja por color) y una pieza por flor.
		java.util.List<Integer> colors = new java.util.ArrayList<>();
		char[][] chars = new char[FLOWER_COLORS.length][FLOWER.length];
		for (int i = 0; i < FLOWER_COLORS.length; i++) {
			for (int j = 0; j < FLOWER.length; j++) {
				int color = flowerPieceColor(FLOWER_COLORS[i], j);
				if (!colors.contains(color)) colors.add(color);
				chars[i][j] = (char) (0x100 + colors.indexOf(color));
			}
		}
		Object[] pairs = new Object[colors.size() * 2];
		for (int c = 0; c < colors.size(); c++) {
			pairs[c * 2] = (char) (0x100 + c);
			pairs[c * 2 + 1] = 0xFF000000 | colors.get(c);
		}
		Vox.Palette flowerPalette = new Vox.Palette("guitar_juliet_flowers", pairs);
		flowerShapes = new Vox.Shape[FLOWER_COLORS.length];
		for (int i = 0; i < FLOWER_COLORS.length; i++) {
			Vox.Shape shape = new Vox.Shape(flowerPalette);
			for (int j = 0; j < FLOWER.length; j++) {
				float[] l = FLOWER[j];
				shape.box(chars[i][j], l[0] - l[3] / 2.0F, l[1] - l[3] / 2.0F, l[2] - l[3] / 2.0F, l[3], l[3], l[3]);
			}
			flowerShapes[i] = shape;
		}
		// Los números aleatorios del diseño: rnd() de Park-Miller con semilla 7, en el mismo orden.
		long[] seed = {7L};
		java.util.function.DoubleSupplier rnd = () -> (seed[0] = seed[0] * 16807L % 2147483647L) / 2147483647.0;
		petals = new float[FLOWER_COLORS.length][];
		for (int i = 0; i < petals.length; i++) {
			double r = 16 + rnd.getAsDouble() * 16;
			double y = 70 - rnd.getAsDouble() * 140;
			double v = 0.012 + rnd.getAsDouble() * 0.009;
			double w = 0.0012 + rnd.getAsDouble() * 0.0012;
			double ph = rnd.getAsDouble() * 6.28;
			double sz = 0.36 + rnd.getAsDouble() * 0.16;
			Vector3f axis = new Vector3f((float) (rnd.getAsDouble() - 0.5), (float) (rnd.getAsDouble() - 0.5), (float) (rnd.getAsDouble() - 0.5)).normalize();
			double rs = 0.004 + rnd.getAsDouble() * 0.008;
			petals[i] = new float[] {(float) r, (float) y, (float) v, (float) w, (float) ph, (float) sz, axis.x, axis.y, axis.z, (float) rs};
		}
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		if (guitar == null) build();
		poseStack.pushPose();
		// Mide unos 101 vóxeles de alto: se escala para que quede del tamaño de las otras guitarras.
		float scale = size.getFloat() * 0.42F;
		// El diseño avanza un paso por fotograma a 60 por segundo: tres por tick.
		float tick = state.ageInTicks * 3.0F % 1_000_000.0F;
		boolean hands = inHands(state);
		float tilt = side.is("Right") ? TILT : -TILT;
		if (hands) {
			inHands(parent, poseStack, state, scale, scale, 2.0F);
			poseStack.scale(-1.0F, 1.0F, 1.0F);
		} else {
			parent.body.translateAndRotate(poseStack);
			// La tapa de atrás del cuerpo llega 2 vóxeles detrás de su plano central: así queda pegada a la espalda.
			poseStack.translate(0.0F, 6.5F / 16.0F, (2.1F + backClearance(state) + 2.0F * scale) / 16.0F);
			poseStack.scale(scale, scale, scale);
			// Flota arriba y abajo muy despacio, como en el diseño (y hacia abajo aquí, de ahí el signo).
			poseStack.translate(0.0F, -Mth.sin(tick * 0.012F) * 1.6F / 16.0F, 0.0F);
			// El espacio de los modelos de Minecraft es el del diseño en espejo: se da la vuelta en x para que la
			// guitarra (y el sentido en que giran las flores) quede igual que en el diseño y no al revés.
			poseStack.scale(-1.0F, 1.0F, 1.0F);
			poseStack.mulPose(Axis.ZP.rotation(tilt));
		}
		// La flor de la boca cae en el centro de la espalda.
		poseStack.translate(0.0F, -CENTER_Y / 16.0F, 0.0F);
		guitar.draw(poseStack, collector, light);
		if (flowers.get()) {
			poseStack.translate(0.0F, PIVOT_Y / 16.0F, 0.0F);
			// En el diseño las flores no se inclinan ni flotan con la guitarra.
			if (!hands) {
				poseStack.mulPose(Axis.ZP.rotation(-tilt));
				poseStack.translate(0.0F, Mth.sin(tick * 0.012F) * 1.6F / 16.0F, 0.0F);
			}
			drawFlowers(poseStack, collector, light, tick);
		}
		poseStack.popPose();
	}

	/**
	 * Flores del diseño: caen girando alrededor de la guitarra, cada una sobre su propio eje, y crecen y menguan en los
	 * extremos. Mismas fórmulas que el diseño; lo único distinto es la profundidad de la órbita, que se aplasta hacia
	 * fuera para que no atraviesen al jugador.
	 */
	private void drawFlowers(PoseStack poseStack, SubmitNodeCollector collector, int light, float tick) {
		poseStack.pushPose();
		// A partir de aquí, y hacia arriba como en el diseño.
		poseStack.scale(1.0F, -1.0F, 1.0F);
		Quaternionf spin = new Quaternionf();
		for (int i = 0; i < petals.length; i++) {
			float[] a = petals[i];
			// y baja a.v por fotograma y vuelve a 70 al pasar de -70.
			float y = 70.0F - ((70.0F - a[1] + a[2] * tick) % 140.0F);
			float f = Math.min(1.0F, Math.min((70.0F - y) / 22.0F, (y + 70.0F) / 22.0F));
			float ang = a[4] + tick * a[3];
			float rr = a[0] + Mth.sin(tick * 0.006F + a[4]) * 2.5F;
			float k = a[5] * f + 0.001F;
			poseStack.pushPose();
			poseStack.translate(Mth.cos(ang) * rr / 16.0F, y / 16.0F, (6.0F + Mth.sin(ang) * rr * 0.25F) / 16.0F);
			poseStack.mulPose(spin.identity().rotateAxis(a[4] + tick * a[9], a[6], a[7], a[8]));
			poseStack.scale(k, k, k);
			flowerShapes[i].draw(poseStack, collector, light);
			poseStack.popPose();
		}
		poseStack.popPose();
	}

	/**
	 * Fondo fijo de su tarjeta: un campo de flores rojas y blancas en un vacío blanco que se pierde a lo lejos, con una
	 * neblina que pasa despacio por el horizonte, la flor morada plantada en el medio y unos pétalos que lleva el
	 * viento. Cinco dibujos de textura y unos pocos píxeles por fotograma.
	 */
	@Override
	public boolean drawCardBackground(GuiGraphics g, int x, int y, int w, int h) {
		long time = System.currentTimeMillis() % 1_000_000L;
		drawCardLayer(g, CARD_FAR, x, y, w, h, 0, CARD_WIDTH, CARD_HEIGHT);
		// La neblina da una vuelta entera cada 51 segundos.
		drawCardLayer(g, CARD_HAZE, x, y, w, h, (int) (time / 160L % CARD_WIDTH), CARD_WIDTH, CARD_HEIGHT);
		int flowerX = x + w / 2 + 6 - FLOWER_WIDTH / 2;
		int flowerY = y + h - FLOWER_HEIGHT - 3;
		g.blit(RenderPipelines.GUI_TEXTURED, CARD_FLOWER, flowerX, flowerY, 0.0F, 0.0F, FLOWER_WIDTH, FLOWER_HEIGHT, FLOWER_WIDTH, FLOWER_HEIGHT,
				FLOWER_WIDTH, FLOWER_HEIGHT);
		drawCardLayer(g, CARD_NEAR, x, y, w, h, 0, CARD_WIDTH, CARD_HEIGHT);
		// Pétalos que cruzan la tarjeta con el viento, bajando poco a poco y meciéndose.
		int count = Math.max(4, w / 30);
		for (int i = 0; i < count; i++) {
			float period = 7000.0F + i * 1300.0F;
			float p = ((time + i * 2300L) % (long) period) / period;
			int px = x + Math.round(p * (w + 20)) - 10;
			int py = y + 6 + (i * 13) % Math.max(1, h - 20) + Math.round(p * 10.0F + Mth.sin(p * Mth.TWO_PI * 2.0F + i) * 3.0F);
			if (px < x || px >= x + w - 1 || py < y || py >= y + h - 1) continue;
			int color = PETAL_COLORS[i % PETAL_COLORS.length];
			float alpha = Mth.sin(p * Mth.PI);
			g.fill(px, py, px + 2, py + 1, ThemeManager.withAlpha(color, alpha));
			if ((time / 300L + i) % 2 == 0) g.fill(px + 1, py + 1, px + 2, py + 2, ThemeManager.withAlpha(color, alpha * 0.7F));
		}
		return true;
	}

	/**
	 * Borde propio: raíces blancas con espinas recorriendo los cuatro lados, una flor morada en la esquina de arriba a
	 * la derecha y una roja en la de abajo a la izquierda. Con el cosmético puesto, el marco se tiñe de morado.
	 */
	@Override
	public boolean drawCardBorder(GuiGraphics g, int x, int y, int w, int h, float on, float hover) {
		int frame = ThemeManager.mix(0xFFB9BAC2, 0xFF8A6FD8, 0.7F * on + 0.3F * hover);
		g.fill(x + 1, y, x + w - 1, y + 1, frame);
		g.fill(x + 1, y + h - 1, x + w - 1, y + h, frame);
		g.fill(x, y + 1, x + 1, y + h - 1, frame);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, frame);
		drawCardLayer(g, ROOT_TOP, x, y, w, ROOT, 0, CARD_WIDTH, ROOT);
		drawCardLayer(g, ROOT_BOTTOM, x, y + h - ROOT, w, ROOT, 0, CARD_WIDTH, ROOT);
		int side = Math.min(h, CARD_HEIGHT);
		g.blit(RenderPipelines.GUI_TEXTURED, ROOT_LEFT, x, y, 0.0F, 0.0F, ROOT, side, ROOT, side, ROOT, CARD_HEIGHT);
		g.blit(RenderPipelines.GUI_TEXTURED, ROOT_RIGHT, x + w - ROOT, y, 0.0F, 0.0F, ROOT, side, ROOT, side, ROOT, CARD_HEIGHT);
		g.blit(RenderPipelines.GUI_TEXTURED, CORNER_PURPLE, x + w - CORNER + 5, y - 5, 0.0F, 0.0F, CORNER, CORNER, CORNER, CORNER, CORNER, CORNER);
		g.blit(RenderPipelines.GUI_TEXTURED, CORNER_RED, x - 5, y + h - CORNER + 5, 0.0F, 0.0F, CORNER, CORNER, CORNER, CORNER, CORNER, CORNER);
		return true;
	}

	/** Su nombre en la tarjeta: el rótulo pixel "JULIET" en morado, sin la línea de descripción. */
	@Override
	public boolean drawCardTitle(GuiGraphics g, int x, int y, float on) {
		g.blit(RenderPipelines.GUI_TEXTURED, TITLE, x, y - 1, 0.0F, 0.0F, TITLE_WIDTH, TITLE_HEIGHT, TITLE_WIDTH, TITLE_HEIGHT, TITLE_WIDTH, TITLE_HEIGHT);
		return true;
	}

	@Override
	public boolean showCardDescription() {
		return false;
	}

	@Override
	public boolean lightCardBackground() {
		return true;
	}
}
