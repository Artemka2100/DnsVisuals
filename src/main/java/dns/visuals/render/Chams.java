package dns.visuals.render;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Chams logic shared by the client/entity mixins.
 *
 * In 1.21.11, the most reliable "chams" for other players is a colored, see-through silhouette:
 * we force the vanilla entity outline on for matching players ({@code MinecraftClient.hasOutline})
 * and override the outline color via {@code Entity.getTeamColorValue}. This avoids re-rendering
 * entity models with a flat shader, which is fragile on the new render pipeline.
 *
 * First-person hand chams is handled separately (it is not an entity outline).
 */
public final class Chams {
	private Chams() {}

	private static Module module() {
		Module m = ModuleManager.INSTANCE.find("Chams");
		return (m != null && m.isEnabled()) ? m : null;
	}

	/** True if this entity should be highlighted as a player chams target. */
	public static boolean shouldGlow(Entity e) {
		Module m = module();
		if (m == null) return false;
		if (!m.boolVal("Players")) return false;
		if (!(e instanceof PlayerEntity)) return false;

		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || e == mc.player) return false;
		if (m.boolVal("Visible only") && !mc.player.canSee(e)) return false;
		return true;
	}

	/** RGB (no alpha) outline color for the given chams target. */
	public static int glowColor(Entity e) {
		Module m = module();
		int c = (m != null) ? m.colorVal("Player color") : 0xFFFFFFFF;
		return c & 0xFFFFFF;
	}
}
