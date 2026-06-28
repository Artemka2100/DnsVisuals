package dns.visuals.render;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import dns.visuals.util.AttackTracker;
import dns.visuals.util.ColorUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Draws a flat ring (circle) or square band on the ground around the entity you most recently hit,
 * then fades it out and removes it after the configured duration (default 5s). Uses the same
 * camera-relative IDENTITY-matrix + {@code debugFilledBox} approach as {@link BlockOutlineRenderer}
 * so it needs no per-vertex normals and survives the 1.21.11 render changes.
 */
public class HitMarker {
	public static final HitMarker INSTANCE = new HitMarker();

	private final MinecraftClient mc = MinecraftClient.getInstance();

	private HitMarker() {}

	public void onWorldRender(WorldRenderContext context) {
		Module m = ModuleManager.INSTANCE.find("HitCircle");
		if (m == null || !m.isEnabled()) return;
		if (mc.world == null || mc.player == null) return;

		long durationMs = (long) (m.numVal("Duration") * 1000);
		LivingEntity e = AttackTracker.recentLiving(durationMs);
		if (e == null) return;

		VertexConsumerProvider consumers = context.consumers();
		if (consumers == null) return;
		Camera camera = mc.gameRenderer.getCamera();
		if (camera == null) return;
		Vec3d cam = camera.getCameraPos();

		MatrixStack ms = new MatrixStack();
		Matrix4f mat = ms.peek().getPositionMatrix();
		VertexConsumer vc = consumers.getBuffer(RenderLayers.debugFilledBox());

		// Interpolated render position so the marker tracks the smoothly-moving model.
		double td = mc.getRenderTickCounter().getTickProgress(false);
		double ix = MathHelper.lerp(td, e.lastRenderX, e.getX());
		double iy = MathHelper.lerp(td, e.lastRenderY, e.getY());
		double iz = MathHelper.lerp(td, e.lastRenderZ, e.getZ());

		double cx = ix - cam.x;
		double cy = iy + m.numVal("Height") - cam.y;
		double cz = iz - cam.z;

		double ro = m.numVal("Radius");
		double ri = ro * 0.82;
		int points = (int) m.numVal("Points");
		if (points < 4) points = 4;
		boolean square = m.modeVal("Shape").equals("Square");
		if (square && points < 8) points = 8;

		double rot = (System.currentTimeMillis() / 1000.0) * m.numVal("Spin") * Math.PI;

		// Fade alpha over the last 30% of the window.
		long age = System.currentTimeMillis() - AttackTracker.lastAttackTime;
		double life = 1.0 - (double) age / durationMs;
		double fade = life > 0.3 ? 1.0 : Math.max(0.0, life / 0.3);

		int base = m.boolVal("Rainbow")
				? ColorUtil.rainbow(m.numVal("Rainbow speed"), 0)
				: m.colorVal("Color");
		int alpha = (int) (((base >> 24) & 0xFF) * fade);
		int color = ColorUtil.withAlpha(base, alpha);

		for (int i = 0; i < points; i++) {
			double t0 = (double) i / points;
			double t1 = (double) (i + 1) / points;
			double[] i0 = pt(square, t0, ri, rot);
			double[] o0 = pt(square, t0, ro, rot);
			double[] o1 = pt(square, t1, ro, rot);
			double[] i1 = pt(square, t1, ri, rot);
			quad(vc, mat, color,
					cx + i0[0], cy, cz + i0[1],
					cx + o0[0], cy, cz + o0[1],
					cx + o1[0], cy, cz + o1[1],
					cx + i1[0], cy, cz + i1[1]);
		}
	}

	/** Point on a circle or square perimeter at fraction t (0..1) and radius r, rotated by rot. */
	private static double[] pt(boolean square, double t, double r, double rot) {
		double x, z;
		if (square) {
			double tt = t * 4.0;
			int side = ((int) tt) % 4;
			double f = tt - (int) tt;
			double a = (f * 2.0 - 1.0) * r;
			switch (side) {
				case 0 -> { x = a; z = -r; }
				case 1 -> { x = r; z = a; }
				case 2 -> { x = -a; z = r; }
				default -> { x = -r; z = -a; }
			}
		} else {
			double ang = t * Math.PI * 2.0;
			x = Math.cos(ang) * r;
			z = Math.sin(ang) * r;
		}
		double cs = Math.cos(rot), sn = Math.sin(rot);
		return new double[]{x * cs - z * sn, x * sn + z * cs};
	}

	/** Double-sided quad so the band is visible from above and below. */
	private static void quad(VertexConsumer vc, Matrix4f m, int c,
			double ax, double ay, double az, double bx, double by, double bz,
			double cx, double cy, double cz, double dx, double dy, double dz) {
		vc.vertex(m, (float) ax, (float) ay, (float) az).color(c);
		vc.vertex(m, (float) bx, (float) by, (float) bz).color(c);
		vc.vertex(m, (float) cx, (float) cy, (float) cz).color(c);
		vc.vertex(m, (float) dx, (float) dy, (float) dz).color(c);
		vc.vertex(m, (float) dx, (float) dy, (float) dz).color(c);
		vc.vertex(m, (float) cx, (float) cy, (float) cz).color(c);
		vc.vertex(m, (float) bx, (float) by, (float) bz).color(c);
		vc.vertex(m, (float) ax, (float) ay, (float) az).color(c);
	}
}
