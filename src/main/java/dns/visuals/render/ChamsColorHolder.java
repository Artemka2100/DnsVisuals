package dns.visuals.render;

/**
 * Duck-typing interface mixed into LivingEntityRenderState so we can carry a per-entity chams fill
 * color from the extraction phase (updateRenderState, where we still know the entity) to the
 * drawing phase (getMixColor, which only receives the render state).
 *
 * A value of 0 means "no chams" (use the vanilla mix color).
 */
public interface ChamsColorHolder {
	int dnsvisuals$getChamsColor();

	void dnsvisuals$setChamsColor(int color);
}
