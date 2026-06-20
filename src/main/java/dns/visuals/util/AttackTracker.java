package dns.visuals.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

/**
 * Tracks the most recently attacked entity so multiple features can react to it:
 *  - PlayerInfo HUD panel (shows target name + health)
 *  - Hitboxes hit-flash / particles
 */
public final class AttackTracker {
	public static volatile Entity lastAttacked = null;
	public static volatile long lastAttackTime = 0L;

	private AttackTracker() {}

	public static void record(Entity e) {
		lastAttacked = e;
		lastAttackTime = System.currentTimeMillis();
	}

	public static boolean recent(long withinMs) {
		return lastAttacked != null && (System.currentTimeMillis() - lastAttackTime) < withinMs;
	}

	/** Returns the recently-attacked entity if it is a living, alive entity within the time window. */
	public static LivingEntity recentLiving(long withinMs) {
		Entity e = lastAttacked;
		if (recent(withinMs) && e instanceof LivingEntity le && le.isAlive()) return le;
		return null;
	}
}
