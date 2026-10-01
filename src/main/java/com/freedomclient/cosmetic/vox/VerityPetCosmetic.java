package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.PetBehavior;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;

/**
 * Verity en su forma emoji: una bolita amarilla que flota a tu lado y cambia de cara cada pocos segundos (sonrisa,
 * contenta, guiño, enamorada, sorprendida). Duerme con los ojos cerrados, llora escondida con poca vida, se ríe al
 * celebrar una kill y vuela con gafas de sol cuando vas con élitros.
 */
public class VerityPetCosmetic extends FollowPetCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("verity_pet",
			'y', 0xFFFFD447, 'Y', 0xFFF0B020, 'l', 0xFFFFEA8A, 'k', 0xFF5A3A1A, 'r', 0xFFE8344A, 'w', 0xFFFFFFFF,
			'b', 0xFF5BC8FF, 'K', 0xFF161218);
	private static final String[][] FACES = {
			// 0 sonrisa
			{".......", ".k...k.", ".k...k.", ".......", "k.....k", ".kkkkk."},
			// 1 contenta ^ ^ con la boca abierta
			{".......", ".k...k.", "k.k.k.k", ".......", ".kkkkk.", "..krk.."},
			// 2 guiño
			{".......", ".k.....", ".k..kkk", ".......", "k.....k", ".kkkkk."},
			// 3 enamorada: ojos de corazón
			{"r.r.r.r", "rrr.rrr", ".r...r.", ".......", ".kkkkk.", "..kkk.."},
			// 4 sorprendida
			{".......", ".k...k.", ".k...k.", ".......", "..kkk..", "..k.k..", "..kkk.."},
			// 5 dormida
			{".......", ".......", "kk...kk", ".......", "..kk...", "......."},
			// 6 gafas de sol (volando)
			{"KKKKKKK", "KKK.KKK", ".K...K.", ".......", "k.....k", ".kkkkk."},
			// 7 riéndose XD (celebrando)
			{"k.k.k.k", ".k...k.", "k.k.k.k", ".......", "kkkkkkk", ".krrrk.", "..kkk.."},
			// 8 llorando (escondida)
			{".......", "kk...kk", ".b...b.", ".b...b.", "..kkk..", ".k...k."},
	};
	private static final int SMILE = 0;
	private static final int HAPPY = 1;
	private static final int LOVE = 3;
	private static final int SLEEPY = 5;
	private static final int COOL = 6;
	private static final int LAUGH = 7;
	private static final int CRY = 8;

	private Vox.Shape ball;
	private final Vox.Shape[] faces = new Vox.Shape[FACES.length];

	public VerityPetCosmetic() {
		super("Verity", "Verity in her emoji form: a little yellow face that floats next to you and keeps changing expression. "
				+ "Sleeps, cries when you are low, laughs on kills and wears sunglasses when you fly.", "Right");
	}

	private void build() {
		// Bolita: amarilla con brillo arriba y sombra abajo.
		ball = new Vox.Shape(PALETTE).sphere(0.0F, -4.5F, 0.0F, 4.5F, dy -> dy <= -4 ? 'l' : dy >= 3 ? 'Y' : 'y');
		for (int i = 0; i < FACES.length; i++) {
			faces[i] = new Vox.Shape(PALETTE).art(FACES[i], -3.5F, -7.6F, -4.75F, 2.6F);
		}
	}

	/** Cara que toca ahora: la del estado de ánimo o, tranquila, una distinta cada 3 segundos. */
	private static int face(float time, PetBehavior.Mood mood) {
		if (PetBehavior.flying()) return COOL;
		switch (mood) {
			case SLEEP -> {
				return SLEEPY;
			}
			case HIDE -> {
				return CRY;
			}
			case CELEBRATE -> {
				return LAUGH;
			}
			case WAVE -> {
				return HAPPY;
			}
			default -> {
			}
		}
		switch (PetBehavior.emotion()) {
			case HEARTS -> {
				return LOVE;
			}
			case HAPPY -> {
				return HAPPY;
			}
			case BLUSH -> {
				return SMILE;
			}
			default -> {
			}
		}
		int[] idle = {SMILE, HAPPY, 2, SMILE, LOVE, 4};
		return idle[Math.floorMod((int) (time / 60.0F), idle.length)];
	}

	@Override
	protected void renderPet(PoseStack poseStack, SubmitNodeCollector collector, int light, float time, PetBehavior.Mood mood, float moodSeconds) {
		if (ball == null) build();
		int face = face(time, mood);
		float bob = Mth.sin(time * 0.12F) * 0.9F;
		float tilt = Mth.sin(time * 0.07F) * 8.0F;
		float spin = 0.0F;
		float squash = 0.0F;
		switch (face) {
			case LAUGH -> {
				// Se ríe dando botes y temblando.
				bob -= Math.abs(Mth.sin(moodSeconds * 10.0F)) * 2.0F;
				tilt = Mth.sin(time * 1.4F) * 10.0F;
			}
			case CRY -> tilt = Mth.sin(time * 3.0F) * 4.0F;
			case SLEEPY -> {
				tilt = 14.0F;
				squash = 0.06F + Mth.sin(time * 0.08F) * 0.04F;
			}
			case COOL -> {
				// Volando: inclinada hacia delante y girando un poco, muy chula.
				tilt = Mth.sin(time * 0.3F) * 12.0F;
				poseStack.translate(0.0F, -4.5F / 16.0F, 0.0F);
				poseStack.mulPose(Axis.XP.rotationDegrees(35.0F));
				poseStack.translate(0.0F, 4.5F / 16.0F, 0.0F);
			}
			case 4 -> squash = -0.08F * Mth.sin((time % 60.0F) / 60.0F * Mth.PI);
			default -> {
			}
		}
		// Cambio de cara con un pequeño giro, como un emoji que se da la vuelta.
		float sinceChange = time % 60.0F;
		if (mood == PetBehavior.Mood.IDLE && !PetBehavior.flying() && sinceChange < 6.0F) spin = (1.0F - sinceChange / 6.0F) * 360.0F;

		poseStack.translate(0.0F, bob / 16.0F, 0.0F);
		poseStack.translate(0.0F, -4.5F / 16.0F, 0.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(spin));
		poseStack.mulPose(Axis.ZP.rotationDegrees(tilt));
		poseStack.scale(1.0F + squash, 1.0F - squash, 1.0F + squash);
		poseStack.translate(0.0F, 4.5F / 16.0F, 0.0F);
		ball.draw(poseStack, collector, light);
		faces[face].draw(poseStack, collector, light);
		PetEmotes.blush(poseStack, collector, light, 2.8F, -3.4F, -4.6F);
	}
}
