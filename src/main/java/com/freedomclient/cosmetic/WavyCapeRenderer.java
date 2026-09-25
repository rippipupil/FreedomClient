package com.freedomclient.cosmetic;

import com.freedomclient.module.visual.WavyCapesModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * Capa en 16 tiras de 1 píxel encadenadas: cada tira gira un poco respecto a la anterior,
 * así la capa se curva hacia atrás al moverse y ondea. Usa la misma textura y posición que la capa de vanilla.
 */
public final class WavyCapeRenderer {
	private static final int SEGMENTS = 16;
	private static final float DEGREES = Mth.DEG_TO_RAD;

	private final ModelPart root;
	private final ModelPart[] segments = new ModelPart[SEGMENTS];

	public WavyCapeRenderer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition parent = mesh.getRoot();
		for (int i = 0; i < SEGMENTS; i++) {
			// Cada tira usa la fila i de la textura de capa (64x32): cara delantera en (1, 1+i) y trasera en (12, 1+i).
			parent = parent.addOrReplaceChild("segment" + i, CubeListBuilder.create().texOffs(0, i).addBox(-5.0F, 0.0F, -1.0F, 10, 1, 1),
					PartPose.offset(0.0F, i == 0 ? 0.0F : 1.0F, 0.0F));
		}
		root = LayerDefinition.create(mesh, 64, 32).bakeRoot();
		ModelPart part = root;
		for (int i = 0; i < SEGMENTS; i++) {
			part = part.getChild("segment" + i);
			segments[i] = part;
		}
	}

	/** Si hay que dibujar la capa de este jugador (mismas condiciones que la capa de vanilla). */
	public static boolean shouldRender(AvatarRenderState state) {
		return WavyCapesModule.active() != null && !state.isInvisible && state.showCape && state.skin != null
				&& state.skin.cape() != null && !state.chestEquipment.has(DataComponents.GLIDER);
	}

	public void render(PlayerModel parentModel, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		WavyCapesModule module = WavyCapesModule.active();
		if (module == null) return;

		// Ángulos de la capa de vanilla (PlayerCapeModel): inclinación hacia atrás y hacia los lados.
		float lean = 6.0F + state.capeLean / 2.0F + state.capeFlap;
		float side = state.capeLean2 / 2.0F;
		float extra = lean - 6.0F;
		float movement = Mth.clamp(state.capeLean / 60.0F, 0.0F, 1.0F);
		float time = state.ageInTicks * 0.25F * module.speed.getFloat();
		float amplitude = (1.5F + movement * 5.0F) * module.wind.getFloat();

		// La parte de arriba se inclina menos y el resto se reparte por las tiras, de más a menos rígido.
		float rootAngle = 6.0F + extra * 0.3F;
		float bend = extra * 0.7F;
		float weights = SEGMENTS * (SEGMENTS - 1) / 2.0F;
		for (int i = 0; i < SEGMENTS; i++) {
			float angle = bend * i / weights + Mth.sin(time - i * 0.45F) * amplitude * i / SEGMENTS;
			// La capa está girada 180º, así que un giro negativo en X la curva hacia atrás.
			segments[i].xRot = -angle * DEGREES;
		}

		poseStack.pushPose();
		parentModel.body.translateAndRotate(poseStack);
		if (state.chestEquipment.has(DataComponents.EQUIPPABLE)) {
			poseStack.translate(0.0F, -0.053125F, 0.06875F);
		}
		poseStack.translate(0.0F, 0.0F, 2.0F / 16.0F);
		poseStack.mulPose(new Quaternionf().rotationX(rootAngle * DEGREES).rotateZ(side * DEGREES).rotateY((180.0F - side) * DEGREES));
		collector.submitModelPart(root, poseStack, RenderTypes.entitySolid(state.skin.cape().texturePath()), light, OverlayTexture.NO_OVERLAY, null);
		poseStack.popPose();
	}
}
