package com.blessingofpandora;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import java.util.ArrayList;
import java.util.List;

/** Transforms only the initial necklace reward, never an existing player inventory. */
public final class StartingGiftContents
{
    private StartingGiftContents() {}

    public static List<CompoundTag> configure(CompoundTag necklace, boolean giveNecklace, boolean giveCurses)
    {
        if (giveNecklace)
        {
            CompoundTag result = necklace.copy();
            if (!giveCurses)
            {
                result.getCompound("tag").remove("PandoraCharms");
            }
            return List.of(result);
        }
        List<CompoundTag> result = new ArrayList<>();
        if (giveCurses)
        {
            var charms = necklace.getCompound("tag").getList("PandoraCharms", Tag.TAG_COMPOUND);
            for (int i = 0; i < charms.size(); i++)
            {
                CompoundTag charm = charms.getCompound(i);
                if (charm.getByte("Count") > 0 && !charm.getString("id").equals("minecraft:air"))
                {
                    result.add(charm.copy());
                }
            }
        }
        return result;
    }
}
