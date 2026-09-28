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

/** Tide Popper (Bee Swarm): aguja azul hielo con anillos, a la espalda de lado, y bolas blancas que giran alrededor. */
public class TidePopperCosmetic extends VoxCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("tide_popper",
			'c', 0xFF6FD8F2, 'l', 0xFFA8F0FF, 'd', 0xFF3FA8CC, 'w', 0xFFFFFFFF, 's', 0xFFD8F2FA);
	private static final int ORBS = 4;

	public final NumberSetting size = add(new NumberSetting("Size", "How big the Tide Popper is.", 0.8, 0.5, 1.2, 0.05, "x"));
	public final ModeSetting side = add(new ModeSetting("Side", "Which shoulder the tip sticks out over.", "Left", "Left", "Right"));
	private Vox.Shape tool;
	private Vox.Shape orb;

	public TidePopperCosmetic() {
		super("Tide Popper", "Bee Swarm: the ice-blue Tide Popper on your back, with white bubbles orbiting around it.", CosmeticSlot.BACK);
	}

	private void build() {
		Vox.Shape s = new Vox.Shape(PALETTE);
		// Mango abajo, cono que se abre, cuerpo con anillos y la aguja que sube hasta la punta.
		s.box('d', -0.5F, 8.0F, -0.5F, 1, 6, 1);
		float[] cone = {1.0F, 1.4F, 1.8F, 2.2F, 2.6F};
		for (int i = 0; i < cone.length; i++) s.disc('c', 0.0F, 7.0F - i, 0.0F, cone[i], 1.0F);
		s.disc('l', 0.0F, 2.0F, 0.0F, 3.4F, 1.0F);
		s.disc('c', 0.0F, -6.0F, 0.0F, 1.6F, 8.0F);
		s.disc('l', 0.0F, -2.0F, 0.0F, 3.0F, 1.0F);
		s.disc('l', 0.0F, -6.0F, 0.0F, 2.5F, 1.0F);
		float[] spire = {1.3F, 1.2F, 1.1F, 1.0F, 0.9F, 0.8F, 0.7F, 0.6F, 0.55F, 0.5F};
		for (int i = 0; i < spire.length; i++) {
			float r = spire[i];
			s.box(i % 3 == 0 ? 's' : 'c', -r, -7.0F - i * 1.2F, -r, r * 2, 1.2F, r * 2);
		}
		s.box('l', -0.25F, -20.0F, -0.25F, 0.5F, 1.0F, 0.5F);
		tool = s;
		orb = new Vox.Shape(PALETTE).sphere(0.0F, 0.0F, 0.0F, 1.3F, dy -> dy < 0 ? 'w' : 's');
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		if (tool == null) build();
		float time = state.ageInTicks;
		poseStack.pushPose();
		onBackDiagonal(parent, poseStack, size.getFloat(), side.is("Left"));
		tool.draw(poseStack, collector, light);
		// Las bolas giran en horizontal alrededor de la aguja, cada una subiendo y bajando un poco.
		for (int i = 0; i < ORBS; i++) {
			float angle = time * 0.08F + i * Mth.TWO_PI / ORBS;
			poseStack.pushPose();
			poseStack.translate(Mth.cos(angle) * 5.0F / 16.0F, (-1.0F + Mth.sin(time * 0.1F + i) * 1.2F) / 16.0F, Mth.sin(angle) * 5.0F / 16.0F);
			orb.drawGlow(poseStack, collector);
			poseStack.popPose();
		}
		poseStack.popPose();
	}
}
