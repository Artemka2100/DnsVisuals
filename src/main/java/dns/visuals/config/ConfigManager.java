package dns.visuals.config;

import dns.visuals.hud.HudManager;
import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import dns.visuals.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Saves/loads module enabled-state and settings.
 * Line format:  moduleName|key|value   (key "#enabled" stores the toggle state).
 * HUD element positions use a reserved module name "#hud":  #hud|elementName|x,y
 *
 * The default config is config/dnsvisuals.txt (auto-saved on quit, loaded on start).
 * Named configs live in config/dnsvisuals/<name>.txt and are managed from the Config Manager
 * screen: you can create them (snapshot the current state), load them, and delete them.
 */
public final class ConfigManager {
	private ConfigManager() {}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("dnsvisuals.txt");
	}

	/** Folder holding named configs (created on demand). */
	private static Path dir() {
		return FabricLoader.getInstance().getConfigDir().resolve("dnsvisuals");
	}

	/** Sanitises a user-supplied config name into a safe file stem. */
	private static String sanitize(String name) {
		String s = name == null ? "" : name.trim().replaceAll("[^a-zA-Z0-9_\\- ]", "");
		return s.isEmpty() ? "config" : s;
	}

	// ------------------------------------------------------------------
	// Default config (used at startup / shutdown)
	// ------------------------------------------------------------------

	public static void save() {
		writeTo(file());
	}

	public static void load() {
		readFrom(file());
	}

	// ------------------------------------------------------------------
	// Named configs
	// ------------------------------------------------------------------

	/** Returns the names of all saved named configs (without extension), sorted. */
	public static List<String> list() {
		List<String> out = new ArrayList<>();
		Path d = dir();
		if (!Files.isDirectory(d)) return out;
		try (Stream<Path> s = Files.list(d)) {
			s.filter(p -> p.getFileName().toString().endsWith(".txt"))
					.forEach(p -> {
						String n = p.getFileName().toString();
						out.add(n.substring(0, n.length() - 4));
					});
		} catch (Exception e) {
			System.err.println("[DnsVisuals] Failed to list configs: " + e.getMessage());
		}
		out.sort(String.CASE_INSENSITIVE_ORDER);
		return out;
	}

	/** Snapshots the current module state into a named config. Returns the stored name. */
	public static String saveNamed(String name) {
		String stem = sanitize(name);
		try {
			Files.createDirectories(dir());
		} catch (Exception ignored) {}
		writeTo(dir().resolve(stem + ".txt"));
		return stem;
	}

	/** Loads a named config into the live module state. Returns false if it doesn't exist. */
	public static boolean loadNamed(String name) {
		Path p = dir().resolve(sanitize(name) + ".txt");
		if (!Files.exists(p)) return false;
		readFrom(p);
		// Mirror the loaded state into the default file so it survives the next restart.
		save();
		return true;
	}

	/** Deletes a named config. Returns true if a file was removed. */
	public static boolean deleteNamed(String name) {
		try {
			return Files.deleteIfExists(dir().resolve(sanitize(name) + ".txt"));
		} catch (Exception e) {
			System.err.println("[DnsVisuals] Failed to delete config: " + e.getMessage());
			return false;
		}
	}

	// ------------------------------------------------------------------
	// Shared serialisation
	// ------------------------------------------------------------------

	private static void writeTo(Path path) {
		try {
			if (path.getParent() != null) Files.createDirectories(path.getParent());
			try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				for (Module m : ModuleManager.INSTANCE.all()) {
					w.write(m.name + "|#enabled|" + m.isEnabled());
					w.newLine();
					for (Setting s : m.settings) {
						w.write(m.name + "|" + s.name + "|" + s.save());
						w.newLine();
					}
				}
				// HUD element positions
				for (Map.Entry<String, int[]> e : HudManager.savedPositions().entrySet()) {
					int[] xy = e.getValue();
					if (xy == null || xy.length < 2) continue;
					w.write("#hud|" + e.getKey() + "|" + xy[0] + "," + xy[1]);
					w.newLine();
				}
			}
		} catch (Exception e) {
			System.err.println("[DnsVisuals] Failed to save config: " + e.getMessage());
		}
	}

	private static void readFrom(Path p) {
		if (!Files.exists(p)) return;
		try (BufferedReader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
			String line;
			while ((line = r.readLine()) != null) {
				String[] parts = line.split("\\|", 3);
				if (parts.length != 3) continue;
				// HUD element position line
				if (parts[0].equals("#hud")) {
					String[] xy = parts[2].split(",");
					if (xy.length == 2) {
						try {
							HudManager.loadPosition(parts[1], Integer.parseInt(xy[0].trim()), Integer.parseInt(xy[1].trim()));
						} catch (NumberFormatException ignored) {}
					}
					continue;
				}
				Module m = ModuleManager.INSTANCE.find(parts[0]);
				if (m == null) continue;
				if (parts[1].equals("#enabled")) {
					m.setEnabled(Boolean.parseBoolean(parts[2]));
					m.toggleAnim = m.isEnabled() ? 1 : 0;
				} else {
					Setting s = m.get(parts[1]);
					if (s != null) s.load(parts[2]);
				}
			}
		} catch (Exception e) {
			System.err.println("[DnsVisuals] Failed to load config: " + e.getMessage());
		}
	}
}
