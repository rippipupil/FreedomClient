package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.CosmeticSlot;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Máscaras de Bee Swarm: cubren toda la cabeza. La parte de la cara es medio transparente (se ve tu cara detrás)
 * con los rasgos de la máscara encima; el ala, la cúpula y los adornos son opacos.
 */
public abstract class MaskCosmetic extends VoxCosmetic {
	/** Todo lo opaco (lados, ala, cúpula, adornos y rasgos de la cara). */
	private Vox.Shape solid;
	/** El panel de la cara, medio transparente. */
	private Vox.Shape face;

	protected MaskCosmetic(String name, String description) {
		super(name, description, CosmeticSlot.HAT);
	}

	protected abstract Vox.Palette palette();

	/** Añade lo propio de cada máscara (cúpula, adornos y rasgos) sobre la base común. */
	protected abstract void decorate(Vox.Shape solid);

	/**
	 * Base común: banda alrededor de la cabeza (lados y nuca en 'b'), ala en 'B' a la altura de la frente y panel de
	 * cara en 'f' (color con alfa). La cabeza del jugador va de -4 a 4 en x y z y de -8 a 0 en y.
	 */
	private void build() {
		Vox.Palette palette = palette();
		solid = new Vox.Shape(palette)
				.box('b', -4.7F, -5.4F, -4.7F, 0.6F, 6.0F, 9.4F)
				.box('b', 4.1F, -5.4F, -4.7F, 0.6F, 6.0F, 9.4F)
				.box('b', -4.1F, -5.4F, 4.1F, 8.2F, 6.0F, 0.6F)
				.disc('B', 0.0F, -6.4F, 0.0F, 6.3F, 1.0F);
		decorate(solid);
		face = new Vox.Shape(palette).box('f', -4.1F, -5.4F, -4.7F, 8.2F, 6.0F, 0.6F);
	}

	/** Rasgos de la cara (9x6 píxeles) justo delante del panel transparente. */
	protected static void features(Vox.Shape shape, String[] rows) {
		shape.art(rows, -4.5F, -5.2F, -5.0F, 0.4F);
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		if (solid == null) build();
		poseStack.pushPose();
		parent.head.translateAndRotate(poseStack);
		// Siempre algo más grande que la cabeza para quedar por fuera de la capa 3D de la skin (sobresale medio
		// píxel), y más con casco (sobresale 1 px).
		float grow = state.headEquipment.isEmpty() ? 1.16F : 1.3F;
		poseStack.translate(0.0F, -4.0F / 16.0F, 0.0F);
		poseStack.scale(grow, grow, grow);
		poseStack.translate(0.0F, 4.0F / 16.0F, 0.0F);
		solid.draw(poseStack, collector, light);
		face.drawTranslucent(poseStack, collector, light);
		poseStack.popPose();
	}
}
