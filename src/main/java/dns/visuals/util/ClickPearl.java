package dns.visuals.util;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

/**
 * "ClickPearl" macro: when the bound key is pressed, this finds an ender pearl in the hotbar,
 * silently switches to it, throws it, then switches back to whatever the player was holding.
 *
 * Driven once per client tick from {@link dns.visuals.DnsVisuals#onClientTick}. A small
 * configurable swap delay (in ticks) is used so the server registers the held-item change before
 * the use packet, and again before swapping back.
 */
public final class ClickPearl {
	private static boolean keyWasDown = false;

	// Pending restore state while a throw is in progress.
	private static int restoreSlot = -1;
	private static int delayLeft = 0;
	private static int phase = 0; // 0 = idle, 1 = wait then throw, 2 = wait then restore

	private ClickPearl() {}

	public static void tick(MinecraftClient mc) {
		Module m = ModuleManager.INSTANCE.find("ClickPearl");
		if (m == null || !m.isEnabled()) {
			keyWasDown = false;
			phase = 0;
			return;
		}
		if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

		// Advance an in-progress throw sequence.
		if (phase != 0) {
			if (delayLeft > 0) {
				delayLeft--;
				return;
			}
			if (phase == 1) {
				// Pearl slot is selected now: throw it.
				mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
				mc.player.swingHand(Hand.MAIN_HAND);
				delayLeft = (int) m.numVal("Swap delay");
				phase = 2;
			} else {
				// Restore the original slot.
				if (restoreSlot >= 0) {
					mc.player.getInventory().selectedSlot = restoreSlot;
					mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(restoreSlot));
				}
				restoreSlot = -1;
				phase = 0;
			}
			return;
		}

		// Rising-edge key detection on the module's keybind.
		int key = m.keyVal("Key");
		boolean down = key != InputUtil.UNKNOWN_KEY.getCode()
				&& InputUtil.isKeyPressed(mc.getWindow().getHandle(), key);
		if (down && !keyWasDown) {
			startThrow(mc, m);
		}
		keyWasDown = down;
	}

	private static void startThrow(MinecraftClient mc, Module m) {
		int pearlSlot = findPearl(mc);
		if (pearlSlot < 0) {
			if (m.boolVal("Notify")) {
				mc.player.sendMessage(Text.literal("\u00a7c[DnsVisuals] No ender pearl found"), true);
			}
			return;
		}
		restoreSlot = mc.player.getInventory().selectedSlot;
		if (pearlSlot == restoreSlot) {
			// Already holding a pearl: throw immediately, no restore needed.
			mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
			mc.player.swingHand(Hand.MAIN_HAND);
			restoreSlot = -1;
			return;
		}
		mc.player.getInventory().selectedSlot = pearlSlot;
		mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(pearlSlot));
		delayLeft = (int) m.numVal("Swap delay");
		phase = 1;
	}

	/** Returns the first hotbar slot (0-8) holding an ender pearl, or -1 if none. */
	private static int findPearl(MinecraftClient mc) {
		for (int i = 0; i < 9; i++) {
			if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) return i;
		}
		return -1;
	}
}
