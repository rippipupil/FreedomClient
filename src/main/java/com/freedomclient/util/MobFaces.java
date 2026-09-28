package com.freedomclient.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caras de los mobs para la interfaz: se recorta la cara (y el hocico, la nariz o los ojos) de la textura del propio
 * mob, así sale con el paquete de texturas que tengas. Los mobs que no están en la lista usan su huevo de spawn.
 */
public final class MobFaces {
	/** Un trozo de la textura (u, v, ancho, alto) dibujado en (dx, dy) dentro de la cara, en píxeles de textura. */
	private record Part(Identifier texture, int textureWidth, int textureHeight, int u, int v, int w, int h, float dx, float dy) {
	}

	/** Cara de {@code w} x {@code h} píxeles hecha de trozos que se dibujan en orden. */
	private record Face(int w, int h, List<Part> parts) {
	}

	private static final Map<String, Face> FACES = new HashMap<>();

	private MobFaces() {
	}

	/** Constructor de caras: la primera llamada a {@link #part} suele ser la cara entera en (0, 0). */
	private static final class Builder {
		private final String texture;
		private final int textureWidth;
		private final int textureHeight;
		private final int w;
		private final int h;
		private final List<Part> parts = new ArrayList<>();

		Builder(String texture, int textureWidth, int textureHeight, int w, int h) {
			this.texture = texture;
			this.textureWidth = textureWidth;
			this.textureHeight = textureHeight;
			this.w = w;
			this.h = h;
		}

		Builder part(int u, int v, int pw, int ph, float dx, float dy) {
			return part(texture, u, v, pw, ph, dx, dy);
		}

		Builder part(String otherTexture, int u, int v, int pw, int ph, float dx, float dy) {
			parts.add(new Part(Identifier.withDefaultNamespace("textures/entity/" + otherTexture + ".png"), textureWidth, textureHeight, u, v, pw, ph, dx, dy));
			return this;
		}

		void as(String... ids) {
			Face face = new Face(w, h, List.copyOf(parts));
			for (String id : ids) FACES.put(id, face);
		}
	}

	private static Builder face(String texture, int textureWidth, int textureHeight, int w, int h) {
		return new Builder(texture, textureWidth, textureHeight, w, h);
	}

	/** Cabeza de humanoide de 8x8 en (8, 8), con la capa exterior en (40, 8) si la tiene. */
	private static void humanoid(String texture, int textureHeight, boolean overlay, String... ids) {
		Builder builder = face(texture, 64, textureHeight, 8, 8).part(8, 8, 8, 8, 0, 0);
		if (overlay) builder.part(40, 8, 8, 8, 0, 0);
		builder.as(ids);
	}

	/** Cabeza de aldeano o illager (8x10 con la nariz grande). */
	private static void villagerLike(String texture, int textureHeight, String... ids) {
		face(texture, 64, textureHeight, 8, 10).part(8, 8, 8, 10, 0, 0).part(26, 2, 2, 4, 3, 7).as(ids);
	}

