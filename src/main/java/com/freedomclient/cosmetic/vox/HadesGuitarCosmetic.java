package com.freedomclient.cosmetic.vox;

import com.freedomclient.FreedomClient;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.theme.ThemeManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Hades (Música): la guitarra invernal diseñada en Claude Design, en blanco, grises y negro. Cuerpo con llamas en
 * franjas (blanco abajo, negro arriba) y golpeador blanco, lenguas de llama que se enroscan a los lados, ramas secas
 * en la pala y en el cuerno, y el mástil oscuro con trastes claros. Su canción es "hades in the dead of winter".
 * En el menú, su tarjeta tiene un fondo propio que no cambia con el tema: vegetación de noche con espíritus blancos,
 * grises y negros flotando.
 */
public class HadesGuitarCosmetic extends MusicGuitarCosmetic {
	/** Sonido de su canción (sounds.json). */
	public static final String SONG = "guitar.hades";
	/** Tempo de la canción (medido sobre el audio: 73,5 pulsos; el rasgueo va al doble). */
	private static final float BPM = 147.0F;

	private static final Vox.Palette PALETTE = new Vox.Palette("guitar_hades",
			'k', 0xFF111114, 'd', 0xFF3A3A42, 'g', 0xFF8A8A94, 'l', 0xFFC9C9CF, 'w', 0xFFF2F2F0, 's', 0xFFB8B8C0,
			'n', 0xFF24242A, 'f', 0xFF18181C);
	/** Cajas del diseño (scripts/textures/hades_guitar): "color x y z ancho alto fondo", 1 vóxel = 1 px del modelo. */
	private static final String MODEL = "/assets/freedomclient/vox/hades.vox";
	/** Capas del bosque muerto de su tarjeta (scripts/textures/hades_guitar/forest.py). */
	private static final Identifier FAR = FreedomClient.id("textures/cosmetic/hades_card_far.png");
	private static final Identifier FOG = FreedomClient.id("textures/cosmetic/hades_card_fog.png");
	private static final Identifier NEAR = FreedomClient.id("textures/cosmetic/hades_card_near.png");
	/** Rótulo "HADES" en letras pixel góticas (scripts/textures/hades_guitar/title.py). */
	private static final Identifier TITLE = FreedomClient.id("textures/cosmetic/hades_title.png");
	private static final int TITLE_WIDTH = 45;
	private static final int TITLE_HEIGHT = 11;
	/** Grosor respecto al diseño (en profundidad): el suyo, fina como en Claude Design. */
	private static final float THICKNESS = 1.0F;
	/** Copos de ceniza y nieve del diseño que suben en espiral alrededor de la guitarra. */
	private static final int SNOW = 26;
	private static final int CARD_WIDTH = 320;
	private static final int CARD_HEIGHT = 64;

	/** Rama seca de la esquina de arriba a la izquierda: corre por el borde y suelta ramitas hacia dentro. */
	private static final String[] BRANCH_TL = {
			"##############.###....",
			"####.##..#.##.....#...",
			"##.....#...........#..",
			"##......#.............",
			"#.#...................",
			"##....................",
			"#.#...................",
			"#.....................",
			"##....................",
			".#....................",
	};
	private static final String[] BRANCH_TR = mirrorX(BRANCH_TL);
	private static final String[] BRANCH_BL = mirrorY(BRANCH_TL);
	private static final String[] BRANCH_BR = mirrorX(BRANCH_BL);
	/** Espíritu sin cara: una gotita redonda con la cola deshilachada. */
	private static final String[] WISP = {".##.", "####", "####", "#.##"};

	public final NumberSetting size = add(new NumberSetting("Size", "How big the guitar is.", 0.75, 0.5, 1.1, 0.05, "x"));
	public final ModeSetting side = add(new ModeSetting("Neck side", "Which shoulder the neck sticks out over.", "Left", "Left", "Right"));
	public final BooleanSetting snow = add(new BooleanSetting("Snow", "Ash and snow flakes rising around the guitar, like in its design.", true));

