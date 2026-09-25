package com.freedomclient.mixin;

import com.freedomclient.FreedomClient;
import com.freedomclient.cosmetic.CapeCosmetic;
import com.freedomclient.cosmetic.CosmeticModule;
import com.freedomclient.module.visual.CapesModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
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

	/** Pone la capa de FreedomClient en tu jugador y las capas de OptiFine (Capes) en quien no tenga capa. */
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void freedomclient$cape(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
		if (state.skin == null) return;
		CapeCosmetic cape = CosmeticModule.get(CapeCosmetic.class);
		PlayerSkin skin = state.skin;
		if (cape != null && cape.isEnabled() && entity == Minecraft.getInstance().player) {
			state.skin = new PlayerSkin(skin.body(), FREEDOM_CAPE, skin.elytra(), skin.model(), skin.secure());
			state.showCape = true;
			return;
		}

		if (skin.cape() == null && entity instanceof AbstractClientPlayer player) {
			ClientAsset.Texture optifine = CapesModule.capeFor(player.getName().getString());
			if (optifine != null) {
				state.skin = new PlayerSkin(skin.body(), optifine, skin.elytra(), skin.model(), skin.secure());
			}
		}
	}
}
