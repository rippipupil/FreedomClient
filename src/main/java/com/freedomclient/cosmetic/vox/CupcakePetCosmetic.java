package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.CosmeticSlot;
import com.freedomclient.cosmetic.PetBehavior;
import com.freedomclient.cosmetic.PetFollower;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mr. Cupcake (FNAF 2): la magdalena robótica de Chica, con su plato, su vela y sus ojos saltones. No vuela: va por
 * el suelo detrás de ti a saltos de robot (se agacha, se gira a golpes, salta y aterriza con un "clonc" y la cabeza
 * rebotando en un muelle). Parada hace tonterías: ojos locos, castañetear los dientes, girar a tirones, bailar,
 * sacar la cabeza con el muelle, mirar a los lados y darse de morros contra el suelo. También celebra, duerme
 * (con la vela apagada) y tiembla escondida detrás de ti.
 */
public class CupcakePetCosmetic extends VoxCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("cupcake_pet",
			'p', 0xFFE8457A, 'P', 0xFFC23262, 'h', 0xFFF57DA2, 'o', 0xFFE0602A, 'O', 0xFFB8421C,
			'w', 0xFFF6F2EA, 'k', 0xFF141014, 'i', 0xFFC8702A, 'c', 0xFFF4E8C8, 'y', 0xFFE8B83A,
			'f', 0xFFFF9A20, 'F', 0xFFFFE070, 'g', 0xFFB8BCC4, 'G', 0xFF7A7E88);
	private static final double GRAVITY = 0.08;
	/** Centro de cada ojo en píxeles del modelo (y hacia abajo, cara hacia -z). */
	private static final float EYE_X = 2.9F;
	private static final float EYE_Y = -10.6F;
	/** Altura a la que la cabeza (el glaseado) se apoya en el envoltorio. */
	private static final float NECK_Y = -7.0F;
	private static final int EMOTES = 7;
	private static final int EMOTE_TICKS = 40;
	private static final int EMOTE_EVERY = 110;
	/** Emotes de los estados de ánimo (no salen al azar). */
	private static final int DANCE = 4;
	private static final int SPIN_JUMP = 7;

	public final ModeSetting side = add(new ModeSetting("Side", "Which side the cupcake hops next to.", "Left", "Right", "Left"));
	public final NumberSetting size = add(new NumberSetting("Size", "Size of the cupcake.", 0.4, 0.25, 0.7, 0.02, "x"));

	private Vox.Shape base;
	private Vox.Shape head;
	private Vox.Shape spring;
	private Vox.Shape iris;
	private Vox.Shape lid;
	private Vox.Shape teeth;
	private Vox.Shape flame;

	// Física en el mundo (se actualiza cada tick; al dibujar se interpola).
	private ClientLevel level;
	private Vec3 previous;
	private Vec3 current;
	private double velocityX;
	private double velocityY;
	private double velocityZ;
	private boolean grounded;
	private float yaw;
	private int crouchTicks;
	private double hopStartY;
	private int landTicks = 100;
	private int idleTicks;
	private int turnCooldown;
	private int emote = -1;
	private int emoteTicks;
	private int emoteCount;
	private long ticks;

	public CupcakePetCosmetic() {
		super("Mr. Cupcake", "FNAF: Chica's robotic killer cupcake. Hops after you along the ground like a little robot and does silly things: "
				+ "crazy eyes, chattering teeth, jerky spins, dances, spring-head pops and faceplants.", CosmeticSlot.PET);
	}

	// ---------------------------------------------------------------- modelo

	private void build() {
		base = new Vox.Shape(PALETTE)
				// Plato plateado.
				.disc('G', 0.0F, -0.6F, 0.0F, 6.5F, 0.6F)
				.disc('g', 0.0F, -1.0F, 0.0F, 6.0F, 0.4F);
		// Envoltorio con pliegues, más ancho arriba.
		for (int layer = 0; layer < 6; layer++) pleats(base, -2.0F - layer, 4.3F + layer * 0.25F);

		head = new Vox.Shape(PALETTE);
		float[] radius = {6.8F, 7.0F, 7.0F, 6.8F, 6.4F, 5.8F, 4.8F, 3.4F};
		for (int i = 0; i < radius.length; i++) {
			char c = i == 0 ? 'P' : i == radius.length - 1 ? 'h' : 'p';
			head.disc(c, 0.0F, NECK_Y - 1.0F - i, 0.0F, radius[i], 1.0F);
		}
		// Chorretones de glaseado por el borde (ninguno delante, donde van los dientes).
		float[][] drips = {{20, 1.5F}, {70, 1.0F}, {115, 2.0F}, {160, 1.0F}, {205, 1.5F}, {335, 1.0F}, {240, 1.2F}, {300, 1.6F}};
		for (float[] drip : drips) {
			float angle = drip[0] * Mth.DEG_TO_RAD;
			float x = Mth.cos(angle) * 6.0F;
			float z = Mth.sin(angle) * 6.0F;
			head.box('P', x - 0.5F, NECK_Y, z - 0.5F, 1.0F, drip[1], 1.0F);
		}
		// Ojos saltones: borde negro grueso (más por arriba, cara de loco) y lo blanco.
		for (int s = -1; s <= 1; s += 2) {
			float ex = s * EYE_X;
			head.box('k', ex - 2.3F, EYE_Y - 2.2F, -7.0F, 4.6F, 4.2F, 1.2F)
					.box('k', ex - 2.5F, EYE_Y - 2.7F, -7.1F, 5.0F, 0.9F, 1.1F)
					.box('w', ex - 1.9F, EYE_Y - 1.8F, -7.3F, 3.8F, 3.4F, 0.4F);
		}
		// Vela de rayas con la mecha.
		float top = NECK_Y - radius.length;
		for (int i = 0; i < 8; i++) head.box(i % 2 == 0 ? 'c' : 'y', -0.75F, top - 1.0F - i, -0.75F, 1.5F, 1.0F, 1.5F);
		head.box('k', -0.2F, top - 8.8F, -0.2F, 0.4F, 0.8F, 0.4F);

		// Muelle del cuello: dentro de la magdalena, solo se ve cuando la cabeza sube.
		spring = new Vox.Shape(PALETTE);
		for (int i = 0; i < 5; i++) spring.box(i % 2 == 0 ? 'G' : 'g', -2.0F, NECK_Y - 4.0F + i, -2.0F, 4.0F, 1.0F, 4.0F);

		iris = new Vox.Shape(PALETTE)
				.box('i', -1.0F, -1.0F, -0.3F, 2.0F, 2.0F, 0.3F)
				.box('k', -0.5F, -0.5F, -0.45F, 1.0F, 1.0F, 0.2F)
				.box('w', 0.1F, -0.8F, -0.5F, 0.4F, 0.4F, 0.1F);
		// Párpado: cae desde arriba del ojo (se escala en y).
		lid = new Vox.Shape(PALETTE).box('P', -2.1F, 0.0F, -0.6F, 4.2F, 3.8F, 0.6F);
		// Los dos dientes de conejo, colgando del glaseado por delante del envoltorio.
		teeth = new Vox.Shape(PALETTE)
				.box('w', -1.5F, -0.6F, -0.7F, 1.4F, 1.9F, 0.7F)
				.box('w', 0.1F, -0.6F, -0.7F, 1.4F, 1.9F, 0.7F);
		flame = new Vox.Shape(PALETTE)
				.box('f', -0.8F, -2.4F, -0.8F, 1.6F, 2.4F, 1.6F)
				.box('F', -0.45F, -3.4F, -0.45F, 0.9F, 1.6F, 0.9F);
	}

	/** Una capa del envoltorio: aro de pliegues de dos colores y el relleno por dentro. */
	private static void pleats(Vox.Shape shape, float y, float r) {
		int n = (int) Math.ceil(r);
		for (int dz = -n; dz < n; dz++) {
			for (int dx = -n; dx < n; dx++) {
				float mx = dx + 0.5F;
				float mz = dz + 0.5F;
				float d = Mth.sqrt(mx * mx + mz * mz);
				if (d > r || d < r - 1.3F) continue;
				int pleat = (int) Math.floor((Mth.atan2(mz, mx) + Math.PI) / (Math.PI * 2.0) * 18.0);
				shape.box(pleat % 2 == 0 ? 'o' : 'O', dx, y, dz, 1.0F, 1.0F, 1.0F);
			}
		}
		float inner = (r - 1.0F) * 0.72F;
		shape.box('O', -inner, y, -inner, inner * 2.0F, 1.0F, inner * 2.0F);
	}

	// ---------------------------------------------------------------- física

	@Override
	protected void onDisable(Minecraft client) {
		super.onDisable(client);
		reset();
	}

	private void reset() {
		previous = null;
		current = null;
		level = null;
		emote = -1;
	}

	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			reset();
			return;
		}
		if (client.isPaused()) return;
		ticks++;
		PetBehavior.Mood mood = PetBehavior.mood();
		Vec3 target = target(player, mood);
		if (current == null || client.level != level || current.distanceToSqr(target) > 12.0 * 12.0
				|| Math.abs(current.y - player.getY()) > 8.0) {
			level = client.level;
			place(target);
			return;
		}
		previous = current;
		double x = current.x;
		double y = current.y;
		double z = current.z;
		double dx = target.x - x;
		double dz = target.z - z;
		double distance = Math.sqrt(dx * dx + dz * dz);
		boolean hurry = distance > 2.2;

		if (grounded) {
			landTicks++;
			double floor = groundAt(x, z, y);
			if (Double.isNaN(floor) || floor < y - 0.05) {
				// Se le ha acabado el suelo: cae.
				grounded = false;
				hopStartY = y;
				velocityX = 0.0;
				velocityY = 0.0;
				velocityZ = 0.0;
			} else {
				y = floor;
				if (crouchTicks > 0) {
					// Agachado cargando el salto: se orienta a golpes hacia donde va.
					turnTowards(yawTo(dx, dz), 60.0F);
					if (--crouchTicks == 0) launch(dx, dz, distance, hurry);
				} else if (distance > 0.55 && landTicks >= (hurry ? 1 : 5) && emote < 0) {
					crouchTicks = hurry ? 2 : 4;
				} else if (distance <= 0.55 && --turnCooldown <= 0) {
					// Parado mira hacia ti, girando a tirones de robot.
					turnCooldown = 6;
					turnTowards(yawTo(player.getX() - x, player.getZ() - z), 45.0F);
				}
			}
		}
		if (!grounded) {
			// Sube escalones de un bloque, pero contra una pared más alta se queda al pie.
			if (solid(x + velocityX, hopStartY + 1.2, z + velocityZ)) {
				velocityX = 0.0;
				velocityZ = 0.0;
			}
			x += velocityX;
			z += velocityZ;
			y += velocityY;
			velocityY -= GRAVITY;
			double floor = groundAt(x, z, Math.max(y, current.y));
			if (velocityY < 0.0 && !Double.isNaN(floor) && y <= floor) {
				y = floor;
				grounded = true;
				landTicks = 0;
				velocityX = 0.0;
				velocityZ = 0.0;
			}
		}
		current = new Vec3(x, y, z);

		// Tonterías cuando está quieto a tu lado.
		boolean resting = grounded && crouchTicks == 0 && landTicks > 8 && distance <= 0.8;
		idleTicks = resting ? idleTicks + 1 : 0;
		if (emote >= 0) {
			if (++emoteTicks >= EMOTE_TICKS || !grounded || distance > 1.5) emote = -1;
		} else if (resting && mood == PetBehavior.Mood.CELEBRATE) {
			startEmote(SPIN_JUMP);
		} else if (resting && mood == PetBehavior.Mood.WAVE) {
			startEmote(DANCE);
		} else if (resting && mood == PetBehavior.Mood.IDLE && idleTicks >= EMOTE_EVERY) {
			startEmote(Math.floorMod(emoteCount * 5 + 2, EMOTES));
			emoteCount++;
		}
	}

	private void startEmote(int id) {
		emote = id;
		emoteTicks = 0;
		idleTicks = 0;
	}

	private void place(Vec3 target) {
		double floor = groundAt(target.x, target.z, target.y + 1.0);
		current = new Vec3(target.x, Double.isNaN(floor) ? target.y : floor, target.z);
		previous = current;
		grounded = true;
		landTicks = 100;
		crouchTicks = 0;
		emote = -1;
	}

	private void launch(double dx, double dz, double distance, boolean hurry) {
		double step = Math.min(distance, hurry ? 3.2 : 1.1);
		velocityY = hurry ? 0.44 : 0.34;
		double airTicks = 2.0 * velocityY / GRAVITY;
		double speed = step / airTicks / Math.max(distance, 1.0E-4);
		velocityX = dx * speed;
		velocityZ = dz * speed;
		grounded = false;
		hopStartY = current.y;
		yaw = yawTo(dx, dz);
	}

	/** Sitio que le toca: a tu lado un poco por detrás; escondida, pegada a tu espalda. */
	private Vec3 target(LocalPlayer player, PetBehavior.Mood mood) {
		float rot = player.yBodyRot * Mth.DEG_TO_RAD;
		double forwardX = -Mth.sin(rot);
		double forwardZ = Mth.cos(rot);
		double rightX = -forwardZ;
		double rightZ = forwardX;
		double sideways = (side.is("Right") ? 1.0 : -1.0) * (mood == PetBehavior.Mood.HIDE ? 0.25 : 1.0);
		double back = mood == PetBehavior.Mood.HIDE ? 0.9 : 0.3;
		return new Vec3(player.getX() + rightX * sideways - forwardX * back, player.getY(), player.getZ() + rightZ * sideways - forwardZ * back);
	}

	/** Altura del suelo en (x, z): lo más alto que tenga colisión sin pasar de un bloque por encima de {@code fromY}. */
	private double groundAt(double x, double z, double fromY) {
		if (level == null) return Double.NaN;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int top = Mth.floor(fromY + 1.0);
		for (int by = top; by >= top - 8; by--) {
			pos.set(Mth.floor(x), by, Mth.floor(z));
			VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
			if (shape.isEmpty()) continue;
			double surface = by + shape.max(Direction.Axis.Y);
			if (surface <= fromY + 1.01) return surface;
		}
		return Double.NaN;
	}

	private boolean solid(double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);
		return level != null && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	private static float yawTo(double dx, double dz) {
		return (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
	}

	/** Gira de golpe (sin interpolar, como un robot), como mucho {@code maxStep} grados. */
	private void turnTowards(float wanted, float maxStep) {
		float diff = Mth.wrapDegrees(wanted - yaw);
		if (Math.abs(diff) < 8.0F) return;
		yaw += Mth.clamp(diff, -maxStep, maxStep);
	}

	// ---------------------------------------------------------------- dibujo

	/** Valor a escalones, para que los movimientos parezcan de robot. */
	private static float steps(float value, float step) {
		return Mth.floor(value / step) * step;
	}

	private static float noise(long seed) {
		long h = seed * 0x9E3779B97F4A7C15L;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return (h & 0xFFFF) / 65535.0F;
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		boolean preview = com.freedomclient.cosmetic.CosmeticPreview.drawing();
		if (current == null && !preview) return;
		if (base == null) build();
		float partial = state.ageInTicks % 1.0F;
		// En la vista previa del menú, en el suelo al lado del jugador y mirando al frente.
		Vec3 model = preview ? new Vec3(-12.0 / 16.0, 1.5, 0.0)
				: PetFollower.worldOffsetToModel(previous.lerp(current, partial).subtract(state.x, state.y, state.z), state.bodyRot, state.scale);
		float time = ticks + partial;
		PetBehavior.Mood mood = PetBehavior.mood();
		boolean sleeping = mood == PetBehavior.Mood.SLEEP;

		// Postura: todo en píxeles del modelo y grados.
		float lift = 0.0F;
		float spinY = 0.0F;
		float leanX = 0.0F;
		float swayZ = 0.0F;
		float squash = 1.0F;
		float headLift = 0.0F;
		float headTurn = 0.0F;
		float headTilt = 0.0F;
		float headNod = 0.0F;
		float teethDrop = 0.0F;
		float blink = 0.0F;
		float flameSize = 1.0F;
		float jitterX = 0.0F;
		float jitterZ = 0.0F;
		// Mirada: cambia de sitio a saltos cada poco (movimientos sacádicos de animatrónico).
		long gaze = (long) (time / 22.0F);
		float lookX = (Mth.floor(noise(gaze) * 3.0F) - 1) * 0.7F;
		float lookY = (Mth.floor(noise(gaze + 91) * 3.0F) - 1) * 0.4F;
		float rightLookX = lookX;
		float rightLookY = lookY;
		if (time % 70.0F < 4.0F) blink = 1.0F;

		if (!grounded) {
			// En el aire: se estira al subir, la cabeza se queda atrás en el muelle al bajar e inclina hacia delante.
			squash = velocityY > 0.0 ? 1.12F : 1.04F;
			headLift = velocityY > 0.0 ? 1.0F : 2.4F;
			leanX = 10.0F;
			lookX = rightLookX = 0.0F;
			lookY = rightLookY = -0.3F;
		} else if (crouchTicks > 0) {
			// Agachado antes del salto, a golpes.
			squash = 0.78F;
			headLift = -0.6F;
			leanX = 4.0F;
		} else if (landTicks < 14) {
			// Aterrizaje: "clonc" en tres tiempos y la cabeza rebotando en el muelle.
			float t = landTicks + partial;
			squash = t < 2.0F ? 0.8F : t < 4.0F ? 1.07F : t < 6.0F ? 0.97F : 1.0F;
			headLift = 2.6F * Mth.cos(t * 1.4F) * (float) Math.exp(-t * 0.32F);
		}

		if (sleeping && grounded) {
			blink = 1.0F;
			flameSize = 0.0F;
			headLift = -0.7F;
			headNod = 9.0F;
			squash = 1.0F + Mth.sin(time * 0.08F) * 0.025F;
		} else if (mood == PetBehavior.Mood.HIDE) {
			// Tiembla de miedo con los ojos muy abiertos mirando a todos lados.
			jitterX = (noise(ticks * 3) - 0.5F) * 0.5F;
			jitterZ = (noise(ticks * 3 + 1) - 0.5F) * 0.5F;
			blink = 0.0F;
			lookX = rightLookX = (noise(ticks / 4) - 0.5F) * 1.6F;
			flameSize = 0.5F;
		}

		if (emote >= 0) {
			float p = Math.min(1.0F, (emoteTicks + partial) / EMOTE_TICKS);
			float wave = Mth.sin(p * Mth.PI);
			switch (emote) {
				case 0 -> {
					// Ojos locos: las pupilas dan vueltas cada una hacia un lado.
					float a = p * Mth.PI * 8.0F;
					lookX = Mth.cos(a) * 0.8F;
					lookY = Mth.sin(a) * 0.6F;
					rightLookX = Mth.cos(-a + 1.3F) * 0.8F;
					rightLookY = Mth.sin(-a + 1.3F) * 0.6F;
					headTilt = Mth.sin(p * Mth.PI * 4.0F) * 8.0F;
					blink = 0.0F;
				}
				case 1 -> {
					// Vuelta completa en cuatro golpes de 90º, con un "clonc" en cada uno.
					float quarter = p * 4.0F;
					float inQuarter = quarter - Mth.floor(quarter);
					spinY = Mth.floor(quarter) * 90.0F + (inQuarter > 0.7F ? (inQuarter - 0.7F) / 0.3F * 90.0F : 0.0F);
					squash = inQuarter < 0.15F ? 0.9F : 1.0F;
				}
				case 2 -> {
					// Castañetea los dientes y vibra la cabeza.
					teethDrop = (Mth.floor(time * 1.5F) % 2 == 0 ? 1.2F : 0.0F) * wave;
					headTurn = (Mth.floor(time) % 2 == 0 ? 3.0F : -3.0F) * wave;
					lookY = rightLookY = 0.5F;
				}
				case 3 -> {
					// Saca la cabeza con el muelle ("¡boing!") y la vela se aviva.
					float t = p * EMOTE_TICKS;
					headLift = t < 6.0F ? t / 6.0F * 7.0F : 7.0F * Mth.cos((t - 6.0F) * 0.55F) * (float) Math.exp(-(t - 6.0F) * 0.12F);
					flameSize = 1.0F + wave * 0.8F;
					blink = 0.0F;
					lookY = rightLookY = -0.5F;
				}
				case DANCE -> {
					// Baile de robot: se balancea a escalones y da saltitos.
					swayZ = steps(Mth.sin(p * Mth.PI * 6.0F) * 14.0F, 7.0F);
					lift = Math.abs(Mth.sin(p * Mth.PI * 6.0F)) * 2.5F;
					headTurn = -swayZ;
					teethDrop = lift > 1.5F ? 0.8F : 0.0F;
				}
				case 5 -> {
					// Mira a un lado y a otro a tirones, ladeando la cabeza, como si no entendiera nada.
					headTurn = p < 0.3F ? 0.0F : p < 0.55F ? 40.0F : p < 0.8F ? -40.0F : 0.0F;
					headTilt = p < 0.3F ? 0.0F : p < 0.8F ? 14.0F : 0.0F;
					lookX = rightLookX = headTurn > 0.0F ? -0.8F : headTurn < 0.0F ? 0.8F : 0.0F;
				}
				case 6 -> {
					// Intenta un salto enorme, se cae de morros, se queda tirada y se levanta de golpe.
					if (p < 0.25F) {
						float q = p / 0.25F;
						lift = 4.0F * q * (1.0F - q) * 6.0F;
						leanX = q * 90.0F;
					} else if (p < 0.75F) {
						leanX = 90.0F;
						lookX = Mth.cos(p * 40.0F) * 0.8F;
						rightLookX = -lookX;
						teethDrop = 0.6F;
					} else {
						float q = (p - 0.75F) / 0.25F;
						leanX = q < 0.4F ? 90.0F : q < 0.6F ? 40.0F : 0.0F;
						squash = q > 0.6F && q < 0.8F ? 0.85F : 1.0F;
					}
				}
				case SPIN_JUMP -> {
					// Celebración: saltos girando sobre sí misma, vela a tope y dientes castañeteando.
					float jump = (p * 3.0F) % 1.0F;
					lift = 4.0F * jump * (1.0F - jump) * 4.0F;
					spinY = jump * 360.0F;
					squash = jump < 0.1F || jump > 0.9F ? 0.85F : 1.1F;
					flameSize = 1.8F;
					teethDrop = (Mth.floor(time * 1.5F) % 2 == 0 ? 1.0F : 0.0F);
					blink = 0.0F;
				}
				default -> {
				}
			}
		}

		poseStack.pushPose();
		poseStack.translate(model.x, model.y, model.z);
		poseStack.mulPose(Axis.YP.rotationDegrees(preview ? 0.0F : yaw - state.bodyRot));
		float s = size.getFloat();
		poseStack.scale(s, s, s);
		poseStack.translate(jitterX / 16.0F, -lift / 16.0F, jitterZ / 16.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(spinY));
		poseStack.mulPose(Axis.ZP.rotationDegrees(swayZ));
		// Se inclina hacia delante sobre el borde delantero del plato.
		if (leanX != 0.0F) {
			poseStack.translate(0.0F, 0.0F, -6.0F / 16.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(leanX));
			poseStack.translate(0.0F, 0.0F, 6.0F / 16.0F);
		}
		poseStack.scale(2.0F - squash, squash, 2.0F - squash);
		base.draw(poseStack, collector, light);

		// Muelle estirado hasta donde esté la cabeza.
		float springLift = Math.max(0.0F, headLift);
		poseStack.pushPose();
		poseStack.translate(0.0F, NECK_Y / 16.0F, 0.0F);
		poseStack.scale(1.0F, 1.0F + springLift / 4.0F, 1.0F);
		poseStack.translate(0.0F, -NECK_Y / 16.0F, 0.0F);
		spring.draw(poseStack, collector, light);
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0.0F, (NECK_Y - headLift) / 16.0F, 0.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(headTurn));
		poseStack.mulPose(Axis.ZP.rotationDegrees(headTilt));
		poseStack.mulPose(Axis.XP.rotationDegrees(headNod));
		poseStack.translate(0.0F, -NECK_Y / 16.0F, 0.0F);
		head.draw(poseStack, collector, light);
		for (int side = -1; side <= 1; side += 2) {
			float ex = side * EYE_X;
			poseStack.pushPose();
			poseStack.translate((ex + (side < 0 ? lookX : rightLookX)) / 16.0F, (EYE_Y + (side < 0 ? lookY : rightLookY)) / 16.0F, -7.3F / 16.0F);
			iris.draw(poseStack, collector, light);
			poseStack.popPose();
			if (blink > 0.0F) {
				poseStack.pushPose();
				poseStack.translate(ex / 16.0F, (EYE_Y - 2.0F) / 16.0F, -7.3F / 16.0F);
				poseStack.scale(1.0F, blink, 1.0F);
				lid.draw(poseStack, collector, light);
				poseStack.popPose();
			}
		}
		poseStack.pushPose();
		poseStack.translate(0.0F, (NECK_Y + teethDrop) / 16.0F, -5.6F / 16.0F);
		teeth.draw(poseStack, collector, light);
		poseStack.popPose();
		if (flameSize > 0.0F) {
			// Llama que parpadea y brilla en la oscuridad.
			float flicker = flameSize * (1.0F + Mth.sin(time * 0.9F) * 0.12F + (noise(ticks) - 0.5F) * 0.15F);
			poseStack.translate(0.0F, (NECK_Y - 16.6F) / 16.0F, 0.0F);
			poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.35F) * 6.0F));
			poseStack.scale(Math.min(flicker, 1.3F), flicker, Math.min(flicker, 1.3F));
			flame.drawGlow(poseStack, collector);
		}
		poseStack.popPose();
		poseStack.popPose();
	}
}
