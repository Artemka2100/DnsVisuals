package dns.visuals.render;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import dns.visuals.util.ColorUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Draws a conical "china hat" above the LOCAL player's head only (the client running the visuals),
 * never other players. Visible in third person (F5) or by other observers' perspective on your
 * client. Built from triangles fed into the {@code debugFilledBox} buffer (camera-relative, identity
 * matrix) so no per-vertex normals are required.
 */
public class ChinaHat {
	public static final ChinaHat INSTANCE = new ChinaHat();

	private final MinecraftClient mc = MinecraftClient.getInstance();

	private ChinaHat() {}

	public void onWorldRender(WorldRenderContext context) {
		Module m = ModuleManager.INSTANCE.find("ChinaHat");
		if (m == null || !m.isEnabled()) return;
		if (mc.world == null || mc.player == null) return;

		ClientPlayerEntity p = mc.player;

		VertexConsumerProvider consumers = context.consumers();
		if (consumers == null) return;
		Camera camera = mc.gameRenderer.getCamera();
		if (camera == null) return;
		Vec3d cam = camera.getCameraPos();

		MatrixStack ms = new MatrixStack();
		Matrix4f mat = ms.peek().getPositionMatrix();
		VertexConsumer vc = consumers.getBuffer(RenderLayers.debugFilledBox());

		double td = mc.getRenderTickCounter().getTickProgress(false);
		double ix = MathHelper.lerp(td, p.lastRenderX, p.getX());
		double iy = MathHelper.lerp(td, p.lastRenderY, p.getY());
		double iz = MathHelper.lerp(td, p.lastRenderZ, p.getZ());

		double size = m.numVal("Size");
		double baseY = iy + p.getHeight() + m.numVal("Height");
		double apexY = baseY + size * 0.85;

		double cx = ix - cam.x;
		double cz = iz - cam.z;
		double byw = baseY - cam.y;
		double ayw = apexY - cam.y;

		int sides = (int) m.numVal("Sides");
		if (sides < 3) sides = 3;
		double rot = (System.currentTimeMillis() / 1000.0) * m.numVal("Spin") * Math.PI;

		int color = m.boolVal("Rainbow")
				? ColorUtil.rainbow(m.numVal("Rainbow speed"), 0)
				: m.colorVal("Color");

		for (int i = 0; i < sides; i++) {
			double a0 = (double) i / sides * Math.PI * 2.0 + rot;
			double a1 = (double) (i + 1) / sides * Math.PI * 2.0 + rot;
			double x0 = Math.cos(a0) * size, z0 = Math.sin(a0) * size;
			double x1 = Math.cos(a1) * size, z1 = Math.sin(a1) * size;
			// Side face (apex + two rim points) as a degenerate quad triangle.
			tri(vc, mat, color,
					cx, ayw, cz,
					cx + x0, byw, cz + z0,
					cx + x1, byw, cz + z1);
			// Underside rim (center + two rim points) so it looks solid from below.
			tri(vc, mat, color,
					cx, byw, cz,
					cx + x1, byw, cz + z1,
					cx + x0, byw, cz + z0);
		}
	}

	/** Double-sided triangle emitted as a degenerate quad (4th vertex repeats the 3rd). */
	private static void tri(VertexConsumer vc, Matrix4f m, int c,
			double ax, double ay, double az, double bx, double by, double bz,
			double cx, double cy, double cz) {
		vc.vertex(m, (float) ax, (float) ay, (float) az).color(c);
		vc.vertex(m, (float) bx, (float) by, (float) bz).color(c);
		vc.vertex(m, (float) cx, (float) cy, (float) cz).color(c);
		vc.vertex(m, (float) cx, (float) cy, (float) cz).color(c);
		vc.vertex(m, (float) cx, (float) cy, (float) cz).color(c);
		vc.vertex(m, (float) cx, (float) cy, (float) cz).color(c);
		vc.vertex(m, (float) bx, (float) by, (float) bz).color(c);
		vc.vertex(m, (float) ax, (float) ay, (float) az).color(c);
	}
}
