package dns.visuals.render;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
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
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity ESP. Draws an outline and/or color fill around living entities (or players only),
 * an optional nametag with health, and can be limited to entities the player can actually see.
 *
 * Boxes are drawn into the engine-managed {@link WorldRenderContext#consumers()} buffer (same
 * approach as HitboxRenderer) so the world renderer flushes them; we never call draw() ourselves.
 * Nametags are projected from 3D to screen space during the world-render pass, then drawn as 2D
 * text in {@link #renderOverlay(DrawContext)} from the InGameHud tail.
 */
public class EspRenderer {
	public static final EspRenderer INSTANCE = new EspRenderer();

	private final MinecraftClient mc = MinecraftClient.getInstance();

	/** Screen-space nametags computed this frame (consumed by the HUD overlay pass). */
	private final List<Tag> tags = new ArrayList<>();

	private EspRenderer() {}

	private record Tag(float x, float y, String text, float hpFrac, int color) {}

	public void onWorldRender(WorldRenderContext context) {
		tags.clear();

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
		boolean nametag = esp.boolVal("Nametag");
		boolean hpInName = esp.boolVal("Health in name");
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
		Matrix4f modelView = ms.peek().getPositionMatrix();

		// proj * modelView for screen projection of nametags. Neither WorldRenderContext nor
		// RenderSystem expose the projection matrix in 1.21.11, so we rebuild a perspective matrix
		// from the current FOV + aspect ratio. Near/far don't affect screen x/y, so any sane values
		// are fine here.
		float fovDeg = mc.gameRenderer.getFov(camera, 1.0f, true);
		int fbW = mc.getWindow().getFramebufferWidth();
		int fbH = mc.getWindow().getFramebufferHeight();
		float aspect = fbH == 0 ? 1f : (float) fbW / (float) fbH;
		Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(fovDeg), aspect, 0.05f, 1000.0f);
		Matrix4f mvp = new Matrix4f(proj).mul(modelView);
		int sw = mc.getWindow().getScaledWidth();
		int sh = mc.getWindow().getScaledHeight();

		// Engine-managed buffer; flushed by the world renderer after this pass.
		VertexConsumerProvider consumers = context.consumers();

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

			if (consumers != null) {
				if (fill) {
					VertexConsumer fvc = consumers.getBuffer(RenderLayers.debugFilledBox());
					filledBox(fvc, modelView, fillColor, box);
				}
				if (outline) {
					VertexConsumer vc = consumers.getBuffer(RenderLayers.lines());
					VertexRendering.drawOutline(ms, vc, VoxelShapes.cuboid(box), 0.0, 0.0, 0.0, color, lineWidth);
				}
			}

			if (nametag) {
				double hx = e.getX();
				double hy = e.getY() + e.getHeight() + 0.45;
				double hz = e.getZ();
				Vector4f clip = new Vector4f((float) hx, (float) hy, (float) hz, 1f).mul(mvp);
				if (clip.w() <= 0f) continue;
				float ndcX = clip.x() / clip.w();
				float ndcY = clip.y() / clip.w();
				float screenX = (ndcX * 0.5f + 0.5f) * sw;
				float screenY = (0.5f - ndcY * 0.5f) * sh;

				String name = e.getName().getString();
				float hpFrac = -1f;
				if (e instanceof LivingEntity le) {
					float max = le.getMaxHealth();
					hpFrac = max > 0 ? Math.max(0f, Math.min(1f, le.getHealth() / max)) : -1f;
					if (hpInName) name = name + " " + (int) Math.ceil(le.getHealth());
				}
				tags.add(new Tag(screenX, screenY, name, hpFrac, color));
			}
		}
	}

	/** Draws the nametags computed in {@link #onWorldRender}; called from the InGameHud tail. */
	public void renderOverlay(DrawContext ctx) {
		if (tags.isEmpty()) return;
		TextRenderer tr = mc.textRenderer;
		for (Tag t : tags) {
			int w = tr.getWidth(t.text());
			int x = (int) t.x() - w / 2;
			int y = (int) t.y() - tr.fontHeight - 3;
			ctx.fill(x - 2, y - 2, x + w + 2, y + tr.fontHeight + 1, 0x90000000);
			ctx.drawText(tr, t.text(), x, y, 0xFFFFFFFF, true);
			if (t.hpFrac() >= 0f) {
				int barY = y + tr.fontHeight + 1;
				ctx.fill(x - 1, barY, x + w + 1, barY + 2, 0xFF300000);
				int filled = Math.round((w + 2) * t.hpFrac());
				int hpColor = 0xFF000000
						| ((int) ((1f - t.hpFrac()) * 255) << 16)
						| ((int) (t.hpFrac() * 205) << 8)
						| 0x20;
				ctx.fill(x - 1, barY, x - 1 + filled, barY + 2, hpColor);
			}
		}
	}

	/** Emits the 6 faces of a box as POSITION_COLOR quads into the debug filled-box layer. */
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
