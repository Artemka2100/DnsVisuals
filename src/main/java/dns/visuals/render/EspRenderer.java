package dns.visuals.render;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;

/**
 * Entity ESP. Draws an outline and/or color fill around living entities (or players only),
 * optionally limited to entities the local player can actually see.
 *
 * Uses the same camera-relative MatrixStack approach as {@link HitboxRenderer} because
 * WorldRenderContext#matrixStack was removed in 1.21.9+. The fill box is emitted manually into
 * the debug filled-box layer (POSITION_COLOR) to avoid depending on an unverified helper signature.
 */
public class EspRenderer {
	public static final EspRenderer INSTANCE = new EspRenderer();

	private final MinecraftClient mc = MinecraftClient.getInstance();

	private EspRenderer() {}

	public void onWorldRender() {
		Module esp = ModuleManager.INSTANCE.find("ESP");
		if (esp == null || !esp.isEnabled()) return;
		if (mc.world == null || mc.player == null) return;

		Camera camera = mc.gameRenderer.getCamera();
		if (camera == null) return;
		Vec3d cam = camera.getCameraPos();

		String mode = esp.modeVal("Mode");
		boolean outline = mode.equals("Outline") || mode.equals("Both");
		boolean fill = mode.equals("Fill") || mode.equals("Both");
		boolean visibleOnly = esp.boolVal("Visible only");
		boolean playersOnly = esp.boolVal("Players only");
		double range = esp.numVal("Range");
		double rangeSq = range * range;
		int color = esp.colorVal("Color");
		float lineWidth = (float) esp.numVal("Line width");
		int fillAlpha = Math.max(0, Math.min(255, (int) esp.numVal("Fill opacity")));
		int fillColor = (fillAlpha << 24) | (color & 0x00FFFFFF);

		MatrixStack ms = new MatrixStack();
		ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180f));
		ms.translate(-cam.x, -cam.y, -cam.z);
		Matrix4f matrix = ms.peek().getPositionMatrix();

		VertexConsumerProvider.Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();

		for (Entity e : mc.world.getEntities()) {
			if (e == mc.player) continue;
			if (playersOnly) {
				if (!(e instanceof PlayerEntity)) continue;
			} else {
				if (!(e instanceof LivingEntity)) continue;
			}
			if (mc.player.squaredDistanceTo(e) > rangeSq) continue;
			if (visibleOnly && !mc.player.canSee(e)) continue;

			Box box = e.getBoundingBox();

			if (fill) {
				VertexConsumer fvc = imm.getBuffer(RenderLayers.debugFilledBox());
				filledBox(fvc, matrix, fillColor, box);
			}
			if (outline) {
				VertexConsumer vc = imm.getBuffer(RenderLayers.lines());
				VertexRendering.drawOutline(ms, vc, VoxelShapes.cuboid(box), 0.0, 0.0, 0.0, color, lineWidth);
			}
		}

		imm.draw();
	}

	/** Emits the 6 faces of a box as POSITION_COLOR quads into the debug filled-box layer. */
	private static void filledBox(VertexConsumer vc, Matrix4f m, int c, Box b) {
		float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ;
		float x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;
		// bottom
		quad(vc, m, c, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
		// top
		quad(vc, m, c, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
		// north (z1)
		quad(vc, m, c, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
		// south (z2)
		quad(vc, m, c, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
		// west (x1)
		quad(vc, m, c, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
		// east (x2)
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
