package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Soul Scythe: guadaña voxel (como las alas) colgada en diagonal a la espalda, con la hoja asomando por un hombro.
 * La silueta se convierte en cubos de 1 píxel de alto; la guarda con púas es más gruesa.
 */
public final class ScytheRenderer {
	/** Franjas de color (64x32): blanco en v = 0, plata en 4, plata oscura en 8, negro en 12, gris oscuro en 16. */
	private static final Identifier TEXTURE = FreedomClient.id("textures/cosmetic/scythe.png");

	/** W = filo blanco, S = plata, D = plata oscura, K = negro, H = gris oscuro del mango y la guarda. */
	private static final String[] SCYTHE = {
			"...............K........K.",
			"...............KK......KK.",
			"...............KHK....KHK.",
			"...........WWKKKHHK..KHHK.",
			".........WWWSKKHHHHKKHHHKK",
			".......WWWSSSSKKHHHHHHHKK.",
			"......WWSSSSSSSKKKKKKKKK..",
			".....WWSSSSSSSSSSKKKKK....",
			".....WSSSSSSSSSSSS.HK.....",
			"....WSSSSSSSSSSSSS.HHK....",
			"....WSSSSSSSSSDDDD..HK....",
			"...WSSSSSSSDDD......HK....",
			"...WSSSSSDDD........HK....",
			"..WWSSSSDD..........HK....",
			"..WSSSSD............HK..H.",
			"..WSSSD.............HK.K..",
			".WWSSD..............HKK...",
			".WSSDD..............HK....",
			".WSDD...............HK....",
			".WSD................HK....",
			".WD.................HK....",
			"WWD.................HK....",
			"WD..................HK....",
			"WD...............H..HK....",
			"W.................K.HK....",
			"W..................KHK....",
			"W...................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK...H",
			"....................HK..K.",
			"....................HHKK..",
			".....................HK...",
			".....................HK...",
			".....................HHK..",
	};
	/** Píxel de la silueta por el que se cuelga (en el mango, a media altura). */
	private static final float PIVOT_X = 20.5F;
	private static final float PIVOT_Y = 21.0F;
	/** Filas de la guarda con púas, que tiene doble grosor. */
	private static final int GUARD_ROWS = 8;

	private final ModelPart model = create();

	private static ModelPart create() {
		MeshDefinition mesh = new MeshDefinition();
		CubeListBuilder builder = CubeListBuilder.create();
		for (int row = 0; row < SCYTHE.length; row++) {
			String line = SCYTHE[row];
			int x = 0;
			while (x < line.length()) {
				char color = line.charAt(x);
				int end = x + 1;
				while (end < line.length() && line.charAt(end) == color) {
					end++;
				}
				if (color != '.') {
					float depth = row < GUARD_ROWS && (color == 'K' || color == 'H') ? 2.0F : 1.0F;
					builder.texOffs(0, colorRow(color)).addBox(x - PIVOT_X, row - PIVOT_Y, -depth / 2, end - x, 1, depth);
				}
				x = end;
			}
		}
		mesh.getRoot().addOrReplaceChild("scythe", builder, PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32).bakeRoot();
	}

	private static int colorRow(char color) {
		return switch (color) {
			case 'W' -> 0;
			case 'S' -> 4;
			case 'D' -> 8;
			case 'K' -> 12;
			default -> 16;
		};
	}

	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, ScytheCosmetic module) {
		poseStack.pushPose();
		parent.body.translateAndRotate(poseStack);
		// Detrás de la espalda, por fuera de la capa, con la hoja asomando por encima del hombro.
		poseStack.translate(0.0F, 4.5F / 16.0F, 4.2F / 16.0F);
		float size = module.size.getFloat();
		poseStack.scale(size, size, size);
		// Sin reflejar, la hoja queda sobre el hombro derecho del jugador: para el izquierdo se refleja.
		if (module.side.is("Left")) {
			poseStack.scale(-1.0F, 1.0F, 1.0F);
		}
		// En diagonal: la hoja arriba, junto a la cabeza, y el mango bajando hacia la cadera contraria.
		poseStack.mulPose(Axis.ZP.rotationDegrees(-32.0F));
		collector.submitModelPart(model, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE), light, OverlayTexture.NO_OVERLAY, null);
		poseStack.popPose();
	}
}
