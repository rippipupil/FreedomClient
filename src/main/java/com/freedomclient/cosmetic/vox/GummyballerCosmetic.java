package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.CosmeticSlot;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/**
 * Gummyballer (Bee Swarm): varita morada con un plato lleno de bolitas de goma que giran y una gran bola de goma
 * que levita encima girando sobre sí misma. A la espalda, de lado.
 */
public class GummyballerCosmetic extends VoxCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("gummyballer",
			'p', 0xFFB06BD8, 'P', 0xFFD690F0, 'm', 0xFF46E6B0, 'M', 0xFF2FCB97,
			'k', 0xFFF08BD8, 'c', 0xFF3FE6F0, 'b', 0xFFD27BE6, 'B', 0xFFE8A6F4, 'h', 0xFFFFFFFF);

	public final NumberSetting size = add(new NumberSetting("Size", "How big the Gummyballer is.", 0.8, 0.5, 1.2, 0.05, "x"));
	public final ModeSetting side = add(new ModeSetting("Side", "Which shoulder the dish sticks out over.", "Left", "Left", "Right"));
	private Vox.Shape wand;
	private Vox.Shape dots;
	private Vox.Shape ball;

	public GummyballerCosmetic() {
		super("Gummyballer", "Bee Swarm: the purple Gummyballer on your back, with spinning gummy dots and a big gummy ball levitating above it.",
				CosmeticSlot.BACK);
	}

	private void build() {
		wand = new Vox.Shape(PALETTE)
				.box('p', -0.6F, 0.0F, -0.6F, 1.2F, 13.0F, 1.2F)
				.sphere(0.0F, 14.2F, 0.0F, 1.5F, dy -> dy < 0 ? 'm' : 'M')
				.disc('m', 0.0F, -1.5F, 0.0F, 1.9F, 1.5F)
				.disc('p', 0.0F, -2.5F, 0.0F, 2.6F, 1.0F)
				.disc('p', 0.0F, -3.5F, 0.0F, 4.1F, 1.0F)
				.disc('P', 0.0F, -4.5F, 0.0F, 5.3F, 1.0F)
				.ring('p', 0.0F, -5.3F, 0.0F, 5.8F, 4.6F, 0.8F);
		// Bolitas de goma incrustadas en el plato (giran juntas).
		dots = new Vox.Shape(PALETTE);
		for (int i = 0; i < 4; i++) {
			double angle = i * Math.PI / 2;
			dots.sphere(i % 2 == 0 ? 'k' : 'c', (float) Math.cos(angle) * 2.6F, -5.2F, (float) Math.sin(angle) * 2.6F, 1.1F);
		}
		ball = new Vox.Shape(PALETTE).sphere(0.0F, 0.0F, 0.0F, 3.6F, dy -> dy < -2 ? 'B' : 'b').box('h', -2.2F, -2.8F, -2.6F, 1.0F, 1.0F, 0.6F);
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		if (wand == null) build();
		float time = state.ageInTicks;
		poseStack.pushPose();
		onBackDiagonal(parent, poseStack, state, size.getFloat(), side.is("Left"));
		poseStack.translate(0.0F, 2.0F / 16.0F, 0.0F);
		wand.draw(poseStack, collector, light);
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(time * 3.0F));
		dots.draw(poseStack, collector, light);
		poseStack.popPose();
		// La bola grande levita sobre el plato, subiendo y bajando, y gira sobre sí misma.
		poseStack.translate(0.0F, (-11.5F + Mth.sin(time * 0.09F) * 1.2F) / 16.0F, 0.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.2F));
		poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(time * 0.05F) * 12.0F));
		ball.draw(poseStack, collector, light);
		poseStack.popPose();
	}
}
