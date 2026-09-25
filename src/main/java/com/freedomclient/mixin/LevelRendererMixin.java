package com.freedomclient.mixin;

import com.freedomclient.module.visual.BlockOutlineModule;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
}
