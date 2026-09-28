package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Neon Pack en 3D: dos placas de cobre a los lados, un panel gris oscuro en el centro con el anillo cian que brilla
 * (dibujado a luz completa y latiendo), una placa hexagonal de cobre debajo y las correas por encima de los hombros.
 * Coordenadas en píxeles del cuerpo del jugador: la espalda está en z = 2.
 */
public final class NeonPackRenderer {
	private static final Identifier TEXTURE = FreedomClient.id("textures/cosmetic/neon_pack.png");
	/** Franjas de color de neon_pack.png (16 px de alto cada una, para que quepan las caras de las piezas altas). */
	private static final int GRAY = 0;
	private static final int GRAY_LIGHT = 16;
	private static final int COPPER = 32;
	private static final int COPPER_LIGHT = 48;
	private static final int COPPER_DARK = 64;
	private static final int CYAN = 80;
	private static final int CYAN_BRIGHT = 96;
	private static final int BLACK = 112;
	/** Anillo de 5x5 con un hueco abajo, como el de la mochila. */
	private static final String[] RING = {".###.", "#...#", "#...#", "#...#", ".#.#."};

	private final ModelPart pack = createPack();
	private final ModelPart ring = createRing(CYAN);
	private final ModelPart ringBright = createRing(CYAN_BRIGHT);

	private static ModelPart createPack() {
		MeshDefinition mesh = new MeshDefinition();
		CubeListBuilder b = CubeListBuilder.create();
		// Panel central y la pestaña de arriba.
		b.texOffs(0, GRAY).addBox(-1.8F, 0.8F, 2.0F, 3.6F, 6.8F, 1.6F);
		b.texOffs(0, GRAY_LIGHT).addBox(-0.6F, 0.2F, 2.0F, 1.2F, 0.7F, 1.4F);
		b.texOffs(0, BLACK).addBox(-1.4F, 1.6F, 3.6F, 2.8F, 3.6F, 0.1F);
		// Placa hexagonal de cobre bajo el anillo.
		b.texOffs(0, COPPER).addBox(-0.9F, 5.4F, 3.6F, 1.8F, 1.4F, 0.3F);
		b.texOffs(0, COPPER_LIGHT).addBox(-0.5F, 5.1F, 3.6F, 1.0F, 0.3F, 0.3F);
		b.texOffs(0, COPPER_DARK).addBox(-0.5F, 6.8F, 3.6F, 1.0F, 0.3F, 0.3F);
		for (int side = -1; side <= 1; side += 2) {
			float inner = side * 1.7F;
			// Placa de cobre: un bloque redondeado (más estrecho arriba y abajo) con un brillo y dos remaches.
			b.texOffs(0, COPPER).addBox(side < 0 ? inner - 2.6F : inner, 1.6F, 2.0F, 2.6F, 5.6F, 1.3F);
			b.texOffs(0, COPPER).addBox(side < 0 ? inner - 2.1F : inner, 1.1F, 2.0F, 2.1F, 0.5F, 1.2F);
			b.texOffs(0, COPPER_DARK).addBox(side < 0 ? inner - 2.1F : inner, 7.2F, 2.0F, 2.1F, 0.6F, 1.2F);
			b.texOffs(0, COPPER_LIGHT).addBox(side < 0 ? inner - 1.9F : inner + 0.6F, 2.3F, 3.3F, 1.3F, 4.0F, 0.2F);
			b.texOffs(0, BLACK).addBox(side < 0 ? inner - 1.1F : inner + 0.8F, 3.4F, 3.35F, 0.3F, 1.2F, 0.2F);
			b.texOffs(0, COPPER_DARK).addBox(side < 0 ? inner - 1.7F : inner + 1.2F, 1.8F, 3.3F, 0.5F, 0.5F, 0.25F);
			// Correa por encima del hombro y bajando por delante.
			float strapX = side < 0 ? -3.4F : 2.2F;
			b.texOffs(0, GRAY).addBox(strapX, -0.35F, -2.3F, 1.2F, 0.5F, 4.6F);
			b.texOffs(0, GRAY).addBox(strapX, 0.0F, -2.35F, 1.2F, 5.0F, 0.3F);
			b.texOffs(0, GRAY_LIGHT).addBox(strapX + 0.2F, 3.8F, -2.45F, 0.8F, 0.6F, 0.2F);
		}
		mesh.getRoot().addOrReplaceChild("pack", b, PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 128).bakeRoot();
	}

	/** El anillo que brilla: 5x5 cubitos de 0,6 px sobre el panel. */
	private static ModelPart createRing(int strip) {
		MeshDefinition mesh = new MeshDefinition();
		CubeListBuilder b = CubeListBuilder.create();
		float cell = 0.6F;
		for (int row = 0; row < RING.length; row++) {
			for (int column = 0; column < RING[row].length(); column++) {
				if (RING[row].charAt(column) == '#') {
					b.texOffs(0, strip).addBox(-1.5F + column * cell, 1.9F + row * cell, 3.62F, cell, cell, 0.25F);
				}
			}
		}
		// Punto central más tenue.
		b.texOffs(0, strip).addBox(-0.3F, 2.8F, 3.61F, 0.6F, 0.6F, 0.1F);
		mesh.getRoot().addOrReplaceChild("ring", b, PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 128).bakeRoot();
	}

	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		poseStack.pushPose();
		parent.body.translateAndRotate(poseStack);
		// Con peto o élitros se separa de la espalda para no quedar dentro ni tapada.
		poseStack.translate(0.0F, 0.0F, CosmeticModule.backClearance(state) / 16.0F);
		collector.submitModelPart(pack, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE), light, OverlayTexture.NO_OVERLAY, null);
		// El anillo late despacio y brilla aunque sea de noche.
		boolean bright = Mth.sin(state.ageInTicks * 0.15F) > 0.2F;
		collector.submitModelPart(bright ? ringBright : ring, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE), LightTexture.FULL_BRIGHT,
				OverlayTexture.NO_OVERLAY, null);
		poseStack.popPose();
	}
}
