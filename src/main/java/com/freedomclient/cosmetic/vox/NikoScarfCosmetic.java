package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.CosmeticSlot;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/** Niko Scarf (OneShot): bufanda morada/azul alrededor del cuello con dos puntas separadas que ondean al andar. */
public class NikoScarfCosmetic extends VoxCosmetic {
	static final Vox.Palette PALETTE = new Vox.Palette("niko_scarf",
			'p', 0xFF5B3C9E, 'b', 0xFF3E4FB8, 'h', 0xFFB45BD6, 'd', 0xFF3A2670);
	private Vox.Shape wrap;
	private Vox.Shape longTail;
	private Vox.Shape shortTail;

	public NikoScarfCosmetic() {
		super("Niko Scarf", "OneShot: Niko's purple-blue scarf around your neck, with two split tails that sway as you walk.", CosmeticSlot.NECK);
	}

	private void build() {
		wrap = new Vox.Shape(PALETTE)
				.box('p', -4.7F, -0.8F, -2.7F, 9.4F, 2.2F, 0.6F)
				.box('b', -4.7F, -0.8F, 2.1F, 9.4F, 2.2F, 0.6F)
				.box('p', -4.7F, -0.8F, -2.1F, 0.6F, 2.2F, 4.2F)
				.box('b', 4.1F, -0.8F, -2.1F, 0.6F, 2.2F, 4.2F)
				.box('h', -4.7F, -0.9F, -2.8F, 9.4F, 0.5F, 0.3F)
				.box('d', 1.0F, 1.2F, -3.2F, 2.2F, 1.6F, 0.8F);
		longTail = tail(7);
		shortTail = tail(5);
	}

	/** Punta de la bufanda colgando desde el pivote hacia abajo: morado arriba, azul abajo y borde rosa. */
	private static Vox.Shape tail(int length) {
		Vox.Shape s = new Vox.Shape(PALETTE);
		for (int i = 0; i < length; i++) {
			char c = i < length / 2 ? 'p' : 'b';
			s.box(c, -1.1F, i, 0.0F, 2.2F, 1.0F, 0.6F);
		}
		s.box('h', -1.1F, length, 0.0F, 2.2F, 0.6F, 0.6F);
		return s;
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		if (wrap == null) build();
		float time = state.ageInTicks;
		float walk = Math.min(1.0F, state.walkAnimationSpeed);
		// Las puntas se levantan hacia atrás al andar y ondean un poco siempre.
		float lift = walk * 0.9F + Mth.sin(time * 0.1F) * 0.06F;
		poseStack.pushPose();
		parent.body.translateAndRotate(poseStack);
		// Con peto se agranda para quedar por fuera (el peto sobresale 1 px del cuerpo).
		if (!state.chestEquipment.isEmpty()) poseStack.scale(1.2F, 1.0F, 1.45F);
		wrap.draw(poseStack, collector, light);
		for (int i = 0; i < 2; i++) {
			poseStack.pushPose();
			poseStack.translate((i == 0 ? -1.6F : 1.4F) / 16.0F, 0.6F / 16.0F, 2.7F / 16.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(-(lift + Mth.sin(time * 0.17F + i * 1.3F) * 0.08F * (1 + walk * 2)) * Mth.RAD_TO_DEG));
			poseStack.mulPose(Axis.ZP.rotationDegrees(i == 0 ? 12.0F : -14.0F));
			(i == 0 ? longTail : shortTail).draw(poseStack, collector, light);
			poseStack.popPose();
		}
		poseStack.popPose();
	}
}