	private Vox.Shape guitar;
	/** Un cubito por color de copo (gris, gris claro y blanco) y los datos de cada copo. */
	private final Vox.Shape[] flake = {
			new Vox.Shape(PALETTE).box('g', -0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
			new Vox.Shape(PALETTE).box('l', -0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
			new Vox.Shape(PALETTE).box('w', -0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F)};
	private float[][] flakes;

	public HadesGuitarCosmetic() {
		super("Hades", "Music: a winter guitar in white, greys and black, with curling flames, dry branches and its own song. "
				+ "Its card has a night garden full of spirits.");
	}

	@Override
	public String song() {
		return SONG;
	}

	@Override
	public String songTitle() {
		return "hades in the dead of winter — My Dead Girlfriend, toumobits";
	}

	@Override
	public float bpm() {
		return BPM;
	}

	private void build() {
		guitar = new Vox.Shape(PALETTE);
		InputStream stream = HadesGuitarCosmetic.class.getResourceAsStream(MODEL);
		if (stream == null) {
			FreedomClient.LOGGER.warn("Missing guitar model {}", MODEL);
			return;
		}
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

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		if (guitar == null) build();
		poseStack.pushPose();
		// El diseño mide unos 134 vóxeles de alto: se escala para que quede del tamaño de las otras guitarras.
		float scale = size.getFloat() * 0.32F;
		float thickness = scale * THICKNESS;
		if (inHands(state)) {
			inHands(parent, poseStack, state, scale, thickness, 5.0F);
		} else {
			parent.body.translateAndRotate(poseStack);
			// La parte de atrás del cuerpo llega 5 vóxeles detrás de su plano central: así queda pegada a la espalda.
			poseStack.translate(0.0F, 6.5F / 16.0F, (2.1F + backClearance(state) + 5.0F * thickness) / 16.0F);
			poseStack.scale(scale, scale, thickness);
			if (side.is("Left")) poseStack.scale(-1.0F, 1.0F, 1.0F);
			// Menos inclinada que las otras guitarras: el cuerpo queda en el centro de la espalda y el mástil asoma por
			// encima del hombro.
			poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-20.0F));
		}
		// El centro del cuerpo (x -0.4, 21.6 vóxeles por encima de la base) cae justo en el centro de la espalda.
		poseStack.translate(0.44F / 16.0F, 21.6F / 16.0F, 0.0F);
		guitar.draw(poseStack, collector, light);
		if (snow.get()) drawSnow(poseStack, collector, state.ageInTicks);
		poseStack.popPose();
	}

	/**
	 * Partículas del diseño: copos grises, gris claro y blancos que suben girando alrededor de la guitarra, más
	 * abiertos abajo y más cerrados arriba, y se apagan al llegar a los extremos. Van por delante de la espalda (la
	 * órbita se aplasta hacia fuera para no meterse en el jugador). Coordenadas del diseño: y hacia arriba con la
	 * base de la guitarra en y = -56.
	 */
	private void drawSnow(PoseStack poseStack, SubmitNodeCollector collector, float ageInTicks) {
		if (flakes == null) {
			RandomSource rnd = RandomSource.create(7L);
			flakes = new float[SNOW][];
			for (int i = 0; i < SNOW; i++) {
				float kind = rnd.nextFloat();
				flakes[i] = new float[] {20.0F + rnd.nextFloat() * 9.0F, (rnd.nextFloat() - 0.5F) * 140.0F, 0.028F + rnd.nextFloat() * 0.03F,
						0.0045F + rnd.nextFloat() * 0.003F, rnd.nextFloat() * Mth.TWO_PI, 0.7F + rnd.nextFloat() * 0.7F, kind < 0.3F ? 0 : kind < 0.5F ? 1 : 2};
			}
		}
		// El diseño avanza un paso por fotograma a 60 por segundo: tres por tick.
		float t = ageInTicks * 3.0F % 100_000.0F;
		for (float[] f : flakes) {
			float y = (f[1] + 70.0F + f[2] * t) % 140.0F - 70.0F;
			float fade = Math.min(1.0F, Math.min((70.0F - y) / 22.0F, (y + 70.0F) / 22.0F));
			float angle = f[4] + t * f[3];
			float radius = f[0] * (0.5F + 0.5F / (1.0F + (float) Math.exp((y + 8.0F) / 14.0F))) + Mth.sin(t * 0.006F + f[4]) * 1.5F;
			float size = (f[5] * fade * (0.85F + 0.15F * Mth.sin(t * 0.01F + f[4])) + 0.01F) * 1.6F;
			if (size <= 0.02F) continue;
			poseStack.pushPose();
			poseStack.translate(Mth.cos(angle) * radius / 16.0F, -(y + 56.0F) / 16.0F, (6.0F + Mth.sin(angle) * radius * 0.35F) / 16.0F);
			poseStack.scale(size, size, size);
			flake[(int) f[6]].drawGlow(poseStack, collector);
			poseStack.popPose();
		}
	}

