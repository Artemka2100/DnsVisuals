package dns.visuals.util;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * AutoTool: while the player is breaking a block with the attack key held, automatically switches
 * the hotbar selection to the most effective tool for that block. Optionally restores the previous
 * slot once mining stops, and avoids tools that are about to break.
 *
 * Uses only public API ({@code ItemStack#getMiningSpeedMultiplier}) so it stays compatible across
 * 1.21.x.
 */
public final class AutoTool {
	private AutoTool() {}

	private static int previousSlot = -1;
	private static boolean switched = false;

	public static void tick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity p = mc.player;
		Module m = ModuleManager.INSTANCE.find("AutoTool");

		if (m == null || !m.isEnabled() || p == null || mc.world == null) {
			restoreIfNeeded(p, m);
			return;
		}

		boolean mining = mc.options.attackKey.isPressed()
				&& mc.crosshairTarget != null
				&& mc.crosshairTarget.getType() == HitResult.Type.BLOCK;
		if (!mining) {
			restoreIfNeeded(p, m);
			return;
		}

		BlockHitResult bhr = (BlockHitResult) mc.crosshairTarget;
		BlockState state = mc.world.getBlockState(bhr.getBlockPos());
		PlayerInventory inv = p.getInventory();
		int current = inv.getSelectedSlot();

		int best = current;
		double bestScore = scoreSlot(inv.getStack(current), state, m);
		for (int i = 0; i < 9; i++) {
			double s = scoreSlot(inv.getStack(i), state, m);
			if (s > bestScore) {
				bestScore = s;
				best = i;
			}
		}

		if (best != current) {
			if (!switched) {
				previousSlot = current;
				switched = true;
			}
			inv.setSelectedSlot(best);
		}
	}

	private static double scoreSlot(ItemStack stack, BlockState state, Module m) {
		if (stack.isEmpty()) return -1;
		float speed = stack.getMiningSpeedMultiplier(state);
		if (speed <= 1.0f) return -1; // not an effective tool for this block
		double score = speed;
		if (m.boolVal("Avoid low durability") && stack.isDamageable()) {
			int left = stack.getMaxDamage() - stack.getDamage();
			if (left <= 5) score -= 1000; // strongly avoid nearly-broken tools
		}
		return score;
	}

	private static void restoreIfNeeded(ClientPlayerEntity p, Module m) {
		if (switched && p != null && m != null && m.boolVal("Switch back")
				&& previousSlot >= 0 && previousSlot < 9) {
			p.getInventory().setSelectedSlot(previousSlot);
		}
		switched = false;
		previousSlot = -1;
	}
}
