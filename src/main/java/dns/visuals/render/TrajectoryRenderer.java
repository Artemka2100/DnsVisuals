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
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * "Direction" module: simulates and draws the flight path of the projectile you're about to throw
 * (arrow from a bow/crossbow, ender pearl, snowball, egg, trident) so you can see where it will go.
 *
 * The path is sampled tick-by-tick using the vanilla projectile motion model (initial speed along
 * the look vector, per-tick gravity, and air drag) and rendered as a single continuous line.
 *
 * <p>IMPORTANT: the line is drawn as a thin camera-independent "tube" (two perpendicular thin quads
 * per segment) through the {@code RenderLayers.debugFilledBox()} buffer — the SAME proven, crash-free
 * path used by {@link HitMarker} and {@link ChinaHat}. The previous version wrote vertices straight
 * into the {@code lines()} layer with per-vertex normals, which crashed the game on buffer flush in
 * 1.21.11. The filled-box layer needs only position + color, so it is safe here.
 */
public class TrajectoryRenderer {
	public static final TrajectoryRenderer INSTANCE = new TrajectoryRenderer();

	/** Half-thickness of the rendered line, in blocks. */
	private static final double HALF_WIDTH = 0.025;

	private final MinecraftClient mc = MinecraftClient.getInstance();

	private TrajectoryRenderer() {}

	public void onWorldRender(WorldRenderContext context) {
		Module m = ModuleManager.INSTANCE.find("Direction");
		if (m == null || !m.isEnabled()) return;
		if (mc.world == null || mc.player == null) return;

		ClientPlayerEntity p = mc.player;
		Item main = p.getMainHandStack().getItem();
		Item off = p.getOffHandStack().getItem();

		// Choose physics by held projectile/launcher; null = nothing throwable held.
		ProjType type = pick(main);
		if (type == null) type = pick(off);
		boolean always = m.modeVal("Mode").equals("Always");
		if (type == null) {
			if (!always) return;
			type = ProjType.PEARL; // Always mode with nothing held: show a default pearl arc.
		}

		VertexConsumerProvider consumers = context.consumers();
		if (consumers == null) return;
		Camera camera = mc.gameRenderer.getCamera();
		if (camera == null) return;
		Vec3d cam = camera.getCameraPos();

		MatrixStack ms = new MatrixStack();
		Matrix4f mat = ms.peek().getPositionMatrix();
		VertexConsumer vc = consumers.getBuffer(RenderLayers.debugFilledBox());

		Vec3d pos = p.getEyePos();
		Vec3d look = p.getRotationVec(1.0f);
		double vx = look.x * type.speed;
		double vy = look.y * type.speed;
		double vz = look.z * type.speed;

		int steps = (int) m.numVal("Steps");

		boolean rainbow = m.boolVal("Rainbow");
		double rspeed = m.numVal("Rainbow speed");
		int baseColor = m.colorVal("Color");

		Vec3d last = pos;
		for (int i = 0; i < steps; i++) {
			Vec3d next = new Vec3d(last.x + vx, last.y + vy, last.z + vz);
			int color = rainbow ? ColorUtil.rainbow(rspeed, i * 0.01) : baseColor;
			segment(vc, mat, color, HALF_WIDTH,
					last.x - cam.x, last.y - cam.y, last.z - cam.z,
					next.x - cam.x, next.y - cam.y, next.z - cam.z);

			// Stop if this segment ends inside a solid block.
			BlockPos bp = BlockPos.ofFloored(next);
			if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) break;

			vy -= type.gravity;
			vx *= type.drag;
			vy *= type.drag;
			vz *= type.drag;
			last = next;
		}
	}

	private static ProjType pick(Item item) {
		if (item == Items.BOW || item == Items.CROSSBOW) return ProjType.ARROW;
		if (item == Items.ENDER_PEARL) return ProjType.PEARL;
		if (item == Items.SNOWBALL || item == Items.EGG) return ProjType.SNOWBALL;
		if (item == Items.TRIDENT) return ProjType.TRIDENT;
		if (item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) return ProjType.POTION;
		return null;
	}

	/**
	 * Draws one line segment as a thin "tube": two perpendicular thin quads around the segment axis
	 * so the line stays visible from any viewing angle. All coordinates are camera-relative.
	 */
	private static void segment(VertexConsumer vc, Matrix4f m, int c, double half,
			double x1, double y1, double z1, double x2, double y2, double z2) {
		double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1.0e-5) return;
		dx /= len; dy /= len; dz /= len;

		// Reference "up" that is not parallel to the segment direction.
		double ux = 0, uy = 1, uz = 0;
		if (Math.abs(dy) > 0.99) { ux = 1; uy = 0; uz = 0; }

		// side = dir x up
		double sx = dy * uz - dz * uy, sy = dz * ux - dx * uz, sz = dx * uy - dy * ux;
		double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
		sx /= sl; sy /= sl; sz /= sl;

		// vert = dir x side
		double vxn = dy * sz - dz * sy, vyn = dz * sx - dx * sz, vzn = dx * sy - dy * sx;
		double vl = Math.sqrt(vxn * vxn + vyn * vyn + vzn * vzn);
		vxn /= vl; vyn /= vl; vzn /= vl;

		sx *= half; sy *= half; sz *= half;
		vxn *= half; vyn *= half; vzn *= half;

		// Quad in the "side" plane.
		quad(vc, m, c,
				x1 - sx, y1 - sy, z1 - sz, x1 + sx, y1 + sy, z1 + sz,
				x2 + sx, y2 + sy, z2 + sz, x2 - sx, y2 - sy, z2 - sz);
		// Quad in the "vert" plane.
		quad(vc, m, c,
				x1 - vxn, y1 - vyn, z1 - vzn, x1 + vxn, y1 + vyn, z1 + vzn,
				x2 + vxn, y2 + vyn, z2 + vzn, x2 - vxn, y2 - vyn, z2 - vzn);
	}

	/** Double-sided quad so the band is visible from both faces (position + color only). */
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

	private enum ProjType {
		ARROW(3.0, 0.05, 0.99),
		PEARL(1.5, 0.03, 0.99),
		SNOWBALL(1.5, 0.03, 0.99),
		TRIDENT(2.5, 0.05, 0.99),
		POTION(0.5, 0.05, 0.99);

		final double speed;
		final double gravity;
		final double drag;

		ProjType(double speed, double gravity, double drag) {
			this.speed = speed;
			this.gravity = gravity;
			this.drag = drag;
		}
	}
}
