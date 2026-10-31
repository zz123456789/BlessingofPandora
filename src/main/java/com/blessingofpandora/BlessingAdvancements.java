package com.blessingofpandora;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Awarded by successful conversions, never by merely obtaining or wearing a blessing. */
public final class BlessingAdvancements
{
    private static final ResourceLocation ROOT = ResourceLocation.parse("blessingofpandora:root");

    private BlessingAdvancements() {}

    public static void onCurseConverted(Player player, BlessingDefinitions definition)
    {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        // Also covers a conversion before the player's first normal advancement tick.
        award(serverPlayer, ROOT, "joined");
        String name = definition.id().substring("blessing_of_".length());
        award(serverPlayer, ResourceLocation.parse("blessingofpandora:reversal/" + name), "converted");
    }

    private static void award(ServerPlayer player, ResourceLocation id, String criterion)
    {
        // Look up the live definition so data-pack reloads are respected. Missing/disabled data
        // must not interrupt a completed item conversion; vanilla handles duplicate awards.
        var advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement != null) player.getAdvancements().award(advancement, criterion);
    }
}
