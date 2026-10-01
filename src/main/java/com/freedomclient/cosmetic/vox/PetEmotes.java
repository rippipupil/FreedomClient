package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.PetBehavior;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;

/**
 * Emociones de las mascotas, iguales para todas: saltito de alegría, mejillas sonrojadas y corazoncitos que suben
 * por encima de la cabeza. Las coordenadas van en píxeles del modelo de cada mascota (y hacia abajo, cara a -z).
 */
public final class PetEmotes {
	private static final Vox.Palette PALETTE = new Vox.Palette("pet_emotes",
			'r', 0xFFFF4F7A, 'R', 0xFFD8325A, 'w', 0xFFFFD0DC, 'b', 0xC8FF7A9A, 's', 0x90FFFFFF);
	private static final String[] HEART = {
			".rr.rr.",
			"rwrrrrr",
			"rrrrrrr",
			".rrrrR.",
			"..rrR..",
			"...R...",
	};
	private static Vox.Shape heart;
	private static Vox.Shape cheek;
	private static Vox.Shape streak;

	private PetEmotes() {
	}

	private static void build() {
		if (heart != null) return;
		heart = new Vox.Shape(PALETTE).art(HEART, -3.5F, -6.0F, -0.5F, 1.0F);
		cheek = new Vox.Shape(PALETTE).box('b', -0.9F, -0.4F, -0.2F, 1.8F, 0.8F, 0.3F);
		streak = new Vox.Shape(PALETTE).box('s', -0.25F, -0.25F, 0.0F, 0.5F, 0.5F, 7.0F);
	}

	/** Cuánto sube la mascota por el saltito de alegría (en píxeles, positivo hacia arriba). */
	public static float hop() {
		if (PetBehavior.emotion() != PetBehavior.Emotion.HAPPY) return 0.0F;
		float t = PetBehavior.emotionSeconds();
		if (t > 1.6F) return 0.0F;
		return Math.abs(Mth.sin(t * Mth.PI * 2.5F)) * 2.2F * (1.0F - t / 1.6F * 0.4F);
	}

	/** Si toca dibujar las mejillas sonrojadas. */
	public static boolean blushing() {
		PetBehavior.Emotion emotion = PetBehavior.emotion();
		return emotion == PetBehavior.Emotion.BLUSH || emotion == PetBehavior.Emotion.HEARTS;
	}

	/**
	 * Mejillas rosas a los dos lados de la cara: centradas en ({@code ±spacing}, {@code y}) sobre la cara en
	 * {@code z}. Se llama dentro de la transformación de la cabeza de la mascota.
	 */
	public static void blush(PoseStack poseStack, SubmitNodeCollector collector, int light, float spacing, float y, float z) {
		if (!blushing()) return;
		build();
		for (int side = -1; side <= 1; side += 2) {
			poseStack.pushPose();
			poseStack.translate(side * spacing / 16.0F, y / 16.0F, z / 16.0F);
			cheek.drawTranslucent(poseStack, collector, light);
			poseStack.popPose();
		}
	}

	/**
	 * Líneas de velocidad detrás de la mascota (hacia +z) que parpadean, para cuando vuela contigo con élitros.
	 * {@code height} es la altura del centro de la mascota.
	 */
	public static void speedLines(PoseStack poseStack, SubmitNodeCollector collector, int light, float time, float height) {
		build();
		for (int i = 0; i < 5; i++) {
			float phase = (time * 0.35F + i * 0.37F) % 1.0F;
			float x = Mth.sin(i * 2.4F) * 4.0F;
			float y = height + Mth.cos(i * 1.7F) * 3.5F;
			poseStack.pushPose();
			poseStack.translate(x / 16.0F, y / 16.0F, (4.0F + phase * 8.0F) / 16.0F);
			poseStack.scale(1.0F, 1.0F, 1.0F - phase * 0.7F);
			streak.drawTranslucent(poseStack, collector, light);
			poseStack.popPose();
		}
	}

	/**
	 * Corazoncitos que salen de ({@code 0}, {@code top}) y suben haciendo eses mientras crecen y se encogen. Brillan
	 * aunque sea de noche.
	 */
	public static void hearts(PoseStack poseStack, SubmitNodeCollector collector, float top) {
		if (PetBehavior.emotion() != PetBehavior.Emotion.HEARTS) return;
		build();
		float t = PetBehavior.emotionSeconds();
		for (int i = 0; i < 3; i++) {
			float life = (t - i * 0.75F) / 1.7F;
			if (life < 0.0F || life > 1.0F) continue;
			float scale = 0.25F + 0.55F * Mth.sin(life * Mth.PI);
			poseStack.pushPose();
			poseStack.translate((Mth.sin(life * 6.0F + i * 2.0F) * 2.2F + (i - 1) * 1.5F) / 16.0F, (top - life * 9.0F) / 16.0F, 0.0F);
			poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(life * 5.0F + i) * 12.0F));
			poseStack.scale(scale, scale, scale);
			heart.drawGlow(poseStack, collector);
			poseStack.popPose();
		}
	}
}
