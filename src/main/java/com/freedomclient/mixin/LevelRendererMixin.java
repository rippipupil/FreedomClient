package com.freedomclient.mixin;

import com.freedomclient.module.visual.BlockOutlineModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int freedomclient$outlineColor(int color) {
		return BlockOutlineModule.color(color);
	}

	@ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float freedomclient$outlineWidth(float width) {
		return BlockOutlineModule.width(width);
	}

	/** Block Outline: tiñe el bloque al que apuntas con un relleno translúcido del color elegido. */
	@Inject(method = "renderBlockOutline", at = @At("TAIL"))
	private void freedomclient$fillBlock(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, boolean translucent,
			LevelRenderState levelState, CallbackInfo ci) {
		BlockOutlineRenderState outline = levelState.blockOutlineRenderState;
		int color = BlockOutlineModule.fillColor();
		if (outline == null || color == 0 || outline.isTranslucent() != translucent) return;

		Vec3 camera = levelState.cameraRenderState.pos;
		BlockPos pos = outline.pos();
		ClientLevel level = Minecraft.getInstance().level;
		VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.debugQuads());
		PoseStack.Pose pose = poseStack.last();
		for (AABB box : outline.shape().toAabbs()) {
			// Un poco más grande que el bloque para que no parpadee contra sus caras.
			AABB b = box.move(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z).inflate(0.002);
			for (Direction face : Direction.values()) {
				// Este relleno no usa la profundidad: solo se dibujan las caras que miran a la cámara
				// y que no están pegadas a un bloque opaco, que son las que se ven de verdad.
				if (!freedomclient$facesCamera(b, face)) continue;
				if (level != null && freedomclient$onBlockEdge(box, face)
						&& level.getBlockState(pos.relative(face)).isSolidRender()) continue;
				freedomclient$face(consumer, pose, b, face, color);
			}
		}
		bufferSource.endLastBatch();
	}

	/** La cámara está en el origen: la cara se ve si está delante de ella según su normal. */
	@Unique
	private static boolean freedomclient$facesCamera(AABB b, Direction face) {
		return switch (face) {
			case DOWN -> b.minY > 0;
			case UP -> b.maxY < 0;
			case NORTH -> b.minZ > 0;
			case SOUTH -> b.maxZ < 0;
			case WEST -> b.minX > 0;
			case EAST -> b.maxX < 0;
		};
	}

	/** Si la cara de esta caja coincide con el borde del bloque (si no, está dentro de él y no la tapa el vecino). */
	@Unique
	private static boolean freedomclient$onBlockEdge(AABB box, Direction face) {
		return switch (face) {
			case DOWN -> box.minY <= 0.0;
			case UP -> box.maxY >= 1.0;
			case NORTH -> box.minZ <= 0.0;
			case SOUTH -> box.maxZ >= 1.0;
			case WEST -> box.minX <= 0.0;
			case EAST -> box.maxX >= 1.0;
		};
	}

	@Unique
	private static void freedomclient$face(VertexConsumer c, PoseStack.Pose p, AABB b, Direction face, int color) {
		float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
		float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
		switch (face) {
			case DOWN -> freedomclient$quad(c, p, color, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
			case UP -> freedomclient$quad(c, p, color, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
			case NORTH -> freedomclient$quad(c, p, color, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
			case SOUTH -> freedomclient$quad(c, p, color, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
			case WEST -> freedomclient$quad(c, p, color, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
			case EAST -> freedomclient$quad(c, p, color, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
		}
	}

	@Unique
	private static void freedomclient$quad(VertexConsumer c, PoseStack.Pose p, int color, float... v) {
		for (int i = 0; i < 12; i += 3) {
			c.addVertex(p, v[i], v[i + 1], v[i + 2]).setColor(color);
		}
	}
}
