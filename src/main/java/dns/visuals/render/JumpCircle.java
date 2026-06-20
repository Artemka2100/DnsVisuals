package dns.visuals.render;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

/**
 * Spawns a ring or square of particles under the player the moment they jump.
 *
 * Pure particle effect (no custom rendering) so it relies only on the same {@code addParticleClient}
 * API the hitbox/attack particles already use. Particles fade out on their own, matching the
 * "appear then disappear" behaviour.
 */
public final class JumpCircle {
	private JumpCircle() {}

	public static void onJump(MinecraftClient mc) {
		Module m = ModuleManager.INSTANCE.find("JumpCircles");
		if (m == null || !m.isEnabled()) return;
		if (mc.world == null || mc.player == null) return;

		ParticleEffect type;
		switch (m.modeVal("Particle")) {
			case "Flame" -> type = ParticleTypes.FLAME;
			case "Crit" -> type = ParticleTypes.CRIT;
			case "Cloud" -> type = ParticleTypes.CLOUD;
			case "Note" -> type = ParticleTypes.NOTE;
			case "Happy" -> type = ParticleTypes.HAPPY_VILLAGER;
			default -> type = ParticleTypes.END_ROD;
		}

		double r = m.numVal("Radius");
		int points = (int) m.numVal("Points");
		if (points < 1) points = 1;

		double cx = mc.player.getX();
		double cy = mc.player.getY() + 0.05;
		double cz = mc.player.getZ();
		boolean square = m.modeVal("Shape").equals("Square");

		for (int i = 0; i < points; i++) {
			double ox;
			double oz;
			if (square) {
				// Walk the perimeter of a square with half-size r (4 sides).
				double t = (double) i / points * 4.0;
				int side = (int) t;
				double f = t - side;
				double a = (f * 2.0 - 1.0) * r; // -r..r along the current side
				switch (side) {
					case 0 -> { ox = a; oz = -r; }
					case 1 -> { ox = r; oz = a; }
					case 2 -> { ox = -a; oz = r; }
					default -> { ox = -r; oz = -a; }
				}
			} else {
				double ang = (Math.PI * 2.0) * i / points;
				ox = Math.cos(ang) * r;
				oz = Math.sin(ang) * r;
			}
			mc.world.addParticleClient(type, cx + ox, cy, cz + oz, 0.0, 0.0, 0.0);
		}
	}
}
