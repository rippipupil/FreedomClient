package com.freedomclient.mixin;

import com.freedomclient.cosmetic.CosmeticPreview;
import com.freedomclient.cosmetic.vox.GuitarEmote;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class PlayerModelMixin {
	/**
	 * Emote de la guitarra: el brazo derecho rasguea en bucle sobre las cuerdas al ritmo de la canción, la mano
	 * izquierda va al mástil y cambia de acorde y la cabeza mira la guitarra. Cuánto se mueve depende de la guitarra
	 * ({@link GuitarEmote#motion()}): las canciones lentas apenas mueven el brazo. El cuerpo no gira, para que las
	 * capas exteriores de la skin (chaqueta, mangas, sombrero) sigan pegadas a su parte.
	 */
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void freedomclient$guitarEmote(AvatarRenderState state, CallbackInfo ci) {
		if (!GuitarEmote.isPlaying() || CosmeticPreview.of(state) != null || !GuitarEmote.isLocal(state)) return;
		PlayerModel model = (PlayerModel) (Object) this;
		float motion = GuitarEmote.motion();
		float strum = GuitarEmote.strum() * motion;
		float beats = GuitarEmote.beats();
		// Mano derecha sobre las cuerdas, delante de la cadera: baja y sube rasgueando en bucle, a cada pulso.
		model.rightArm.xRot = -0.72F + strum * 0.42F;
		model.rightArm.yRot = -0.45F + strum * 0.08F;
		model.rightArm.zRot = 0.05F;
		// Mano izquierda adelantada sobre el mástil; cambia de acorde cada dos pulsos.
		float chord = Mth.sin(beats * 0.5F * Mth.PI) * motion;
		model.leftArm.xRot = -1.28F + chord * 0.05F;
		model.leftArm.yRot = -0.12F + chord * 0.1F;
		model.leftArm.zRot = 0.0F;
		// Mira la guitarra y cabecea con el ritmo.
		model.head.xRot = 0.34F + Math.abs(strum) * 0.1F;
		model.head.yRot = 0.15F;
		model.head.zRot = 0.0F;
		model.body.xRot = 0.0F;
		model.body.yRot = 0.0F;
		model.body.zRot = 0.0F;
		// Las capas exteriores son hijas de su parte (y ya la siguen): copiarles el giro lo sumaría dos veces y las
		// despegaría. Solo se copian si en esta versión fueran partes sueltas.
		freedomclient$follow(model.head, model.hat, "hat");
		freedomclient$follow(model.body, model.jacket, "jacket");
		freedomclient$follow(model.rightArm, model.rightSleeve, "right_sleeve");
		freedomclient$follow(model.leftArm, model.leftSleeve, "left_sleeve");
	}

	@Unique
	private static void freedomclient$follow(ModelPart part, ModelPart layer, String name) {
		if (part.hasChild(name)) {
			layer.resetPose();
		} else {
			layer.x = part.x;
			layer.y = part.y;
			layer.z = part.z;
			layer.xRot = part.xRot;
			layer.yRot = part.yRot;
			layer.zRot = part.zRot;
		}
	}
}
