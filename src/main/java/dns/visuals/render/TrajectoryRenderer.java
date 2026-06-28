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
 * the look vector, per-tick gravity, and air drag) and rendered as a trail of small camera-relative
 * cubes via the {@code debugFilledBox} buffer. Simulation stops early when the path enters a solid
 * block, and an optional larger cube marks the landing point.
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
		VertexConsumer vc = consumers.getBuffer(RenderLayers.debugFilledBox());

		Vec3d pos = p.getEyePos();
		Vec3d look = p.getRotationVec(1.0f);
		double vx = look.x * type.speed;
		double vy = look.y * type.speed;
		double vz = look.z * type.speed;

		int steps = (int) m.numVal("Steps");
		double half = 0.03 + (m.numVal("Width") - 1) * 0.015;

		boolean rainbow = m.boolVal("Rainbow");
		double rspeed = m.numVal("Rainbow speed");
		int baseColor = m.colorVal("Color");

		Vec3d last = pos;
		Vec3d landing = null;
		for (int i = 0; i < steps; i++) {
			Vec3d next = new Vec3d(last.x + vx, last.y + vy, last.z + vz);
			// Stop if this segment ends inside a solid block.
			BlockPos bp = BlockPos.ofFloored(next);
			if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
				landing = next;
				break;
			}
			int color = rainbow ? ColorUtil.rainbow(rspeed, i * 0.01) : baseColor;
			cube(vc, mat, color, next.x - cam.x, next.y - cam.y, next.z - cam.z, half);

			vy -= type.gravity;
			vx *= type.drag;
			vy *= type.drag;
			vz *= type.drag;
			last = next;
		}

		if (landing != null && m.boolVal("Landing marker")) {
			int color = rainbow ? ColorUtil.rainbow(rspeed, 0) : baseColor;
			cube(vc, mat, ColorUtil.withAlpha(color, 255),
					landing.x - cam.x, landing.y - cam.y, landing.z - cam.z, half * 3.0);
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

	/** Camera-relative axis-aligned cube of half-size h, drawn as 6 quads (color only). */
	private static void cube(VertexConsumer vc, Matrix4f m, int c, double x, double y, double z, double h) {
		float x1 = (float) (x - h), y1 = (float) (y - h), z1 = (float) (z - h);
		float x2 = (float) (x + h), y2 = (float) (y + h), z2 = (float) (z + h);
		quad(vc, m, c, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
		quad(vc, m, c, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
		quad(vc, m, c, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
		quad(vc, m, c, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
		quad(vc, m, c, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
		quad(vc, m, c, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
	}

	private static void quad(VertexConsumer vc, Matrix4f m, int c,
			float ax, float ay, float az, float bx, float by, float bz,
			float cx, float cy, float cz, float dx, float dy, float dz) {
		vc.vertex(m, ax, ay, az).color(c);
		vc.vertex(m, bx, by, bz).color(c);
		vc.vertex(m, cx, cy, cz).color(c);
		vc.vertex(m, dx, dy, dz).color(c);
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
