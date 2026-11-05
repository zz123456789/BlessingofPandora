package com.blessingofpandora.compat.love;

import net.minecraft.world.entity.player.Abilities;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A separate server-side flight lease. The vanilla mayfly field remains owned by
 * vanilla and other mods, so their grants/withdrawals remain observable even
 * while Eternal Love is active. Vanilla authority and outgoing packets combine
 * the two sources; no reflective inspection or equipment polling is necessary.
 */
public final class EternalLoveFlight
{
    // Abilities uses object identity. Concurrent reads also remain safe in an
    // integrated server where client-side vanilla code shares this class.
    private static final Set<Abilities> LEASES = ConcurrentHashMap.newKeySet();
    private EternalLoveFlight() {}

    public static boolean allowed(Abilities abilities)
    {
        return abilities.mayfly || LEASES.contains(abilities);
    }

    static boolean acquire(Abilities abilities) { return LEASES.add(abilities); }
    static void release(Abilities abilities) { LEASES.remove(abilities); }
    static void clear() { LEASES.clear(); }
}
