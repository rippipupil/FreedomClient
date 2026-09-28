package com.freedomclient.setting;

import com.freedomclient.util.MobFaces;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Conjunto de mobs elegidos en una cuadrícula con sus caras (se guarda como lista de ids, p. ej. "iron_golem"). */
public class MobListSetting extends Setting<Set<String>> {
	/** Un mob de la cuadrícula: id y nombre traducido. */
	public record Mob(String id, String name) {
	}

	private static List<Mob> mobs;

	public MobListSetting(String name, String description) {
		super(name, description, Set.of());
		set(new HashSet<>());
	}

	/** Todos los mobs que tienen huevo de spawn (incluye gólems y aldeanos), por orden alfabético. */
	public static List<Mob> mobs() {
		if (mobs == null) {
			List<Mob> list = new ArrayList<>();
			for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
				String id = MobFaces.id(type);
				if (MobFaces.spawnEgg(id) == Items.AIR) continue;
				list.add(new Mob(id, type.getDescription().getString()));
			}
			list.sort(Comparator.comparing(Mob::name, String.CASE_INSENSITIVE_ORDER));
			mobs = List.copyOf(list);
		}
		return mobs;
	}

	public boolean contains(String id) {
		return get().contains(id);
	}

	public void toggle(String id) {
		if (!get().remove(id)) get().add(id);
	}

	@Override
	public void reset() {
		set(new HashSet<>());
	}

	@Override
	public JsonElement toJson() {
		JsonArray array = new JsonArray();
		get().stream().sorted().forEach(array::add);
		return array;
	}

	@Override
	public void fromJson(JsonElement json) {
		if (!json.isJsonArray()) return;
		Set<String> ids = new HashSet<>();
		for (JsonElement element : json.getAsJsonArray()) {
			if (element.isJsonPrimitive()) ids.add(element.getAsString());
		}
		set(ids);
	}
}
