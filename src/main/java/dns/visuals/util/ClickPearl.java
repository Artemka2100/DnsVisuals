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
 * "ClickPearl" macro: when the bound key is pressed, instantly switch to an ender pearl in the
 * hotbar, throw it, and switch straight back to whatever the player was holding — all within a
 * single client tick (no swap delay).
 *
 * Driven once per client tick from {@link dns.visuals.DnsVisuals#onClientTick}.
 */
public final class ClickPearl {
	private static boolean keyWasDown = false;

	private ClickPearl() {}

	public static void tick(MinecraftClient mc) {
		Module m = ModuleManager.INSTANCE.find("ClickPearl");
		if (m == null || !m.isEnabled()) {
			keyWasDown = false;
			return;
		}
		if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

		// Rising-edge key detection on the module's keybind.
		int key = m.keyVal("Key");
		boolean down = key != InputUtil.UNKNOWN_KEY.getCode()
				&& InputUtil.isKeyPressed(mc.getWindow(), key);
		if (down && !keyWasDown) {
			throwPearl(mc, m);
		}
		keyWasDown = down;
	}

	/** Switches to a pearl, throws it, and restores the previous slot — all in one tick. */
	private static void throwPearl(MinecraftClient mc, Module m) {
		int pearlSlot = findPearl(mc);
		if (pearlSlot < 0) {
			if (m.boolVal("Notify")) {
				mc.player.sendMessage(Text.literal("\u00a7c[DnsVisuals] No ender pearl found"), true);
			}
			return;
		}
		int prev = mc.player.getInventory().getSelectedSlot();
		boolean swap = pearlSlot != prev;
		if (swap) {
			mc.player.getInventory().setSelectedSlot(pearlSlot);
			mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(pearlSlot));
		}
		// Throw the pearl.
		mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
		mc.player.swingHand(Hand.MAIN_HAND);
		// Restore the original slot immediately.
		if (swap) {
			mc.player.getInventory().setSelectedSlot(prev);
			mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prev));
		}
	}

	/** Returns the first hotbar slot (0-8) holding an ender pearl, or -1 if none. */
	private static int findPearl(MinecraftClient mc) {
		for (int i = 0; i < 9; i++) {
			if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) return i;
		}
		return -1;
	}
}
