import com.freedomclient.cosmetic.vox.GhostMotion;
import com.freedomclient.cosmetic.vox.GhostMotion.Emotion;
import com.freedomclient.cosmetic.vox.GhostMotion.Mood;
import com.freedomclient.cosmetic.vox.GhostMotion.Pose;

/** Saca en JSON las mismas muestras que check_java.js saca del diseño, para compararlas (ver check_java.js). */
public class CheckGhost {
	public static void main(String[] args) {
		StringBuilder poses = new StringBuilder(), faces = new StringBuilder(), limbs = new StringBuilder();
		Pose from = new Pose().set(3, 20, -4, 2, 0.1, -0.3), p = new Pose();
		double[] l = new double[3];
		for (Mood m : Mood.values()) for (double t = 0; t < 30; t += 0.43) for (double s : new double[] {0, 0.3, 0.79, 1.3, 2.7}) {
			sep(poses).append(pose(GhostMotion.pose(m, t, s, null, p))).append(',').append(pose(GhostMotion.pose(m, t, s, from, p)));
			for (Emotion e : Emotion.values()) {
				sep(faces).append(GhostMotion.face(m, e, t, s));
				GhostMotion.limbs(m, e, t, s, s, l);
				sep(limbs).append(arr(l));
			}
		}
		StringBuilder aura = new StringBuilder(), sparkles = new StringBuilder(), flowers = new StringBuilder(), petals = new StringBuilder();
		double[] q = new double[8];
		for (int i = 0; i < 6; i++) for (double t = 0; t < 60; t += 0.37) { GhostMotion.aura(i, t, q); sep(aura).append(arr(q)); }
		double[] o = new double[5];
		for (double[] sp : GhostMotion.SPARKLES) for (double t = 0; t < 40; t += 0.61) { GhostMotion.sparkle(sp, t, o); sep(sparkles).append(arr(o)); }
		double[] f = new double[3];
		for (double a = -0.2; a < 3.3; a += 0.07) for (double seed : new double[] {0, 0.37, 0.9}) sep(flowers).append(GhostMotion.flower(a, seed, 3, f) ? arr(f) : "[]");
		for (double a = -0.1; a < 1.3; a += 0.05) sep(petals).append(GhostMotion.petal(a, 2.5, -1.5, 4.2, o) ? arr(o) : "[]");
		System.out.println("{\"poses\":[" + poses + "],\"faces\":[" + faces + "],\"limbs\":[" + limbs + "],\"aura\":[" + aura + "],\"sparkles\":["
				+ sparkles + "],\"flowers\":[" + flowers + "],\"petals\":[" + petals + "]}");
	}

	static StringBuilder sep(StringBuilder sb) {
		return sb.length() > 0 ? sb.append(',') : sb;
	}

	static String pose(Pose p) {
		return arr(new double[] {p.x, p.y, p.z, p.yaw, p.pitch, p.roll});
	}

	static String arr(double[] v) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < v.length; i++) sb.append(i > 0 ? "," : "").append(Double.isFinite(v[i]) ? Double.toString(v[i]) : "0");
		return sb.append("]").toString();
	}
}
