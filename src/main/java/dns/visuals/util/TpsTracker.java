package dns.visuals.util;

/**
 * Client-side TPS estimate.
 *
 * NOTE: only the server truly knows its TPS. We approximate it from the rate of
 * world-time update packets, smoothed and capped at 20. Good enough for a HUD readout.
 */
public class TpsTracker {
	private long lastTime = 0;
	private double tps = 20.0;

	/** Call when a world-time update is received from the server. */
	public void onTimeUpdate(long ticksAdvanced) {
		long now = System.currentTimeMillis();
		if (lastTime != 0 && ticksAdvanced > 0) {
			double seconds = (now - lastTime) / 1000.0;
			if (seconds > 0) {
				double instant = ticksAdvanced / seconds;
				tps = Math.min(20.0, tps * 0.85 + instant * 0.15);
			}
		}
		lastTime = now;
	}

	public double get() {
		return tps;
	}
}
