package dns.visuals.render;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import dns.visuals.util.ColorUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

/**
 * Custom block selection outline.
 *
 * The Fabric BLOCK_OUTLINE event + BlockOutlineContext were removed/redesigned during the 1.21.9
 * port, so instead of subscribing to that unstable event we draw our outline during the same
 * BEFORE_DEBUG_RENDER pass used by the hitbox/ESP renderers and read the targeted block from the
 * player's crosshair target.
 *
 * The world renderer applies the camera VIEW matrix to the consumers() buffer at flush time and
 * (per the Fabric javadoc) expects every vertex to be CAMERA-RELATIVE. So geometry is submitted
 * through an IDENTITY matrix with coordinates offset by -cam; applying our own pitch/yaw rotation on
 * top would transform twice and make the outline drift/skew as the camera turns. wrc.matrices() is
 * null in BEFORE_DEBUG_RENDER on 1.21.11, but we don't need it — identity is correct here.
 */
public class BlockOutlineRenderer {
	public static final BlockOutlineRenderer INSTANCE = new BlockOutlineRenderer();

	private final MinecraftClient mc = MinecraftClient.getInstance();

	private BlockOutlineRenderer() {}

	public void onWorldRender(WorldRenderContext wrc) {
		Module m = ModuleManager.INSTANCE.find("BlockOutline");
		if (m == null || !m.isEnabled()) return;
		if (mc.world == null || mc.player == null) return;

		HitResult hit = mc.crosshairTarget;
		if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) return;

		BlockPos pos = bhr.getBlockPos();
		BlockState state = mc.world.getBlockState(pos);
		VoxelShape shape = state.getOutlineShape(mc.world, pos);
		if (shape.isEmpty()) return;

		VertexConsumerProvider consumers = wrc.consumers();
		if (consumers == null) return;

		Camera camera = mc.gameRenderer.getCamera();
		if (camera == null) return;
		Vec3d cam = camera.getCameraPos();

		// Identity matrix: the engine applies the camera view when it flushes the consumers() buffer,
		// so we only supply camera-relative coordinates (offset by -cam below).
		MatrixStack ms = new MatrixStack();
		Matrix4f identity = ms.peek().getPositionMatrix();

		double ox = pos.getX() - cam.x;
		double oy = pos.getY() - cam.y;
		double oz = pos.getZ() - cam.z;

		int color = m.boolVal("Rainbow")
				? ColorUtil.rainbow(m.numVal("Rainbow speed"), 0)
				: m.colorVal("Color");
		float width = (float) m.numVal("Width");

		if (m.boolVal("Fill")) {
			int fillColor = (color & 0x00FFFFFF) | (0x50 << 24);
			VertexConsumer fvc = consumers.getBuffer(RenderLayers.debugFilledBox());
			Box bb = shape.getBoundingBox().offset(ox, oy, oz);
			filledBox(fvc, identity, fillColor, bb);
		}

		VertexConsumer vc = consumers.getBuffer(RenderLayers.lines());
		VertexRendering.drawOutline(ms, vc, shape, ox, oy, oz, color, width);
	}

	private static void filledBox(VertexConsumer vc, Matrix4f m, int c, Box b) {
		float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ;
		float x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;
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
}
