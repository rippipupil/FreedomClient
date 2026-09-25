package com.freedomclient.module.pvp;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.KeybindSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** Freelook: mantén la tecla para mirar alrededor sin girar a tu personaje. */
public class FreelookModule extends Module {
	private final KeybindSetting key = add(new KeybindSetting("Freelook key", "Hold this key to look around.", GLFW.GLFW_KEY_LEFT_ALT));
	private final BooleanSetting toggleMode = add(new BooleanSetting("Toggle mode", "Press once to start and again to stop instead of holding.", false));
	private final BooleanSetting thirdPerson = add(new BooleanSetting("Third person", "Switch to third person while looking around.", true));
	private final BooleanSetting invertPitch = add(new BooleanSetting("Invert up/down", "Invert vertical mouse movement.", false));

	private boolean active;
	private boolean keyWasDown;
	private float yaw;
	private float pitch;
	private CameraType savedCamera;

	public FreelookModule() {
		super("Freelook", "Hold a key to look around your character without turning.", Category.PVP, true);
	}

	@Override
	public void onTick(Minecraft client) {
		boolean down = key.isBound() && client.screen == null && client.player != null
				&& InputConstants.isKeyDown(client.getWindow(), key.get());
		boolean shouldBeActive = toggleMode.get() ? (down && !keyWasDown ? !active : active) : down;
		keyWasDown = down;

		if (shouldBeActive && !active) start(client);
		else if (!shouldBeActive && active) stop(client);
	}

	private void start(Minecraft client) {
		active = true;
		yaw = client.player.getYRot();
		pitch = client.player.getXRot();
		savedCamera = client.options.getCameraType();
		if (thirdPerson.get()) client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
	}

	private void stop(Minecraft client) {
		active = false;
		if (savedCamera != null) client.options.setCameraType(savedCamera);
		savedCamera = null;
	}

	@Override
	protected void onDisable(Minecraft client) {
		if (active) stop(client);
	}

	private static FreelookModule instance() {
		ModuleManager manager = FreedomClient.getModuleManager();
		return manager == null ? null : manager.get(FreelookModule.class);
	}

	/** Llamado desde EntityMixin: si Freelook está activo, el movimiento del ratón gira solo la cámara. */
	public static boolean consumeTurn(double yawDelta, double pitchDelta) {
		FreelookModule module = instance();
		if (module == null || !module.active) return false;

		// Mismo factor que usa vanilla en Entity.turn.
		float pitchChange = (float) pitchDelta * 0.15F * (module.invertPitch.get() ? -1 : 1);
		module.yaw += (float) yawDelta * 0.15F;
		module.pitch = Mth.clamp(module.pitch + pitchChange, -90.0F, 90.0F);
		return true;
	}

	/** Rotación de la cámara mientras Freelook está activo, o null si no lo está. */
	public static float[] getCameraRotation() {
		FreelookModule module = instance();
		return module != null && module.active ? new float[] {module.yaw, module.pitch} : null;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
