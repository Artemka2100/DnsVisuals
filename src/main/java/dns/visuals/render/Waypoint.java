package dns.visuals.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Holds a single navigation target set by the player via the chat command:
 *   .goto <x> <y> <z>   set target
 *   .goto <x> <z>       set target (keeps current Y)
 *   .goto clear         remove target
 *
 * The Waypoint HUD module draws an on-screen arrow pointing toward this target.
 */
public final class Waypoint {
	private static boolean active = false;
	private static double tx, ty, tz;

	private Waypoint() {}

	public static boolean isActive() { return active; }
	public static double x() { return tx; }
	public static double y() { return ty; }
	public static double z() { return tz; }

	public static void set(double x, double y, double z) {
		tx = x; ty = y; tz = z; active = true;
	}

	public static void clear() { active = false; }

	/**
	 * Handle an outgoing chat line. Returns true if it was a .goto command and should NOT be sent
	 * to the server.
	 */
	public static boolean handleCommand(String message) {
		if (message == null) return false;
		String m = message.trim();
		if (!m.toLowerCase().startsWith(".goto")) return false;

		MinecraftClient mc = MinecraftClient.getInstance();
		String rest = m.substring(5).trim();

		if (rest.equalsIgnoreCase("clear") || rest.equalsIgnoreCase("off")) {
			clear();
			info(mc, "Waypoint cleared");
			return true;
		}

		String[] parts = rest.split("\\s+");
		try {
			if (parts.length >= 3) {
				set(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
			} else if (parts.length == 2) {
				double y = mc.player != null ? mc.player.getY() : 64;
				set(Double.parseDouble(parts[0]), y, Double.parseDouble(parts[1]));
			} else {
				info(mc, "Usage: .goto <x> <y> <z>  |  .goto <x> <z>  |  .goto clear");
				return true;
			}
			info(mc, "Waypoint set: " + (int) tx + " " + (int) ty + " " + (int) tz);
		} catch (NumberFormatException e) {
			info(mc, "Bad coordinates. Usage: .goto <x> <y> <z>");
		}
		return true;
	}

	private static void info(MinecraftClient mc, String s) {
		if (mc.player != null) {
			mc.player.sendMessage(Text.literal("\u00A76[DnsVisuals]\u00A7r " + s), false);
		}
	}
}
