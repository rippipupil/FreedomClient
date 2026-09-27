package com.freedomclient.cosmetic;

import com.freedomclient.FreedomClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Soul Scythe: guadaña voxel (como las alas) colgada en diagonal a la espalda, con la hoja asomando por un hombro.
 * Dos estilos: Classic (silueta plana) y 3D (con volumen), y un aura fantasmal gris opcional.
 */
public final class ScytheRenderer {
	/** Franjas de color (64x32): blanco en v = 0, plata en 4, plata oscura en 8, negro en 12, gris oscuro en 16 y gris en 20. */
	private static final Identifier TEXTURE = FreedomClient.id("textures/cosmetic/scythe.png");

	/** W = filo blanco, S = plata, D = plata oscura, K = negro, H = gris oscuro del mango y la guarda. */
	private static final String[] SCYTHE = {
			"...............K........K.",
			"...............KK......KK.",
			"...............KHK....KHK.",
			"...........WWKKKHHK..KHHK.",
			".........WWWSKKHHHHKKHHHKK",
			".......WWWSSSSKKHHHHHHHKK.",
			"......WWSSSSSSSKKKKKKKKK..",
			".....WWSSSSSSSSSSKKKKK....",
			".....WSSSSSSSSSSSS.HK.....",
			"....WSSSSSSSSSSSSS.HHK....",
			"....WSSSSSSSSSDDDD..HK....",
			"...WSSSSSSSDDD......HK....",
			"...WSSSSSDDD........HK....",
			"..WWSSSSDD..........HK....",
			"..WSSSSD............HK..H.",
			"..WSSSD.............HK.K..",
			".WWSSD..............HKK...",
			".WSSDD..............HK....",
			".WSDD...............HK....",
			".WSD................HK....",
			".WD.................HK....",
			"WWD.................HK....",
			"WD..................HK....",
			"WD...............H..HK....",
			"W.................K.HK....",
			"W..................KHK....",
			"W...................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK....",
			"....................HK...H",
			"....................HK..K.",
			"....................HHKK..",
			".....................HK...",
			".....................HK...",
			".....................HHK..",
	};
	/** Píxel de la silueta por el que se cuelga (en el mango, a media altura). */
	private static final float PIVOT_X = 20.5F;
	private static final float PIVOT_Y = 21.0F;
	/** Filas de la guarda con púas, que tiene doble grosor. */
	private static final int GUARD_ROWS = 8;

	/** Estilo Classic: silueta plana de 1 píxel de grosor. */
	private final ModelPart classic = build(false, 0.0F);
	/** Estilo 3D: hoja más gruesa en el centro, mango cuadrado con vendas, guarda ancha con púas y pomo. */
	private final ModelPart threeD = build(true, 0.0F);
	/** Aura fantasmal: los mismos cubos un poco inflados, dibujados translúcidos alrededor de la guadaña. */
	private final ModelPart classicGhost = build(false, 0.8F);
	private final ModelPart threeDGhost = build(true, 0.8F);

	private static final Identifier[] GHOST_TEXTURES = {
			FreedomClient.id("textures/cosmetic/scythe_ghost_1.png"),
			FreedomClient.id("textures/cosmetic/scythe_ghost_2.png"),
			FreedomClient.id("textures/cosmetic/scythe_ghost_3.png"),
	};

	private static boolean isBlade(char c) {
		return c == 'W' || c == 'S' || c == 'D';
	}

	private static char at(int row, int x) {
		if (row < 0 || row >= SCYTHE.length || x < 0 || x >= SCYTHE[row].length()) return '.';
		return SCYTHE[row].charAt(x);
	}

	/** Distancia (en píxeles, tipo tablero) de un píxel de la hoja al borde de la hoja, hasta 3. */
	private static int bladeDepth(int row, int x) {
		for (int d = 1; d <= 2; d++) {
			for (int dy = -d; dy <= d; dy++) {
				for (int dx = -d; dx <= d; dx++) {
					if (!isBlade(at(row + dy, x + dx))) return d;
				}
			}
		}
		return 3;
	}

	private static float depthOf(boolean volume, int row, int x, char color) {
		if (!volume) return row < GUARD_ROWS && (color == 'K' || color == 'H') ? 2.0F : 1.0F;
		if (isBlade(color)) return bladeDepth(row, x);
		return row < GUARD_ROWS ? 3.0F : 2.0F;
	}

