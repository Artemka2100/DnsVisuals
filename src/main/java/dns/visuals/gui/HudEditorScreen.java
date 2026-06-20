package dns.visuals.gui;

import dns.visuals.hud.HudManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Dedicated screen for repositioning HUD elements. Opened with the chat command {@code .hud}.
 *
 * Why a screen instead of a mixin: in 1.21.11 Screen's mouse methods are default interface methods
 * (not declared on net.minecraft.client.gui.screen.Screen itself), so @Inject into "mouseClicked"
 * fails at runtime. Subclassing Screen and overriding these methods is the supported, robust way.
 *
 * The in-game HUD is not drawn behind an open screen, so this screen draws the HUD itself (with
 * drag handles) via {@link HudManager#renderEditor(DrawContext)} and routes mouse events to it.
 */
public class HudEditorScreen extends Screen {

	public HudEditorScreen() {
		super(Text.literal("DnsVisuals HUD Editor"));
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
		ctx.fill(0, 0, this.width, this.height, 0x55000000); // light dim so HUD stays readable
		HudManager.renderEditor(ctx);
		super.render(ctx, mouseX, mouseY, tickDelta);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (HudManager.mouseClicked(click.x(), click.y(), click.button())) return true;
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (HudManager.mouseDragged(click.x(), click.y())) return true;
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(Click click) {
		HudManager.mouseReleased();
		return super.mouseReleased(click);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
			this.close();
			return true;
		}
		return super.keyPressed(input);
	}
}