	/** Hades no suelta notas musicales: su ambiente son los copos y los espíritus. */
	@Override
	public boolean beatNotes() {
		return false;
	}

	/**
	 * Fondo fijo de su tarjeta: un bosque muerto en tres capas (árboles lejanos, niebla que pasa despacio y árboles
	 * cercanos) y unas lucecitas espirituales que suben. Son cuatro dibujos de textura y unos pocos píxeles por
	 * fotograma, sin nada calculado píxel a píxel.
	 */
	@Override
	public boolean drawCardBackground(GuiGraphics g, int x, int y, int w, int h) {
		long time = System.currentTimeMillis() % 1_000_000L;
		layer(g, FAR, x, y, w, h, 0);
		// La niebla da una vuelta entera cada 40 segundos.
		layer(g, FOG, x, y, w, h, (int) (time / 125L % CARD_WIDTH));
		layer(g, NEAR, x, y, w, h, 0);
		// Lucecitas que suben despacio entre los árboles y se apagan arriba.
		int count = Math.max(3, w / 40);
		for (int i = 0; i < count; i++) {
			float period = 5200.0F + i * 900.0F;
			float p = ((time + i * 1700L) % (long) period) / period;
			int lx = x + 6 + (i * 53 + (int) (Mth.sin(time / 900.0F + i) * 3.0F)) % Math.max(1, w - 12);
			int ly = y + h - 6 - Math.round(p * (h - 12));
			float alpha = Mth.sin(p * Mth.PI) * 0.8F;
			g.fill(lx, ly, lx + 1, ly + 1, ThemeManager.withAlpha(i % 2 == 0 ? 0xFFF2F2F0 : 0xFFC9C9CF, alpha));
			g.fill(lx - 1, ly, lx + 2, ly + 1, ThemeManager.withAlpha(0xFFF2F2F0, alpha * 0.25F));
		}
		return true;
	}

	/** Su nombre en la tarjeta: el rótulo pixel "HADES" en vez del texto, y sin la línea de descripción. */
	@Override
	public boolean drawCardTitle(GuiGraphics g, int x, int y, float on) {
		g.blit(RenderPipelines.GUI_TEXTURED, TITLE, x, y - 1, 0.0F, 0.0F, TITLE_WIDTH, TITLE_HEIGHT, TITLE_WIDTH, TITLE_HEIGHT, TITLE_WIDTH, TITLE_HEIGHT);
		return true;
	}

	@Override
	public boolean showCardDescription() {
		return false;
	}

	/** Una capa del fondo pegada abajo; {@code scroll} la desplaza (las capas se repiten en bucle a lo ancho). */
	private static void layer(GuiGraphics g, Identifier texture, int x, int y, int w, int h, int scroll) {
		int drawH = Math.min(h, CARD_HEIGHT);
		int v = CARD_HEIGHT - drawH;
		int top = y + h - drawH;
		int done = 0;
		int u = scroll;
		while (done < w) {
			int part = Math.min(CARD_WIDTH - u, w - done);
			g.blit(RenderPipelines.GUI_TEXTURED, texture, x + done, top, u, v, part, drawH, part, drawH, CARD_WIDTH, CARD_HEIGHT);
			done += part;
			u = 0;
		}
	}