	/**
	 * Convierte la silueta en cubos: cada tramo de píxeles con el mismo color y grosor de una fila es un cubo de
	 * 1 píxel de alto. {@code grow} infla todos los cubos (para el brillo del aura fantasmal).
	 */
	private static ModelPart build(boolean volume, float grow) {
		MeshDefinition mesh = new MeshDefinition();
		CubeListBuilder builder = CubeListBuilder.create();
		CubeDeformation deformation = new CubeDeformation(grow);
		for (int row = 0; row < SCYTHE.length; row++) {
			String line = SCYTHE[row];
			int x = 0;
			while (x < line.length()) {
				char color = line.charAt(x);
				float depth = depthOf(volume, row, x, color);
				int end = x + 1;
				while (end < line.length() && line.charAt(end) == color && depthOf(volume, row, end, color) == depth) {
					end++;
				}
				if (color != '.') {
					builder.texOffs(0, colorRow(color)).addBox(x - PIVOT_X, row - PIVOT_Y, -depth / 2, end - x, 1, depth, deformation);
				}
				x = end;
			}
		}
		if (volume) {
			addDetails(builder, grow);
		}
		mesh.getRoot().addOrReplaceChild("scythe", builder, PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32).bakeRoot();
	}

	/** Detalles del estilo 3D: vendas grises en el mango, pomo al final y púas de la guarda hacia delante y atrás. */
	private static void addDetails(CubeListBuilder builder, float grow) {
		CubeDeformation deformation = new CubeDeformation(grow);
		CubeDeformation wrap = new CubeDeformation(grow + 0.3F);
		for (int row = GUARD_ROWS + 3; row < SCYTHE.length - 3; row += 5) {
			String line = SCYTHE[row];
			int first = line.indexOf('H');
			if (first < 0) continue;
			builder.texOffs(0, colorRow('G')).addBox(first - PIVOT_X, row - PIVOT_Y, -1.0F, 2, 1, 2, wrap);
		}
		// Pomo: un bloque de 3x2x3 al final del mango.
		int last = SCYTHE.length - 1;
		int handle = SCYTHE[last].indexOf('H');
		builder.texOffs(0, colorRow('H')).addBox(handle - 0.5F - PIVOT_X, last + 1 - PIVOT_Y, -1.5F, 3, 2, 3, deformation);
		// Púas de la guarda que salen hacia delante y hacia atrás, en el centro de la guarda.
		for (int side = -1; side <= 1; side += 2) {
			builder.texOffs(0, colorRow('K')).addBox(19.5F - PIVOT_X, 4 - PIVOT_Y, side < 0 ? -3.5F : 1.5F, 2, 2, 2, deformation);
			builder.texOffs(0, colorRow('H')).addBox(20.0F - PIVOT_X, 4.5F - PIVOT_Y, side < 0 ? -4.5F : 3.5F, 1, 1, 1, deformation);
		}
	}

	private static int colorRow(char color) {
		return switch (color) {
			case 'W' -> 0;
			case 'S' -> 4;
			case 'D' -> 8;
			case 'K' -> 12;
			case 'G' -> 20;
			default -> 16;
		};
	}

	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, ScytheCosmetic module) {
		poseStack.pushPose();
		parent.body.translateAndRotate(poseStack);
		// Detrás de la espalda, por fuera de la capa, con la hoja asomando por encima del hombro.
		poseStack.translate(0.0F, 4.5F / 16.0F, 4.2F / 16.0F);
		float size = module.size.getFloat();
		poseStack.scale(size, size, size);
		// Sin reflejar, la hoja queda sobre el hombro derecho del jugador: para el izquierdo se refleja.
		if (module.side.is("Left")) {
			poseStack.scale(-1.0F, 1.0F, 1.0F);
		}
		// En diagonal: la hoja arriba, junto a la cabeza, y el mango bajando hacia la cadera contraria.
		poseStack.mulPose(Axis.ZP.rotationDegrees(-32.0F));
		boolean volume = module.style.is("3D");
		collector.submitModelPart(volume ? threeD : classic, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE), light, OverlayTexture.NO_OVERLAY, null);
		if (module.ghostAura.get()) {
			// Brillo gris translúcido que late despacio (tres texturas con más o menos transparencia).
			int phase = (int) ((Math.sin(state.ageInTicks * 0.12F) + 1.0F) * 1.5F);
			Identifier ghost = GHOST_TEXTURES[Math.min(GHOST_TEXTURES.length - 1, phase)];
			collector.submitModelPart(volume ? threeDGhost : classicGhost, poseStack, RenderTypes.entityTranslucent(ghost), light,
					OverlayTexture.NO_OVERLAY, null);
		}
		poseStack.popPose();
	}
}
