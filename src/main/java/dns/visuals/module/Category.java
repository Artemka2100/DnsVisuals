package dns.visuals.module;

/** The five ClickGUI tabs. */
public enum Category {
	HUD("HUD"),
	RENDER("Render"),
	INTERFACE("Interface"),
	CHAT("Chat"),
	MISC("Misc");

	public final String title;

	Category(String title) {
		this.title = title;
	}
}
