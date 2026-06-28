package dns.visuals.util;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players are running DnsVisuals and decorates their names with a "DV" prefix in the
 * tab list and above their head (and anywhere else that routes a name through {@link #decorate}).
 *
 * <h3>Important limitation</h3>
 * Truly detecting that ANOTHER player runs the same client requires both clients to exchange a
 * handshake. On Minecraft that handshake has to travel through the server, and vanilla servers do
 * NOT relay unknown custom plugin-channel packets between clients (and may even reject them). So
 * automatic cross-client detection only works on a server that runs a small companion plugin which
 * relays the DnsVisuals handshake channel.
 *
 * To keep this useful and dependency-free out of the box, the framework here:
 *  - always marks the LOCAL player as a DV user (you are obviously running the visuals), so the
 *    "DV" tag is visible on yourself in tab and on your own nametag in third person;
 *  - exposes {@link #markDvUser(UUID)} / {@link #unmarkDvUser(UUID)} so a server-side relay plugin
 *    (or a future networking layer) can flag other players as DV users at runtime.
 */
public final class DvNetwork {
	/** UUIDs of players known to be running DnsVisuals (besides the always-included local player). */
	private static final Set<UUID> DV_USERS = ConcurrentHashMap.newKeySet();

	private DvNetwork() {}

	/** Flags a player (by UUID) as a confirmed DnsVisuals user. Intended for a relay plugin/bridge. */
	public static void markDvUser(UUID id) {
		if (id != null) DV_USERS.add(id);
	}

	public static void unmarkDvUser(UUID id) {
		if (id != null) DV_USERS.remove(id);
	}

	/** True if the DVTag module is enabled. */
	public static boolean enabled() {
		Module m = ModuleManager.INSTANCE.find("DVTag");
		return m != null && m.isEnabled();
	}

	/** True if the given UUID is a known DV user (the local player always counts). */
	public static boolean isDvUser(UUID id) {
		if (id == null) return false;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player != null && id.equals(mc.player.getUuid())) return true;
		return DV_USERS.contains(id);
	}

	/**
	 * Returns the name with a gold "DV" prefix when the DVTag module is on and the player is a known
	 * DV user; otherwise returns the original name unchanged.
	 */
	public static Text decorate(Text original, UUID id) {
		if (original == null) return null;
		if (!enabled() || !isDvUser(id)) return original;
		MutableText prefix = Text.literal("DV ").formatted(Formatting.GOLD, Formatting.BOLD);
		return prefix.append(original);
	}
}
