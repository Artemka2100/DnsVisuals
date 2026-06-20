package dns.visuals.render;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Draws entity hitboxes (recolored briefly when you hit something) and spits particles on attack. */
public final class HitboxRenderer {
	private HitboxRenderer() {}

	private static Entity lastAttacked = null;
	private static long lastAttackTime = 0L;

	public static void onWorldRender(WorldRenderContext context) {
		Module hb = ModuleManager.INSTANCE.find("Hitboxes");
		if (hb == null || !hb.isEnabled()) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world == null || mc.player == null) return;

		MatrixStack ms = context.matrixStack();
		if (ms == null) return;
		Vec3d cam = context.camera().getPos();

		VertexConsumerProvider.Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();
		VertexConsumer vc = imm.getBuffer(RenderLayer.getLines());

		int normal = hb.colorVal("Color");
		int hitCol = hb.colorVal("Hit color");
		double range = hb.numVal("Range");
		boolean playersOnly = hb.boolVal("Players only");
		long flash = (long) hb.numVal("Hit flash");

		ms.push();
		ms.translate(-cam.x, -cam.y, -cam.z);
		for (Entity e : mc.world.getEntities()) {
			if (e == mc.player) continue;
			if (playersOnly ? !(e instanceof PlayerEntity) : !(e instanceof LivingEntity)) continue;
			if (e.squaredDistanceTo(mc.player) > range * range) continue;
			boolean hit = e == lastAttacked && (System.currentTimeMillis() - lastAttackTime) < flash;
			int c = hit ? hitCol : normal;
			float a = ((c >> 24) & 0xFF) / 255f;
			float r = ((c >> 16) & 0xFF) / 255f;
			float g = ((c >> 8) & 0xFF) / 255f;
			float b = (c & 0xFF) / 255f;
			Box box = e.getBoundingBox();
			WorldRenderer.drawBox(ms, vc, box, r, g, b, a);
		}
		ms.pop();
		imm.draw(RenderLayer.getLines());
	}

	public static ActionResult onAttack(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
		Module hb = ModuleManager.INSTANCE.find("Hitboxes");
		if (hb != null && hb.isEnabled()) {
			lastAttacked = entity;
			lastAttackTime = System.currentTimeMillis();
			if (world.isClient && hb.boolVal("Particles")) {
				spawnParticles(world, entity, hb);
			}
		}
		return ActionResult.PASS;
	}

	private static void spawnParticles(World world, Entity entity, Module hb) {
		DefaultParticleType type = switch (hb.modeVal("Particle")) {
			case "Flame" -> ParticleTypes.FLAME;
			case "Heart" -> ParticleTypes.HEART;
			case "Cloud" -> ParticleTypes.CLOUD;
			case "Angry" -> ParticleTypes.ANGRY_VILLAGER;
			default -> ParticleTypes.CRIT;
		};
		int count = (int) hb.numVal("Particle count");
		for (int i = 0; i < count; i++) {
			world.addParticle(type,
					entity.getParticleX(0.6),
					entity.getRandomBodyY(),
					entity.getParticleZ(0.6),
					0.0, 0.0, 0.0);
		}
	}
}
