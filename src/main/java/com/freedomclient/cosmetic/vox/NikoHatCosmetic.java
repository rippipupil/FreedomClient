package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.CosmeticSlot;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Niko Hat (OneShot): sombrero marrón de ala ancha con dos orejas de gato. */
public class NikoHatCosmetic extends VoxCosmetic {
	static final Vox.Palette PALETTE = new Vox.Palette("niko_hat",
			'h', 0xFF5A3128, 'H', 0xFF46251E, 'd', 0xFF331A15, 'i', 0xFF7A4438);
	private Vox.Shape hat;

	public NikoHatCosmetic() {
		super("Niko Hat", "OneShot: Niko's wide-brimmed brown hat with its two cat ears.", CosmeticSlot.HAT);
	}

	/** Ala ancha, copa redondeada, cinta oscura y orejas de gato (triángulos escalonados con el interior claro). */
	static Vox.Shape build() {
		Vox.Shape s = new Vox.Shape(PALETTE)
				.disc('H', 0.0F, -0.8F, 0.0F, 7.0F, 0.8F)
				.disc('d', 0.0F, -1.8F, 0.0F, 4.6F, 1.0F);
		float[] radii = {4.4F, 4.2F, 3.8F, 3.1F};
		for (int i = 0; i < radii.length; i++) s.disc('h', 0.0F, -2.8F - i, 0.0F, radii[i], 1.0F);
		for (int side = -1; side <= 1; side += 2) {
			float cx = side * 2.4F;
			s.box('h', cx - 1.4F, -6.8F, -0.8F, 2.8F, 1.0F, 1.6F);
			s.box('h', cx - 1.0F, -7.8F, -0.7F, 2.0F, 1.0F, 1.4F);
			s.box('h', cx - 0.6F, -8.8F, -0.6F, 1.2F, 1.0F, 1.2F);
			s.box('h', cx - 0.3F, -9.6F, -0.5F, 0.6F, 0.8F, 1.0F);
			s.box('i', cx - 0.8F, -7.6F, -0.95F, 1.6F, 1.6F, 0.4F);
		}
		return s;
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		if (hat == null) hat = build();
		poseStack.pushPose();
		parent.head.translateAndRotate(poseStack);
		poseStack.translate(0.0F, -7.6F / 16.0F, 0.0F);
		poseStack.mulPose(Axis.XP.rotationDegrees(-4.0F));
		hat.draw(poseStack, collector, light);
		poseStack.popPose();
	}
}
