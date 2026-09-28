package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.CosmeticSlot;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Freddy Hat (FNAF): el sombrero de copa negro moteado de Freddy, un poco ladeado sobre la cabeza. */
public class FreddyHatCosmetic extends VoxCosmetic {
	static final Vox.Palette PALETTE = new Vox.Palette("freddy_hat",
			'k', 0xFF141417, 'g', 0xFF8C8E96, 'G', 0xFFC4C6CE, 'r', 0xFF8A2A30, 'u', 0xFF3A4A7A);
	private Vox.Shape hat;

	public FreddyHatCosmetic() {
		super("Freddy Hat", "FNAF: Freddy's little black top hat with its silver speckles, tilted on your head.", CosmeticSlot.HAT);
	}

	/** Sombrero de copa: ala negra y copa con motas plateadas, rojizas y azuladas (repetibles, no al azar). */
	static Vox.Shape build(float brim, float crown, int height) {
		Vox.Shape s = new Vox.Shape(PALETTE).disc('k', 0.0F, -1.0F, 0.0F, brim, 1.0F);
		int n = (int) Math.ceil(crown);
		for (int y = 0; y < height; y++) {
			for (int dz = -n; dz < n; dz++) {
				for (int dx = -n; dx < n; dx++) {
					float mx = dx + 0.5F;
					float mz = dz + 0.5F;
					if (mx * mx + mz * mz > crown * crown) continue;
					int hash = Math.floorMod(dx * 73 + dz * 151 + y * 37 + dx * dz * 11, 23);
					char c = y == 0 ? 'k' : hash < 3 ? 'G' : hash < 7 ? 'g' : hash == 8 ? 'r' : hash == 9 ? 'u' : 'k';
					s.box(c, dx, -2.0F - y, dz, 1, 1, 1);
				}
			}
		}
		return s;
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		if (hat == null) hat = build(4.2F, 2.9F, 5);
		poseStack.pushPose();
		parent.head.translateAndRotate(poseStack);
		poseStack.translate(0.8F / 16.0F, (state.headEquipment.isEmpty() ? -8.6F : -9.3F) / 16.0F, 0.0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(-9.0F));
		hat.draw(poseStack, collector, light);
		poseStack.popPose();
	}
}
