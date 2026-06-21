package dns.visuals;

import dns.visuals.config.ConfigManager;
import dns.visuals.gui.ClickGuiScreen;
import dns.visuals.gui.HudEditorScreen;
import dns.visuals.module.Category;
import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import dns.visuals.render.Ambient;
import dns.visuals.render.BlockOutlineRenderer;
import dns.visuals.render.EspRenderer;
import dns.visuals.render.HitboxRenderer;
import dns.visuals.render.JumpCircle;
import dns.visuals.render.Waypoint;
import dns.visuals.render.WaypointHud;
import dns.visuals.setting.BooleanSetting;
import dns.visuals.setting.ColorSetting;
import dns.visuals.setting.ModeSetting;
import dns.visuals.setting.SliderSetting;
import dns.visuals.util.AttackTracker;
import dns.visuals.util.AutoTool;
import dns.visuals.util.CpsTracker;
import dns.visuals.util.TpsTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

/** Mod entrypoint. Wires up modules, config, CPS/TPS tracking, world visuals and the GUI open key. */
public class DnsVisuals implements ClientModInitializer {
	public static final String MOD_ID = "dnsvisuals";
	public static final CpsTracker CPS = new CpsTracker();
	public static final TpsTracker TPS = new TpsTracker();

	private boolean openKeyWasDown = false;
	private boolean fullbrightApplied = false;
	private boolean prevOnGround = true;