	static {
		humanoid("zombie/zombie", 64, true, "zombie");
		humanoid("zombie/husk", 64, true, "husk");
		humanoid("zombie/drowned", 64, true, "drowned");
		humanoid("skeleton/skeleton", 32, false, "skeleton");
		humanoid("skeleton/stray", 32, false, "stray");
		humanoid("skeleton/wither_skeleton", 32, false, "wither_skeleton");
		humanoid("skeleton/bogged", 32, false, "bogged");
		humanoid("creeper/creeper", 32, false, "creeper");
		humanoid("blaze", 32, false, "blaze");
		humanoid("snow_golem", 64, false, "snow_golem");
		face("enderman/enderman", 64, 32, 8, 8).part(8, 8, 8, 8, 0, 0).part("enderman/enderman_eyes", 8, 8, 8, 8, 0, 0).as("enderman");
		villagerLike("villager/villager", 64, "villager");
		villagerLike("zombie_villager/zombie_villager", 64, "zombie_villager");
		villagerLike("wandering_trader", 64, "wandering_trader");
		villagerLike("witch", 128, "witch");
		villagerLike("illager/pillager", 64, "pillager");
		villagerLike("illager/vindicator", 64, "vindicator");
		villagerLike("illager/evoker", 64, "evoker");
		villagerLike("illager/illusioner", 64, "illusioner");
		face("spider/spider", 64, 32, 8, 8).part(40, 12, 8, 8, 0, 0).as("spider");
		face("spider/cave_spider", 64, 32, 8, 8).part(40, 12, 8, 8, 0, 0).as("cave_spider");
		face("iron_golem/iron_golem", 128, 128, 8, 10).part(8, 8, 8, 10, 0, 0).part(26, 2, 2, 4, 3, 7).as("iron_golem");
		// Slime: el cubo de dentro con los ojos y la boca, y por encima el de fuera medio transparente.
		face("slime/slime", 64, 32, 8, 8).part(6, 22, 6, 6, 1, 1).part(34, 2, 2, 2, 0.75F, 2).part(34, 6, 2, 2, 5.25F, 2)
				.part(33, 9, 1, 1, 4, 5).part(8, 8, 8, 8, 0, 0).as("slime");
		// Cubo de magma: cada fila de la cara es un aro distinto de la textura.
		face("slime/magmacube", 64, 32, 8, 8).part(8, 8, 8, 1, 0, 0).part(8, 9, 8, 1, 0, 1).part(32, 18, 8, 1, 0, 2)
				.part(32, 27, 8, 1, 0, 3).part(8, 12, 8, 1, 0, 4).part(8, 13, 8, 1, 0, 5).part(8, 14, 8, 1, 0, 6)
				.part(8, 15, 8, 1, 0, 7).as("magma_cube");
		face("ghast/ghast", 64, 32, 16, 16).part(16, 16, 16, 16, 0, 0).as("ghast");
		face("ghast/happy_ghast", 64, 64, 16, 16).part(16, 16, 16, 16, 0, 0).as("happy_ghast");
		face("piglin/piglin", 64, 64, 10, 8).part(8, 8, 10, 8, 0, 0).part(32, 2, 4, 4, 3, 4).as("piglin");
		face("piglin/piglin_brute", 64, 64, 10, 8).part(8, 8, 10, 8, 0, 0).part(32, 2, 4, 4, 3, 4).as("piglin_brute");
		face("piglin/zombified_piglin", 64, 64, 10, 8).part(8, 8, 10, 8, 0, 0).part(32, 2, 4, 4, 3, 4).as("zombified_piglin");
		face("hoglin/hoglin", 128, 64, 14, 6).part(80, 20, 14, 6, 0, 0).as("hoglin");
		face("hoglin/zoglin", 128, 64, 14, 6).part(80, 20, 14, 6, 0, 0).as("zoglin");
		face("cow/temperate_cow", 64, 64, 8, 8).part(6, 6, 8, 8, 0, 0).part(2, 34, 6, 3, 1, 5).as("cow");
		face("cow/red_mooshroom", 64, 64, 8, 8).part(6, 6, 8, 8, 0, 0).part(2, 34, 6, 3, 1, 5).as("mooshroom");
		face("pig/temperate_pig", 64, 64, 8, 8).part(8, 8, 8, 8, 0, 0).part(17, 17, 4, 3, 2, 4).as("pig");
		face("sheep/sheep", 64, 32, 6, 6).part(8, 8, 6, 6, 0, 0).as("sheep");
		face("chicken/temperate_chicken", 64, 32, 4, 6).part(3, 3, 4, 6, 0, 0).part(16, 2, 4, 2, 0, 2).part(16, 6, 2, 2, 1, 4).as("chicken");
		face("wolf/wolf", 64, 32, 6, 6).part(4, 4, 6, 6, 0, 0).part(4, 14, 3, 3, 1.5F, 3).as("wolf");
		face("cat/tabby", 64, 32, 5, 4).part(5, 5, 5, 4, 0, 0).part(2, 26, 3, 2, 1, 2).as("cat");
		face("cat/ocelot", 64, 32, 5, 4).part(5, 5, 5, 4, 0, 0).part(2, 26, 3, 2, 1, 2).as("ocelot");
		face("fox/fox", 48, 32, 8, 6).part(7, 11, 8, 6, 0, 0).part(9, 21, 4, 2, 2, 4).as("fox");
		face("bee/bee", 64, 64, 7, 7).part(10, 10, 7, 7, 0, 0).as("bee");
		face("bear/polarbear", 128, 64, 7, 7).part(7, 7, 7, 7, 0, 0).part(3, 47, 5, 3, 1, 4).as("polar_bear");
		face("guardian", 64, 64, 12, 12).part(16, 16, 12, 12, 0, 0).part(9, 1, 2, 2, 5, 5).as("guardian");
		face("guardian_elder", 64, 64, 12, 12).part(16, 16, 12, 12, 0, 0).part(9, 1, 2, 2, 5, 5).as("elder_guardian");
		face("wither/wither", 64, 64, 8, 8).part(8, 24, 8, 8, 0, 0).as("wither");
		face("warden/warden", 128, 128, 16, 16).part(10, 42, 16, 16, 0, 0).as("warden");
		face("illager/vex", 32, 32, 5, 5).part(5, 5, 5, 5, 0, 0).as("vex");
		face("allay/allay", 32, 32, 5, 5).part(5, 5, 5, 5, 0, 0).as("allay");
		face("strider/strider", 64, 128, 16, 14).part(16, 16, 16, 14, 0, 0).as("strider");
	}

	/** Id del tipo de entidad sin "minecraft:" (p. ej. "iron_golem"). */
	public static String id(EntityType<?> type) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
	}

	/** Si hay cara recortada para este mob (si no, se usa el huevo). */
	public static boolean hasFace(String id) {
		return FACES.containsKey(id);
	}

	/** Huevo de spawn del mob (aire si no tiene). */
	public static Item spawnEgg(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id + "_spawn_egg"));
	}

	/** Dibuja la cara del mob centrada en un cuadrado de {@code size} px; devuelve false si no hay nada que dibujar. */
	public static boolean draw(GuiGraphics graphics, String id, int x, int y, int size) {
		Face face = FACES.get(id);
		if (face == null) {
			Item egg = spawnEgg(id);
			if (egg == Items.AIR) return false;
			float scale = size / 16.0F;
			graphics.pose().pushMatrix();
			graphics.pose().translate(x, y);
			graphics.pose().scale(scale, scale);
			graphics.renderItem(new ItemStack(egg), 0, 0);
			graphics.pose().popMatrix();
			return true;
		}
		float scale = size / (float) Math.max(face.w(), face.h());
		graphics.pose().pushMatrix();
		graphics.pose().translate(x + (size - face.w() * scale) / 2.0F, y + (size - face.h() * scale) / 2.0F);
		graphics.pose().scale(scale, scale);
		for (Part part : face.parts()) {
			graphics.pose().pushMatrix();
			graphics.pose().translate(part.dx(), part.dy());
			graphics.blit(RenderPipelines.GUI_TEXTURED, part.texture(), 0, 0, part.u(), part.v(), part.w(), part.h(), part.w(), part.h(),
					part.textureWidth(), part.textureHeight());
			graphics.pose().popMatrix();
		}
		graphics.pose().popMatrix();
		return true;
	}
}
