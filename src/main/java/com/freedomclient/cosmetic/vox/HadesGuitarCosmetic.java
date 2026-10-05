package com.freedomclient.cosmetic.vox;

import com.freedomclient.FreedomClient;
import com.freedomclient.cosmetic.CosmeticSlot;
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
public class HadesGuitarCosmetic extends VoxCosmetic {
	/** Sonido de su canción (sounds.json), para el sistema de música de las guitarras. */
	public static final String SONG = "guitar.hades";

	private static final Vox.Palette PALETTE = new Vox.Palette("guitar_hades",
			'k', 0xFF111114, 'd', 0xFF3A3A42, 'g', 0xFF8A8A94, 'l', 0xFFC9C9CF, 'w', 0xFFF2F2F0, 's', 0xFFB8B8C0,
			'n', 0xFF24242A, 'f', 0xFF18181C);
	/** Cajas del diseño (scripts/textures/hades_guitar): "color x y z ancho alto fondo", 1 vóxel = 1 px del modelo. */
	private static final String MODEL = "/assets/freedomclient/vox/hades.vox";
	private static final Identifier CARD = FreedomClient.id("textures/cosmetic/hades_card.png");
	private static final int CARD_WIDTH = 320;
	private static final int CARD_HEIGHT = 64;

	/** Espíritu: cabeza redonda y la cola ondulada (dos fotogramas). */
	private static final String[] SPIRIT_A = {".###.", "#####", "#####", "#####", "#.#.#"};
	private static final String[] SPIRIT_B = {".###.", "#####", "#####", "#####", ".#.#."};
	private static final String[] SPIRIT_SMALL = {".#.", "###", "#.#"};

	public final NumberSetting size = add(new NumberSetting("Size", "How big the guitar is.", 0.75, 0.5, 1.1, 0.05, "x"));
	public final ModeSetting side = add(new ModeSetting("Neck side", "Which shoulder the neck sticks out over.", "Left", "Left", "Right"));
	public final BooleanSetting notes = add(new BooleanSetting("Music notes", "Little music notes float out of the guitar now and then.", true));

	private final RandomSource random = RandomSource.create();
	private Vox.Shape guitar;

	public HadesGuitarCosmetic() {
		super("Hades", "Music: a winter guitar in white, greys and black, with curling flames, dry branches and its own song. "
				+ "Its card has a night garden full of spirits.", CosmeticSlot.BACK);
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
		// El diseño mide unos 134 vóxeles de alto: se escala para que quede del tamaño de las otras guitarras. La parte
		// de atrás del cuerpo llega 5 vóxeles detrás de su plano central.
		float scale = size.getFloat() * 0.22F;
		onBackDiagonal(parent, poseStack, state, scale, side.is("Left"), 5.0F);
		// El cuerpo (centrado unos 24 vóxeles por encima de su base) queda en el centro de la espalda.
		poseStack.translate(0.0F, 24.0F / 16.0F, 0.0F);
		guitar.draw(poseStack, collector, light);
		poseStack.popPose();
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
			int sy = y + 6 + (i * 13) % Math.max(1, h - 22) + Math.round(Mth.sin(time / 600.0F + i * 1.7F) * 3.0F);
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
