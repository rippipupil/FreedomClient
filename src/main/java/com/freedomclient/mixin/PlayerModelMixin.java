package com.freedomclient.mixin;

import com.freedomclient.cosmetic.CosmeticPreview;
import com.freedomclient.cosmetic.vox.GuitarEmote;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class PlayerModelMixin {
	/**
	 * Emote de la guitarra: el brazo derecho rasguea al ritmo sobre el cuerpo de la guitarra, la mano izquierda va al
	 * mástil y se desliza, la cabeza mira la guitarra y cabecea, y el cuerpo se mece un poco.
	 */
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void freedomclient$guitarEmote(AvatarRenderState state, CallbackInfo ci) {
		if (!GuitarEmote.isPlaying() || CosmeticPreview.of(state) != null || !GuitarEmote.isLocal(state)) return;
		PlayerModel model = (PlayerModel) (Object) this;
		float strum = GuitarEmote.strum();
		float beats = GuitarEmote.beats();
		model.rightArm.xRot = -0.55F + strum * 0.32F;
		model.rightArm.yRot = -0.3F;
		model.rightArm.zRot = 0.08F;
		model.leftArm.xRot = -1.2F + Mth.sin(beats * 0.5F * Mth.PI) * 0.06F;
		model.leftArm.yRot = 0.62F + Mth.sin(beats * 0.25F * Mth.PI) * 0.12F;
		model.leftArm.zRot = -0.12F;
		model.head.xRot = 0.32F + Math.abs(strum) * 0.1F;
		model.head.yRot = 0.18F;
		model.hat.xRot = model.head.xRot;
		model.hat.yRot = model.head.yRot;
		model.body.yRot = Mth.sin(beats * 0.5F * Mth.PI) * 0.06F;
	}
}
