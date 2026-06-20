package dns.visuals.util;

import java.awt.Color;

/** Color helpers: ARGB packing, alpha overrides, and a rainbow cycle. */
public final class ColorUtil {
	private ColorUtil() {}

	public static int argb(int a, int r, int g, int b) {
		return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	public static int withAlpha(int color, int alpha) {
		return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
	}

	public static int withAlpha(int color, double alphaFraction) {
		return withAlpha(color, (int) (Math.max(0, Math.min(1, alphaFraction)) * 255));
	}

	/** Linear blend between two ARGB colors. */
	public static int blend(int from, int to, double t) {
		t = Math.max(0, Math.min(1, t));
		int a = (int) (((from >> 24) & 0xFF) + (((to >> 24) & 0xFF) - ((from >> 24) & 0xFF)) * t);
		int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return argb(a, r, g, b);
	}

	/** Animated rainbow color. {@code offset} shifts the hue (e.g. per element). */
	public static int rainbow(double speed, double offset) {
		float hue = (float) (((System.currentTimeMillis() % (long) (10000 / Math.max(0.01, speed)))
				/ (10000.0 / Math.max(0.01, speed))) + offset);
		hue = hue - (float) Math.floor(hue);
		return 0xFF000000 | (Color.HSBtoRGB(hue, 0.8f, 1.0f) & 0x00FFFFFF);
	}
}
