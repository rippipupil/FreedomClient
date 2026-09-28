package com.freedomclient.cosmetic.vox;

import com.freedomclient.FreedomClient;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Ayuda para los cosméticos "pixel 3D": cada pieza se describe como cubos de colores (cajas, pixel art de frente,
 * discos y esferas hechas de cubos). Los colores salen de una paleta con la que se genera la textura al vuelo:
 * una franja sólida de 64x32 por color, así cualquier cubo de hasta 32 px (ancho+fondo y alto+fondo) sale de un
 * solo color. Las coordenadas van en píxeles del modelo, con y hacia abajo y la cara hacia -z.
 */
public final class Vox {
	private static final int STRIP = 32;
	private static final int WIDTH = 64;

	private Vox() {
	}

	/** Colores de un cosmético: cada carácter es un color ARGB (con alfa para las partes medio transparentes). */
	public static final class Palette {
		private final String name;
		private final String chars;
		private final int[] colors;
		private Identifier texture;

		/** {@code pairs}: carácter, color ARGB, carácter, color ARGB… */
		public Palette(String name, Object... pairs) {
			this.name = name;
			StringBuilder builder = new StringBuilder();
			colors = new int[pairs.length / 2];
			for (int i = 0; i < pairs.length; i += 2) {
				builder.append((char) (Character) pairs[i]);
				colors[i / 2] = (Integer) pairs[i + 1];
			}
			chars = builder.toString();
		}

		int row(char c) {
			int index = chars.indexOf(c);
			if (index < 0) throw new IllegalArgumentException("Color '" + c + "' is not in palette " + name);
			return index * STRIP;
		}

		int height() {
			return Math.max(STRIP, colors.length * STRIP);
		}

