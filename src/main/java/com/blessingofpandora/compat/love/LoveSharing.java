package com.blessingofpandora.compat.love;

import com.blessingofpandora.*;
import dev.xkmc.curseofpandora.content.complex.AttrAdder;
import dev.xkmc.curseofpandora.content.complex.ISlotAdderItem;
import dev.xkmc.curseofpandora.content.pandora.AttributeItem;
import dev.xkmc.curseofpandora.content.pandora.StabilityCharm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotAttribute;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Server-only, read-only equipment sharing. No virtual player, copied inventory or reflected calls. */
public final class LoveSharing
{
    public static final String SLOT = "pandora_charm";
    private static final ResourceLocation MAID_ID = ResourceLocation.parse("touhou_little_maid:maid");
    private static final Map<UUID, Snapshot> OWNERS = new HashMap<>();
    private static final Map<LivingEntity, MaidState> MAIDS = new WeakHashMap<>();
    private static final Set<UUID> DIRTY = new HashSet<>();
    private static EntityType<?> maidType;
    private static boolean registered;
    private static volatile int configRevision;

    private record Bonus(Attribute attribute, AttributeModifier modifier) {}
    private record ModifierKey(Attribute attribute, UUID uuid) {}
    private record Snapshot(ServerPlayer owner, List<ItemStack> stacks, Set<String> items,
                            int blessings, List<Bonus> bonuses, int enchantments, int configRevision) {}
    private static final class MaidState
    {
        Snapshot snapshot;
        final Map<ModifierKey, AttributeModifier> applied = new HashMap<>();
    }

    private LoveSharing() {}

    public static void invalidateConfig() { configRevision++; }

    public static void register()
    {
        if (registered || !LoveOrbRegistration.isAvailable()) return;
        registered = true;
        maidType = ForgeRegistries.ENTITY_TYPES.getValue(MAID_ID);
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(LoveSharing::onPlayerTick);
        // Set up the shared blessing mask before BlessingBenefits' LOWEST tick listener.
        bus.addListener(EventPriority.LOW, LoveSharing::onLivingTick);
        bus.addListener(LoveSharing::onCurioChange);
        bus.addListener(LoveSharing::onLogout);
        bus.addListener(LoveSharing::onDeath);
        bus.addListener(LoveSharing::onClone);
        bus.addListener(LoveSharing::onEntityLeave);
        bus.addListener(LoveSharing::onServerStopped);
        LovePandoraEffects.register();
    }

    public static void modifyAttributes(EntityAttributeModificationEvent event)
    {
        if (!LoveOrbRegistration.isAvailable()) return;
        for (EntityType<? extends LivingEntity> type : event.getTypes())
        {
            if (!MAID_ID.equals(ForgeRegistries.ENTITY_TYPES.getKey(type))) continue;
            for (var entry : ForgeRegistries.ATTRIBUTES.getEntries())
            {
                ResourceLocation id = entry.getKey().location();
                // These are the attributes used by the audited Pandora charms. Default
                // suppliers are added once, never as a per-tick dynamic attribute registry.
                if (id.getNamespace().equals("curseofpandora") || id.getNamespace().equals("l2damagetracker")
                        || id.toString().equals("minecraft:generic.attack_speed")
                        || id.toString().equals("minecraft:generic.luck")
                        || id.toString().equals("forge:entity_reach")
                        || id.toString().equals("forge:block_reach"))
                {
                    if (!event.has(type, entry.getValue())) event.add(type, entry.getValue());
                }
            }
        }
    }

