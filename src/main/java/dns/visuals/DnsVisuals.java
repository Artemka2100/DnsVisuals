package dns.visuals;

import dns.visuals.config.ConfigManager;
import dns.visuals.gui.ClickGuiScreen;
import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import dns.visuals.render.HitboxRenderer;
import dns.visuals.util.AttackTracker;
import dns.visuals.util.CpsTracker;
import dns.visuals.util.TpsTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

/** Mod entrypoint. Wires up modules, config, CPS/TPS tracking, world visuals and the GUI open key. */
public class DnsVisuals implements ClientModInitializer {
	public static final String MOD_ID = "dnsvisuals";
	public static final CpsTracker CPS = new CpsTracker();
	public static final TpsTracker TPS = new TpsTracker();

	private boolean openKeyWasDown = false;

	@Override
	public void onInitializeClient() {
		ModuleManager.INSTANCE.init();
		ConfigManager.load();

		// 1.21.11: WorldRenderEvents moved to ...rendering.v1.world; BEFORE_DEBUG_RENDER is the
		// recommended hook for drawing lines/overlays (vanilla draws hitboxes here too).
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> HitboxRenderer.INSTANCE.onWorldRender());
		AttackEntityCallback.EVENT.register(HitboxRenderer.INSTANCE::onAttack);

		// Record the most recently attacked entity for the PlayerInfo HUD panel.
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			AttackTracker.record(entity);
			return ActionResult.PASS;
		});

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());

		System.out.println("[DnsVisuals] Loaded " + ModuleManager.INSTANCE.all().size() + " modules.");
	}

	private void onClientTick(MinecraftClient mc) {
		// CPS: drain queued click presses (accurate, not sampled)
		while (mc.options.attackKey.wasPressed()) CPS.onLeftClick();
		while (mc.options.useKey.wasPressed()) CPS.onRightClick();

		// TPS: approximate by feeding a tick each client tick while in a world
		if (mc.world != null) TPS.onTimeUpdate(1);

		// Open ClickGUI on the configured key (default Right Shift)
		Module clickGui = ModuleManager.INSTANCE.find("ClickGUI");
		int key = clickGui != null ? clickGui.keyVal("Open key") : GLFW.GLFW_KEY_RIGHT_SHIFT;
		boolean down = key != GLFW.GLFW_KEY_UNKNOWN
				&& InputUtil.isKeyPressed(mc.getWindow(), key);
		if (down && !openKeyWasDown && mc.currentScreen == null) {
			mc.setScreen(new ClickGuiScreen());
		}
		openKeyWasDown = down;
	}
}
