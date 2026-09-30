package com.freedomclient.module.performance;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;

/**
 * No Break Particles: no salen trocitos de bloque al picar ni al romper bloques. Se cortan antes de crearse (no
 * solo se esconden), así que minando rápido o con mucha gente rompiendo cosas se ganan FPS.
 */
public class NoBreakParticlesModule extends Module {
	private static NoBreakParticlesModule instance;

	private final BooleanSetting whileMining = add(new BooleanSetting("While mining",
			"No little bits flying off the block while you hit it.", true));
	private final BooleanSetting whenBroken = add(new BooleanSetting("When broken",
			"No burst of pieces when a block breaks (yours and other players').", true));

	public NoBreakParticlesModule() {
		super("No Break Particles", "Removes the block pieces that fly off while mining and when blocks break. Less clutter and more FPS.",
				Category.PERFORMANCE, false);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** Si hay que saltarse las partículas de ir picando un bloque. */
	public static boolean hidesMining() {
		return instance != null && instance.isEnabled() && instance.whileMining.get();
	}

	/** Si hay que saltarse la explosión de trozos al romperse un bloque. */
	public static boolean hidesBreaking() {
		return instance != null && instance.isEnabled() && instance.whenBroken.get();
	}
}
