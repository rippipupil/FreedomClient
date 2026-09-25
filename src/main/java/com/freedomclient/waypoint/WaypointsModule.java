package com.freedomclient.waypoint;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.ActionSetting;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.KeybindSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.util.ColorUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/** Waypoints: guarda puntos con una tecla, crea uno donde mueres y los muestra en pantalla con su distancia. */
public class WaypointsModule extends Module {
	private final KeybindSetting addKey = add(new KeybindSetting("Add waypoint key", "Key that saves a waypoint where you stand.", GLFW.GLFW_KEY_B));
	private final BooleanSetting deathWaypoint = add(new BooleanSetting("Death waypoint", "Save a waypoint where you die.", true));
	private final BooleanSetting showDistance = add(new BooleanSetting("Show distance", "Show how far each waypoint is.", true));
	private final NumberSetting maxDistance = add(new NumberSetting("Max distance", "Hide waypoints further than this (0 = no limit).", 0, 0, 10000, 100, " m"));
	private final WaypointListSetting list = add(new WaypointListSetting());

	private boolean keyWasDown;
	private boolean wasDead;

	public WaypointsModule() {
		super("Waypoints", "Save points in the world with a key and see them on screen with their distance. A waypoint is added where you die.",
				Category.UTILITY, true);
		add(new ActionSetting("Add waypoint here", "Save a waypoint at your position.", "Add", () -> addHere(Minecraft.getInstance(), false)));
		add(new ActionSetting("Delete death waypoints", "Remove all death waypoints in this world.", "Delete",
				() -> {
					WaypointStore.current().removeIf(waypoint -> waypoint.death);
					WaypointStore.save();
				}));
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			wasDead = false;
			return;
		}

		boolean down = addKey.isBound() && client.screen == null && InputConstants.isKeyDown(client.getWindow(), addKey.get());
		if (down && !keyWasDown) addHere(client, false);
		keyWasDown = down;

		boolean dead = player.isDeadOrDying();
		if (dead && !wasDead && deathWaypoint.get()) addHere(client, true);
		wasDead = dead;
	}

	private static void addHere(Minecraft client, boolean death) {
		LocalPlayer player = client.player;
		if (player == null) return;

		List<Waypoint> waypoints = WaypointStore.current();
		if (death) waypoints.removeIf(waypoint -> waypoint.death);
		String name = death ? "Death" : "Waypoint " + (waypoints.stream().filter(w -> !w.death).count() + 1);
		int color = death ? 0xFFFF3B3B : 0xFF000000 | ColorUtil.hsvToRgb((float) Math.random(), 0.6F, 1.0F);
		String dimension = player.level().dimension().toString();
		WaypointStore.add(new Waypoint(name, player.getBlockX(), player.getBlockY(), player.getBlockZ(), dimension, color, death));

		if (!death) {
			player.displayClientMessage(Component.literal("Waypoint saved: " + name), true);
		}
	}

	/** Dibuja los marcadores de los waypoints proyectados sobre la pantalla. */
	public void render(GuiGraphics graphics, Minecraft client) {
		if (!isEnabled() || client.player == null || client.options.hideGui) return;

		Camera camera = client.gameRenderer.getMainCamera();
		Vec3 cameraPos = camera.position();
		Vector3fc forward = camera.forwardVector();
		String dimension = client.player.level().dimension().toString();

		for (Waypoint waypoint : WaypointStore.current()) {
			if (!waypoint.visible || !waypoint.dimension.equals(dimension)) continue;

			Vec3 target = new Vec3(waypoint.x + 0.5, waypoint.y + 1.0, waypoint.z + 0.5);
			Vec3 relative = target.subtract(cameraPos);
			double distance = client.player.position().distanceTo(target);
			if (maxDistance.getInt() > 0 && distance > maxDistance.getInt()) continue;
			// Detrás de la cámara no se dibuja.
			if (relative.x * forward.x() + relative.y * forward.y() + relative.z * forward.z() <= 0.1) continue;

			Vec3 ndc = client.gameRenderer.projectPointToScreen(target);
			int x = (int) Math.round((ndc.x + 1.0) / 2.0 * graphics.guiWidth());
			int y = (int) Math.round((1.0 - ndc.y) / 2.0 * graphics.guiHeight());
			if (x < -50 || y < -50 || x > graphics.guiWidth() + 50 || y > graphics.guiHeight() + 50) continue;

			drawMarker(graphics, client, waypoint, x, y, distance);
		}
	}

	private void drawMarker(GuiGraphics graphics, Minecraft client, Waypoint waypoint, int x, int y, double distance) {
		// Rombo pixel del color del waypoint con contorno oscuro.
		for (int i = 0; i < 5; i++) {
			graphics.fill(x - i - 1, y - 5 + i - 1, x + i + 2, y - 5 + i + 1, 0xC0000000);
			graphics.fill(x - i - 1, y + 5 - i - 1, x + i + 2, y + 5 - i + 1, 0xC0000000);
		}
		for (int i = 0; i < 5; i++) {
			graphics.fill(x - i, y - 5 + i, x + i + 1, y - 5 + i + 1, waypoint.color);
			graphics.fill(x - i, y + 5 - i, x + i + 1, y + 5 - i + 1, waypoint.color);
		}

		String label = showDistance.get()
				? waypoint.name + " (" + String.format(Locale.ROOT, "%.0f", distance) + "m)"
				: waypoint.name;
		int width = client.font.width(label);
		graphics.fill(x - width / 2 - 2, y - 18, x + width / 2 + 2, y - 8, 0x90000000);
		graphics.drawString(client.font, label, x - width / 2, y - 17, 0xFFFFFFFF, false);
	}
}