    /** At most one inventory pass per owner, never one pass per maid or damage event. */
    public static void refresh(ServerPlayer player)
    {
        if (LoveOrbRegistration.isAvailable()) LoveProgressEvents.prepareInventory(player);
        if (!LoveOrbRegistration.isAvailable() || !player.isAlive() || player.isRemoved())
        {
            logout(player);
            return;
        }
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        var handler = inventory == null ? null : inventory.getCurios().get(SLOT);
        if (handler == null)
        {
            LoveProgressEvents.observeWorn(player, List.of());
            OWNERS.remove(player.getUUID());
            return;
        }
        var slots = handler.getStacks();
        boolean linked = false;
        for (int i = 0; i < slots.getSlots(); i++)
            if (LoveProgressEvents.canUse(player, slots.getStackInSlot(i))) linked = true;
        if (!linked)
        {
            LoveProgressEvents.observeWorn(player, List.of());
            OWNERS.remove(player.getUUID());
            return;
        }
        // Keep real source indices: duplicate-allowed attribute charms have distinct UUIDs.
        List<ItemStack> next = new ArrayList<>(slots.getSlots());
        for (int i = 0; i < slots.getSlots(); i++) next.add(slots.getStackInSlot(i));
        LoveProgressEvents.observeWorn(player, next);
        Snapshot previous = OWNERS.get(player.getUUID());
        if (previous != null && previous.owner == player && previous.configRevision == configRevision
                && sameStacks(previous.stacks, next)) return;
        List<ItemStack> copies = next.stream().map(ItemStack::copy).toList();
        Set<String> items = new HashSet<>();
        Map<ModifierKey, Bonus> bonuses = new LinkedHashMap<>();
        int blessings = 0;
        int enchantments = 0;
        for (int i = 0; i < copies.size(); i++)
        {
            ItemStack stack = copies.get(i);
            if (stack.isEmpty() || LoveOrbRegistration.isLoveFamily(stack)) continue;
            BlessingDefinitions blessing = BlessingOwners.isUsableBy(stack, player)
                    ? BlessingNbt.getBlessing(stack) : null;
            // Never reinterpret an unusable, owner-bound blessing as its underlying curse.
            if (BlessingNbt.isBlessing(stack) && blessing == null) continue;
            if (!LoveSharingPolicy.isKnown(stack)) continue;
            if (blessing != null) blessings |= 1 << blessing.ordinal();
            else
            {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (id != null && id.getNamespace().equals("curseofpandora")) items.add(id.getPath());
            }
            collectAttributes(player, stack, i, blessing, bonuses);
            enchantments += enchantments(stack, BlessingEnchantmentLevels.MAX_USEFUL_LEVELS - enchantments);
        }
        OWNERS.put(player.getUUID(), new Snapshot(player, copies, Set.copyOf(items), blessings,
                List.copyOf(bonuses.values()), enchantments, configRevision));
    }

    private static boolean sameStacks(List<ItemStack> previous, List<ItemStack> current)
    {
        if (previous.size() != current.size()) return false;
        for (int i = 0; i < previous.size(); i++)
            if (!ItemStack.matches(previous.get(i), current.get(i))) return false;
        return true;
    }

