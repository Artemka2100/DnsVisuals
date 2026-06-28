package dns.visuals.util;

import com.mojang.authlib.minecraft.client.MinecraftClient; // not used directly; kept out below
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.session.Session;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages a list of OFFLINE (cracked) account profiles for the in-lobby Profile Manager.
 *
 * IMPORTANT LIMITATION: this only supports offline accounts — you type a nickname and it builds an
 * offline {@link Session} with the deterministic offline UUID (the same scheme the vanilla server
 * uses for cracked players). It does NOT perform Microsoft/Mojang authentication, so these accounts
 * will only work on servers running in offline mode (or singleplayer/LAN). Premium login would
 * require an OAuth device-code flow and bundling authlib network calls, which is out of scope here.
 *
 * Profiles are persisted as one nickname per line in config/dnsvisuals_alts.txt.
 */
public final class AltManager {
	private static final List<String> PROFILES = new ArrayList<>();
	private static boolean loaded = false;

	private AltManager() {}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("dnsvisuals_alts.txt");
	}

	/** Returns the stored profile nicknames (loads from disk on first use). */
	public static List<String> profiles() {
		if (!loaded) load();
		return PROFILES;
	}

	/** Adds an offline profile by nickname. Returns false if blank or already present. */
	public static boolean add(String name) {
		if (!loaded) load();
		String n = name == null ? "" : name.trim();
		if (n.isEmpty() || n.length() > 16) return false;
		for (String existing : PROFILES) {
			if (existing.equalsIgnoreCase(n)) return false;
		}
		PROFILES.add(n);
		save();
		return true;
	}

	/** Removes a profile by nickname. */
	public static boolean remove(String name) {
		if (!loaded) load();
		boolean changed = PROFILES.removeIf(p -> p.equalsIgnoreCase(name));
		if (changed) save();
		return changed;
	}

	/** Deterministic offline UUID matching the vanilla "OfflinePlayer:<name>" scheme. */
	public static UUID offlineUuid(String name) {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Builds an offline {@link Session} for the given nickname. The access token is the literal
	 * "0" (invalid for online auth), which is fine for offline-mode servers.
	 */
	public static Session createOfflineSession(String name) {
		UUID uuid = offlineUuid(name);
		return new Session(
				name,
				uuid,
				"0",
				Optional.empty(),
				Optional.empty(),
				Session.AccountType.LEGACY
		);
	}

	private static void load() {
		loaded = true;
		PROFILES.clear();
		Path p = file();
		if (!Files.exists(p)) return;
		try (BufferedReader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
			String line;
			while ((line = r.readLine()) != null) {
				String n = line.trim();
				if (!n.isEmpty()) PROFILES.add(n);
			}
		} catch (Exception e) {
			System.err.println("[DnsVisuals] Failed to load alts: " + e.getMessage());
		}
	}

	private static void save() {
		try (BufferedWriter w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
			for (String n : PROFILES) {
				w.write(n);
				w.newLine();
			}
		} catch (Exception e) {
			System.err.println("[DnsVisuals] Failed to save alts: " + e.getMessage());
		}
	}
}
