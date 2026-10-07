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
		double[] out = new double[5];
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
			sb.append(first ? "" : ",").append(arr(out[0], out[1], out[2], out[3], out[4]));
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
