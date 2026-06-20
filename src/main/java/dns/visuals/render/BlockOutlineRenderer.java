package dns.visuals.render;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import dns.visuals.util.ColorUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

/**
 * Custom block selection outline. Subscribes to Fabric's BLOCK_OUTLINE event: when the module is
 * enabled we draw our own (colored / optionally rainbow / optionally filled) outline and return
 * false to suppress the vanilla one. When disabled we return true so vanilla draws as normal.
 *
 * Uses the same camera-relative matrix + engine-managed consumers approach as the hitbox/ESP
 * renderers so it lines up correctly and flushes with the world render pass.
 */
public class BlockOutlineRenderer {
	public static final BlockOutlineRenderer INSTANCE = new BlockOutlineRenderer();

	private final MinecraftClient mc = MinecraftClient.getInstance();

	private BlockOutlineRenderer() {}

	public boolean onBlockOutline(WorldRenderContext wrc, WorldRenderEvents.BlockOutlineContext boc) {
		Module m = ModuleManager.INSTANCE.find("BlockOutline");
		if (m == null || !m.isEnabled()) return true; // let vanilla draw
		if (mc.world == null) return true;

		BlockPos pos = boc.blockPos();
		BlockState state = boc.blockState();
		if (pos == null || state == null) return true;

		VoxelShape shape = state.getOutlineShape(mc.world, pos);
		if (shape.isEmpty()) return false; // nothing to draw, but still suppress vanilla

		VertexConsumerProvider consumers = wrc.consumers();
		if (consumers == null) return true;

		Camera camera = mc.gameRenderer.getCamera();
		if (camera == null) return true;
		Vec3d cam = camera.getCameraPos();

		MatrixStack ms = new MatrixStack();
		ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180f));
		ms.translate(-cam.x, -cam.y, -cam.z);
		Matrix4f modelView = ms.peek().getPositionMatrix();

		int color = m.boolVal("Rainbow")
				? ColorUtil.rainbow(m.numVal("Rainbow speed"), 0)
				: m.colorVal("Color");
		float width = (float) m.numVal("Width");

		if (m.boolVal("Fill")) {
			int fillColor = (color & 0x00FFFFFF) | (0x50 << 24);
			VertexConsumer fvc = consumers.getBuffer(RenderLayers.debugFilledBox());
			Box bb = shape.getBoundingBox().offset(pos.getX(), pos.getY(), pos.getZ());
			filledBox(fvc, modelView, fillColor, bb);
		}

		VertexConsumer vc = consumers.getBuffer(RenderLayers.lines());
		VertexRendering.drawOutline(ms, vc, shape, pos.getX(), pos.getY(), pos.getZ(), color, width);

		return false; // suppress the vanilla outline
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
