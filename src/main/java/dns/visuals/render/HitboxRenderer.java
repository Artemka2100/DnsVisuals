package dns.visuals.render;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
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
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;

/**
 * Renders entity hitboxes and spawns hit particles.
 * Migrated to Minecraft 1.21.11 rendering APIs.
 */
public class HitboxRenderer {
	public static final HitboxRenderer INSTANCE = new HitboxRenderer();

	private final MinecraftClient mc = MinecraftClient.getInstance();

	private Entity lastAttacked = null;
	private long lastAttackTime = 0L;

	private HitboxRenderer() {}

	/**
	 * Called from the GameRenderer mixin after the world has been rendered.
	 * Builds a camera-relative MatrixStack because WorldRenderContext#matrixStack
	 * was removed in 1.21.9+.
	 */
	public void onWorldRender() {
		Module hb = ModuleManager.INSTANCE.find("Hitboxes");
		if (hb == null || !hb.isEnabled()) return;
		if (mc.world == null || mc.player == null) return;

		Camera camera = mc.gameRenderer.getCamera();
		if (camera == null) return;
		Vec3d cam = camera.getPos();

		MatrixStack ms = new MatrixStack();
		ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180f));

		VertexConsumerProvider.Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();
		VertexConsumer vc = imm.getBuffer(RenderLayer.getLines());

		boolean playersOnly = hb.boolVal("Players only");
		double range = hb.numVal("Range");
		double rangeSq = range * range;

		int baseColor = hb.colorVal("Color");
		int hitColor = hb.colorVal("Hit color");
		long flash = (long) hb.numVal("Hit flash");

		ms.push();
		ms.translate(-cam.x, -cam.y, -cam.z);

		long now = System.currentTimeMillis();
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

			float a = ((color >> 24) & 0xFF) / 255f;
			float r = ((color >> 16) & 0xFF) / 255f;
			float g = ((color >> 8) & 0xFF) / 255f;
			float b = (color & 0xFF) / 255f;

			Box box = e.getBoundingBox();
			VertexRendering.drawBox(ms, vc, box, r, g, b, a);
		}

		ms.pop();
		imm.draw(RenderLayer.getLines());
	}

	/**
	 * Records the attacked entity (for the hit flash) and spawns particles.
	 * Wired to the attack-entity callback.
	 */
	public ActionResult onAttack(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hit) {
		Module hb = ModuleManager.INSTANCE.find("Hitboxes");
		if (hb != null && hb.isEnabled()) {
			lastAttacked = entity;
			lastAttackTime = System.currentTimeMillis();
			if (world.isClient) {
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