    private static void collectAttributes(ServerPlayer player, ItemStack stack, int slot,
                                           BlessingDefinitions blessing, Map<ModifierKey, Bonus> bonuses)
    {
        UUID uuid = sharedId("slot/" + slot);
        SlotContext context = new SlotContext(SLOT, player, slot, false, false);
        if (blessing != null)
        {
            BlessingAttributes.get(blessing, context, uuid, stack).forEach((attr, modifier) ->
                    putBonus(bonuses, attr, modifier));
        }
        else if (stack.getItem() instanceof AttributeItem || stack.getItem() instanceof StabilityCharm)
        {
            // Only these audited providers are called; unknown third-party ICurio code
            // cannot cast a maid to Player, mutate inventories, or trigger player-only APIs.
            CuriosApi.getCurio(stack).ifPresent(curio ->
                    curio.getAttributeModifiers(context, uuid).forEach((attr, modifier) ->
                            putBonus(bonuses, attr, modifier)));
        }
        else if (stack.getItem() instanceof ISlotAdderItem<?> provider)
        {
            for (var adder : provider.getSlotAdder())
            {
                if (adder instanceof AttrAdder attr)
                {
                    putBonus(bonuses, attr.attr().get(), new AttributeModifier(
                            sharedId("token/" + attr.uuid()), "blessingofpandora:love/" + attr.name(),
                            attr.value().getAsDouble(), attr.op()));
                }
                // SlotAdder is deliberately excluded: sharing never registers Curios slots.
            }
        }
        else
        {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null && id.getNamespace().equals("curseofpandora")
                    && (id.getPath().equals("illusory_affection") || id.getPath().equals("fading_dream")))
                putBonus(bonuses, ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.parse("curseofpandora:reality_index")),
                        new AttributeModifier(uuid, "blessingofpandora:love/el_reality", -1, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void putBonus(Map<ModifierKey, Bonus> bonuses, Attribute attribute, AttributeModifier modifier)
    {
        if (attribute == null || attribute instanceof SlotAttribute || !Double.isFinite(modifier.getAmount())) return;
        bonuses.put(new ModifierKey(attribute, modifier.getId()), new Bonus(attribute, modifier));
    }

    private static UUID sharedId(String key)
    {
        return UUID.nameUUIDFromBytes(("blessingofpandora:love/" + key).getBytes(StandardCharsets.UTF_8));
    }

    private static int enchantments(ItemStack stack, int remaining)
    {
        int total = 0;
        if (remaining <= 0 || stack.isEmpty()) return 0;
        for (var entry : stack.getAllEnchantments().entrySet())
        {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0
                    && ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey()) != null)
                total += Math.min(remaining - total, entry.getValue());
            if (total >= remaining) break;
        }
        return total;
    }

    private static Snapshot ownerSnapshot(LivingEntity entity)
    {
        return ownerSnapshot(entity, false);
    }

    private static Snapshot ownerSnapshot(LivingEntity entity, boolean duringDeath)
    {
        if (entity.level().isClientSide || maidType == null || entity.getType() != maidType || !duringDeath && !entity.isAlive()
                || entity.isRemoved() || !(entity instanceof TamableAnimal tame) || !tame.isTame()) return null;
        UUID owner = tame.getOwnerUUID();
        Snapshot snapshot = owner == null ? null : OWNERS.get(owner);
        return snapshot != null && snapshot.owner.isAlive() && !snapshot.owner.isRemoved() ? snapshot : null;
    }

    public static boolean active(LivingEntity entity) { return ownerSnapshot(entity) != null; }

    /** LivingDeathEvent runs after Health reaches zero, before another listener can save the maid. */
    public static boolean activeAtDeath(LivingEntity entity) { return ownerSnapshot(entity, true) != null; }

    public static boolean hasAtDeath(LivingEntity entity, String itemPath)
    {
        Snapshot snapshot = ownerSnapshot(entity, true);
        return snapshot != null && snapshot.items.contains(itemPath);
    }

    public static boolean has(LivingEntity entity, String itemPath)
    {
        Snapshot snapshot = ownerSnapshot(entity);
        return snapshot != null && snapshot.items.contains(itemPath);
    }

    public static int blessingMask(LivingEntity entity)
    {
        Snapshot snapshot = ownerSnapshot(entity);
        return snapshot == null ? 0 : snapshot.blessings;
    }

    /** Internal read-only snapshots. Effects must never mutate these stacks. */
    public static List<ItemStack> stacks(LivingEntity entity)
    {
        Snapshot snapshot = ownerSnapshot(entity);
        return snapshot == null ? List.of() : snapshot.stacks;
    }

    public static int enchantmentLevels(LivingEntity entity)
    {
        Snapshot snapshot = ownerSnapshot(entity);
        return snapshot == null ? 0 : snapshot.enchantments;
    }

