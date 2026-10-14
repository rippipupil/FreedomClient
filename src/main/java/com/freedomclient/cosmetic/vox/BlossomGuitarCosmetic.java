package com.freedomclient.cosmetic.vox;

import com.freedomclient.FreedomClient;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Blossom: una guitarra con la forma de una Jackson Kelly (pala puntiaguda, aletas de tiburón, dos humbuckers, Floyd
 * Rose y cuerpo de puntas) con la paleta Blossom de flor de cerezo: tapa rosa pétalo con filete blanco, rama de cerezo
 * con flores en relieve, diapasón ciruela y herrajes de oro rosa. Va a la espalda en diagonal y se toca con la tecla de
 * tocar (sin canción todavía: rasgueo con notas musicales).
 * <p>
 * El modelo sale del diseño (scripts/textures/blossom_guitar/design.js) con export.py, como Juliet.
 */
public class BlossomGuitarCosmetic extends MusicGuitarCosmetic {
	/** Colores del diseño (PAL_NAMES, en el mismo orden): a b p r c k K d i f g G w o s B n. */
	private static final Vox.Palette PALETTE = new Vox.Palette("guitar_blossom",
			'a', 0xFFFFF4F8, 'b', 0xFFFFD6E6, 'p', 0xFFF7A8C8, 'r', 0xFFE784AE, 'c', 0xFFC2326F, 'k', 0xFF5A3440, 'K', 0xFF7A4A58,
			'd', 0xFF3D2430, 'i', 0xFFFFE4F0, 'f', 0xFFE8C4B8, 'g', 0xFFE2A48C, 'G', 0xFFB9786A, 'w', 0xFFFFFFFF, 'o', 0xFFFF6FAE,
			's', 0xFFF5F0F5, 'B', 0xFFF2C9DA, 'n', 0xFFF6E6DC);
	private static final String MODEL = "/assets/freedomclient/vox/blossom.vox";
	/** Inclinación en la espalda, como Juliet (el mástil asoma por encima del hombro). */
	private static final float TILT = 0.38F;
	/** Centro de las pastillas en el .vox (y = 66 del diseño menos su base 93,5): queda en el centro de la espalda. */
	private static final float CENTER_Y = -27.5F;

	public final NumberSetting size = add(new NumberSetting("Size", "How big the guitar is.", 0.75, 0.5, 1.1, 0.05, "x"));
	public final ModeSetting side = add(new ModeSetting("Neck side", "Which shoulder the neck sticks out over.", "Right", "Left", "Right"));

	private Vox.Shape guitar;

	public BlossomGuitarCosmetic() {
		super("Blossom", "Music: a pointy Jackson-style guitar in cherry blossom pink with a raised sakura branch, "
				+ "plum fretboard with shark-fin inlays and rose-gold hardware.");
	}

	private void build() {
		guitar = new Vox.Shape(PALETTE);
		InputStream stream = BlossomGuitarCosmetic.class.getResourceAsStream(MODEL);
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
		// Mide unos 95 vóxeles de largo: a la escala de Juliet queda del tamaño de las otras guitarras.
		float scale = size.getFloat() * 0.42F;
		float tilt = side.is("Right") ? TILT : -TILT;
		if (inHands(state)) {
			inHands(parent, poseStack, state, scale, scale, 2.0F);
			poseStack.scale(-1.0F, 1.0F, 1.0F);
		} else {
			parent.body.translateAndRotate(poseStack);
			// La tapa de atrás llega 2 vóxeles detrás de su plano central: así queda pegada a la espalda.
			poseStack.translate(0.0F, 6.5F / 16.0F, (2.1F + backClearance(state) + 2.0F * scale) / 16.0F);
			poseStack.scale(scale, scale, scale);
			// El espacio de los modelos de Minecraft es el del diseño en espejo: se da la vuelta en x (como Juliet).
			poseStack.scale(-1.0F, 1.0F, 1.0F);
			poseStack.mulPose(Axis.ZP.rotation(tilt));
		}
		poseStack.translate(0.0F, -CENTER_Y / 16.0F, 0.0F);
		guitar.draw(poseStack, collector, light);
		poseStack.popPose();
	}
}
