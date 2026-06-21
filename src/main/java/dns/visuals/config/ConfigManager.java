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
import java.util.Map;

/**
 * Saves/loads module enabled-state and settings to config/dnsvisuals.txt.
 * Line format:  moduleName|key|value   (key "#enabled" stores the toggle state).
 * HUD element positions use a reserved module name "#hud":  #hud|elementName|x,y
 */
public final class ConfigManager {
	private ConfigManager() {}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("dnsvisuals.txt");
	}

	public static void save() {
		try (BufferedWriter w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
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
		} catch (Exception e) {
			System.err.println("[DnsVisuals] Failed to save config: " + e.getMessage());
		}
	}

	public static void load() {
		Path p = file();
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
