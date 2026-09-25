package com.freedomclient.module.performance;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity Culling: no dibuja las entidades ni los bloques con modelo propio (cofres, carteles, estandartes,
 * cajas de shulker...) que quedan completamente tapados por bloques opacos. Cada pocos ticks lanza rayos
 * desde la cámara al centro y a las esquinas de cada uno; el cristal, las hojas y los bloques no completos no tapan.
 */
public class EntityCullingModule extends Module {
	private static EntityCullingModule instance;

	private final NumberSetting range = add(new NumberSetting("Range", "Entities farther than this are never culled.", 64, 16, 128, 8, " blocks"));
	private final NumberSetting interval = add(new NumberSetting("Check every", "How often visibility is recalculated.", 2, 1, 5, 1, " ticks"));
	private final BooleanSetting cullPlayers = add(new BooleanSetting("Cull players", "Also hide players behind walls (their name tags too).", false));
	private final BooleanSetting cullBlockEntities = add(new BooleanSetting("Cull chests and signs",
			"Also hide chests, signs, banners, shulker boxes and other block entities behind walls.", true));

	private Set<Integer> hidden = new HashSet<>();
	/** Bloques con modelo propio: posición -> tick de la última comprobación, y los que estaban tapados. */
	private final Long2IntOpenHashMap blockCheckedAt = new Long2IntOpenHashMap();
	private final LongOpenHashSet hiddenBlocks = new LongOpenHashSet();
	private int ticks;

	public EntityCullingModule() {
		super("Entity Culling", "Skips rendering mobs, items, chests and signs that are hidden behind walls.", Category.PERFORMANCE, true);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	public static boolean isCulled(Entity entity) {
		return instance != null && instance.isEnabled() && instance.hidden.contains(entity.getId());
	}

	/**
	 * Si un bloque con modelo propio está tapado. Se llama al dibujar cada uno (solo los que están en pantalla),
	 * y la visibilidad se recalcula como mucho una vez cada "Check every" ticks por bloque.
	 */
	public static boolean isCulled(BlockEntity blockEntity) {
		if (instance == null || !instance.isEnabled() || !instance.cullBlockEntities.get()) return false;
		return instance.blockHidden(blockEntity.getBlockPos());
	}

	private boolean blockHidden(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) return false;
		long key = pos.asLong();
		int checkedAt = blockCheckedAt.getOrDefault(key, Integer.MIN_VALUE);
		if (ticks - checkedAt < interval.getInt()) return hiddenBlocks.contains(key);

		if (blockCheckedAt.size() > 20000) {
			blockCheckedAt.clear();
			hiddenBlocks.clear();
		}
		blockCheckedAt.put(key, ticks);
		Vec3 eye = client.gameRenderer.getMainCamera().position();
		double maxDistance = range.get() * range.get();
		// Los puntos se toman un poco hacia dentro del propio bloque, para que el suelo o la pared de al lado no cuenten.
		boolean culled = pos.distToCenterSqr(eye) <= maxDistance && !isVisible(client.level, eye, new AABB(pos).deflate(0.05));
		if (culled) hiddenBlocks.add(key);
		else hiddenBlocks.remove(key);
		return culled;
	}

	@Override
	protected void onDisable(Minecraft client) {
		hidden = new HashSet<>();
		blockCheckedAt.clear();
		hiddenBlocks.clear();
	}

	@Override
	public void onTick(Minecraft client) {
		ClientLevel level = client.level;
		ticks++;
		if (level == null) {
			hidden = new HashSet<>();
			blockCheckedAt.clear();
			hiddenBlocks.clear();
			return;
		}
		if (ticks % interval.getInt() != 0) return;

		Vec3 eye = client.gameRenderer.getMainCamera().position();
		double maxDistance = range.get() * range.get();
		Set<Integer> next = new HashSet<>();
		for (Entity entity : level.entitiesForRendering()) {
			if (entity == client.getCameraEntity() || entity.isCurrentlyGlowing()) continue;
			if (entity instanceof Player && !cullPlayers.get()) continue;
			if (entity.distanceToSqr(eye) > maxDistance) continue;
			if (!isVisible(level, eye, entity.getBoundingBox().inflate(0.05))) {
				next.add(entity.getId());
			}
		}
		hidden = next;
	}

	private static boolean isVisible(BlockGetter level, Vec3 eye, AABB box) {
		if (box.contains(eye)) return true;
		if (!blocked(level, eye, box.getCenter())) return true;
		for (int i = 0; i < 8; i++) {
			Vec3 corner = new Vec3((i & 1) == 0 ? box.minX : box.maxX, (i & 2) == 0 ? box.minY : box.maxY, (i & 4) == 0 ? box.minZ : box.maxZ);
			if (!blocked(level, eye, corner)) return true;
		}
		return false;
	}

	/** Si algún bloque opaco completo corta la línea entre los dos puntos. */
	private static boolean blocked(BlockGetter level, Vec3 from, Vec3 to) {
		Boolean hit = BlockGetter.traverseBlocks(from, to, level, (getter, pos) -> {
			BlockState state = getter.getBlockState(pos);
			return state.canOcclude() && state.isSolidRender() ? Boolean.TRUE : null;
		}, getter -> Boolean.FALSE);
		return hit != null && hit;
	}
}
