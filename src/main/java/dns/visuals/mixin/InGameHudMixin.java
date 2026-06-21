package dns.visuals.mixin;

import dns.visuals.hud.HudManager;
import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import dns.visuals.render.EspRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Custom crosshair + HUD rendering hook. HudRenderCallback was removed in 1.21.x, so HUD is drawn at the tail of InGameHud#render. */
@Mixin(InGameHud.class)
public class InGameHudMixin {

	@Inject(method = "render", at = @At("TAIL"))
	private