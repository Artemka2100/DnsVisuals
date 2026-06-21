package dns.visuals.render;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

/**
 * Renders entity hitboxes and spawns hit particles (Minecraft 1.21.11 render APIs).
 *
 * We draw into the engine-managed {@link WorldRenderContext#consumers()} buffer and let the world
 * renderer flush it. The world renderer applies the camera VIEW matrix to that buffer at flush time
 * and (per the Fabric javadoc) expects every vertex to be CAMERA-RELATIVE. So we submit geometry
 * through an IDENTITY matrix with coordinates offset by -cam; applying our own pitch/yaw rotation on
 * top (as a previous version did) transforms twice and makes the boxes drift/skew as the camera
 * turns. context.matrices() is null in BEFORE_DEBUG_RENDER on 1.21.11, but we don't need it —
 * identity is the correct matrix here.
 *
 * Boxes are positioned at the entity's INTERPOLATED render position (lastRenderX/Y/Z lerped by the
 * frame tick progress) to track the smoothly-rendered model. The "Box size" setting visually grows
 * or shrinks every box by the same amount on all axes via Box#expand.
 */
public class HitboxRenderer {
	public static final HitboxRenderer INSTANCE = new HitboxRenderer();

	private final MinecraftClient mc = MinecraftClient.getInstance();

	private Entity lastAttacked = null;
	private long lastAttackTime = 0L;

	private HitboxRenderer() {}

	public void onWorldRender(WorldRenderContext context) {
		Module hb = ModuleManager.INSTANCE.find("Hitboxes");
		if (hb == null || !hb.isEnabled()) return;
		if (mc.world == null || mc.player == null) return;

		VertexConsumerProvider consumers = context.consumers();
		if (consumers == null) return;

		Camera camera = mc.gameRenderer.getCamera();
		if (camera == null) return;
		Vec3d cam = camera.getCameraPos();

		// Identity matrix: the engine applies the camera view when it flushes the consumers() buffer,
		// so we only supply camera-relative coordinates (offset by -cam in drawOutline below).
		MatrixStack ms = new MatrixStack();

		VertexConsumer vc = consumers.getBuffer(RenderLayers.lines());

		boolean playersOnly = hb.boolVal("Players only");
		double range = hb.numVal("Range");
		double rangeSq = range * range;
		double expand = hb.numVal("Box size");

		int baseColor = hb.colorVal("Color");
		int hitColor = hb.colorVal("Hit color");
		long flash = (long) hb.numVal("Hit flash");
		long now = System.currentTimeMillis();

		// Interpolation factor for this frame so boxes track the smoothly-rendered model.
		double td = mc.getRenderTickCounter().getTickProgress(false);

		for (Entity e : mc.world.getEntities()) {
			if (e == mc.player) continue;
			if (playersOnly) {
				if (!(e instanceof PlayerEntity)) continue;
			} else {
				if (!(e instanceof LivingEntity)) continue;
			}
			if (mc.player.squaredDistanceTo(e) > rangeSq) continue;

			int color = baseColor;
			if (e == lastAttacked && (now - lastAttackTime) < flash) {
				color = hitColor;
			}

			// Offset the (last-tick) bounding box to the interpolated render position.
			double ix = MathHelper.lerp(td, e.lastRenderX, e.getX());
			double iy = MathHelper.lerp(td, e.lastRenderY, e.getY());
			double iz = MathHelper.lerp(td, e.lastRenderZ, e.getZ());
			Box box = e.getBoundingBox().offset(ix - e.getX(), iy - e.getY(), iz - e.getZ());
			if (expand != 0) box = box.expand(expand);

			VertexRendering.drawOutline(ms, vc, VoxelShapes.cuboid(box), -cam.x, -cam.y, -cam.z, color, 1.5f);
		}
	}

	public ActionResult onAttack(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hit) {
		Module hb = ModuleManager.INSTANCE.find("Hitboxes");
		if (hb != null && hb.isEnabled()) {
			lastAttacked = entity;
			lastAttackTime = System.currentTimeMillis();
			if (world.isClient()) {
				spawnParticles(hb, world, entity);
			}
		}
		return ActionResult.PASS;
	}

	private void spawnParticles(Module hb, World world, Entity entity) {
		SimpleParticleType type;
		switch (hb.modeVal("Particle")) {
			case "Flame" -> type = ParticleTypes.FLAME;
			case "Heart" -> type = ParticleTypes.HEART;
			case "Cloud" -> type = ParticleTypes.CLOUD;
			case "Angry" -> type = ParticleTypes.ANGRY_VILLAGER;
			default -> type = ParticleTypes.CRIT;
		}

		int count = (int) hb.numVal("Particle count");
		Box box = entity.getBoundingBox();
		double cx = (box.minX + box.maxX) / 2.0;
		double cy = (box.minY + box.maxY) / 2.0;
		double cz = (box.minZ + box.maxZ) / 2.0;
		double sx = (box.maxX - box.minX) / 2.0;
		double sy = (box.maxY - box.minY) / 2.0;
		double sz = (box.maxZ - box.minZ) / 2.0;

		for (int i = 0; i < count; i++) {
			double ox = (world.random.nextDouble() * 2 - 1) * sx;
			double oy = (world.random.nextDouble() * 2 - 1) * sy;
			double oz = (world.random.nextDouble() * 2 - 1) * sz;
			world.addParticleClient(type, cx + ox, cy + oy, cz + oz, 0, 0, 0);
		}
	}
}
