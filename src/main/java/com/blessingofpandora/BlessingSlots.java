package com.blessingofpandora;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Equipment-owned slots, independent of Curios' remove/reapply cycle for ItemStack NBT. */
@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public final class BlessingSlots
{
    private static final String[] TYPES = {"necklace", "bracelet", "curio", "hands", "charm"};
    private static final UUID[] IDS = {
            UUID.fromString("b1e55b1e-1630-4000-8000-000000000001"),
            UUID.fromString("b1e55b1e-1630-4000-8000-000000000002"),
            UUID.fromString("b1e55b1e-1630-4000-8000-000000000003"),
            UUID.fromString("b1e55b1e-1630-4000-8000-000000000004"),
            UUID.fromString("b1e55b1e-1630-4000-8000-000000000005")
    };

    private static final Map<Player, int[]> PENDING_REMOVALS = new WeakHashMap<>();

    private BlessingSlots() {}

    static int[] newCounts() { return new int[TYPES.length]; }

    private static int type(BlessingDefinitions definition)
    {
        return switch (definition) {
            case FREEDOM -> 0;
            case INFINITY -> 1;
            case SPIRIT -> 2;
            case FEARLESSNESS, LIBERATION -> 3;
            case REQUIEM -> 4;
            case STRENGTH -> -1;
        };
    }

    static void count(int[] counts, BlessingDefinitions definition)
    {
        int index = type(definition);
        if (index >= 0) counts[index] += definition == BlessingDefinitions.REQUIEM ? 3 : 1;
    }

    /** Called once after the full equipment scan, never inside an attribute/capability callback. */
    static void apply(ICuriosItemHandler inventory, int[] counts)
    {
        // Original curse tokens apply replacement slots at PlayerTick END/NORMAL.
        // Retain existing capacity until then when reverting a blessing to a curse.
        boolean deferRemoval = inventory.getWearer() instanceof Player;
        boolean pending = false;
        for (int i = 0; i < TYPES.length; i++)
        {
            ICurioStacksHandler handler = inventory.getCurios().get(TYPES[i]);
            if (handler == null) continue;
            AttributeModifier old = handler.getModifiers().get(IDS[i]);
            if (deferRemoval && old != null && old.getAmount() > counts[i]) pending = true;
            else setCount(handler, i, counts[i]);
        }
        if (inventory.getWearer() instanceof Player player)
        {
            if (pending) PENDING_REMOVALS.put(player, counts);
            else PENDING_REMOVALS.remove(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        int[] counts = PENDING_REMOVALS.remove(event.player);
        if (counts == null || !event.player.isAlive()) return;
        CuriosApi.getCuriosInventory(event.player).ifPresent(inventory -> {
            for (int i = 0; i < TYPES.length; i++)
            {
                ICurioStacksHandler handler = inventory.getCurios().get(TYPES[i]);
                if (handler != null) setCount(handler, i, counts[i]);
            }
        });
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        PENDING_REMOVALS.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event)
    {
        PENDING_REMOVALS.remove(event.getOriginal());
        PENDING_REMOVALS.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event)
    {
        PENDING_REMOVALS.clear();
    }

    private static void setCount(ICurioStacksHandler handler, int index, double count)
    {
        AttributeModifier old = handler.getModifiers().get(IDS[index]);
        if (old != null && old.getAmount() == count
                && old.getOperation() == AttributeModifier.Operation.ADDITION)
        {
            handler.getCachedModifiers().remove(old);
            if (!handler.getPermanentModifiers().contains(old)) handler.addPermanentModifier(old);
            return;
        }
        // No getStacks/update between removal and addition: consumers see only the final size.
        if (old != null)
        {
            handler.getCachedModifiers().remove(old);
            handler.removeModifier(IDS[index]);
        }
        if (count > 0)
        {
            // Persist through login so Curios' cached-modifier cleanup cannot briefly shrink slots.
            handler.addPermanentModifier(new AttributeModifier(IDS[index],
                    "blessingofpandora:equipped_slots/" + TYPES[index], count,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeCuriosTick(LivingEvent.LivingTickEvent event)
    {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide) return;
        CuriosApi.getCuriosInventory(wearer).ifPresent(BlessingSlots::migrateCachedSlots);
    }

    /** Hand off only our 1.6.2 saved modifiers before Curios clears its deserialization cache. */
    private static void migrateCachedSlots(ICuriosItemHandler inventory)
    {
        for (int i = 0; i < TYPES.length; i++)
        {
            ICurioStacksHandler handler = inventory.getCurios().get(TYPES[i]);
            if (handler == null || handler.getCachedModifiers().isEmpty()) continue;
            double migrated = 0;
            var legacy = new ArrayList<AttributeModifier>();
            for (AttributeModifier modifier : new ArrayList<>(handler.getCachedModifiers()))
            {
                if (modifier.getOperation() != AttributeModifier.Operation.ADDITION
                        || !isLegacyName(i, modifier.getName())) continue;
                migrated += modifier.getAmount();
                legacy.add(modifier);
            }
            if (migrated > 0)
            {
                AttributeModifier current = handler.getModifiers().get(IDS[i]);
                setCount(handler, i, Math.max(migrated, current == null ? 0 : current.getAmount()));
                for (AttributeModifier modifier : legacy)
                {
                    handler.getCachedModifiers().remove(modifier);
                    handler.removeModifier(modifier.getId());
                }
            }
        }
    }

    private static boolean isLegacyName(int index, String name)
    {
        return switch (index) {
            case 0 -> name.equals("blessing_of_freedom_necklace");
            case 1 -> name.equals("blessing_of_infinity_bracelet");
            case 2 -> name.equals("blessing_of_spirit_curio");
            case 3 -> name.equals("blessing_of_fearlessness_hands")
                    || name.equals("blessing_of_liberation_hands");
            case 4 -> name.equals("blessing_of_requiem_charm");
            default -> false;
        };
    }

}
