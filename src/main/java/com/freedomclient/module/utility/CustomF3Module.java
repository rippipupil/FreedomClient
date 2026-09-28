package com.freedomclient.module.utility;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.ui.Draw;
import com.freedomclient.ui.NeonStyle;
import com.freedomclient.ui.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Custom F3: cambia la pantalla de depuración por una limpia, con el estilo del cliente y solo lo que sirve en
 * survival (FPS, coordenadas, dirección, bioma, luz, hora, velocidad, mobs cerca, bloque apuntado y memoria).
 * También puede ocultar el HUD del cliente mientras está abierta.
 */
public class CustomF3Module extends Module {
	private static CustomF3Module instance;

	private final BooleanSetting hideHud = add(new BooleanSetting("Hide client HUD", "Hide the FreedomClient HUD elements while F3 is open.", true));
	private final BooleanSetting biome = add(new BooleanSetting("Biome", "Show the biome you are in.", true));
	private final BooleanSetting light = add(new BooleanSetting("Light", "Show the light level (mobs spawn at 0).", true));
	private final BooleanSetting time = add(new BooleanSetting("Day and time", "Show the day and the in-game time.", true));
	private final BooleanSetting speed = add(new BooleanSetting("Speed", "Show how fast you move in blocks per second.", true));
	private final BooleanSetting mobs = add(new BooleanSetting("Mob count", "Hostile, passive and total mobs within 64 blocks.", true));
	private final BooleanSetting target = add(new BooleanSetting("Target block", "Name and position of the block you look at.", true));
	private final BooleanSetting memory = add(new BooleanSetting("Memory", "Memory used by the game.", true));

	private double lastX;
	private double lastZ;
	private double blocksPerSecond;
	private long mobsCountedAt;
	private int hostile;
	private int passive;
	private int total;

