package com.blessingofpandora;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

public final class ReversalConfigUtil
{
    // Only immutable identifier parsing is cached. Registry contents are looked up live.
    private static final Cache<String, Optional<ResourceLocation>> PARSED_IDS =
            CacheBuilder.newBuilder().maximumSize(128).build();

    private ReversalConfigUtil()
    {
    }

    public static boolean isAny(String id)
    {
        return id == null || id.isBlank() || "any".equalsIgnoreCase(id)
                || "minecraft:any".equalsIgnoreCase(id);
    }

    public static MobEffect effect(String id)
    {
        ResourceLocation location = safeId(id);
        if (location == null || !ForgeRegistries.MOB_EFFECTS.containsKey(location))
        {
            return null;
        }
        return ForgeRegistries.MOB_EFFECTS.getValue(location);
    }

    public static Item item(String id)
    {
        ResourceLocation location = safeId(id);
        if (location == null || !ForgeRegistries.ITEMS.containsKey(location))
        {
            return null;
        }
        return ForgeRegistries.ITEMS.getValue(location);
    }

    public static EntityType<?> entityType(String id)
    {
        if (isAny(id))
        {
            return null;
        }
        ResourceLocation location = safeId(id);
        // A missing key can otherwise resolve to the registry's default entity (pig).
        return location == null || !ForgeRegistries.ENTITY_TYPES.containsKey(location)
                ? null : ForgeRegistries.ENTITY_TYPES.getValue(location);
    }

    public static boolean entityMatches(LivingEntity entity, String id)
    {
        if (isAny(id))
        {
            return true;
        }
        EntityType<?> configured = entityType(id);
        return configured != null && entity.getType() == configured;
    }

    public static boolean isHolding(Player player, String id)
    {
        Item item = item(id);
        return item != null
                && (player.getMainHandItem().is(item) || player.getOffhandItem().is(item));
    }

    public static Component displayEntityName(String id)
    {
        EntityType<?> entityType = entityType(id);
        if (entityType != null)
        {
            return Component.translatable(entityType.getDescriptionId());
        }
        return Component.literal(id);
    }

    public static Component displayEffectName(String id)
    {
        MobEffect effect = effect(id);
        if (effect != null)
        {
            return Component.translatable(effect.getDescriptionId());
        }
        return Component.literal(id);
    }

    public static Component displayItemName(String id)
    {
        Item item = item(id);
        if (item != null)
        {
            return new ItemStack(item).getHoverName();
        }
        return Component.literal(id);
    }

    private static ResourceLocation safeId(String id)
    {
        if (id == null || id.isBlank())
        {
            return null;
        }
        Optional<ResourceLocation> cached = PARSED_IDS.getIfPresent(id);
        if (cached != null) return cached.orElse(null);
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        PARSED_IDS.put(id, Optional.ofNullable(parsed));
        return parsed;
    }
}
