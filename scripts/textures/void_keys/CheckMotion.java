import com.freedomclient.cosmetic.vox.VoidKeysMotion;
import com.freedomclient.cosmetic.vox.VoidKeysMotion.Pose;
import com.freedomclient.cosmetic.vox.VoidKeysMotion.Play;
import com.freedomclient.cosmetic.vox.VoidKeysMotion.Score;

/** Saca en JSON las mismas muestras que check_java.js saca del diseño, para compararlas (ver check_java.js). */
public class CheckMotion {
	public static void main(String[] args) throws Exception {
		String text = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(args[0])));
		Score score = new Score(text, 205.3);
		StringBuilder sb = new StringBuilder("{");
		// Teclas.
		sb.append("\"keys\":[");
		double[] out = new double[6];
		boolean first = true;
		for (int key = 0; key < VoidKeysMotion.KEYS; key++) for (double t : new double[] {0, 1.3, 17.77, 123.4}) {
			VoidKeysMotion.keyPose(VoidKeysMotion.keyAngle(key), t, out);
			sb.append(first ? "" : ",").append(arr(out[0], out[1], out[2]));
			first = false;
		}
		sb.append("],\"noteKeys\":[");
		for (int n = 0; n < 44; n++) sb.append(n > 0 ? "," : "").append(VoidKeysMotion.noteKey(n));
		// Partículas.
		sb.append("],\"particles\":[");
		first = true;
		for (double[][] set : new double[][][] {VoidKeysMotion.STREAK_DATA, VoidKeysMotion.ROCK_DATA}) for (double[] p : set) for (double t : new double[] {0, 2.5, 33.3, 400}) {
			VoidKeysMotion.particle(p, t, out);
			sb.append(first ? "" : ",").append(arr(out[0], out[1], out[2], out[3], out[4], out[5]));
			first = false;
		}
		// Trozos de terreno: sus piezas (entero, partido, polvo).
		sb.append("],\"rocks\":[");
		first = true;
		double[][] pieces = new double[5][];
		for (double[] p : VoidKeysMotion.ROCK_DATA) for (double t = 0; t < 60; t += 0.7) {
			int n = VoidKeysMotion.rockPieces(p, t, pieces);
			sb.append(first ? "" : ",").append("[");
			for (int i = 0; i < n; i++) sb.append(i > 0 ? "," : "").append(arr(pieces[i]));
			sb.append("]");
			first = false;
		}
		// Poses.
		sb.append("],\"poses\":[");
		first = true;
		Play play = new Play();
		for (double t = 0; t < 40; t += 0.37) for (double[] c : new double[][] {{1, 0, 0}, {1, 0.6, 0}, {1, 1.4, 0}, {1, 0, 0.3}, {1, 1, -0.2}, {0, 1, 0}, {0, 0, 0.1}}) {
			Pose p = VoidKeysMotion.figurePose(t, c[0] == 1, c[1], c[2], null, 0);
			sb.append(first ? "" : ",").append(pose(p));
			first = false;
		}
		for (double t = 0; t < 205; t += 0.53) for (double in : new double[] {0.4, 1}) {
			Pose p = VoidKeysMotion.figurePose(t, true, 0, 0, score.play(t, play), in);
			sb.append(",").append(pose(p)).append(",").append(arr(play.facing, play.down[0], play.down[1], play.reach[0], play.reach[1], play.look));
		}
		// Luz de las teclas.
		sb.append("],\"light\":[");
		first = true;
		for (int key = 0; key < VoidKeysMotion.KEYS; key++) for (double t = 0; t < 205; t += 1.7) {
			double hit = score.lastHit(key, t);
			sb.append(first ? "" : ",").append(fmt(Double.isNaN(hit) ? 0 : VoidKeysMotion.keyLight(t - hit)));
			first = false;
		}
		// Detalles: ola de luz, teclas fantasma, círculo del suelo y orbe / pilar en los drops.
		sb.append("],\"chase\":[");
		first = true;
		for (int key = 0; key < VoidKeysMotion.KEYS; key++) for (double t = 0; t < 40; t += 0.13) {
			sb.append(first ? "" : ",").append(fmt(VoidKeysMotion.chaseLight(VoidKeysMotion.keyAngle(key), t)));
			first = false;
		}
		sb.append("],\"ghostKeys\":[");
		for (int i = 0; i < 5000; i++) sb.append(i > 0 ? "," : "").append(VoidKeysMotion.ghostKey(i));
		sb.append("],\"ghost\":[");
		first = true;
		for (int key = 0; key < VoidKeysMotion.KEYS; key++) for (double t = 0; t < 60; t += 0.11) {
			sb.append(first ? "" : ",").append(fmt(VoidKeysMotion.ghostLight(key, t)));
			first = false;
		}
		sb.append("],\"sigil\":[");
		first = true;
		for (double t = 0; t < 80; t += 0.29) {
			double[] q = new double[3 + 2 * VoidKeysMotion.SIGIL_SPIKES];
			VoidKeysMotion.sigil(t, q);
			for (int k = 0; k < VoidKeysMotion.SIGIL_SPIKES; k++) {
				q[3 + 2 * k] = VoidKeysMotion.spikeAngle(k);
				q[4 + 2 * k] = VoidKeysMotion.spikeHeight(k, t);
			}
			sb.append(first ? "" : ",").append(arr(q));
			first = false;
		}
		sb.append("],\"orb\":[");
		first = true;
		double[] boom = new double[4];
		double[][] shards = new double[10][];
		for (double songT = 0; songT < 205; songT += 0.047) {
			double d = VoidKeysMotion.dropDelta(songT), t = songT * 1.37 + 3, y = VoidKeysMotion.orbY(t);
			boolean on = VoidKeysMotion.drop(d, boom);
			double[] row = {Double.isNaN(d) ? 0 : d, y, VoidKeysMotion.orbScale(t, d), VoidKeysMotion.orbRot(t)};
			sb.append(first ? "" : ",").append(on ? arr(row[0], row[1], row[2], row[3], boom[0], boom[1], boom[2], boom[3]) : arr(row)).append(",[");
			int n = VoidKeysMotion.dropShards(d, y, shards);
			for (int i = 0; i < n; i++) sb.append(i > 0 ? "," : "").append(arr(shards[i]));
			sb.append("]");
			first = false;
		}
		System.out.println(sb.append("]}"));
	}

	static String pose(Pose p) {
		return arr(p.lift, p.pitch, p.yaw, p.roll, p.headX, p.headZ, p.headYaw, p.rArmX, p.rArmZ, p.lArmX, p.lArmZ, p.rLegX, p.rLegZ, p.lLegX, p.lLegZ);
	}

	static String arr(double... v) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < v.length; i++) sb.append(i > 0 ? "," : "").append(fmt(v[i]));
		return sb.append("]").toString();
	}

	static String fmt(double v) {
		return Double.isFinite(v) ? Double.toString(v) : "0";
	}
}
