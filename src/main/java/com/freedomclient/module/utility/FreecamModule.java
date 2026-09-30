package com.freedomclient.module.utility;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.KeybindSetting;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Freecam: separa la cámara de tu jugador y la mueves volando como en el modo espectador (atraviesa bloques).
 * Tu personaje se queda quieto donde estaba; las teclas de movimiento y el ratón mueven solo la cámara.
 * Es solo visual: no ves nada que no pudieras ver ya desde el cliente.
 */
public class FreecamModule extends Module {
	private static FreecamModule instance;

	private final KeybindSetting key = add(new KeybindSetting("Freecam key", "Press to start or stop the free camera.", GLFW.GLFW_KEY_F6));
	private final NumberSetting speed = add(new NumberSetting("Speed", "How fast the camera flies (sprint doubles it).", 10, 2, 40, 1, " b/s"));
	private final BooleanSetting smooth = add(new BooleanSetting("Smooth", "The camera speeds up and slows down smoothly.", true));
	private final BooleanSetting message = add(new BooleanSetting("Message", "Say in chat when the free camera starts and stops.", false));

	private boolean active;
	private boolean keyWasDown;
	private ClientLevel level;
	private Vec3 position;
	private Vec3 previous;
	private Vec3 velocity = Vec3.ZERO;
	private float yaw;
	private float pitch;
	/** Teclas de movimiento de este tick (se le quitan al jugador para que no ande). */
	private Input input = Input.EMPTY;

	public FreecamModule() {
		super("Freecam", "Press F6 to leave your body and fly the camera around like in spectator mode. Your player stays still.",
				Category.UTILITY, true);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** Si la cámara libre está en marcha ahora mismo. */
	public static boolean isActive() {
		return instance != null && instance.active;
	}

	@Override
	public void onTick(Minecraft client) {
		boolean down = key.isBound() && client.screen == null && client.player != null && InputConstants.isKeyDown(client.getWindow(), key.get());
		if (down && !keyWasDown) {
			if (active) stop(client);
			else start(client);
		}
		keyWasDown = down;
		if (!active) return;
		if (client.player == null || client.level != level || !client.player.isAlive()) {
			stop(client);
			return;
		}
		fly();
	}

	private void start(Minecraft client) {
		if (client.player == null || client.level == null) return;
		active = true;
		level = client.level;
		position = client.player.getEyePosition();
		previous = position;
		velocity = Vec3.ZERO;
		yaw = client.player.getYRot();
		pitch = client.player.getXRot();
		input = Input.EMPTY;
		if (message.get()) client.player.displayClientMessage(Component.literal("§bFreecam§r on (" + key.getKeyName() + " to go back)"), true);
	}

	private void stop(Minecraft client) {
		if (!active) return;
		active = false;
		level = null;
		position = null;
		previous = null;
		if (message.get() && client.player != null) client.player.displayClientMessage(Component.literal("§bFreecam§r off"), true);
	}

	@Override
	protected void onDisable(Minecraft client) {
		stop(client);
	}

	/** Un tick de vuelo: como el espectador, adelante/atrás/lados en horizontal, salto sube y agacharse baja. */
	private void fly() {
		previous = position;
		double forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
		double strafe = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
		double vertical = (input.jump() ? 1 : 0) - (input.shift() ? 1 : 0);
		float rad = yaw * Mth.DEG_TO_RAD;
		double sin = Mth.sin(rad);
		double cos = Mth.cos(rad);
		Vec3 wanted = new Vec3(-sin * forward + cos * strafe, vertical, cos * forward + sin * strafe);
		if (wanted.lengthSqr() > 1.0E-4) wanted = wanted.normalize();
		double blocksPerTick = speed.get() / 20.0 * (input.sprint() ? 2.0 : 1.0);
		wanted = wanted.scale(blocksPerTick);
		velocity = smooth.get() ? velocity.lerp(wanted, 0.35) : wanted;
		position = position.add(velocity);
	}

	/** Llamado desde KeyboardInputMixin: guarda las teclas para la cámara y devuelve si hay que quitárselas al jugador. */
	public static boolean captureInput(Input keys) {
		if (!isActive()) return false;
		instance.input = keys;
		return true;
	}

	/** Llamado desde EntityMixin: con la cámara libre, el ratón gira la cámara y no al jugador. */
	public static boolean consumeTurn(double yawDelta, double pitchDelta) {
		if (!isActive()) return false;
		instance.yaw += (float) yawDelta * 0.15F;
		instance.pitch = Mth.clamp(instance.pitch + (float) pitchDelta * 0.15F, -90.0F, 90.0F);
		return true;
	}

	/** Para las pruebas: enciende la cámara libre y la lleva a un sitio relativo al jugador, mirando hacia él. */
	public static void startForTest(Minecraft client, Vec3 offset, float yaw, float pitch) {
		if (instance == null) return;
		instance.start(client);
		instance.position = instance.position.add(offset);
		instance.previous = instance.position;
		instance.yaw = yaw;
		instance.pitch = pitch;
	}

	public static void stopForTest(Minecraft client) {
		if (instance != null) instance.stop(client);
	}

	/** Posición interpolada de la cámara, o null si no está activa. */
	public static Vec3 cameraPosition(float partialTick) {
		if (!isActive() || instance.position == null) return null;
		return instance.previous.lerp(instance.position, partialTick);
	}

	public static float yaw() {
		return instance.yaw;
	}

	public static float pitch() {
		return instance.pitch;
	}
}
