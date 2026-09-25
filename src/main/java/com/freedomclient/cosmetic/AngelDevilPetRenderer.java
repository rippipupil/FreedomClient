package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Mascota: un Angel Devil pixel en miniatura (halo, pelo rojo, camisa blanca con corbata y alas)
 * hecho de cubos a partir de un dibujo de 10x16, que flota junto a tu hombro y aletea.
 */
public final class AngelDevilPetRenderer {
	private static final Identifier TEXTURE = FreedomClient.id("textures/cosmetic/pet.png");
	/** Letras de la paleta en el mismo orden que las franjas de pet.png (ver tools/make_cosmetics.py). */
	private static final String PALETTE = "gGrRsephkKwW";

	private static final String[] BODY = {
			"...gggg...",
			"..g....g..",
			"...gggg...",
			"..rrrrrr..",
			".rrrrrrrr.",
			".rrssssrr.",
			".rsessesr.",
			".rssssssr.",
			".rrsppsrr.",
			".rr.ss.rr.",
			".hhhkkhhh.",
			".hhhkkhhh.",
			".s.hkkh.s.",
			"...kkkk...",
			"...kk.kk..",
			"..KK..KK..",
	};
	/** Filas de la cabeza: por delante se ve la cara y por detrás el pelo. */
	private static final int HEAD_FROM = 5;
	private static final int HEAD_TO = 9;
	/** Ala derecha; la columna 0 queda junto a la espalda. La izquierda es su reflejo. */
	private static final String[] WING = {
			"....ww",
			"..wwww",
			".wwwWw",
			"wwwWw.",
			"wwWw..",
			"wWw...",
			"ww....",
	};
	private static final int WING_ROW = 5;

	private final ModelPart root;
	private final ModelPart leftWing;
	private final ModelPart rightWing;

	public AngelDevilPetRenderer() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("body", body(), PartPose.ZERO);
		// Las alas salen de la espalda, a la altura de los hombros.
		float wingY = WING_ROW - BODY.length;
		mesh.getRoot().addOrReplaceChild("right_wing", wing(true), PartPose.offset(-3.0F, wingY, 1.0F));
		mesh.getRoot().addOrReplaceChild("left_wing", wing(false), PartPose.offset(3.0F, wingY, 1.0F));
		root = LayerDefinition.create(mesh, 64, 64).bakeRoot();
		leftWing = root.getChild("left_wing");
		rightWing = root.getChild("right_wing");
	}

	private static CubeListBuilder body() {
		CubeListBuilder builder = CubeListBuilder.create();
		int half = BODY[0].length() / 2;
		for (int row = 0; row < BODY.length; row++) {
			String line = BODY[row];
			float y = row - BODY.length;
			boolean head = row >= HEAD_FROM && row <= HEAD_TO;
			for (int x = 0; x < line.length(); ) {
				char c = line.charAt(x);
				int end = x + 1;
				while (end < line.length() && line.charAt(end) == c) end++;
				if (c != '.') {
					int length = end - x;
					if (head && c != 'r' && c != 'R') {
						// Cara delante (1 px) y pelo detrás, para que desde atrás no se vean los ojos.
						box(builder, c, x - half, y, -1.0F, length, 1);
						box(builder, 'r', x - half, y, 0.0F, length, 1);
					} else {
						int depth = row <= 2 ? 1 : 2;
						box(builder, c, x - half, y, depth == 1 ? -0.5F : -1.0F, length, depth);
					}
				}
				x = end;
			}
		}
		return builder;
	}

	private static CubeListBuilder wing(boolean right) {
		CubeListBuilder builder = CubeListBuilder.create();
		for (int row = 0; row < WING.length; row++) {
			String line = WING[row];
			for (int x = 0; x < line.length(); ) {
				char c = line.charAt(x);
				int end = x + 1;
				while (end < line.length() && line.charAt(end) == c) end++;
				if (c != '.') {
					int length = end - x;
					float boxX = right ? -x - length : x;
					box(builder, c, boxX, row, 0.0F, length, 1);
				}
				x = end;
			}
		}
		return builder;
	}

	private static void box(CubeListBuilder builder, char color, float x, float y, float z, int length, int depth) {
		builder.texOffs(0, PALETTE.indexOf(color) * 4).addBox(x, y, z, length, 1, depth);
	}

	public void render(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, PetCosmetic module) {
		float time = state.ageInTicks;
		float bob = Mth.sin(time * 0.1F) * 1.5F;
		float flap = 0.35F + Mth.sin(time * 0.35F) * 0.35F;
		rightWing.yRot = flap;
		leftWing.yRot = -flap;

		// En el espacio del modelo del jugador: x negativo es su derecha, y negativo es hacia arriba, 16 = 1 bloque.
		float side = module.side.is("Right") ? -1.0F : 1.0F;
		poseStack.pushPose();
		poseStack.translate(side * 13.0F / 16.0F, (-1.0F + bob) / 16.0F, 2.0F / 16.0F);
		float size = module.size.getFloat();
		poseStack.scale(size, size, size);
		collector.submitModelPart(root, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE), light, OverlayTexture.NO_OVERLAY, null);
		poseStack.popPose();
	}
}
