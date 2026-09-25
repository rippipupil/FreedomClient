package com.freedomclient.hud;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

/** Cuenta los clics por segundo (CPS) de los botones izquierdo y derecho mientras se juega. */
public final class ClickTracker {
	private static final Deque<Long> LEFT = new ArrayDeque<>();
	private static final Deque<Long> RIGHT = new ArrayDeque<>();

	private ClickTracker() {
	}

	/** Llamado desde el mixin del ratón al pulsar un botón. */
	public static void onPress(int button) {
		if (Minecraft.getInstance().screen != null) return;
		long now = System.currentTimeMillis();
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) LEFT.addLast(now);
		else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) RIGHT.addLast(now);
	}

	public static int leftCps() {
		return count(LEFT);
	}

	public static int rightCps() {
		return count(RIGHT);
	}

	private static int count(Deque<Long> clicks) {
		long limit = System.currentTimeMillis() - 1000;
		while (!clicks.isEmpty() && clicks.peekFirst() < limit) clicks.pollFirst();
		return clicks.size();
	}
}
