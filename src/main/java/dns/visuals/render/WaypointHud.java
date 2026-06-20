package dns.visuals.render;

import dns.visuals.module.Category;
import dns.visuals.module.Module;
import dns.visuals.setting.BooleanSetting;
import dns.visuals.setting.ColorSetting;
import dns.visuals.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

/** HUD arrow pointing toward the current {@link Waypoint} target, plus distance/coords text. */
public final class WaypointHud {
	private WaypointHud() {}

	/** Built and registered from DnsVisuals so it persists with the rest of the module list. */
	public static Module createModule() {
		return new Module("Waypoint", "Type .goto x y z in chat; an arrow points to it", Category.HUD)
				.add(new BooleanSetting("Distance", "Show distance", true))
				.add(new BooleanSetting("Coords", "Show target coords", true))
				.add(new SliderSetting("Arrow size", "Arrow radius", 10, 5, 24, 1, "px"))
				.add(new BooleanSetting("Shadow", "Text shadow", true))
				.add(new ColorSetting("Color", "Arrow & distance color", 255, 122, 0, 255))
				.hud(WaypointHud::render);
	}

	public static int render(DrawContext ctx, int x, int y, Module self) {
		if (!Waypoint.isActive()) return 0;
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity p = mc.player;
		if (p == null) return 0;
		TextRenderer tr = mc.textRenderer;

		int color = self.colorVal("Color");
		boolean shadow = self.boolVal("Shadow");

		double dx = Waypoint.x() - p.getX();
		double dy = Waypoint.y() - p.getY();
		double dz = Waypoint.z() - p.getZ();
		double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

		// Player yaw 0 == +Z; facing dir == (-sin, cos). Bearing yaw toward target:
		double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
		double rel = MathHelper.wrapDegrees(targetYaw - p.getYaw());
		double theta = Math.toRadians(rel); // 0 == target straight ahead -> arrow points up

		int r = (int) self.numVal("Arrow size");
		int cx = x + r + 2;
		int cy = y + r + 2;

		double tipX = cx + Math.sin(theta) * r;
		double tipY = cy - Math.cos(theta) * r;
		double baseX = cx - Math.sin(theta) * r;
		double baseY = cy + Math.cos(theta) * r;
		double leftX = cx + Math.sin(theta + Math.toRadians(140)) * (r * 0.75);
		double leftY = cy - Math.cos(theta + Math.toRadians(140)) * (r * 0.75);
		double rightX = cx + Math.sin(theta - Math.toRadians(140)) * (r * 0.75);
		double rightY = cy - Math.cos(theta - Math.toRadians(140)) * (r * 0.75);

		line(ctx, (int) baseX, (int) baseY, (int) tipX, (int) tipY, color);
		line(ctx, (int) tipX, (int) tipY, (int) leftX, (int) leftY, color);
		line(ctx, (int) tipX, (int) tipY, (int) rightX, (int) rightY, color);

		int textX = x + 2 * r + 8;
		int th = 0;
		if (self.boolVal("Distance")) {
			ctx.drawText(tr, ((int) dist) + "m", textX, y + th, color, shadow);
			th += tr.fontHeight + 1;
		}
		if (self.boolVal("Coords")) {
			String c = (int) Waypoint.x() + ", " + (int) Waypoint.y() + ", " + (int) Waypoint.z();
			ctx.drawText(tr, c, textX, y + th, 0xFFFFFFFF, shadow);
			th += tr.fontHeight + 1;
		}
		return Math.max(2 * r + 4, th);
	}

	/** Bresenham line drawn as 1px filled cells (DrawContext has no native line primitive). */
	private static void line(DrawContext ctx, int x0, int y0, int x1, int y1, int color) {
		int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
		int err = dx - dy;
		while (true) {
			ctx.fill(x0, y0, x0 + 1, y0 + 1, color);
			if (x0 == x1 && y0 == y1) break;
			int e2 = 2 * err;
			if (e2 > -dy) { err -= dy; x0 += sx; }
			if (e2 < dx) { err += dx; y0 += sy; }
		}
	}
}
