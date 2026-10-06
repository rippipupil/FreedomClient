package com.freedomclient.mixin;

import com.freedomclient.cosmetic.ClientCapeCosmetic;
import com.freedomclient.module.visual.CapesModule;
import com.freedomclient.module.visual.HideArmorModule;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
	/** Pone la capa de FreedomClient en tu jugador y las capas de OptiFine (Capes) en quien no tenga capa. */
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void freedomclient$cape(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
		// Hide Armor con la élitra oculta: se quita también del estado, así la capa vuelve a verse (con élitra puesta
		// Minecraft no dibuja la capa) y los cosméticos de la espalda no se apartan para dejarle sitio.
		if (state.chestEquipment.has(DataComponents.GLIDER) && HideArmorModule.hidesElytra(state)) state.chestEquipment = ItemStack.EMPTY;
		// Tocando la guitarra, las manos van en la guitarra: no se dibuja lo que lleves en ellas.
		if (com.freedomclient.cosmetic.vox.GuitarEmote.isPlaying() && entity == Minecraft.getInstance().player) {
			state.rightHandItemState.clear();
			state.leftHandItemState.clear();
			state.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
			state.leftArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
		}
		if (state.skin == null) return;
		ClientCapeCosmetic cape = ClientCapeCosmetic.active();
		PlayerSkin skin = state.skin;
		if (cape != null && entity == Minecraft.getInstance().player) {
			state.skin = new PlayerSkin(skin.body(), cape.texture(), skin.elytra(), skin.model(), skin.secure());
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
