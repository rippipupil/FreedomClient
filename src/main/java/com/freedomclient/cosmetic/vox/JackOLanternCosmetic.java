package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.PetBehavior;
import com.freedomclient.setting.ModeSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Jack-o'-Lantern (Halloween): calabaza que vuela a tu lado. Casi siempre está feliz con cara kawaii, pero se vuelve
 * una calabaza tallada de Halloween de verdad (ojos y nariz en triángulo y sonrisa de dientes, huecos por los que
 * se ve la vela brillando dentro) al azar, al pelear o con una espada en la mano, según el ajuste. El cambio parpadea.
 */
public class JackOLanternCosmetic extends FollowPetCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("jack_o_lantern",
			'o', 0xFFF08A24, 'O', 0xFFD06E14, 'g', 0xFF5A7A2A, 'l', 0xFF6FB83A,
			'k', 0xFF3A1A08, 'w', 0xFFFFFFFF, 'p', 0xFFF48AA6, 'y', 0xFFFFC53A, 'Y', 0xFFFF8A1E);
	/** Cara tallada (columnas de x = -4 a 4, filas de y = -7 a -2): '#' es un hueco en la calabaza. */
	private static final String[] CARVED = {
			"..#...#..",
			".###.###.",
			"....#....",
			".#.#.#.#.",
			".#######.",
			"..#.#.#..",
	};
	private static final int CARVED_TOP = -7;

	public final ModeSetting scaryWhen = add(new ModeSetting("Scary when",
			"When the pumpkin turns into a carved jack-o'-lantern: now and then at random, while you fight or hold a sword, or both.",
			"Randomly", "Randomly", "Fighting", "Both"));
	private final RandomSource random = RandomSource.create();
	private Vox.Shape pumpkin;
	private Vox.Shape carved;
	private Vox.Shape candle;
	private Vox.Shape happy;
	private boolean isEvil;
	private boolean randomEvil;
	private int ticksToSwitch = 400;
	private int flicker;

	public JackOLanternCosmetic() {
		super("Jack-o'-Lantern", "Halloween: a flying pumpkin with a cute kawaii face that turns into a glowing carved jack-o'-lantern.");
	}

	@Override
	public void onTick(Minecraft client) {
		super.onTick(client);
		if (flicker > 0) flicker--;
		// Al azar: feliz casi todo el rato (20-50 s) y tallada un ratito (6-12 s).
		if (--ticksToSwitch <= 0) {
			randomEvil = !randomEvil;
			ticksToSwitch = randomEvil ? 120 + random.nextInt(120) : 400 + random.nextInt(600);
		}
		boolean evil = switch (scaryWhen.get()) {
			case "Fighting" -> fighting(client.player);
			case "Both" -> randomEvil || fighting(client.player);
			default -> randomEvil;
		};
		if (evil != isEvil) {
			isEvil = evil;
			flicker = 16;
		}
	}

	/** Con una espada en la mano o si has golpeado a algo hace menos de 3 s. */
	private static boolean fighting(LocalPlayer player) {
		if (player == null) return false;
		if (player.getMainHandItem().is(ItemTags.SWORDS)) return true;
		return player.getLastHurtMob() != null && player.tickCount - player.getLastHurtMobTimestamp() < 60;
	}

	/** Para las pruebas: pone la cara tallada o la feliz sin parpadeo. */
	public void forceEvil(boolean evil) {
		isEvil = evil;
		randomEvil = evil;
		flicker = 0;
		ticksToSwitch = 400;
	}

	/** Gajos verticales de la calabaza; con {@code holes} la capa de delante lleva la cara tallada. */
	private static Vox.Shape pumpkin(boolean holes) {
		Vox.Shape shape = new Vox.Shape(PALETTE);
		for (int x = -4; x <= 4; x++) {
			boolean edge = Math.abs(x) == 4;
			float inset = edge ? 1.0F : Math.abs(x) == 3 ? 0.4F : 0.0F;
			char color = (x & 1) == 0 ? 'o' : 'O';
			float top = -8.0F + inset;
			float height = 8.0F - inset * 2;
			float front = -4.5F + inset;
			if (!holes) {
				shape.box(color, x - 0.5F, top, front, 1.0F, height, 9.0F - inset * 2);
				continue;
			}
			// El cuerpo empieza 1 px más atrás y delante va una capa de 1 px con los huecos de la cara.
			shape.box(color, x - 0.5F, top, front + 1.0F, 1.0F, height, 8.0F - inset * 2);
			float y = top;
			while (y < top + height) {
				int row = (int) Math.floor(y) - CARVED_TOP;
				boolean hole = row >= 0 && row < CARVED.length && CARVED[row].charAt(x + 4) == '#';
				if (hole) {
					y += 1.0F;
					continue;
				}
				// Tramo seguido sin huecos: un solo cubo.
				float end = y;
				while (end < top + height) {
					int r = (int) Math.floor(end) - CARVED_TOP;
					if (r >= 0 && r < CARVED.length && CARVED[r].charAt(x + 4) == '#') break;
					end = Math.min(top + height, (float) Math.floor(end) + 1.0F);
				}
				shape.box(color, x - 0.5F, y, front, 1.0F, end - y, 1.0F);
				y = end;
			}
		}
		return shape;
	}

	private void build() {
		pumpkin = pumpkin(false).box('g', -0.8F, -10.0F, -0.8F, 1.6F, 2.2F, 1.6F).box('l', 0.8F, -9.0F, -0.6F, 2.6F, 0.4F, 1.6F);
		carved = pumpkin(true).box('g', -0.8F, -10.0F, -0.8F, 1.6F, 2.2F, 1.6F).box('l', 0.8F, -9.0F, -0.6F, 2.6F, 0.4F, 1.6F);
		// La luz de la vela: cada hueco lleva dentro un vóxel brillante algo hundido (amarillo arriba, naranja abajo),
		// así los ojos, la nariz y la sonrisa se ven encendidos desde cualquier lado.
		candle = new Vox.Shape(PALETTE);
		for (int row = 0; row < CARVED.length; row++) {
			for (int column = 0; column < 9; column++) {
				if (CARVED[row].charAt(column) != '#') continue;
				int x = column - 4;
				float front = -4.5F + (Math.abs(x) == 3 ? 0.4F : 0.0F);
				candle.box(row < 3 ? 'y' : 'Y', x - 0.5F, CARVED_TOP + row, front + 0.3F, 1.0F, 1.0F, 0.7F);
			}
		}
		happy = new Vox.Shape(PALETTE).art(new String[] {
				".kk...kk.",
				".wk...wk.",
				".........",
				"p..k.k..p",
				"....k....",
		}, -4.5F, -6.2F, -4.9F, 0.4F);
	}

	@Override
	protected void renderPet(PoseStack poseStack, SubmitNodeCollector collector, int light, float time, PetBehavior.Mood mood, float moodSeconds) {
		if (pumpkin == null) build();
		boolean showEvil = flicker > 0 ? (flicker / 2) % 2 == 0 ? isEvil : !isEvil : isEvil;
		float bob = Mth.sin(time * 0.12F) * 1.0F;
		poseStack.translate(0.0F, bob / 16.0F, 0.0F);
		poseStack.translate(0.0F, -4.0F / 16.0F, 0.0F);
		// Feliz se mece; tallada tiembla un poco.
		if (showEvil) {
			poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 2.2F) * 2.5F));
		} else {
			poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.1F) * 8.0F));
			poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(time * 0.04F) * 15.0F));
		}
		poseStack.translate(0.0F, 4.0F / 16.0F, 0.0F);
		if (showEvil) {
			carved.draw(poseStack, collector, light);
			candle.drawGlow(poseStack, collector);
		} else {
			pumpkin.draw(poseStack, collector, light);
			happy.draw(poseStack, collector, light);
		}
	}
}
