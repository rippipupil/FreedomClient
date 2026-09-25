package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Mascota: un Angel Devil en miniatura hecho como un mini jugador (cabeza de 8x8x8 con flequillo y pelo largo,
 * camisa con corbata, brazos y piernas), con un halo en anillo y alas de plumas voxel que aletean.
 * Flota junto a tu hombro. La textura está en pet.png (ver tools/make_cosmetics.py).
 */
public final class AngelDevilPetRenderer {
	private static final Identifier TEXTURE = FreedomClient.id("textures/cosmetic/pet.png");
	private static final int HALO_SEGMENTS = 12;
	/** Franjas de color de pet.png para el halo y las alas. */
	private static final int STRIP_GOLD = 40;
	private static final int STRIP_WING = 48;
	private static final int STRIP_WING_LIGHT = 52;
	private static final int STRIP_WING_SHADE = 56;
	private static final int STRIP_WING_OUTLINE = 60;
	/** Ala derecha de 9x8; la columna 0 queda junto a la espalda. b/w = blanco, l = claro, s = sombra, o = contorno. */
	private static final String[] WING = {
			"......bbb",
			"....bbwwo",
			"..bbwwwlo",
			".bwwwslo.",
			"bwwwslo..",
			"wwwslo...",
			"wwslo....",
			"wlo......",
	};

	private final ModelPart root;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart halo;

	public AngelDevilPetRenderer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition parts = mesh.getRoot();
		// Coordenadas en píxeles con los pies en y = 0 (y negativo es hacia arriba) y la cara hacia -z, como un jugador.
		PartDefinition headPart = parts.addOrReplaceChild("head", CubeListBuilder.create()
						.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8)
						// Pelo largo que cae por la espalda hasta los hombros.
						.texOffs(32, 0).addBox(-4.0F, 0.0F, 1.5F, 8, 4, 2),
				PartPose.offset(0.0F, -11.0F, 0.0F));
		parts.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, 0.0F, -1.5F, 6, 6, 3),
				PartPose.offset(0.0F, -11.0F, 0.0F));
		parts.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(18, 16).addBox(-2.0F, 0.0F, -1.0F, 2, 6, 2),
				PartPose.offset(-3.0F, -11.0F, 0.0F));
		parts.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(26, 16).addBox(0.0F, 0.0F, -1.0F, 2, 6, 2),
				PartPose.offset(3.0F, -11.0F, 0.0F));
		parts.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 26).addBox(-3.0F, 0.0F, -1.5F, 3, 5, 3),
				PartPose.offset(0.0F, -5.0F, 0.0F));
		parts.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(12, 26).addBox(0.0F, 0.0F, -1.5F, 3, 5, 3),
				PartPose.offset(0.0F, -5.0F, 0.0F));
		parts.addOrReplaceChild("right_wing", wing(true), PartPose.offset(-1.0F, -13.0F, 3.5F));
		parts.addOrReplaceChild("left_wing", wing(false), PartPose.offset(1.0F, -13.0F, 3.5F));

		// Halo: un anillo de cubitos dorados sobre la cabeza (va dentro de la cabeza para moverse con ella).
		PartDefinition haloPart = headPart.addOrReplaceChild("halo", CubeListBuilder.create(), PartPose.offset(0.0F, -10.5F, 0.0F));
		float radius = 3.5F;
		for (int i = 0; i < HALO_SEGMENTS; i++) {
			float angle = (float) (i * Math.PI * 2 / HALO_SEGMENTS);
			haloPart.addOrReplaceChild("segment" + i,
					CubeListBuilder.create().texOffs(0, STRIP_GOLD).addBox(-1.0F, -0.5F, -0.5F, 2, 1, 1),
					PartPose.offsetAndRotation(Mth.cos(angle) * radius, 0.0F, Mth.sin(angle) * radius, 0.0F, -angle + (float) Math.PI / 2, 0.0F));
		}

		root = LayerDefinition.create(mesh, 64, 64).bakeRoot();
		head = root.getChild("head");
		halo = head.getChild("halo");
		rightArm = root.getChild("right_arm");
		leftArm = root.getChild("left_arm");
		rightWing = root.getChild("right_wing");
		leftWing = root.getChild("left_wing");
	}

	/** Ala voxel: cada tramo de píxeles del mismo color de una fila es un cubo de 1 píxel de alto. */
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
					builder.texOffs(0, strip(c)).addBox(boxX, row, 0.0F, length, 1, 1);
				}
				x = end;
			}
		}
		return builder;
	}

	private static int strip(char c) {
		return switch (c) {
			case 'l' -> STRIP_WING_LIGHT;
			case 's' -> STRIP_WING_SHADE;
			case 'o' -> STRIP_WING_OUTLINE;
			default -> STRIP_WING;
		};
	}

	public void render(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, PetCosmetic module) {
		float time = state.ageInTicks;
		float bob = Mth.sin(time * 0.1F) * 1.2F;
		// Alas: se abren hacia atrás y aletean suave.
		float flap = 0.45F + Mth.sin(time * 0.3F) * 0.3F;
		rightWing.yRot = flap;
		leftWing.yRot = -flap;
		// Brazos: un pequeño balanceo; cabeza: mira un poco a los lados.
		rightArm.xRot = Mth.sin(time * 0.1F) * 0.15F;
		leftArm.xRot = -Mth.sin(time * 0.1F) * 0.15F;
		rightArm.zRot = 0.1F;
		leftArm.zRot = -0.1F;
		head.yRot = Mth.sin(time * 0.03F) * 0.25F;
		head.xRot = Mth.sin(time * 0.05F) * 0.05F;
		halo.yRot = time * 0.04F;

		// En el espacio del modelo del jugador: x negativo es su derecha, y negativo es hacia arriba, 16 = 1 bloque.
		float side = module.side.is("Right") ? -1.0F : 1.0F;
		poseStack.pushPose();
		poseStack.translate(side * 14.0F / 16.0F, (-1.0F + bob) / 16.0F, 2.0F / 16.0F);
		float size = module.size.getFloat();
		poseStack.scale(size, size, size);
		collector.submitModelPart(root, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE), light, OverlayTexture.NO_OVERLAY, null);
		poseStack.popPose();
	}
}
