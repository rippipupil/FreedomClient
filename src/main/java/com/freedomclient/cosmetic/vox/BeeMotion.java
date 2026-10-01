package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.PetBehavior;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;

/**
 * Vuelo de las abejas de Bee Swarm: flotan, se inclinan un poco y cada pocos segundos hacen una pirueta limpia
 * (un giro completo o una voltereta), con curvas suaves de entrada y salida. Celebrando dan volteretas seguidas.
 */
final class BeeMotion {
	private static final int CYCLE = 150;
	private static final int TRICK = 26;

	private BeeMotion() {
	}

	private static float ease(float t) {
		return t * t * (3.0F - 2.0F * t);
	}

	/** Aplica el vuelo y la pirueta al modelo, con el centro de la abeja en (0, -4, 0). */
	static void apply(PoseStack poseStack, float time, PetBehavior.Mood mood, float moodSeconds) {
		if (PetBehavior.flying()) {
			// Volando con élitros: se lanza en picado hacia delante, como un cohete con alas, con un balanceo rápido.
			poseStack.translate(0.0F, -4.0F / 16.0F, 0.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(55.0F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.6F) * 10.0F));
			poseStack.translate(0.0F, 4.0F / 16.0F, 0.0F);
			return;
		}
		boolean sleeping = mood == PetBehavior.Mood.SLEEP;
		float bob = sleeping ? Mth.sin(time * 0.05F) * 0.5F : Mth.sin(time * 0.15F) * 1.2F;
		poseStack.translate(0.0F, bob / 16.0F, 0.0F);
		poseStack.translate(0.0F, -4.0F / 16.0F, 0.0F);
		if (!sleeping) {
			poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.07F) * 6.0F));
			poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(time * 0.05F) * 5.0F));
		} else {
			poseStack.mulPose(Axis.XP.rotationDegrees(12.0F));
		}
		int cycle = (int) (time / CYCLE);
		float inCycle = time - cycle * CYCLE;
		if (mood == PetBehavior.Mood.CELEBRATE) {
			poseStack.mulPose(Axis.XP.rotationDegrees((moodSeconds * 540.0F) % 360.0F));
		} else if (mood == PetBehavior.Mood.WAVE) {
			poseStack.mulPose(Axis.YP.rotationDegrees((moodSeconds * 360.0F) % 360.0F));
		} else if (!sleeping && inCycle < TRICK) {
			float angle = ease(inCycle / TRICK) * 360.0F;
			poseStack.mulPose(cycle % 2 == 0 ? Axis.YP.rotationDegrees(angle) : Axis.XP.rotationDegrees(-angle));
		}
		poseStack.translate(0.0F, 4.0F / 16.0F, 0.0F);
	}

	/** Ángulo del aleteo en grados (rápido; lento si duerme). */
	static float flap(float time, PetBehavior.Mood mood) {
		if (PetBehavior.flying()) return Mth.sin(time * 3.4F) * 36.0F;
		return mood == PetBehavior.Mood.SLEEP ? Mth.sin(time * 0.2F) * 6.0F : Mth.sin(time * 1.6F) * 28.0F;
	}
}