	public CustomF3Module() {
		super("Custom F3", "A clean F3 screen in the client style with only what you need in survival. Can hide the client HUD while open.",
				Category.UTILITY, true);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	private static CustomF3Module active() {
		ModuleManager manager = FreedomClient.getModuleManager();
		return instance != null && manager != null && instance.isEnabled() ? instance : null;
	}

	/** Si la pantalla F3 propia sustituye a la de vanilla ahora mismo. */
	public static boolean replacesVanilla() {
		return active() != null && Minecraft.getInstance().gui.getDebugOverlay().showDebugScreen();
	}

	/** Si hay que ocultar el HUD del cliente (F3 abierto y la opción activada). */
	public static boolean hidesHud() {
		CustomF3Module module = active();
		return module != null && module.hideHud.get() && Minecraft.getInstance().gui.getDebugOverlay().showDebugScreen();
	}

	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) return;
		double dx = player.getX() - lastX;
		double dz = player.getZ() - lastZ;
		// Media suave para que el número no baile a cada tick.
		blocksPerSecond = blocksPerSecond * 0.7 + Math.sqrt(dx * dx + dz * dz) * 20.0 * 0.3;
		lastX = player.getX();
		lastZ = player.getZ();
	}

	private void countMobs(Minecraft client, LocalPlayer player) {
		long now = System.currentTimeMillis();
		if (now - mobsCountedAt < 500) return;
		mobsCountedAt = now;
		hostile = 0;
		passive = 0;
		total = 0;
		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity == player || entity.distanceToSqr(player) > 64 * 64) continue;
			MobCategory category = entity.getType().getCategory();
			if (category == MobCategory.MISC) continue;
			total++;
			if (category == MobCategory.MONSTER) hostile++;
			else passive++;
		}
	}

	/** Una fila: etiqueta apagada y valor con color. */
	private record Line(String label, String value, int color) {
	}

	public static void render(GuiGraphics g) {
		CustomF3Module module = active();
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (module == null || player == null || client.level == null) return;
		Font font = client.font;
		int text = ThemeManager.text();
		int accent = ThemeManager.accent();

		List<Line> left = new ArrayList<>();
		int fps = client.getFps();
		int fpsColor = fps >= 60 ? 0xFF6BE35A : fps >= 30 ? 0xFFF2C94C : 0xFFFF5555;
		left.add(new Line("FPS", String.valueOf(fps), fpsColor));
		PlayerInfo info = client.getConnection() == null ? null : client.getConnection().getPlayerInfo(player.getUUID());
		if (info != null && !client.isSingleplayer()) left.add(new Line("Ping", info.getLatency() + " ms", text));
		left.add(new Line("XYZ", String.format(Locale.ROOT, "%.1f / %.1f / %.1f", player.getX(), player.getY(), player.getZ()), accent));
		BlockPos pos = player.blockPosition();
		left.add(new Line("Block", pos.getX() + " " + pos.getY() + " " + pos.getZ(), text));
		left.add(new Line("Chunk", SectionPos.blockToSectionCoord(pos.getX()) + " " + SectionPos.blockToSectionCoord(pos.getZ())
				+ "  (in " + (pos.getX() & 15) + " " + (pos.getZ() & 15) + ")", text));
		Direction facing = player.getDirection();
		String axis = switch (facing) {
			case NORTH -> "-Z";
			case SOUTH -> "+Z";
			case WEST -> "-X";
			default -> "+X";
		};
		String name = facing.getName().substring(0, 1).toUpperCase(Locale.ROOT) + facing.getName().substring(1);
		left.add(new Line("Facing", String.format(Locale.ROOT, "%s (%s)  %.0f / %.0f", name, axis,
				net.minecraft.util.Mth.wrapDegrees(player.getYRot()), player.getXRot()), text));
		if (module.biome.get()) {
			String biomeName = client.level.getBiome(pos).getRegisteredName();
			left.add(new Line("Biome", pretty(biomeName.substring(biomeName.indexOf(':') + 1)), text));
		}
		if (module.light.get()) {
			int sky = client.level.getBrightness(LightLayer.SKY, pos);
			int block = client.level.getBrightness(LightLayer.BLOCK, pos);
			int color = block == 0 ? 0xFFFF7A7A : text;
			left.add(new Line("Light", "block " + block + "  sky " + sky, color));
		}
		if (module.time.get()) {
			long dayTime = client.level.getLevelData().getDayTime();
			long day = dayTime / 24000L;
			long ticks = (dayTime + 6000L) % 24000L;
			left.add(new Line("Time", String.format(Locale.ROOT, "Day %d  %02d:%02d", day, ticks / 1000, ticks % 1000 * 60 / 1000), text));
		}
		if (module.speed.get()) {
			left.add(new Line("Speed", String.format(Locale.ROOT, "%.1f b/s", module.blocksPerSecond), text));
		}
		if (module.mobs.get()) {
			module.countMobs(client, player);
			left.add(new Line("Mobs", module.hostile + " hostile  " + module.passive + " passive  (" + module.total + ")", text));
		}
		if (module.target.get() && client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
			BlockPos targetPos = hit.getBlockPos();
			String id = BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(targetPos).getBlock()).getPath();
			left.add(new Line("Looking at", pretty(id) + "  " + targetPos.getX() + " " + targetPos.getY() + " " + targetPos.getZ(), text));
		}

		panel(g, font, 4, 4, "FreedomClient F3", left, false);

		if (module.memory.get()) {
			Runtime runtime = Runtime.getRuntime();
			long max = runtime.maxMemory() / 1048576L;
			long used = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
			int percent = (int) (used * 100 / Math.max(1, max));
			List<Line> right = new ArrayList<>();
			right.add(new Line("Memory", percent + "%  " + used + "/" + max + " MB", percent > 85 ? 0xFFFF5555 : text));
			right.add(new Line("Java", System.getProperty("java.version", "?"), text));
			panel(g, font, g.guiWidth() - 4, 4, "System", right, true);
		}
	}

	/** Panel con título y filas "etiqueta  valor" alineadas; {@code alignRight} lo coloca pegado a la derecha de x. */
	private static void panel(GuiGraphics g, Font font, int x, int y, String title, List<Line> lines, boolean alignRight) {
		int labelWidth = 0;
		int valueWidth = 0;
		for (Line line : lines) {
			labelWidth = Math.max(labelWidth, font.width(line.label()));
			valueWidth = Math.max(valueWidth, font.width(line.value()));
		}
		int width = Math.max(font.width(title) + 12, labelWidth + valueWidth + 18);
		int height = 16 + lines.size() * 10 + 3;
		int left = alignRight ? x - width : x;
		int fill = ThemeManager.withAlpha(ThemeManager.get(com.freedomclient.ui.theme.ThemeColor.BACKGROUND), 0.82F);
		Draw.panel(g, left, y, width, height, fill, ThemeManager.border());
		if (NeonStyle.on()) NeonStyle.frame(g, left, y, width, height, NeonStyle.flow(), 0.6, 1.0F);
		g.drawString(font, title, left + 6, y + 4, ThemeManager.accent(), true);
		int lineY = y + 14;
		if (NeonStyle.on()) {
			NeonStyle.hLine(g, left + 4, left + width - 4, lineY - 1, 1, 0.0, 0.5, 0.8F);
		} else {
			g.fill(left + 4, lineY - 1, left + width - 4, lineY, ThemeManager.withAlpha(ThemeManager.border(), 0.8F));
		}
		lineY += 2;
		for (Line line : lines) {
			g.drawString(font, line.label(), left + 6, lineY, ThemeManager.textMuted(), false);
			g.drawString(font, line.value(), left + 12 + labelWidth, lineY, line.color(), false);
			lineY += 10;
		}
	}

	/** "dark_oak_forest" → "Dark Oak Forest". */
	private static String pretty(String id) {
		StringBuilder out = new StringBuilder();
		for (String part : id.split("_")) {
			if (part.isEmpty()) continue;
			if (!out.isEmpty()) out.append(' ');
			out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return out.toString();
	}
}