	/**
	 * Borde propio de su tarjeta: un marco oscuro con ramas secas que crecen desde las cuatro esquinas, y espíritus
	 * sin cara que dan vueltas por el borde dejando una estela. Unos 60 rectángulos por tarjeta.
	 */
	@Override
	public boolean drawCardBorder(GuiGraphics g, int x, int y, int w, int h, float on, float hover) {
		int frame = ThemeManager.mix(0xFF4A4A54, 0xFF9A9AA4, 0.5F * on + 0.3F * hover);
		g.fill(x + 1, y, x + w - 1, y + 1, frame);
		g.fill(x + 1, y + h - 1, x + w - 1, y + h, frame);
		g.fill(x, y + 1, x + 1, y + h - 1, frame);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, frame);
		// Ramas en las esquinas (las mismas, en espejo).
		// En gris claro para que se vean sobre el bosque oscuro (más claras con el cosmético puesto).
		int branch = ThemeManager.mix(0xFF7A7A84, 0xFFC9C9CF, 0.6F * on + 0.2F * hover);
		Draw.art(g, BRANCH_TL, x, y, branch);
		Draw.art(g, BRANCH_TR, x + w - BRANCH_TL[0].length(), y, branch);
		Draw.art(g, BRANCH_BL, x, y + h - BRANCH_TL.length, branch);
		Draw.art(g, BRANCH_BR, x + w - BRANCH_TL[0].length(), y + h - BRANCH_TL.length, branch);
		// Espíritus sin cara recorriendo el borde (dos en un sentido y uno en el otro), con su estela.
		long time = System.currentTimeMillis() % 10_000_000L;
		int perimeter = 2 * (w + h);
		float strength = 0.55F + 0.45F * Math.max(on, hover);
		int[] colors = {0xFFF2F2F0, 0xFF8A8A94, 0xFF0B0B0E};
		for (int i = 0; i < 3; i++) {
			int direction = i == 1 ? -1 : 1;
			int s = Math.floorMod((int) (time / (34L + i * 9L)) * direction + i * perimeter / 3, perimeter);
			int color = colors[i];
			for (int k = 3; k >= 1; k--) {
				int[] trail = perimeterPoint(Math.floorMod(s - direction * k * 3, perimeter), x, y, w, h);
				g.fill(trail[0], trail[1], trail[0] + 1, trail[1] + 1, ThemeManager.withAlpha(i == 2 ? 0xFF8A8A94 : color, strength * 0.5F / k));
			}
			int[] p = perimeterPoint(s, x, y, w, h);
			int bob = (int) (time / 220L + i) % 2;
			if (i == 2) Draw.art(g, WISP, p[0] - 2, p[1] - 3 + bob, ThemeManager.withAlpha(0xFF8A8A94, strength));
			Draw.art(g, WISP, p[0] - 1, p[1] - 2 + bob, ThemeManager.withAlpha(color, strength));
		}
		return true;
	}

	/** Punto del borde a {@code s} píxeles de la esquina de arriba a la izquierda, en el sentido de las agujas del reloj. */
	private static int[] perimeterPoint(int s, int x, int y, int w, int h) {
		if (s < w) return new int[] {x + s, y};
		s -= w;
		if (s < h) return new int[] {x + w - 1, y + s};
		s -= h;
		if (s < w) return new int[] {x + w - 1 - s, y + h - 1};
		s -= w;
		return new int[] {x, y + h - 1 - Math.min(s, h - 1)};
	}

	private static String[] mirrorX(String[] rows) {
		String[] out = new String[rows.length];
		for (int i = 0; i < rows.length; i++) out[i] = new StringBuilder(rows[i]).reverse().toString();
		return out;
	}

	private static String[] mirrorY(String[] rows) {
		String[] out = new String[rows.length];
		for (int i = 0; i < rows.length; i++) out[i] = rows[rows.length - 1 - i];
		return out;
	}
}
