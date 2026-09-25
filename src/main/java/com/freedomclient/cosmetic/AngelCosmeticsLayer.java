package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Dibuja las alas y el halo 3D (modelos hechos de cubos) sobre el jugador, y la capa ondulada de Wavy Capes. */
public class AngelCosmeticsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static final Identifier WINGS_TEXTURE = FreedomClient.id("textures/cosmetic/wings.png");
	private static final Identifier HALO_TEXTURE = FreedomClient.id("textures/cosmetic/halo.png");
	private static final int HALO_SEGMENTS = 16;

	private final ModelPart wings = createWings();
	private final ModelPart halo = createHalo();
	private final WavyCapeRenderer wavyCape = new WavyCapeRenderer();
	private final AngelDevilPetRenderer pet = new AngelDevilPetRenderer();

	public AngelCosmeticsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	/**
	 * Silueta de un ala en píxeles (24x18). La columna 0 se une a la espalda y la punta queda arriba a la derecha.
	 * Cada letra es un color de la textura: b = hueso/borde superior, o = contorno, w = blanco, l = claro, s = sombra.
	 */
	private static final String[] WING = {
			"................bbbbbbbb",
			".............bbbwwwslwwo",
			"..........bbbwwwwslwwwoo",
			"........bbwwwwwslwwwwo..",
			".....bbbwwwwwslwwwwwso..",
			"...bblwwwwwslwwwwwsooo..",
			".bblwwwwwslwwwwwsoo.....",
			"blwwwwwslwwwwwslo.o.....",
			"wwwwwslwwwwwsloo........",
			"wwwslwwwwwslwo.o........",
			"wslwwwwwslwoo...........",
			"lwwwwwslwwo.o...........",
			"wwwwslwwoo..............",
			"wwslwwwo................",
			"slwwwoo.................",
			"wwwwo...................",
			"wooo....................",
			"o.......................",
	};
	/** Columna donde el ala se dobla: a partir de aquí los píxeles forman la punta, que gira aparte. */
	private static final int WING_FOLD = 10;
	/** Fila de la silueta que queda a la altura del pivote del ala. */
	private static final int WING_ROOT_ROW = 10;

	/**
	 * Alas voxel: cada tramo de píxeles del mismo color de una fila es un cubo de 1 píxel de alto, así el ala
	 * tiene volumen de verdad. El hueso superior y la base son más gruesos. Cada ala tiene una parte interior
	 * y una punta que se dobla por separado.
	 */
	private static ModelPart createWings() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition left = root.addOrReplaceChild("left", wingBoxes(0, WING_FOLD, false), PartPose.offset(1.0F, 3.0F, 2.5F));
		left.addOrReplaceChild("tip", wingBoxes(WING_FOLD, WING[0].length(), false), PartPose.offset(WING_FOLD, 0.0F, 0.0F));
		PartDefinition right = root.addOrReplaceChild("right", wingBoxes(0, WING_FOLD, true), PartPose.offset(-1.0F, 3.0F, 2.5F));
		right.addOrReplaceChild("tip", wingBoxes(WING_FOLD, WING[0].length(), true), PartPose.offset(-WING_FOLD, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 32).bakeRoot();
	}

	/** Cubos de las columnas [from, to) de la silueta, relativos al pivote de su parte (en {@code from}). */
	private static CubeListBuilder wingBoxes(int from, int to, boolean mirrored) {
		CubeListBuilder builder = CubeListBuilder.create();
		for (int row = 0; row < WING.length; row++) {
			String line = WING[row];
			int x = from;
			while (x < to) {
				char color = line.charAt(x);
				int end = x + 1;
				while (end < to && line.charAt(end) == color) {
					end++;
				}
				if (color != '.') {
					int length = end - x;
					int depth = color == 'b' || x < 4 ? 2 : 1;
					float start = x - from;
					float boxX = mirrored ? -start - length : start;
					// La textura tiene una franja sólida de 4 px de alto por color, así cada cubo sale de un solo color.
					builder.texOffs(0, colorRow(color)).addBox(boxX, row - WING_ROOT_ROW, -0.5F, length, 1, depth);
				}
				x = end;
			}
		}
		return builder;
	}

	private static int colorRow(char color) {
		return switch (color) {
			case 'o' -> 0;
			case 'w' -> 4;
			case 'l' -> 8;
			case 's' -> 12;
			default -> 16;
		};
	}

	/** Anillo de pequeños cubos alrededor de un círculo. */
	private static ModelPart createHalo() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		float radius = 4.5F;
		for (int i = 0; i < HALO_SEGMENTS; i++) {
			float angle = (float) (i * Math.PI * 2 / HALO_SEGMENTS);
			root.addOrReplaceChild("segment" + i,
					CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -0.5F, -0.5F, 2.0F, 1.0F, 1.0F),
					PartPose.offsetAndRotation(Mth.cos(angle) * radius, 0.0F, Mth.sin(angle) * radius, 0.0F, -angle + (float) Math.PI / 2, 0.0F));
		}
		return LayerDefinition.create(mesh, 16, 16).bakeRoot();
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
		if (WavyCapeRenderer.shouldRender(state)) {
			wavyCape.render(getParentModel(), poseStack, collector, light, state);
		}

		WingsCosmetic wingsModule = CosmeticModule.get(WingsCosmetic.class);
		if (wingsModule != null && wingsModule.shouldRender(state)) {
			renderWings(poseStack, collector, light, state, wingsModule);
		}

		PetCosmetic petModule = CosmeticModule.get(PetCosmetic.class);
		if (petModule != null && petModule.shouldRender(state)) {
			pet.render(poseStack, collector, light, state, petModule);
		}

		HaloCosmetic haloModule = CosmeticModule.get(HaloCosmetic.class);
		if (haloModule != null && haloModule.shouldRender(state)) {
			renderHalo(poseStack, collector, light, state, haloModule);
		}
	}

	private void renderWings(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, WingsCosmetic module) {
		float time = state.ageInTicks;
		boolean fast = state.fallFlyingTimeInTicks > 0 || state.walkAnimationSpeed > 0.8F;
		float flap = module.flap.get() ? Mth.sin(time * (fast ? 0.45F : 0.12F)) * (fast ? 0.35F : 0.12F) : 0.0F;

		ModelPart left = wings.getChild("left");
		ModelPart right = wings.getChild("right");
		ModelPart leftTip = left.getChild("tip");
		ModelPart rightTip = right.getChild("tip");
		left.yRot = -0.5F - flap;
		left.zRot = -0.12F;
		right.yRot = 0.5F + flap;
		right.zRot = 0.12F;
		// La punta se dobla hacia atrás y se mueve algo más que la base, como un ala de verdad.
		leftTip.yRot = -0.3F - flap * 0.8F;
		rightTip.yRot = 0.3F + flap * 0.8F;

		poseStack.pushPose();
		getParentModel().body.translateAndRotate(poseStack);
		float size = module.size.getFloat();
		poseStack.scale(size, size, size);
		collector.submitModelPart(wings, poseStack, RenderTypes.entityCutoutNoCull(WINGS_TEXTURE), light, OverlayTexture.NO_OVERLAY, null);
		poseStack.popPose();
	}

	private void renderHalo(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, HaloCosmetic module) {
		float time = state.ageInTicks;
		float bob = module.spin.get() ? Mth.sin(time * 0.08F) * 0.6F : 0.0F;
		halo.yRot = module.spin.get() ? time * 0.03F : 0.0F;
		halo.y = -8.0F - module.height.getFloat() - bob;

		poseStack.pushPose();
		getParentModel().head.translateAndRotate(poseStack);
		collector.submitModelPart(halo, poseStack, RenderTypes.entityCutoutNoCull(HALO_TEXTURE), light, OverlayTexture.NO_OVERLAY, null);
		poseStack.popPose();
	}
}
