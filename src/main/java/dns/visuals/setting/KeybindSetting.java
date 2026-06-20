package dns.visuals.setting;

import org.lwjgl.glfw.GLFW;

/** A key-bind setting. Click it in the GUI, then press a key to rebind. */
public class KeybindSetting extends Setting {
	public int key;
	public boolean listening = false;

	public KeybindSetting(String name, String description, int key) {
		super(name, description);
		this.key = key;
	}

	public String keyName() {
		if (listening) return "...";
		if (key == GLFW.GLFW_KEY_UNKNOWN) return "None";
		String n = GLFW.glfwGetKeyName(key, 0);
		if (n != null) return n.toUpperCase();
		return switch (key) {
			case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R-SHIFT";
			case GLFW.GLFW_KEY_LEFT_SHIFT -> "L-SHIFT";
			case GLFW.GLFW_KEY_LEFT_CONTROL -> "L-CTRL";
			case GLFW.GLFW_KEY_TAB -> "TAB";
			default -> "KEY_" + key;
		};
	}

	@Override
	public String save() {
		return Integer.toString(key);
	}

	@Override
	public void load(String v) {
		try { key = Integer.parseInt(v); } catch (NumberFormatException ignored) {}
	}
}
