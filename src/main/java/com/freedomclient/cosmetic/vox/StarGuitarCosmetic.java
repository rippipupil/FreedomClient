package com.freedomclient.cosmetic.vox;

import com.freedomclient.particle.GlowParticle;
import com.freedomclient.particle.PixelParticles;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayDeque;

/**
 * Star Guitar (Música): guitarra metalera en forma de estrella a la espalda, en diagonal. Cuerpo con dos puntas
 * largas hacia abajo y el cuerno de arriba, tapa de arce flameado en sunburst tabaco (marrón oscuro en el borde y
 * ámbar en el centro) con su filete crema, dos humbuckers con las bobinas crema, puente Floyd Rose cromado con la
 * palanca, potenciómetros dorados, diapasón de palisandro con puntos y la pala negra en V con seis clavijas.
 */
public class StarGuitarCosmetic extends MusicGuitarCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("star_guitar",
			'c', 0xFFF2E3C2, 'B', 0xFF3A1A0C, 'b', 0xFF6E2E10, 'o', 0xFFB8541A, 'a', 0xFFE88A2A, 'y', 0xFFFFB84A,
			'k', 0xFF141216, 'K', 0xFF2A262C, 's', 0xFFE2E6EE, 'S', 0xFF9AA0AC, 'g', 0xFFE8B83A, 'G', 0xFFB08420,
			'n', 0xFF7A4020, 'f', 0xFF3A2016, 'w', 0xFFF4F0E4);
	/** Silueta del cuerpo vista de frente: 24 de ancho; el mástil entra por la fila 7, entre los dos cuernos. */
	private static final String[] BODY_MASK = {
			"##......................",
			"###.....................",
			".###....................",
			".####...................",
			"..####...............##.",
			"..#####.............###.",
			"...######.........####..",
			"...##################...",
			"...##################...",
			"....################....",
			"....################....",
			"...##################...",
			"...##################...",
			"..####################..",
			"..####################..",
			".##########..##########.",
			".#########....#########.",
			"#########......#########",
			"########........########",
			"#######..........#######",
			"######............######",
			"#####..............#####",
			"####................####",
			"###..................###",
			"##....................##",
	};
	/** Pala negra en V, abierta por arriba. */
	private static final String[] HEADSTOCK = {
			"kk....kk",
			"kkk..kkk",
			"kkkkkkkk",
			".kkkkkk.",
			".kkkkkk.",
			"..kkkk..",
	};

	public final NumberSetting size = add(new NumberSetting("Size", "How big the guitar is.", 0.75, 0.5, 1.1, 0.05, "x"));
	public final ModeSetting side = add(new ModeSetting("Neck side", "Which shoulder the neck sticks out over.", "Left", "Left", "Right"));
	public final BooleanSetting notes = add(new BooleanSetting("Music notes", "Little music notes float out of the guitar now and then.", true));

	private final RandomSource random = RandomSource.create();
	private Vox.Shape guitar;

	public StarGuitarCosmetic() {
		super("Star Guitar", "Music: a star-shaped metal guitar on your back with a tobacco sunburst flame top, two humbuckers, "
				+ "a chrome tremolo bridge and a black V headstock. Plays little music notes.");
	}

	/**
	 * Pinta la silueta con el sunburst según la distancia al borde: filete crema, marrón oscuro, marrón, naranja,
	 * ámbar y el centro más claro.
	 */
	private static String[] sunburst(String[] mask) {
		String ramp = "cBboay";
		int h = mask.length;
		int w = mask[0].length();
		int[][] dist = new int[h][w];
		ArrayDeque<int[]> queue = new ArrayDeque<>();
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				if (mask[y].charAt(x) != '#') continue;
				dist[y][x] = Integer.MAX_VALUE;
				for (int[] d : dirs) {
					int nx = x + d[0];
					int ny = y + d[1];
					if (nx < 0 || ny < 0 || nx >= w || ny >= h || mask[ny].charAt(nx) != '#') {
						dist[y][x] = 1;
						queue.add(new int[] {x, y});
						break;
					}
				}
			}
		}
		while (!queue.isEmpty()) {
			int[] p = queue.poll();
			for (int[] d : dirs) {
				int nx = p[0] + d[0];
				int ny = p[1] + d[1];
				if (nx < 0 || ny < 0 || nx >= w || ny >= h || dist[ny][nx] <= dist[p[1]][p[0]] + 1) continue;
				dist[ny][nx] = dist[p[1]][p[0]] + 1;
				queue.add(new int[] {nx, ny});
			}
		}
		String[] rows = new String[h];
		for (int y = 0; y < h; y++) {
			StringBuilder row = new StringBuilder(w);
			for (int x = 0; x < w; x++) row.append(dist[y][x] == 0 ? '.' : ramp.charAt(Math.min(dist[y][x], ramp.length()) - 1));
			rows[y] = row.toString();
		}
		return rows;
	}

	private void build() {
		// La cara de delante (tapa, pastillas y cuerdas) mira hacia +z, hacia fuera de la espalda.
		String[] back = BODY_MASK.clone();
		for (int i = 0; i < back.length; i++) back[i] = back[i].replace('#', 'B');
		guitar = new Vox.Shape(PALETTE)
				// Aros marrón oscuro y, encima, la tapa en sunburst con el filete crema.
				.art(back, -12.0F, -7.0F, -1.2F, 2.0F)
				.art(sunburst(BODY_MASK), -12.0F, -7.0F, 0.8F, 0.45F)
				// Dos humbuckers: marco negro y dos bobinas crema cada uno.
				.box('k', -2.6F, 0.6F, 1.2F, 5.2F, 2.0F, 0.3F)
				.box('c', -2.2F, 0.85F, 1.3F, 4.4F, 0.65F, 0.35F)
				.box('c', -2.2F, 1.75F, 1.3F, 4.4F, 0.65F, 0.35F)
				.box('k', -2.6F, 3.6F, 1.2F, 5.2F, 2.0F, 0.3F)
				.box('c', -2.2F, 3.85F, 1.3F, 4.4F, 0.65F, 0.35F)
				.box('c', -2.2F, 4.75F, 1.3F, 4.4F, 0.65F, 0.35F)
				// Puente Floyd Rose cromado con sus afinadores finos y la palanca hacia abajo a la derecha.
				.box('s', -3.0F, 6.4F, 1.2F, 6.0F, 1.6F, 0.5F)
				.box('S', -2.7F, 6.6F, 1.6F, 5.4F, 0.5F, 0.2F)
				.box('S', -3.0F, 8.0F, 1.2F, 6.0F, 0.3F, 0.5F)
				.box('s', 3.0F, 7.0F, 1.5F, 1.2F, 0.4F, 0.4F)
				.box('s', 4.0F, 7.0F, 1.9F, 0.4F, 6.5F, 0.4F)
				.box('S', 4.0F, 13.1F, 1.9F, 0.4F, 0.4F, 0.4F)
				// Tres potenciómetros dorados a lo largo de la punta derecha.
				.box('g', 4.6F, 9.0F, 1.2F, 1.2F, 1.2F, 0.45F)
				.box('g', 6.2F, 10.6F, 1.2F, 1.2F, 1.2F, 0.45F)
				.box('g', 7.8F, 12.2F, 1.2F, 1.2F, 1.2F, 0.45F)
				.box('G', 4.9F, 9.3F, 1.6F, 0.6F, 0.6F, 0.15F)
				.box('G', 6.5F, 10.9F, 1.6F, 0.6F, 0.6F, 0.15F)
				.box('G', 8.1F, 12.5F, 1.6F, 0.6F, 0.6F, 0.15F)
				// Selector de pastillas en el cuerno de arriba, con la punta dorada.
				.box('K', -7.6F, -1.2F, 1.2F, 1.2F, 1.2F, 0.3F)
				.box('g', -7.25F, -2.2F, 1.4F, 0.5F, 1.4F, 0.5F)
				// Mástil de caoba con el diapasón de palisandro y su filete crema.
				.box('n', -1.2F, -18.0F, -0.6F, 2.4F, 19.0F, 1.6F)
				.box('f', -1.1F, -18.0F, 1.0F, 2.2F, 18.0F, 0.3F)
				.box('c', -1.25F, -18.0F, 1.0F, 0.15F, 18.0F, 0.3F)
				.box('c', 1.1F, -18.0F, 1.0F, 0.15F, 18.0F, 0.3F)
				.box('w', -1.2F, -18.4F, 0.9F, 2.4F, 0.4F, 0.5F)
				// Pala negra en V con un filo cromado.
				.art(HEADSTOCK, -4.0F, -24.4F, -0.5F, 1.3F)
				.box('K', -1.2F, -19.0F, -0.4F, 2.4F, 0.6F, 1.2F);
		// Trastes y los puntos de nácar (doble en el 12).
		for (int i = 0; i < 12; i++) guitar.box('S', -1.1F, -16.6F + i * 1.45F, 1.25F, 2.2F, 0.18F, 0.12F);
		for (int fret : new int[] {2, 4, 6, 8}) guitar.box('w', -0.3F, -16.6F + fret * 1.45F + 0.45F, 1.28F, 0.6F, 0.6F, 0.1F);
		guitar.box('w', -0.9F, -16.6F + 11 * 1.45F + 0.45F, 1.28F, 0.5F, 0.5F, 0.1F)
				.box('w', 0.4F, -16.6F + 11 * 1.45F + 0.45F, 1.28F, 0.5F, 0.5F, 0.1F);
		// Tres clavijas cromadas a cada lado de la pala, siguiendo la V.
		for (int i = 0; i < 3; i++) {
			float y = -24.0F + i * 1.5F;
			float spread = 4.0F - i * 0.5F;
			guitar.box('s', -spread - 1.2F, y, -0.2F, 1.2F, 0.7F, 0.7F)
					.box('s', spread, y, -0.2F, 1.2F, 0.7F, 0.7F)
					.box('S', -spread + 0.5F, y, 0.8F, 0.5F, 0.5F, 0.3F)
					.box('S', spread - 1.0F, y, 0.8F, 0.5F, 0.5F, 0.3F);
		}
		// Seis cuerdas finas desde la cejilla hasta el puente.
		for (int i = 0; i < 3; i++) guitar.box('s', -0.6F + i * 0.5F, -18.0F, 1.7F, 0.12F, 25.0F, 0.08F);
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		if (guitar == null) build();
		poseStack.pushPose();
		float scale = size.getFloat() * 0.64F;
		if (inHands(state)) {
			inHands(parent, poseStack, state, scale, scale, 1.2F);
		} else {
			onBackDiagonal(parent, poseStack, state, scale, side.is("Left"), 1.2F);
		}
		// El cuerpo de la guitarra queda en el centro de la espalda y el mástil asoma por encima del hombro.
		poseStack.translate(0.0F, -5.0F / 16.0F, 0.0F);
		guitar.draw(poseStack, collector, light);
		poseStack.popPose();
	}

	/** Notas musicales que salen de la guitarra y suben despacio (solo en tercera persona). */
	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (!notes.get() || client.level == null || player == null || player.isInvisible() || client.isPaused()) return;
		if (client.options.getCameraType().isFirstPerson() || random.nextFloat() > 0.06F) return;
		float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
		double backX = Mth.sin(yaw);
		double backZ = -Mth.cos(yaw);
		double rightX = -Mth.cos(yaw);
		double rightZ = -Mth.sin(yaw);
		double lateral = (random.nextDouble() - 0.5) * 0.5;
		double x = player.getX() + rightX * lateral + backX * 0.4;
		double y = player.getY() + 0.9 + random.nextDouble() * 0.5;
		double z = player.getZ() + rightZ * lateral + backZ * 0.4;
		GlowParticle note = new GlowParticle(client.level, x, y, z, (random.nextDouble() - 0.5) * 0.01, 0.025, (random.nextDouble() - 0.5) * 0.01,
				PixelParticles.sprite("note"), 0.07F, 0.09F, 30 + random.nextInt(15));
		client.particleEngine.add(note.thirdPersonOnly());
	}
}
