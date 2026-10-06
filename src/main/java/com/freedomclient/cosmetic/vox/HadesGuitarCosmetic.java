package com.freedomclient.cosmetic.vox;

import com.freedomclient.FreedomClient;
import com.freedomclient.particle.GlowParticle;
import com.freedomclient.particle.PixelParticles;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.theme.ThemeManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
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
	private static final Identifier CARD = FreedomClient.id("textures/cosmetic/hades_card.png");
	/** Grosor respecto al diseño (en profundidad): el suyo, fina como en Claude Design. */
	private static final float THICKNESS = 1.0F;
	/** Copos de ceniza y nieve del diseño que suben en espiral alrededor de la guitarra. */
	private static final int SNOW = 26;
	private static final int CARD_WIDTH = 320;
	private static final int CARD_HEIGHT = 64;

	/** Espíritu: cabeza redonda y la cola ondulada (dos fotogramas). */
	private static final String[] SPIRIT_A = {".###.", "#####", "#####", "#####", "#.#.#"};
	private static final String[] SPIRIT_B = {".###.", "#####", "#####", "#####", ".#.#."};
	private static final String[] SPIRIT_SMALL = {".#.", "###", "#.#"};

	public final NumberSetting size = add(new NumberSetting("Size", "How big the guitar is.", 0.75, 0.5, 1.1, 0.05, "x"));
	public final ModeSetting side = add(new ModeSetting("Neck side", "Which shoulder the neck sticks out over.", "Left", "Left", "Right"));
	public final BooleanSetting notes = add(new BooleanSetting("Music notes", "Little music notes float out of the guitar now and then.", true));
	public final BooleanSetting snow = add(new BooleanSetting("Snow", "Ash and snow flakes rising around the guitar, like in its design.", true));

	private final RandomSource random = RandomSource.create();
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

	/** Notas musicales que salen de la guitarra y suben despacio (solo en tercera persona). */
	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (!notes.get() || client.level == null || player == null || player.isInvisible() || client.isPaused()) return;
		if (client.options.getCameraType().isFirstPerson() || random.nextFloat() > 0.06F) return;
		float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
		double backX = Mth.sin(yaw);
		double backZ = -Mth.cos(yaw);
		double rightX = -Mth.cos(yaw);
		double rightZ = -Mth.sin(yaw);
		double lateral = (random.nextDouble() - 0.5) * 0.5;
		double x = player.getX() + rightX * lateral + backX * 0.4;
		double y = player.getY() + 0.9 + random.nextDouble() * 0.5;
		double z = player.getZ() + rightZ * lateral + backZ * 0.4;
		GlowParticle note = new GlowParticle(client.level, x, y, z, (random.nextDouble() - 0.5) * 0.01, 0.025, (random.nextDouble() - 0.5) * 0.01,
				PixelParticles.sprite("note"), 0.07F, 0.09F, 30 + random.nextInt(15));
		client.particleEngine.add(note.thirdPersonOnly());
	}

	/**
	 * Fondo fijo de su tarjeta: el jardín de noche (abajo, que es donde está la vegetación) y espíritus blancos,
	 * grises y negros que cruzan flotando despacio.
	 */
	@Override
	public boolean drawCardBackground(GuiGraphics g, int x, int y, int w, int h) {
		int v = Math.max(0, CARD_HEIGHT - h);
		int drawH = Math.min(h, CARD_HEIGHT);
		for (int dx = 0; dx < w; dx += CARD_WIDTH) {
			int part = Math.min(CARD_WIDTH, w - dx);
			g.blit(RenderPipelines.GUI_TEXTURED, CARD, x + dx, y + h - drawH, 0.0F, v, part, drawH, part, drawH, CARD_WIDTH, CARD_HEIGHT);
		}
		long time = System.currentTimeMillis() % 1_000_000L;
		int count = Math.max(3, w / 32);
		for (int i = 0; i < count; i++) {
			// Cada espíritu cruza la tarjeta a su ritmo y sube y baja meciéndose.
			float speed = 0.004F + (i % 3) * 0.0025F;
			float drift = (time * speed + i * 97.0F) % (w + 12.0F) - 6.0F;
			int sx = x + Math.round(i % 2 == 0 ? drift : w - drift);
			// Flotan en la franja libre entre la descripción y el estado, para no tapar el texto.
			int band = Math.max(1, h - 40);
			int sy = y + 27 + (i * 5) % band + Math.round(Mth.sin(time / 600.0F + i * 1.7F) * 2.0F);
			boolean small = i % 4 == 3;
			String[] art = small ? SPIRIT_SMALL : (time / 300 + i) % 2 == 0 ? SPIRIT_A : SPIRIT_B;
			int kind = i % 3;
			int body = kind == 0 ? 0xFFF2F2F0 : kind == 1 ? 0xFF8A8A94 : 0xFF0B0B0E;
			if (sx < x - 3 || sx > x + w - 3) continue;
			if (kind == 2) {
				// Los negros llevan un contorno gris para verse sobre la noche.
				int rim = ThemeManager.withAlpha(0xFF8A8A94, 0.8F);
				Draw.art(g, art, sx - 1, sy, rim);
				Draw.art(g, art, sx + 1, sy, rim);
				Draw.art(g, art, sx, sy - 1, rim);
			} else {
				// Halo suave.
				Draw.art(g, art, sx, sy + 1, ThemeManager.withAlpha(body, 0.25F));
			}
			Draw.art(g, art, sx, sy, body);
			if (!small) {
				// Ojos: oscuros en los claros, blancos en los negros.
				int eye = kind == 2 ? 0xFFF2F2F0 : 0xFF111114;
				g.fill(sx + 1, sy + 2, sx + 2, sy + 3, eye);
				g.fill(sx + 3, sy + 2, sx + 4, sy + 3, eye);
				// Estela de motitas detrás.
				int trail = ThemeManager.withAlpha(body | 0xFF000000, 0.35F);
				int back = i % 2 == 0 ? -2 : 6;
				g.fill(sx + back, sy + 3, sx + back + 1, sy + 4, trail);
			}
		}
		return true;
	}
}
