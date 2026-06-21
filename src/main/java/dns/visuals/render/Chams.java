package dns.visuals.render;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import dns.visuals.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Chams logic.
 *
 * Two render styles (Mode setting):
 *  - "Fill": the whole model is tinted with a solid color via LivingEntityRenderer.getMixColor.
 *            Depth is respected, so it does NOT show through walls.
 *  - "Outline": the legacy team-color glow outline (EntityMixin.getTeamColorValue + forced hasOutline).
 *  - "Both": fill + outline together.
 *
 * The fill/outline color itself is chosen by the "Color mode" setting:
 *  - "Static":   the configured Players/Mobs color.
 *  - "Rainbow":  an animated hue (per-entity phase offset), speed from "Rainbow speed".
 *  - "Health":   green (full) -> red (low) based on the entity's health.
 *  - "Distance": green (near) -> red (far) based on distance from you.
 *
 * Targets can be configured separately: other players, mobs, and your own model (third person).
 * "Visible only" hides chams on entities you cannot see (line of sight), reinforcing no-wall behavior.
 */
public final class Chams {
	private Chams() {}

	private static Module mod() {
		Module m = ModuleManager.INSTANCE.find("Chams");
		return (m != null && m.isEnabled()) ? m : null;
	}

	/** Is this entity a chams target according to the target toggles? (style-agnostic) */
	private static boolean isTarget(Module m, Entity e) {
		if (!(e instanceof LivingEntity)) return false;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null) return false;
		boolean self = (e == mc.player);
		if (e instanceof PlayerEntity) {
			if (self) {
				if (!m.boolVal("Self")) return false;
			} else if (!m.boolVal("Players")) {
				return false;
			}
		} else if (!m.boolVal("Mobs")) {
			return false;
		}
		if (!self && m.boolVal("Visible only") && !mc.player.canSee(e)) return false;
		return true;
	}

	/** The base RGB (no alpha) for an entity, honoring the "Color mode" setting. */
	private static int baseRgb(Module m, Entity e) {
		switch (m.modeVal("Color mode")) {
			case "Rainbow" -> {
				return ColorUtil.rainbow(m.numVal("Rainbow speed"), (e.getId() % 12) * 30.0) & 0xFFFFFF;
			}
			case "Health" -> {
				if (e instanceof LivingEntity le) {
					float frac = Math.max(0f, Math.min(1f, le.getHealth() / Math.max(1f, le.getMaxHealth())));
					return ColorUtil.blend(0xFFFF4040, 0xFF40FF40, frac) & 0xFFFFFF;
				}
				return staticRgb(m, e);
			}
			case "Distance" -> {
				MinecraftClient mc = MinecraftClient.getInstance();
				double d = mc.player == null ? 0 : Math.sqrt(mc.player.squaredDistanceTo(e));
				float t = (float) Math.max(0.0, Math.min(1.0, d / 48.0));
				return ColorUtil.blend(0xFF40FF40, 0xFFFF4040, t) & 0xFFFFFF;
			}
			default -> {
				return staticRgb(m, e);
			}
		}
	}

	private static int staticRgb(Module m, Entity e) {
		int base = (e instanceof PlayerEntity) ? m.colorVal("Players color") : m.colorVal("Mobs color");
		return base & 0xFFFFFF;
	}

	/** Color (with opacity as alpha) for the given entity. */
	private static int colorFor(Module m, Entity e) {
		int rgb = baseRgb(m, e) & 0xFFFFFF;
		int a = (int) m.numVal("Opacity") & 0xFF;
		int argb = (a << 24) | rgb;
		// Never return exactly 0 (our "no chams" sentinel); nudge alpha if needed.
		return argb == 0 ? 0xFF000001 : argb;
	}

	/**
	 * Fill color for the model tint, or 0 when no fill should be applied.
	 * Called from LivingEntityRendererMixin during updateRenderState (entity is known there).
	 */
	public static int fillColor(Entity e) {
		Module m = mod();
		if (m == null) return 0;
		if (m.modeVal("Mode").equals("Outline")) return 0;
		if (!isTarget(m, e)) return 0;
		return colorFor(m, e);
	}

	/** Should this entity get the glow outline? (Outline/Both modes) */
	public static boolean shouldGlow(Entity e) {
		Module m = mod();
		if (m == null) return false;
		if (m.modeVal("Mode").equals("Fill")) return false;
		return isTarget(m, e);
	}

	/** Outline glow color (no alpha) for the given entity. */
	public static int glowColor(Entity e) {
		Module m = mod();
		if (m == null) return 0xFFFFFF;
		return baseRgb(m, e) & 0xFFFFFF;
	}
}
