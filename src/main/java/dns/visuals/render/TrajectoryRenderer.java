package dns.visuals.render;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import dns.visuals.util.ColorUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
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
 * the look vector, per-tick gravity, and air drag) and rendered as a single continuous line made of
 * connected segments via the {@code getLines} buffer. Simulation stops as soon as the path enters a
 * solid block.
 */
public class TrajectoryRenderer {
	public static final TrajectoryRenderer INSTANCE = new TrajectoryRenderer();

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
		VertexConsumer vc = consumers.getBuffer(RenderLayer.getLines());

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
			line(vc, mat, color,
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

	/** Camera-relative line segment for the {@code getLines} layer (needs a per-vertex normal). */
	private static void line(VertexConsumer vc, Matrix4f m, int c,
			double x1, double y1, double z1, double x2, double y2, double z2) {
		float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
		float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1.0e-5f) return;
		float nx = dx / len, ny = dy / len, nz = dz / len;
		vc.vertex(m, (float) x1, (float) y1, (float) z1).color(c).normal(nx, ny, nz);
		vc.vertex(m, (float) x2, (float) y2, (float) z2).color(c).normal(nx, ny, nz);
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
