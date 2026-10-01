package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.CosmeticSlot;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/**
 * Black Hole Backpack: un agujero negro en miniatura metido en un anillo de contención a la espalda. Esfera negra
 * con su anillo de fotones, un disco de acreción que gira inclinado (más caliente por dentro), el halo curvado por
 * encima y trocitos de materia que caen en espiral hacia el centro. El anillo lleva cuatro emisores que laten.
 * Dos estilos: Gargantua (dorado y naranja) y Void (cian y violeta).
 */
public class BlackHoleBackpackCosmetic extends VoxCosmetic {
	private static final Vox.Palette GARGANTUA = palette("black_hole_gargantua", 0xFFFFFDF0, 0xFFFFE08A, 0xFFFF9A2A, 0xFFB8401E, 0xFF7CF5FF);
	private static final Vox.Palette VOID = palette("black_hole_void", 0xFFF4FAFF, 0xFF9FE8FF, 0xFF8A5CFF, 0xFF3A1E9E, 0xFFFF7AD9);

	public final ModeSetting style = add(new ModeSetting("Style", "Gargantua: gold and orange disk. Void: cyan and violet disk.",
			"Gargantua", "Gargantua", "Void"));
	public final NumberSetting size = add(new NumberSetting("Size", "How big the black hole is.", 0.9, 0.6, 1.2, 0.05, "x"));
	public final NumberSetting spin = add(new NumberSetting("Spin speed", "How fast the accretion disk turns.", 5, 1, 10, 1));

	/** Piezas de cada estilo: arnés y anillo, núcleo negro, anillo de fotones, disco, halo y una mota de materia. */
	private static final class Parts {
		Vox.Shape harness;
		Vox.Shape frame;
		Vox.Shape emitters;
		Vox.Shape core;
		Vox.Shape photonRing;
		Vox.Shape disk;
		Vox.Shape halo;
		Vox.Shape mote;
	}

	private Parts gargantua;
	private Parts voidParts;

	public BlackHoleBackpackCosmetic() {
		super("Black Hole Backpack", "A tiny black hole held in a containment ring on your back: spinning accretion disk, glowing halo "
				+ "and bits of matter spiralling in. Gargantua (gold) or Void (violet) style.", CosmeticSlot.BACK);
	}

	/** w = blanco ardiente, y = interior del disco, o = medio, r = exterior, e = emisores; el resto es fijo. */
	private static Vox.Palette palette(String name, int hot, int inner, int middle, int outer, int emitter) {
		return new Vox.Palette(name,
				'k', 0xFF020204, 'K', 0xFF0E0A16, 'w', hot, 'y', inner, 'o', middle, 'r', outer, 'e', emitter,
				'm', 0xFF4A4458, 'M', 0xFF2C2836, 'l', 0xFF3A3442, 'L', 0xFF24202C);
	}

	private static Parts build(Vox.Palette palette) {
		Parts p = new Parts();
		// Arnés por los hombros y placa de la espalda con el soporte del anillo.
		p.harness = new Vox.Shape(palette)
				.box('l', -3.8F, -0.1F, -2.3F, 1.6F, 7.0F, 0.4F)
				.box('l', 2.2F, -0.1F, -2.3F, 1.6F, 7.0F, 0.4F)
				.box('l', -3.8F, -0.4F, -2.3F, 1.6F, 0.4F, 4.8F)
				.box('l', 2.2F, -0.4F, -2.3F, 1.6F, 0.4F, 4.8F)
				.box('L', -3.2F, 2.6F, 2.1F, 6.4F, 7.0F, 0.9F)
				.box('M', -1.2F, 5.2F, 3.0F, 2.4F, 2.0F, 3.2F);
		// Anillo de contención vertical (mirando hacia atrás) alrededor del agujero negro.
		p.frame = new Vox.Shape(palette)
				.ring('m', 0.0F, -0.5F, 0.0F, 7.0F, 6.0F, 1.0F)
				.ring('M', 0.0F, 0.5F, 0.0F, 6.6F, 6.0F, 0.6F);
		p.emitters = new Vox.Shape(palette)
				.box('e', -0.7F, -1.0F, -7.4F, 1.4F, 1.6F, 1.4F)
				.box('e', -0.7F, -1.0F, 6.0F, 1.4F, 1.6F, 1.4F)
				.box('e', -7.4F, -1.0F, -0.7F, 1.4F, 1.6F, 1.4F)
				.box('e', 6.0F, -1.0F, -0.7F, 1.4F, 1.6F, 1.4F);
		p.core = new Vox.Shape(palette).sphere(0.0F, 0.0F, 0.0F, 3.0F, dy -> dy == 0 ? 'K' : 'k');
		p.photonRing = new Vox.Shape(palette).ring('w', 0.0F, -0.3F, 0.0F, 3.7F, 3.0F, 0.6F);
		// Disco de acreción: blanco por dentro, luego el color del estilo, y oscuro por fuera.
		p.disk = new Vox.Shape(palette)
				.ring('w', 0.0F, -0.25F, 0.0F, 4.6F, 3.6F, 0.5F)
				.ring('y', 0.0F, -0.25F, 0.0F, 5.8F, 4.6F, 0.5F)
				.ring('o', 0.0F, -0.2F, 0.0F, 7.0F, 5.8F, 0.4F)
				.ring('r', 0.0F, -0.15F, 0.0F, 8.2F, 7.0F, 0.3F);
		// El halo: la luz del disco curvada por encima y por debajo del agujero.
		p.halo = new Vox.Shape(palette)
				.ring('y', 0.0F, -0.3F, 0.0F, 4.7F, 3.8F, 0.6F)
				.ring('o', 0.0F, -0.2F, 0.0F, 5.3F, 4.7F, 0.4F);
		p.mote = new Vox.Shape(palette).box('y', -0.35F, -0.35F, -0.35F, 0.7F, 0.7F, 0.7F);
		return p;
	}

