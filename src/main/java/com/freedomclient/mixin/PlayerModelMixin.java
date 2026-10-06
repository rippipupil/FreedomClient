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
	 * Emote de la guitarra: el brazo derecho rasguea en bucle sobre las cuerdas al ritmo de la canción, la mano
	 * izquierda va al mástil y cambia de acorde, la cabeza mira la guitarra y cabecea, y el cuerpo se mece un poco.
	 */
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void freedomclient$guitarEmote(AvatarRenderState state, CallbackInfo ci) {
		if (!GuitarEmote.isPlaying() || CosmeticPreview.of(state) != null || !GuitarEmote.isLocal(state)) return;
		PlayerModel model = (PlayerModel) (Object) this;
		float strum = GuitarEmote.strum();
		float beats = GuitarEmote.beats();
		// Mano derecha sobre las cuerdas, delante de la cadera: baja y sube rasgueando en bucle, a cada pulso.
		model.rightArm.xRot = -0.72F + strum * 0.42F;
		model.rightArm.yRot = -0.45F + strum * 0.08F;
		model.rightArm.zRot = 0.05F;
		// Mano izquierda adelantada sobre el mástil; cambia de acorde cada dos pulsos.
		float chord = Mth.sin(beats * 0.5F * Mth.PI);
		model.leftArm.xRot = -1.28F + chord * 0.05F;
		model.leftArm.yRot = -0.12F + chord * 0.1F;
		model.leftArm.zRot = 0.0F;
		// Mira la guitarra y cabecea con el ritmo; el cuerpo se mece un poco.
		model.head.xRot = 0.34F + Math.abs(strum) * 0.1F;
		model.head.yRot = 0.15F;
		model.hat.xRot = model.head.xRot;
		model.hat.yRot = model.head.yRot;
		model.body.yRot = chord * 0.05F;
	}
}