    public static void tick(LivingEntity maid)
    {
        if (maid.level().isClientSide || maidType == null || maid.getType() != maidType) return;
        Snapshot snapshot = ownerSnapshot(maid);
        if (snapshot == null)
        {
            clear(maid);
            return;
        }
        MaidState state = MAIDS.computeIfAbsent(maid, key -> new MaidState());
        if (state.snapshot != null && !state.snapshot.owner.getUUID().equals(snapshot.owner.getUUID()))
        {
            LovePandoraEffects.clear(maid);
            BlessingBenefits.clear(maid);
        }
        if (state.snapshot != snapshot || maid.tickCount % 20 == maid.getId() % 20 && !attributesIntact(maid, state))
        {
            applyAttributes(maid, state, snapshot);
            state.snapshot = snapshot;
        }
        LovePandoraEffects.tick(maid);
    }

    private static boolean attributesIntact(LivingEntity maid, MaidState state)
    {
        for (var entry : state.applied.entrySet())
        {
            var attribute = maid.getAttribute(entry.getKey().attribute);
            if (attribute != null && !sameModifier(attribute.getModifier(entry.getKey().uuid), entry.getValue()))
                return false;
        }
        return true;
    }

    private static void applyAttributes(LivingEntity maid, MaidState state, Snapshot snapshot)
    {
        Map<ModifierKey, AttributeModifier> wanted = new HashMap<>();
        for (Bonus bonus : snapshot.bonuses)
            wanted.put(new ModifierKey(bonus.attribute, bonus.modifier.getId()), bonus.modifier);
        for (var entry : state.applied.entrySet())
        {
            AttributeModifier next = wanted.get(entry.getKey());
            if (!sameModifier(entry.getValue(), next)) remove(maid, entry.getKey());
        }
        for (var entry : wanted.entrySet())
        {
            var attribute = maid.getAttribute(entry.getKey().attribute);
            if (attribute == null) continue;
            var current = attribute.getModifier(entry.getKey().uuid);
            if (sameModifier(current, entry.getValue())) continue;
            if (current != null) attribute.removeModifier(entry.getKey().uuid);
            attribute.addTransientModifier(entry.getValue());
        }
        state.applied.clear();
        state.applied.putAll(wanted);
        if (maid.getHealth() > maid.getMaxHealth()) maid.setHealth(maid.getMaxHealth());
    }

    private static boolean sameModifier(AttributeModifier first, AttributeModifier second)
    {
        return first != null && second != null && first.getOperation() == second.getOperation()
                && first.getAmount() == second.getAmount();
    }

    private static void remove(LivingEntity maid, ModifierKey key)
    {
        var attribute = maid.getAttribute(key.attribute);
        if (attribute != null) attribute.removeModifier(key.uuid);
    }

    public static void clear(LivingEntity maid)
    {
        MaidState state = MAIDS.remove(maid);
        if (state == null) return;
        for (ModifierKey key : state.applied.keySet()) remove(maid, key);
        LovePandoraEffects.clear(maid);
        if (maid.isAlive() && maid.getHealth() > maid.getMaxHealth()) maid.setHealth(maid.getMaxHealth());
    }

    public static void logout(ServerPlayer player)
    {
        OWNERS.remove(player.getUUID());
        DIRTY.remove(player.getUUID());
        LoveProgressEvents.stopWearing(player);
        EternalLoveEffects.clear(player);
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && (DIRTY.remove(player.getUUID()) || player.tickCount % 10 == player.getId() % 10)) refresh(player);
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) { tick(event.getEntity()); }
    private static void onCurioChange(CurioChangeEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player) DIRTY.add(player.getUUID());
    }
    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player) logout(player);
    }
    private static void onClone(PlayerEvent.Clone event)
    {
        if (event.getOriginal() instanceof ServerPlayer player) logout(player);
    }
    private static void onDeath(LivingDeathEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player) logout(player);
        // Maid death may still be canceled by a protection charm. The next living
        // tick (or leave-level event) clears a genuinely dead maid after that decision.
    }
    private static void onEntityLeave(EntityLeaveLevelEvent event)
    {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity entity)
            clear(entity);
    }
    private static void onServerStopped(ServerStoppedEvent event)
    {
        for (LivingEntity maid : List.copyOf(MAIDS.keySet())) clear(maid);
        OWNERS.clear();
        DIRTY.clear();
    }
}