	private Parts parts() {
		if (style.is("Void")) {
			if (voidParts == null) voidParts = build(VOID);
			return voidParts;
		}
		if (gargantua == null) gargantua = build(GARGANTUA);
		return gargantua;
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		Parts p = parts();
		float time = state.ageInTicks;
		poseStack.pushPose();
		parent.body.translateAndRotate(poseStack);
		poseStack.pushPose();
		if (!state.chestEquipment.isEmpty()) {
			// Con peto, el arnés se agranda para quedar por fuera.
			poseStack.translate(0.0F, -0.6F / 16.0F, 0.0F);
			poseStack.scale(1.25F, 1.0F, 1.45F);
		}
		p.harness.draw(poseStack, collector, light);
		poseStack.popPose();

		// Centro del agujero negro, detrás del soporte.
		poseStack.translate(0.0F, 6.2F / 16.0F, (10.0F + backClearance(state)) / 16.0F);
		float s = size.getFloat() * 0.62F;
		poseStack.scale(s, s, s);
		// Flota un poco arriba y abajo dentro del anillo.
		poseStack.translate(0.0F, Mth.sin(time * 0.09F) * 0.4F / 16.0F, 0.0F);

		// Anillo de contención, de pie y mirando hacia atrás, con los emisores latiendo.
		poseStack.pushPose();
		poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
		p.frame.draw(poseStack, collector, light);
		float beat = 1.0F + 0.12F * Mth.sin(time * 0.3F);
		poseStack.scale(beat, 1.0F, beat);
		p.emitters.drawGlow(poseStack, collector);
		poseStack.popPose();

		p.core.draw(poseStack, collector, light);

		// Anillo de fotones y halo: de pie, mirando hacia atrás (hacia quien te ve por la espalda).
		poseStack.pushPose();
		poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
		p.photonRing.drawGlow(poseStack, collector);
		poseStack.translate(0.0F, 0.3F / 16.0F, 0.0F);
		poseStack.scale(1.0F, 1.0F, 0.82F);
		p.halo.drawGlow(poseStack, collector);
		poseStack.popPose();

		// Disco de acreción girando, inclinado como en las fotos de los agujeros negros.
		poseStack.pushPose();
		poseStack.mulPose(Axis.XP.rotationDegrees(14.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F));
		poseStack.mulPose(Axis.YP.rotationDegrees(time * spin.getFloat() * 1.2F));
		p.disk.drawGlow(poseStack, collector);
		poseStack.popPose();

		// Motas de materia que caen en espiral, cada vez más pequeñas, hasta desaparecer en el horizonte.
		for (int i = 0; i < 8; i++) {
			float phase = ((time * 0.012F * spin.getFloat()) + i / 8.0F) % 1.0F;
			float radius = 3.4F + (1.0F - phase) * 7.0F;
			float angle = (i * 45.0F + phase * 540.0F) * Mth.DEG_TO_RAD;
			poseStack.pushPose();
			poseStack.mulPose(Axis.XP.rotationDegrees(14.0F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F));
			poseStack.translate(Mth.cos(angle) * radius / 16.0F, Mth.sin(angle * 2.0F) * 0.4F / 16.0F, Mth.sin(angle) * radius / 16.0F);
			float mote = 0.4F + (1.0F - phase) * 0.9F;
			poseStack.scale(mote, mote, mote);
			poseStack.mulPose(Axis.YP.rotationDegrees(time * 12.0F + i * 40.0F));
			p.mote.drawGlow(poseStack, collector);
			poseStack.popPose();
		}
		poseStack.popPose();
	}
}
