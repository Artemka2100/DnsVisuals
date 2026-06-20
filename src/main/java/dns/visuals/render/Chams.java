package dns.visuals.render;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Chams logic shared by the client/entity mixins.
 *
 * We highlight matching players with a colored vanilla outline (forced via
 * {@code MinecraftClient.hasOutline}, colored via {@code Entity.getTeamColorValue}). To keep the
 * effect from showing through walls, a player is only highlighted while in the local player's line
 * of sight ({@code canSee}). First-person hand chams is intentionally not implemented.
 */
public final class Chams {
	private Chams() {}

	private static Module module() {
		Module m = ModuleManager.INSTANCE.find("Chams");
		return (m != null && m.isEnabled()) ? m : null;
	}

	/** True if this entity should be highlighted as a player chams target (visible players only). */
	public static boolean shouldGlow(Entity e) {
		Module m = module();
		if (m == null) return false;
		if (!m.boolVal("Players")) return false;
		if (!(e instanceof PlayerEntity)) return false;

		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || e == mc.player) return false;

		// Never highlight through walls: only players actually in line of sight.
		return mc.player.canSee(e);
	}

	/** RGB (no alpha) outline color for the given chams target. */
	public static int glowColor(Entity e) {
		Module m = module();
		int c = (m != null) ? m.colorVal("Player color") : 0xFFFFFFFF;
		return c & 0xFFFFFF;
	}
}
