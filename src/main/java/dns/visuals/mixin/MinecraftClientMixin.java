package dns.visuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin for {@link MinecraftClient}.
 *
 * Exposes a setter for the otherwise-final {@code session} field so the Profile Manager can switch
 * the active (offline) account at runtime.
 *
 * NOTE: this used to also force the vanilla entity outline on for the old Chams feature. Chams was
 * removed (ESP/Chams cleanup), so that inject — which referenced the now-deleted
 * {@code dns.visuals.render.Chams} class and broke compilation — has been deleted.
 */
@Mixin(MinecraftClient.class)
public interface MinecraftClientMixin {

	@Mutable
	@Accessor("session")
	void dnsvisuals$setSession(Session session);
}
