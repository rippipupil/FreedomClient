package com.freedomclient.mixin;

import com.freedomclient.module.visual.BlockOutlineModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
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
		VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.debugQuads());
		PoseStack.Pose pose = poseStack.last();
		for (AABB box : outline.shape().toAabbs()) {
			// Un poco más grande que el bloque para que no parpadee contra sus caras.
			AABB b = box.move(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z).inflate(0.002);
			freedomclient$box(consumer, pose, (float) b.minX, (float) b.minY, (float) b.minZ, (float) b.maxX, (float) b.maxY, (float) b.maxZ, color);
		}
		bufferSource.endLastBatch();
	}

	@Unique
	private static void freedomclient$box(VertexConsumer c, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
		freedomclient$quad(c, p, color, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
		freedomclient$quad(c, p, color, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
		freedomclient$quad(c, p, color, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
		freedomclient$quad(c, p, color, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		freedomclient$quad(c, p, color, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
		freedomclient$quad(c, p, color, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
	}

	@Unique
	private static void freedomclient$quad(VertexConsumer c, PoseStack.Pose p, int color, float... v) {
		for (int i = 0; i < 12; i += 3) {
			c.addVertex(p, v[i], v[i + 1], v[i + 2]).setColor(color);
		}
	}
}
