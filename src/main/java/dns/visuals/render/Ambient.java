package dns.visuals.render;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DustParticleEffect;

import java.util.Random;

/**
 * Spawns soft floating dust particles around the player for ambience.
 *
 * Uses the same client particle API as JumpCircle. Colour comes from the module's ColorSetting
 * (packed ARGB -> RGB int for {@link DustParticleEffect}). Particles drift gently upward and fade
 * on their own.
 */
public final class Ambient {
	private Ambient() {}

	private static final Random RAND = new Random();

	public static void tick(MinecraftClient mc) {
		Module m = ModuleManager.INSTANCE.find("Ambients");
		if (m == null || !m.isEnabled()) return;
		if (mc.world == null || mc.player == null) return;

		int count = (int) m.numVal("Density");
		if (count < 1) count = 1;
		double radius = m.numVal("Radius");
		float size = (float) m.numVal("Size");
		int rgb = m.colorVal("Color") & 0xFFFFFF;
		DustParticleEffect effect = new DustParticleEffect(rgb, size);

		double px = mc.player.getX();
		double py = mc.player.getY() + 1.0;
		double pz = mc.player.getZ();
		for (int i = 0; i < count; i++) {
			double ox = (RAND.nextDouble() * 2 - 1) * radius;
			double oy = (RAND.nextDouble() * 2 - 1) * radius * 0.6;
			double oz = (RAND.nextDouble() * 2 - 1) * radius;
			mc.world.addParticleClient(effect, px + ox, py + oy, pz + oz, 0.0, 0.01, 0.0);
		}
	}
}