		/** Textura de franjas; se crea la primera vez que se dibuja (en el hilo de render). */
		public Identifier texture() {
			if (texture == null) {
				NativeImage image = new NativeImage(WIDTH, height(), false);
				for (int i = 0; i < colors.length; i++) {
					for (int y = i * STRIP; y < (i + 1) * STRIP; y++) {
						for (int x = 0; x < WIDTH; x++) image.setPixel(x, y, colors[i]);
					}
				}
				texture = FreedomClient.id("vox/" + name);
				Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(() -> "FreedomClient " + name, image));
			}
			return texture;
		}
	}

	/** Una pieza (se dibuja de una vez; lo que se anima por separado va en otra pieza). */
	public static final class Shape {
		private final Palette palette;
		private final CubeListBuilder builder = CubeListBuilder.create();
		private ModelPart baked;

		public Shape(Palette palette) {
			this.palette = palette;
		}

		public Shape box(char c, float x, float y, float z, float w, float h, float d) {
			builder.texOffs(0, palette.row(c)).addBox(x, y, z, w, h, d);
			return this;
		}

		public Shape box(char c, float x, float y, float z, float w, float h, float d, float grow) {
			builder.texOffs(0, palette.row(c)).addBox(x, y, z, w, h, d, new CubeDeformation(grow));
			return this;
		}

		/**
		 * Pixel art visto de frente: la fila 0 arriba (en {@code y0}), la columna 0 a la izquierda (en {@code x0}),
		 * con {@code depth} de grosor desde {@code z}. Los tramos seguidos del mismo color son un solo cubo; '.' es hueco.
		 */
		public Shape art(String[] rows, float x0, float y0, float z, float depth) {
			return art(rows, x0, y0, z, depth, 1.0F);
		}

		/** Igual, con cada píxel de {@code pixel} unidades de modelo. */
		public Shape art(String[] rows, float x0, float y0, float z, float depth, float pixel) {
			for (int row = 0; row < rows.length; row++) {
				String line = rows[row];
				int x = 0;
				while (x < line.length()) {
					char c = line.charAt(x);
					int end = x + 1;
					while (end < line.length() && line.charAt(end) == c) end++;
					if (c != '.' && c != ' ') box(c, x0 + x * pixel, y0 + row * pixel, z, (end - x) * pixel, pixel, depth);
					x = end;
				}
			}
			return this;
		}

		/** Pixel art visto de lado (plano YZ): columnas hacia +z desde {@code z0}, grosor {@code width} desde x. */
		public Shape side(String[] rows, float x, float y0, float z0, float width) {
			for (int row = 0; row < rows.length; row++) {
				String line = rows[row];
				int z = 0;
				while (z < line.length()) {
					char c = line.charAt(z);
					int end = z + 1;
					while (end < line.length() && line.charAt(end) == c) end++;
					if (c != '.' && c != ' ') box(c, x, y0 + row, z0 + z, width, 1, end - z);
					z = end;
				}
			}
			return this;
		}

		/** Disco horizontal (cilindro corto) de radio {@code r} centrado en (cx, cz), de y a y + h. */
		public Shape disc(char c, float cx, float y, float cz, float r, float h) {
			int n = (int) Math.ceil(r);
			for (int dz = -n; dz < n; dz++) {
				float mid = dz + 0.5F;
				float half = (float) Math.sqrt(Math.max(0.0F, r * r - mid * mid));
				int cells = Math.round(half);
				if (cells <= 0) continue;
				box(c, cx - cells, y, cz + dz, cells * 2, h, 1);
			}
			return this;
		}

		/** Aro horizontal: disco de radio {@code r} sin el centro de radio {@code inner}. */
		public Shape ring(char c, float cx, float y, float cz, float r, float inner, float h) {
			int n = (int) Math.ceil(r);
			for (int dz = -n; dz < n; dz++) {
				for (int dx = -n; dx < n; dx++) {
					float mx = dx + 0.5F;
					float mz = dz + 0.5F;
					float d = (float) Math.sqrt(mx * mx + mz * mz);
					if (d <= r && d >= inner) box(c, cx + dx, y, cz + dz, 1, h, 1);
				}
			}
			return this;
		}

		/** Esfera de cubos de radio {@code r}; {@code color} elige el color de cada capa (dy de -r a r). */
		public Shape sphere(float cx, float cy, float cz, float r, java.util.function.IntFunction<Character> color) {
			int n = (int) Math.ceil(r);
			for (int dy = -n; dy < n; dy++) {
				float my = dy + 0.5F;
				float layer = (float) Math.sqrt(Math.max(0.0F, r * r - my * my));
				if (layer <= 0.3F) continue;
				disc(color.apply(dy), cx, cy + dy, cz, layer, 1);
			}
			return this;
		}

		public Shape sphere(char c, float cx, float cy, float cz, float r) {
			return sphere(cx, cy, cz, r, dy -> c);
		}

		public ModelPart part() {
			if (baked == null) {
				MeshDefinition mesh = new MeshDefinition();
				mesh.getRoot().addOrReplaceChild("shape", builder, PartPose.ZERO);
				baked = LayerDefinition.create(mesh, WIDTH, palette.height()).bakeRoot();
			}
			return baked;
		}

		public Palette palette() {
			return palette;
		}

		/** Dibuja la pieza opaca con la luz del mundo. */
		public void draw(PoseStack poseStack, SubmitNodeCollector collector, int light) {
			collector.submitModelPart(part(), poseStack, RenderTypes.entityCutoutNoCull(palette.texture()), light, OverlayTexture.NO_OVERLAY, null);
		}

		/** Dibuja la pieza medio transparente (usa el alfa de los colores de la paleta). */
		public void drawTranslucent(PoseStack poseStack, SubmitNodeCollector collector, int light) {
			collector.submitModelPart(part(), poseStack, RenderTypes.entityTranslucent(palette.texture()), light, OverlayTexture.NO_OVERLAY, null);
		}

		/** Dibuja la pieza brillando (no le afecta la oscuridad). */
		public void drawGlow(PoseStack poseStack, SubmitNodeCollector collector) {
			collector.submitModelPart(part(), poseStack, RenderTypes.entityCutoutNoCull(palette.texture()), LightTexture.FULL_BRIGHT,
					OverlayTexture.NO_OVERLAY, null);
		}
	}
}
