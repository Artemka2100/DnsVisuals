package dns.visuals.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks item cooldowns for the text CooldownList HUD (no icons).
 *
 * Vanilla's cooldown manager only exposes a 0..1 progress value (no public remaining-ticks per
 * item), so we estimate the total duration once from the first samples and derive the remaining
 * seconds from the live progress. Uses only public API ({@code getCooldownProgress}) so it stays
 * compatible across 1.21.x.
 */
public final class CooldownTracker {
	private CooldownTracker() {}

	private static final class State {
		long startMs;
		double totalSec = -1;
	}

	private static final Map<Item, State> states = new HashMap<>();

	/** Returns lines like "Ender Pearl  0.8s" for items currently on cooldown. */
	public static List<String> poll() {
		List<String> out = new ArrayList<>();
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity p = mc.player;
		if (p == null) {
			states.clear();
			return out;
		}
		var cm = p.getItemCooldownManager();
		long now = System.currentTimeMillis();

		// Candidate stacks: hotbar (0-8) + offhand. One entry per distinct item.
		List<ItemStack> stacks = new ArrayList<>();
		for (int i = 0; i < 9; i++) stacks.add(p.getInventory().getStack(i));
		stacks.add(p.getOffHandStack());

		Set<Item> seen = new HashSet<>();
		for (ItemStack stack : stacks) {
			if (stack.isEmpty()) continue;
			Item item = stack.getItem();
			if (!seen.add(item)) continue;

			float prog = cm.getCooldownProgress(stack, 0f);
			if (prog <= 0f) {
				states.remove(item);
				continue;
			}

			State st = states.get(item);
			if (st == null) {
				st = new State();
				st.startMs = now;
				states.put(item, st);
			}
			double elapsed = (now - st.startMs) / 1000.0;
			// Estimate total duration once we have a meaningful drop in progress.
			if (st.totalSec < 0 && (1f - prog) > 0.05f && elapsed > 0.05) {
				st.totalSec = elapsed / (1f - prog);
			}
			double remaining = st.totalSec > 0
					? st.totalSec * prog
					: elapsed * prog / Math.max(0.001, (1f - prog));
			if (remaining < 0) remaining = 0;
			out.add(stack.getName().getString() + "  " + String.format("%.1fs", remaining));
		}

		// Forget items no longer present/cooling.
		states.keySet().removeIf(it -> !seen.contains(it));
		return out;
	}
}
