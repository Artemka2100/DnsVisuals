package dns.visuals.util;

/** Tiny easing helpers for smooth GUI animations. */
public final class AnimationUtil {
	private AnimationUtil() {}

	/** Move {@code current} toward {@code target} by a frame-rate independent amount. */
	public static double approach(double current, double target, double speed, double delta) {
		double factor = 1 - Math.pow(0.5, delta * speed);
		double next = current + (target - current) * factor;
		if (Math.abs(target - next) < 0.001) return target;
		return next;
	}

	public static double easeOutCubic(double t) {
		t = clamp01(t);
		return 1 - Math.pow(1 - t, 3);
	}

	public static double easeInOutQuad(double t) {
		t = clamp01(t);
		return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
	}

	public static double clamp01(double v) {
		return Math.max(0, Math.min(1, v));
	}

	public static double lerp(double a, double b, double t) {
		return a + (b - a) * clamp01(t);
	}
}