	@Override
	public void onInitializeClient() {
		ModuleManager.INSTANCE.init();
		// Register the Waypoint HUD module before loading config so its state persists too.
		ModuleManager.INSTANCE.all().add(WaypointHud.createModule());

		// Extra behaviour modules registered here so they show up in their categories.
		Module fullbright = new Module("Fullbright", "Brighten the world (Night Vision)", Category.RENDER);
		ModuleManager.INSTANCE.all().add(fullbright);

		Module sprint = new Module("Sprint", "Automatically keep sprinting", Category.MISC);
		sprint.add(new BooleanSetting("Keep sprint", "Sprint without holding forward", false));
		ModuleManager.INSTANCE.all().add(sprint);

		// Time: client-side visual time-of-day override (handled by WorldMixin).
		Module time = new Module("Time", "Override the time of day (visual only)", Category.RENDER);
		time.add(new ModeSetting("Time", "Time of day to lock", "Day",
				"Morning", "Day", "Noon", "Sunset", "Night", "Midnight"));
		ModuleManager.INSTANCE.all().add(time);

		// Weather: client-side visual weather override (handled by WorldMixin).
		Module weather = new Module("Weather", "Override the weather (visual only)", Category.RENDER);
		weather.add(new ModeSetting("Weather", "Weather to show", "Clear",
				"Clear", "Rain", "Thunder"));
		ModuleManager.INSTANCE.all().add(weather);

		// JumpCircles: ring/square of particles spawned under the player on jump.
		Module jump = new Module("JumpCircles", "Spawn a ring/square of particles when you jump", Category.RENDER);
		jump.add(new ModeSetting("Shape", "Ring shape", "Circle", "Circle", "Square"));
		jump.add(new SliderSetting("Radius", "Ring radius", 0.6, 0.2, 3.0, 0.1, " blocks"));
		jump.add(new SliderSetting("Points", "Particle count", 24, 4, 64, 1, ""));
		jump.add(new ModeSetting("Particle", "Particle type", "End rod",
				"End rod", "Flame", "Crit", "Cloud", "Note", "Happy"));
		ModuleManager.INSTANCE.all().add(jump);

		// AspectRatio: stretch the rendered world horizontally/vertically (handled by GameRendererMixin).
		Module aspect = new Module("AspectRatio", "Stretch the rendered world", Category.RENDER);
		aspect.add(new SliderSetting("Stretch X", "Horizontal stretch", 1.0, 0.5, 2.0, 0.05, "x"));
		aspect.add(new SliderSetting("Stretch Y", "Vertical stretch", 1.0, 0.5, 2.0, 0.05, "x"));
		ModuleManager.INSTANCE.all().add(aspect);

		// Ambients: soft floating particles around the player (handled by Ambient.tick).
		Module ambients = new Module("Ambients", "Floating particles around you", Category.RENDER);
		ambients.add(new SliderSetting("Density", "Particles per tick", 2, 1, 10, 1, ""));
		ambients.add(new SliderSetting("Radius", "Spawn radius", 1.5, 0.5, 4.0, 0.1, " blocks"));
		ambients.add(new SliderSetting("Size", "Particle size", 1.0, 0.3, 3.0, 0.1, "x"));
		ambients.add(new ColorSetting("Color", "Particle color", 180, 220, 255, 255));
		ModuleManager.INSTANCE.all().add(ambients);

		// NOTE: CustomHand and CustomSwing are registered by ModuleManager.buildRender() with their
		// full feature set (per-hand offsets / multiple swing styles). They used to be registered a
		// second time here with a simpler setting set, which created dead duplicate entries in the
		// menu (find() returned the ModuleManager copies, so the duplicates did nothing). Removed.

		ConfigManager.load();

		// 1.21.11: WorldRenderEvents moved to ...rendering.v1.world; BEFORE_DEBUG_RENDER is the
		// recommended hook for drawing lines/overlays (vanilla draws hitboxes here too).
		// BlockOutline also draws here now: the old BLOCK_OUTLINE event/context was removed in the
		// 1.21.9 port, so we read the crosshair target instead.
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
			HitboxRenderer.INSTANCE.onWorldRender(context);
			EspRenderer.INSTANCE.onWorldRender(context);
			BlockOutlineRenderer.INSTANCE.onWorldRender(context);
		});

		AttackEntityCallback.EVENT.register(HitboxRenderer.INSTANCE::onAttack);

		// Record the most recently attacked entity for the PlayerInfo HUD panel.
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			AttackTracker.record(entity);
			return ActionResult.PASS;
		});

		// Intercept client-side chat commands (cancel sending them to the server):
		//  .hud  -> open the HUD editor screen (drag elements to reposition them)
		//  .goto -> waypoint commands handled by Waypoint
		ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
			if (message.trim().equalsIgnoreCase(".hud")) {
				MinecraftClient mc = MinecraftClient.getInstance();
				mc.send(() -> mc.setScreen(new HudEditorScreen()));
				return false;
			}
			return !Waypoint.handleCommand(message);
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

		// Module-driven per-tick behaviour (only while actually in a world)
		if (mc.player != null && mc.world != null) {
			tickFullbright(mc);
			tickSprint(mc);
			AutoTool.tick();
			Ambient.tick(mc);

			// JumpCircles: fire on the rising edge of leaving the ground with upward velocity.
			boolean onGround = mc.player.isOnGround();
			if (prevOnGround && !onGround && mc.player.getVelocity().y > 0.08) {
				JumpCircle.onJump(mc);
			}
			prevOnGround = onGround;
		}

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

	/** Keeps Night Vision applied client-side while Fullbright is enabled; removes it when off. */
	private void tickFullbright(MinecraftClient mc) {
		Module fb = ModuleManager.INSTANCE.find("Fullbright");
		boolean on = fb != null && fb.isEnabled();
		if (on) {
			mc.player.addStatusEffect(new StatusEffectInstance(
					StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
			fullbrightApplied = true;
		} else if (fullbrightApplied) {
			mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
			fullbrightApplied = false;
		}
	}

	/** Auto-sprint: sprints while moving forward (or always, with "Keep sprint") if not starving/sneaking. */
	private void tickSprint(MinecraftClient mc) {
		Module sprint = ModuleManager.INSTANCE.find("Sprint");
		if (sprint == null || !sprint.isEnabled()) return;
		boolean keep = sprint.boolVal("Keep sprint");
		boolean forward = mc.options.forwardKey.isPressed();
		boolean starving = mc.player.getHungerManager().getFoodLevel() <= 6;
		if ((keep || forward) && !mc.player.isSneaking() && !starving) {
			mc.player.setSprinting(true);
		}
	}
}
