package com.freedomclient.mixin;

import com.freedomclient.FreedomClient;
import com.freedomclient.cosmetic.CapeCosmetic;
import com.freedomclient.cosmetic.CosmeticModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
	@Unique
	private static final ClientAsset.Texture FREEDOM_CAPE = new ClientAsset.ResourceTexture(
			FreedomClient.id("cosmetic/cape"), FreedomClient.id("textures/cosmetic/cape.png"));

	/** Pone la capa de FreedomClient en tu jugador (usa el render y la física de capa de vanilla). */
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void freedomclient$cape(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
		CapeCosmetic cape = CosmeticModule.get(CapeCosmetic.class);
		if (cape == null || !cape.isEnabled() || entity != Minecraft.getInstance().player || state.skin == null) return;

		PlayerSkin skin = state.skin;
		state.skin = new PlayerSkin(skin.body(), FREEDOM_CAPE, skin.elytra(), skin.model(), skin.secure());
		state.showCape = true;
	}
}
