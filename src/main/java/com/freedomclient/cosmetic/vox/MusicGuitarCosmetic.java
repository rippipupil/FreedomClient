package com.freedomclient.cosmetic.vox;

import com.freedomclient.FreedomClient;
import com.freedomclient.cosmetic.CosmeticPreview;
import com.freedomclient.cosmetic.CosmeticSlot;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.KeybindSetting;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.lwjgl.glfw.GLFW;

/**
 * Guitarra con música: en tercera persona suena su canción bajita y con la tecla de tocar (X) el jugador se la pone
 * delante y la toca, con la canción desde el principio a volumen normal (ver {@link GuitarEmote} y {@link GuitarMusic}).
 * Solo se puede llevar una guitarra a la vez.
 */
public abstract class MusicGuitarCosmetic extends VoxCosmetic {
	public final KeybindSetting playKey = add(new KeybindSetting("Play key", "Key to take the guitar and play it (and to stop).", GLFW.GLFW_KEY_X));
	public final BooleanSetting music = add(new BooleanSetting("Music in third person",
			"Its song plays quietly while you look at yourself in third person.", true));
	public final NumberSetting idleVolume = add(new NumberSetting("Quiet volume", "Volume of the song in third person.", 25, 0, 100, 5, "%"));
	public final NumberSetting playVolume = add(new NumberSetting("Play volume", "Volume of the song while you play the guitar.", 100, 0, 100, 5, "%"));

	protected MusicGuitarCosmetic(String name, String description) {
		super(name, description, CosmeticSlot.BACK);
	}

	/** Sonido de su canción en sounds.json (sin el espacio de nombres), o null si no tiene. */
	public String song() {
		return null;
	}

	/** Título que sale al empezar a tocar. */
	public String songTitle() {
		return null;
	}

	/** Pulsos por minuto: marcan el rasgueo y las notas mientras se toca. */
	public float bpm() {
		return 110.0F;
	}

	@Override
	protected void onEnable(Minecraft client) {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null) return;
		for (MusicGuitarCosmetic other : manager.ofType(MusicGuitarCosmetic.class)) {
			if (other != this) other.setEnabled(false);
		}
	}

	/** La guitarra que llevas puesta, o null. */
	public static MusicGuitarCosmetic active() {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null) return null;
		for (MusicGuitarCosmetic guitar : manager.ofType(MusicGuitarCosmetic.class)) {
			if (guitar.isEnabled()) return guitar;
		}
		return null;
	}

	/** Si en este dibujado la guitarra va en las manos (tu jugador tocando, nunca en la vista previa del menú). */
	protected static boolean inHands(AvatarRenderState state) {
		return GuitarEmote.isPlaying() && CosmeticPreview.of(state) == null && GuitarEmote.isLocal(state);
	}

	/**
	 * Coloca la guitarra delante del pecho para tocarla: la cara hacia fuera, el cuerpo a la altura de la cadera
	 * derecha y el mástil subiendo hacia la mano izquierda. {@code frontDepth} es lo que sobresale la guitarra hacia
	 * el cuerpo desde su plano central (como en {@link #onBackDiagonal}), ya multiplicado por {@code depthScale}.
	 * Rebota un poco con cada rasgueo.
	 */
	protected static void inHands(PlayerModel parent, PoseStack poseStack, AvatarRenderState state, float scale, float depthScale, float frontDepth) {
		parent.body.translateAndRotate(poseStack);
		float bounce = GuitarEmote.strum() * 0.25F;
		poseStack.translate(-1.0F / 16.0F, (8.0F + bounce) / 16.0F, -(2.1F + backClearance(state) + frontDepth * depthScale) / 16.0F);
		poseStack.scale(scale, scale, depthScale);
		// Vuelta para que la cara de la guitarra (hacia +z en el modelo) mire hacia fuera del pecho.
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
		// El mástil sube hacia el lado izquierdo del jugador, casi horizontal, como al tocar.
		poseStack.mulPose(Axis.ZP.rotationDegrees(-62.0F + GuitarEmote.strum() * 1.5F));
	}
}
