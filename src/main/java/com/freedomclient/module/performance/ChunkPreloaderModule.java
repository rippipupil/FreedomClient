package com.freedomclient.module.performance;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chunk Preloader: en los mundos de un jugador genera y guarda por adelantado los chunks de alrededor, en espiral
 * y poco a poco. Al principio el juego va algo más cargado, pero después, al moverte, los chunks ya existen y se
 * leen del disco en vez de generarse: el servidor interno deja de competir con el renderizado y hay menos tirones.
 * En los servidores los chunks los manda el servidor, así que ahí no hace nada.
 */
public class ChunkPreloaderModule extends Module {
	private final NumberSetting radius = add(new NumberSetting("Radius", "How many chunks around you are prepared in advance.", 32, 12, 64, 4));
	private final ModeSetting speed = add(new ModeSetting("Speed", "How many chunks are prepared at the same time. Faster finishes sooner but costs more at first.",
			"Normal", "Gentle", "Normal", "Fast"));
	private final BooleanSetting notify = add(new BooleanSetting("Notify when done", "Tell you in chat when the area around you is ready.", true));

	private final AtomicInteger inFlight = new AtomicInteger();
	private final AtomicInteger done = new AtomicInteger();
	private final LongSet requested = new LongOpenHashSet();
	private ResourceKey<Level> dimension;
	private ChunkPos center;
	/** Paso de la espiral por el que se va (0 = el chunk del centro). */
	private int step;
	private int total;
	private boolean announced;

	public ChunkPreloaderModule() {
		super("Chunk Preloader", "Singleplayer: generates the chunks around you in advance so they never have to be generated again. "
				+ "A bit heavier at first, smoother afterwards.", Category.PERFORMANCE, false);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	@Override
	protected void onDisable(Minecraft client) {
		reset();
	}

	private void reset() {
		requested.clear();
		dimension = null;
		center = null;
		step = 0;
		done.set(0);
		announced = false;
	}

	private int parallel() {
		return switch (speed.get()) {
			case "Gentle" -> 1;
			case "Fast" -> 6;
			default -> 3;
		};
	}

	@Override
	public void onTick(Minecraft client) {
		IntegratedServer server = client.getSingleplayerServer();
		if (server == null || client.level == null || client.player == null || client.isPaused()) return;
		ResourceKey<Level> currentDimension = client.level.dimension();
		ChunkPos playerChunk = client.player.chunkPosition();
		int r = radius.getInt();
		// Nueva dimensión o te has alejado mucho del centro: la espiral empieza otra vez desde donde estás.
		if (!currentDimension.equals(dimension)) {
			reset();
			dimension = currentDimension;
		}
		if (center == null || Math.abs(playerChunk.x - center.x) > r / 2 || Math.abs(playerChunk.z - center.z) > r / 2) {
			center = playerChunk;
			step = 0;
			announced = false;
		}
		total = (2 * r + 1) * (2 * r + 1);
		if (step >= total) {
			if (!announced && inFlight.get() == 0) {
				announced = true;
				if (notify.get()) {
					client.player.displayClientMessage(Component.literal("§6Chunk Preloader:§r " + done.get() + " chunks ready around you."), false);
				}
			}
			return;
		}
		int free = parallel() - inFlight.get();
		if (free <= 0) return;
		ChunkPos from = center;
		ResourceKey<Level> key = dimension;
		int[] batch = new int[free * 2];
		int count = 0;
		while (count < free && step < total) {
			int[] offset = spiral(step++);
			int x = from.x + offset[0];
			int z = from.z + offset[1];
			if (requested.add(ChunkPos.asLong(x, z))) {
				batch[count * 2] = x;
				batch[count * 2 + 1] = z;
				count++;
			}
		}
		if (count == 0) return;
		int n = count;
		inFlight.addAndGet(n);
		server.execute(() -> {
			ServerLevel level = server.getLevel(key);
			for (int i = 0; i < n; i++) {
				if (level == null) {
					inFlight.decrementAndGet();
					continue;
				}
				level.getChunkSource().getChunkFuture(batch[i * 2], batch[i * 2 + 1], ChunkStatus.FULL, true)
						.whenComplete((result, error) -> {
							inFlight.decrementAndGet();
							done.incrementAndGet();
						});
			}
		});
	}

	/** Posición del paso {@code n} de una espiral cuadrada que sale del centro (0,0). */
	private static int[] spiral(int n) {
		if (n == 0) return new int[] {0, 0};
		int ring = (int) Math.ceil((Math.sqrt(n + 1) - 1) / 2);
		int side = ring * 2;
		int start = (side - 1) * (side - 1);
		int offset = n - start;
		if (offset < side) return new int[] {ring, -ring + 1 + offset};
		offset -= side;
		if (offset < side) return new int[] {ring - 1 - offset, ring};
		offset -= side;
		if (offset < side) return new int[] {-ring, ring - 1 - offset};
		offset -= side;
		return new int[] {-ring + 1 + offset, -ring};
	}
}
