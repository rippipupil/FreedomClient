package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.CosmeticSlot;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/** Sun Backpack (OneShot): el Sol, la gran bombilla de Niko, en un arnés a la espalda. Brilla aunque sea de noche. */
public class SunBackpackCosmetic extends VoxCosmetic {
	static final Vox.Palette PALETTE = new Vox.Palette("sun_backpack",
			'y', 0xFFFFD447, 'Y', 0xFFFFEE9A, 'w', 0xFFFFFFF0, 'o', 0xFFFF9A2A,
			'g', 0xFF8A8C94, 'G', 0xFF5E6068, 'l', 0xFF5A3522, 'L', 0xFF3E2317);
	private Vox.Shape harness;
	private Vox.Shape sun;

	public SunBackpackCosmetic() {
		super("Sun Backpack", "OneShot: the Sun, Niko's glowing lightbulb, carried on your back. It glows even at night.", CosmeticSlot.BACK);
	}

	/** Bombilla: esfera amarilla con brillo claro, filamento naranja por delante y casquillo gris que la sujeta. */
	static Vox.Shape sun(float r) {
		Vox.Shape s = new Vox.Shape(PALETTE).sphere(0.0F, 0.0F, 0.0F, r, dy -> dy < -r / 2 ? 'Y' : 'y');
		s.box('w', -r * 0.55F, -r * 0.6F, -r * 0.9F, r * 0.35F, r * 0.35F, 0.5F);
		// Filamento en espiral pintado en la cara de fuera.
		s.box('o', -1.2F, -0.6F, r - 0.6F, 2.4F, 0.6F, 0.8F);
		s.box('o', -1.2F, 0.4F, r - 0.6F, 0.6F, 1.2F, 0.8F);
		s.box('o', 0.6F, -1.4F, r - 0.6F, 0.6F, 1.2F, 0.8F);
		return s;
	}

	private void build() {
		harness = new Vox.Shape(PALETTE)
				// Correas por los hombros (delante y detrás) y la placa de la espalda con el casquillo.
				.box('l', -3.8F, -0.1F, -2.3F, 1.6F, 7.0F, 0.4F)
				.box('l', 2.2F, -0.1F, -2.3F, 1.6F, 7.0F, 0.4F)
				.box('l', -3.8F, -0.4F, -2.3F, 1.6F, 0.4F, 4.8F)
				.box('l', 2.2F, -0.4F, -2.3F, 1.6F, 0.4F, 4.8F)
				.box('L', -3.0F, 3.0F, 2.1F, 6.0F, 6.0F, 0.8F)
				.box('G', -1.6F, 5.6F, 2.9F, 3.2F, 1.6F, 1.6F)
				.box('g', -1.3F, 5.0F, 4.5F, 2.6F, 2.6F, 1.2F);
		sun = sun(4.2F);
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		if (harness == null) build();
		poseStack.pushPose();
		parent.body.translateAndRotate(poseStack);
		boolean armor = !state.chestEquipment.isEmpty();
		// Con peto, el arnés se agranda para quedar por fuera y la bombilla se aleja un poco.
		poseStack.pushPose();
		if (armor) {
			poseStack.translate(0.0F, -0.6F / 16.0F, 0.0F);
			poseStack.scale(1.25F, 1.0F, 1.45F);
		}
		harness.draw(poseStack, collector, light);
		poseStack.popPose();
		poseStack.translate(0.0F, 0.0F, backClearance(state) / 16.0F);
		// La bombilla, detrás del casquillo, con un latido suave de luz.
		float pulse = 1.0F + Mth.sin(state.ageInTicks * 0.12F) * 0.025F;
		poseStack.translate(0.0F, 6.2F / 16.0F, 9.6F / 16.0F);
		poseStack.scale(pulse, pulse, pulse);
		sun.drawGlow(poseStack, collector);
		poseStack.popPose();
	}
}
