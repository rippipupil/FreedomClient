package com.freedomclient.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Vista previa de un cosmético en su tarjeta del menú: tu jugador dibujado en pequeño llevando solo ese cosmético
 * (aunque esté apagado), con el encuadre de su sitio (cabeza para sombreros, espalda para capas y alas...).
 * El juego dibuja las entidades del menú al final del fotograma, así que el cosmético se apunta en el propio
 * estado de render y las capas lo miran al dibujar.
 */
public final class CosmeticPreview {
	private static final Map<AvatarRenderState, CosmeticModule> PREVIEWS = Collections.synchronizedMap(new WeakHashMap<>());
	/** Cosmético de la vista previa que se está dibujando ahora mismo (entre begin y end de la capa), o null. */
	private static CosmeticModule drawing;

	private CosmeticPreview() {
	}

	/** Si este cosmético se puede enseñar en el modelo (los rastros y efectos de partículas no). */
	public static boolean supports(CosmeticModule cosmetic) {
		return cosmetic.getSlot() != CosmeticSlot.TRAIL && cosmetic.getSlot() != CosmeticSlot.EFFECT;
	}

	/** El cosmético de la vista previa de este estado, o null si es un render normal del juego. */
	public static CosmeticModule of(AvatarRenderState state) {
		return PREVIEWS.get(state);
	}

	/** Llamado por la capa de cosméticos al empezar y acabar de dibujar un jugador. */
	public static void begin(AvatarRenderState state) {
		drawing = PREVIEWS.get(state);
	}

	public static void end() {
		drawing = null;
	}

	/** Si se está dibujando una vista previa (las mascotas se ponen entonces en un sitio fijo). */
	public static boolean drawing() {
		return drawing != null;
	}

	/** Sitio fijo de una mascota en la vista previa, en el espacio del modelo del jugador. */
	public static Vec3 petSlot() {
		return new Vec3(-12.0 / 16.0, 2.0 / 16.0, 0.0);
	}

	/**
	 * Dibuja la vista previa en el recuadro (x0, y0)-(x1, y1). Devuelve false si no se puede (sin mundo o cosmético
	 * de partículas) para que la tarjeta ponga su icono.
	 */
	public static boolean render(GuiGraphics graphics, CosmeticModule cosmetic, int x0, int y0, int x1, int y1) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || !supports(cosmetic)) return false;
		AvatarRenderer<AbstractClientPlayer> renderer = client.getEntityRenderDispatcher().getPlayerRenderer(player);
		AvatarRenderState state = renderer.createRenderState(player, 1.0F);

		CosmeticSlot slot = cosmetic.getSlot();
		boolean back = slot == CosmeticSlot.CAPE || slot == CosmeticSlot.WINGS || slot == CosmeticSlot.BACK;
		// Tres cuartos: se ve de frente (o de espaldas) y un poco de lado, girando despacio.
		float sway = (float) Math.sin(System.currentTimeMillis() / 1400.0) * 18.0F;
		state.bodyRot = (back ? 0.0F : 180.0F) + 28.0F + sway;
		state.yRot = 0.0F;
		state.xRot = 0.0F;
		state.boundingBoxWidth /= state.scale;
		state.boundingBoxHeight /= state.scale;
		state.scale = 1.0F;
		state.lightCoords = 0xF000F0;
		state.isCrouching = false;
		if (cosmetic instanceof CapeCosmetic cape && state.skin != null) {
			PlayerSkin skin = state.skin;
			state.skin = new PlayerSkin(skin.body(), cape.texture(), skin.elytra(), skin.model(), skin.secure());
			state.showCape = true;
		} else {
			state.showCape = false;
		}
		PREVIEWS.put(state, cosmetic);

		// Encuadre: altura del cuerpo que queda en el centro y tamaño (px por bloque) según dónde va el cosmético.
		int boxH = y1 - y0;
		float focus;
		float zoom;
		switch (slot) {
			case HAT -> {
				focus = 1.55F;
				zoom = 2.1F;
			}
			case NECK -> {
				focus = 1.35F;
				zoom = 1.6F;
			}
			case PET -> {
				focus = 1.0F;
				zoom = 0.72F;
			}
			default -> {
				focus = 1.05F;
				zoom = 0.95F;
			}
		}
		float scale = boxH / 1.9F * zoom;
		Quaternionf camera = new Quaternionf().rotateX(8.0F * (float) (Math.PI / 180.0));
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(camera);
		graphics.submitEntityRenderState(state, scale, new Vector3f(0.0F, focus, 0.0F), rotation, camera, x0, y0, x1, y1);
		return true;
	}
}
