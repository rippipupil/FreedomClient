package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.CosmeticPreview;
import com.freedomclient.cosmetic.CosmeticSlot;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Aura de fantasmas: 6 fantasmitas pequeños, blancos y rosas, que giran despacio a tu alrededor a distintas alturas.
 * Cada uno aparece, sube un poco dando vueltas y se desvanece, y vuelve con otra cara y otro color, entre destellos ✦
 * que suben. Las cuentas son las del diseño ({@link GhostMotion#aura}, {@link GhostMotion#sparkle}); la vuelta va fija al
 * mundo.
 */
public class GhostAuraCosmetic extends VoxCosmetic {
	private static final Vox.Palette SPARK_PINK = new Vox.Palette("ghost_spark_pink", 'a', 0xFFFF8CC6, 'b', 0xFFFFFFFF);
	private static final Vox.Palette SPARK_WHITE = new Vox.Palette("ghost_spark_white", 'a', 0xFFFFFFFF, 'b', 0xFFFFFFFF);
	private static Vox.Shape sparkPink, sparkWhite;
	private final double[] ghost = new double[8], spark = new double[5];

	public GhostAuraCosmetic() {
		super("Ghost Aura", "Six little white and pink pixel ghosts slowly circling you, fading in and out with new faces, "
				+ "between rising sparkles.", CosmeticSlot.EFFECT);
	}

	private static Vox.Shape sparkle(Vox.Palette palette) {
		// Destello ✦ de 3 brazos cruzados y el centro (como SPARKLE_BOXES del diseño).
		return new Vox.Shape(palette).box('a', -0.15F, -1, -0.15F, 0.3F, 2, 0.3F).box('a', -1, -0.15F, -0.15F, 2, 0.3F, 0.3F)
				.box('a', -0.15F, -0.15F, -1, 0.3F, 0.3F, 2).box('b', -0.35F, -0.35F, -0.35F, 0.7F, 0.7F, 0.7F);
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		if (sparkPink == null) {
			sparkPink = sparkle(SPARK_PINK);
			sparkWhite = sparkle(SPARK_WHITE);
		}
		double t = state.ageInTicks / 20.0;
		poseStack.pushPose();
		VoidKeysCosmetic.ringFrame(null, CosmeticPreview.of(state) != null ? 0.0F : state.bodyRot, poseStack);
		for (int i = 0; i < GhostMotion.AURA_COUNT; i++) {
			GhostMotion.aura(i, t, ghost);
			if (ghost[5] <= 0.01) continue;
			poseStack.pushPose();
			poseStack.translate((float) ghost[0] / 16.0F, (float) ghost[1] / 16.0F, (float) ghost[2] / 16.0F);
			poseStack.mulPose(Axis.YP.rotation((float) ghost[3]));
			float s = (float) ghost[5];
			poseStack.scale(s, s, s);
			poseStack.mulPose(Axis.ZP.rotation((float) ghost[4]));
			GhostModel.draw(poseStack, collector, light, (int) ghost[6], ghost[7] == 1, 0.15 * Math.sin(t * 2 + i), 0.15 * Math.sin(t * 2 + i + 1));
			poseStack.popPose();
		}
		for (double[] p : GhostMotion.SPARKLES) {
			GhostMotion.sparkle(p, t, spark);
			if (spark[3] <= 0.02) continue;
			poseStack.pushPose();
			poseStack.translate((float) spark[0] / 16.0F, (float) spark[1] / 16.0F, (float) spark[2] / 16.0F);
			poseStack.mulPose(Axis.XP.rotation((float) spark[4]));
			poseStack.mulPose(Axis.YP.rotation((float) (spark[4] * 0.7)));
			float s = (float) spark[3];
			poseStack.scale(s, s, s);
			(p[5] == 1 ? sparkPink : sparkWhite).drawGlow(poseStack, collector);
			poseStack.popPose();
		}
		poseStack.popPose();
	}
}
