package com.blessingofpandora;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public enum BlessingDefinitions
{
    FREEDOM("blessing_of_freedom", "curseofpandora:curse_of_inertia", 101),
    INFINITY("blessing_of_infinity", "curseofpandora:curse_of_proximity", 102),
    SPIRIT("blessing_of_spirit", "curseofpandora:curse_of_flesh", 103),
    STRENGTH("blessing_of_strength", "curseofpandora:curse_of_metabolism", 104),
    FEARLESSNESS("blessing_of_fearlessness", "curseofpandora:curse_of_tension", 105),
    REQUIEM("blessing_of_requiem", "curseofpandora:curse_of_prudence", 106),
    LIBERATION("blessing_of_liberation", "curseofpandora:curse_of_spell", 107);

    private static final Map<String, BlessingDefinitions> BY_ID;
    private static final Map<ResourceLocation, BlessingDefinitions> BY_CURSE_ID;

    static
    {
        Map<String, BlessingDefinitions> ids = new HashMap<>();
        Map<ResourceLocation, BlessingDefinitions> curseIds = new HashMap<>();
        for (BlessingDefinitions definition : values())
        {
            ids.put(definition.id, definition);
            curseIds.put(definition.curseId, definition);
        }
        BY_ID = Map.copyOf(ids);
        BY_CURSE_ID = Map.copyOf(curseIds);
    }

    private final String id;
    private final ResourceLocation curseId;
    private final int modelData;

    BlessingDefinitions(String id, String curseId, int modelData)
    {
        this.id = id;
        this.curseId = ResourceLocation.parse(curseId);
        this.modelData = modelData;
    }

    public String id()
    {
        return id;
    }

    public ResourceLocation curseId()
    {
        return curseId;
    }

    public String nameKey()
    {
        return "item.blessingofpandora." + id;
    }

    public int modelData()
    {
        return modelData;
    }

    public Item curseItem()
    {
        return ForgeRegistries.ITEMS.getValue(curseId);
    }

    public static BlessingDefinitions byId(String id)
    {
        return BY_ID.get(id);
    }

    public static BlessingDefinitions fromCurseItem(ItemStack stack)
    {
        // Cache fixed names, never live items or failed lookups before registry initialization.
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return itemId == null ? null : BY_CURSE_ID.get(itemId);
    }

    public boolean matches(ItemStack stack)
    {
        if (!BlessingNbt.isBlessing(stack)) return false;
        Item curse = curseItem();
        if (curse == null || !stack.is(curse))
        {
            return false;
        }
        String blessingId = BlessingNbt.blessingId(stack);
        return blessingId.isEmpty() || blessingId.equals(id);
    }

    public ItemStack createStack()
    {
        return BlessingNbt.createStack(this);
    }
}
