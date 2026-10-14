package com.freedomclient.cosmetic.vox;

import com.freedomclient.FreedomClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * El fantasmita 3D de FreedomPlayer (assets/freedomclient/vox/ghost.vox, de scripts/textures/ghost): cuerpo redondo
 * de vóxeles de medio píxel, bracitos que se mueven, el brillo de la cabeza y sus 8 caras, en rosa o en blanco. Lo usan
 * la mascota Ghostie y el aura de fantasmas. Coordenadas del diseño: píxeles, y hacia arriba, las colas en y = 0,
 * mirando a +z.
 */
public final class GhostModel {
	private static final String MODEL = "/assets/freedomclient/vox/ghost.vox";
	/** Pivote de los bracitos (hombros), como en el diseño. */
	private static final float ARM_X = 5.0F, ARM_Y = 6.5F;
	/** Papel de cada caja -> carácter de la paleta. */
	private static final String ROLES = "top body shade w k c t";

	private static final Vox.Palette PINK = new Vox.Palette("ghost_pink",
			'T', 0xFFF8BCD9, 'b', 0xFFF4A6CB, 's', 0xFFE58DB8, 'w', 0xFFFFE6F2, 'k', 0xFFC2326F, 'c', 0xFFFF6FAE, 't', 0xFFFF6FAE);
	private static final Vox.Palette WHITE = new Vox.Palette("ghost_white",
			'T', 0xFFFFFFFF, 'b', 0xFFF3EEF4, 's', 0xFFDCD2E4, 'w', 0xFFFFFFFF, 'k', 0xFF1A1420, 'c', 0xFFF4A6CB, 't', 0xFFFF6FAE);

	/** Piezas de un color: cuerpo, brillo, bracitos y una por cara. */
	private static final class Set {
		final Vox.Shape body, shine, armL, armR;
		final Vox.Shape[] faces = new Vox.Shape[8];

		Set(Vox.Palette palette) {
			body = new Vox.Shape(palette);
			shine = new Vox.Shape(palette);
			armL = new Vox.Shape(palette);
			armR = new Vox.Shape(palette);
			for (int i = 0; i < faces.length; i++) faces[i] = new Vox.Shape(palette);
		}
	}

	private static Set pink, white;

	private GhostModel() {
	}

	private static void build() {
		pink = new Set(PINK);
		white = new Set(WHITE);
		Map<String, Integer> faceIndex = new HashMap<>();
		for (int i = 0; i < com.freedomclient.ui.GhostArt.FACES.length; i++) faceIndex.put("face_" + com.freedomclient.ui.GhostArt.FACES[i], i);
		InputStream stream = GhostModel.class.getResourceAsStream(MODEL);
		if (stream == null) {
			FreedomClient.LOGGER.warn("Missing ghost model {}", MODEL);
			return;
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) continue;
				String[] p = line.trim().split("\\s+");
				char c = "Tbswkct".charAt(java.util.Arrays.asList(ROLES.split(" ")).indexOf(p[1]));
				float x = Float.parseFloat(p[2]), y = Float.parseFloat(p[3]), z = Float.parseFloat(p[4]);
				float w = Float.parseFloat(p[5]), h = Float.parseFloat(p[6]), d = Float.parseFloat(p[7]);
				for (Set set : new Set[] {pink, white}) {
					Vox.Shape shape = switch (p[0]) {
						case "body" -> set.body;
						case "shine" -> set.shine;
						// Los bracitos se guardan respecto a su hombro, para girarlos allí.
						case "armL" -> set.armL;
						case "armR" -> set.armR;
						default -> set.faces[faceIndex.get(p[0])];
					};
					float ox = p[0].equals("armL") ? -ARM_X : p[0].equals("armR") ? ARM_X : 0.0F;
					float oy = p[0].startsWith("arm") ? ARM_Y : 0.0F;
					shape.box(c, x - ox, y - oy, z, w, h, d);
				}
			}
		} catch (Exception e) {
			FreedomClient.LOGGER.warn("Could not read ghost model {}", MODEL, e);
		}
	}

	/**
	 * Dibuja el fantasma con las colas en el origen, mirando a +z: la cara {@code face} (índice de GhostArt.FACES), en
	 * blanco o en rosa, con los bracitos levantados {@code left} y {@code right} radianes (positivo hacia arriba).
	 */
	public static void draw(PoseStack poseStack, SubmitNodeCollector collector, int light, int face, boolean whiteBody, double left, double right) {
		if (pink == null) build();
		Set set = whiteBody ? white : pink;
		set.body.draw(poseStack, collector, light);
		set.shine.draw(poseStack, collector, light);
		set.faces[face].draw(poseStack, collector, light);
		arm(poseStack, collector, light, set.armL, -ARM_X, (float) -left);
		arm(poseStack, collector, light, set.armR, ARM_X, (float) right);
	}

	private static void arm(PoseStack poseStack, SubmitNodeCollector collector, int light, Vox.Shape shape, float x, float angle) {
		poseStack.pushPose();
		poseStack.translate(x / 16.0F, ARM_Y / 16.0F, 0.0F);
		poseStack.mulPose(Axis.ZP.rotation(angle));
		shape.draw(poseStack, collector, light);
		poseStack.popPose();
	}
}
