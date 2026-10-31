package com.blessingofpandora;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Positive effects only. Never invokes Curse of Pandora's penalty-bearing ticking tokens. */
@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public final class BlessingBenefits
{
    public static final UUID FREEDOM_SPEED = UUID.fromString("b1e55b1e-1600-4000-8000-000000000001");
    public static final UUID INFINITY_REACH = UUID.fromString("b1e55b1e-1600-4000-8000-000000000002");
    public static final UUID SPIRIT_HEALTH = UUID.fromString("b1e55b1e-1600-4000-8000-000000000003");
    public static final UUID STRENGTH_ATTACK = UUID.fromString("b1e55b1e-1600-4000-8000-000000000004");
    public static final UUID STRENGTH_SPEED = UUID.fromString("b1e55b1e-1600-4000-8000-000000000005");
    // A transient zero modifier also synchronizes the filter to the movement-controlling client.
    public static final UUID IGNORE_SLOWNESS = UUID.fromString("b1e55b1e-1600-4000-8000-000000000006");
    public static final String SPIRIT_FED_TICKS = "blessingofpandora:spirit_fed_ticks";
    private static final ResourceLocation REALITY = ResourceLocation.parse("curseofpandora:reality_index");
    private static final TagKey<DamageType> NO_SCALE = TagKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.parse("l2damagetracker:no_scale"));
    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();
    private static final BlessingDefinitions[] DEFINITIONS = BlessingDefinitions.values();

    private static final class State
    {
        final Map<LivingEntity, FearlessnessMarks> marks = new WeakHashMap<>();
        final Map<LivingEntity, RequiemCombo> combos = new WeakHashMap<>();
    }

    private BlessingBenefits() {}

    /** Called only outside Curios attribute/capability callbacks to avoid recursive inventory updates. */
    public static EnumSet<BlessingDefinitions> equipped(LivingEntity wearer)
    {
        int mask = equippedMask(wearer, false);
        EnumSet<BlessingDefinitions> active = EnumSet.noneOf(BlessingDefinitions.class);
        for (BlessingDefinitions definition : DEFINITIONS)
        {
            if (contains(mask, definition)) active.add(definition);
        }
        return active;
    }

    private static int equippedMask(LivingEntity wearer, boolean updateSlots)
    {
        if (!wearer.isAlive()) return 0;
        int shared = com.blessingofpandora.compat.love.LoveSharing.blessingMask(wearer);
        var inventory = CuriosApi.getCuriosInventory(wearer).orElse(null);
        if (inventory == null) return shared;
        // Seven fixed blessing kinds fit in one primitive; avoid a temporary set on each tick/hit.
        // Continue reading every slot: Pandora's inherited handlers can update during these reads.
        int active = shared;
        int[] slots = updateSlots ? BlessingSlots.newCounts() : null;
        for (var handler : inventory.getCurios().values())
        {
            var stacks = handler.getStacks();
            // Pandora exposes contained charms through its inherited Curios slots.
            for (int i = 0; i < stacks.getSlots(); i++)
            {
                ItemStack stack = stacks.getStackInSlot(i);
                BlessingDefinitions definition = BlessingNbt.getUsableBlessing(stack, wearer);
                if (definition != null)
                {
                    active |= 1 << definition.ordinal();
                    if (slots != null) BlessingSlots.count(slots, definition);
                }
            }
        }
        if (slots != null) BlessingSlots.apply(inventory, slots);
        return active;
    }

    private static boolean contains(int mask, BlessingDefinitions definition)
    {
        return (mask & 1 << definition.ordinal()) != 0;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        LivingEntity wearer = event.getEntity();
        if (!wearer.level().isClientSide) tick(wearer);
    }

    public static void tick(LivingEntity wearer)
    {
        int active = equippedMask(wearer, true);
        conditionalMultiplier(wearer, Attributes.ATTACK_SPEED, FREEDOM_SPEED,
                "freedom_attack_speed", contains(active, BlessingDefinitions.FREEDOM), 0.5, 1.0);
        conditionalMultiplier(wearer, ForgeMod.ENTITY_REACH.get(), INFINITY_REACH,
                "infinity_attack_reach", contains(active, BlessingDefinitions.INFINITY), 2.0, 0.5);

        State state = STATES.get(wearer);
        int food = wearer instanceof Player player ? player.getFoodData().getFoodLevel() : -1;
        int fedTicks = BlessingBenefitMath.updateSpiritTimer(wearer.getPersistentData(),
                contains(active, BlessingDefinitions.SPIRIT) && food >= 14);
        if (state != null)
        {
            if (!contains(active, BlessingDefinitions.FEARLESSNESS)) state.marks.clear();
            else state.marks.entrySet().removeIf(entry -> !entry.getKey().isAlive()
                    || entry.getKey().level() != wearer.level()
                    || entry.getValue().empty(wearer.level().getGameTime()));
            if (!contains(active, BlessingDefinitions.REQUIEM)) state.combos.clear();
            else state.combos.entrySet().removeIf(entry -> !entry.getKey().isAlive()
                    || entry.getKey().level() != wearer.level()
                    || entry.getValue().layers(wearer.level().getGameTime()) == 0);
        }
        var attributes = wearer.getAttributes();
        boolean hadSpiritHealth = attributes.hasModifier(Attributes.MAX_HEALTH, SPIRIT_HEALTH);
        AttributeInstance health = fedTicks >= 2400 || hadSpiritHealth
                ? wearer.getAttribute(Attributes.MAX_HEALTH) : null;
        setModifier(health, SPIRIT_HEALTH, "spirit_max_health",
                AttributeModifier.Operation.MULTIPLY_TOTAL, 1.0, fedTicks >= 2400);

        boolean strength = contains(active, BlessingDefinitions.STRENGTH);
        double foodBonus = 0;
        if (strength)
        {
            Attribute reality = ForgeRegistries.ATTRIBUTES.getValue(REALITY);
            double realityValue = reality != null && attributes.hasAttribute(reality)
                    ? wearer.getAttributeValue(reality) : 0;
            foodBonus = BlessingBenefitMath.foodBonus(food, realityValue);
        }
        AttributeInstance attack = neededAttribute(wearer, Attributes.ATTACK_DAMAGE,
                STRENGTH_ATTACK, foodBonus > 0);
        setModifier(attack, STRENGTH_ATTACK, "strength_attack_damage",
                AttributeModifier.Operation.MULTIPLY_BASE, foodBonus, foodBonus > 0);
        boolean ignoreSlowness = strength && food >= 20;
        AttributeInstance movement = foodBonus > 0 || ignoreSlowness
                || attributes.hasModifier(Attributes.MOVEMENT_SPEED, STRENGTH_SPEED)
                || attributes.hasModifier(Attributes.MOVEMENT_SPEED, IGNORE_SLOWNESS)
                ? wearer.getAttribute(Attributes.MOVEMENT_SPEED) : null;
        setModifier(movement, STRENGTH_SPEED, "strength_movement_speed",
                AttributeModifier.Operation.MULTIPLY_BASE, foodBonus, foodBonus > 0);
        setModifier(movement, IGNORE_SLOWNESS, "strength_ignore_slowness",
                AttributeModifier.Operation.ADDITION, 0, ignoreSlowness);
        if (hadSpiritHealth && fedTicks < 2400 && wearer.getHealth() > wearer.getMaxHealth())
            wearer.setHealth(wearer.getMaxHealth());
        if (state != null && state.marks.isEmpty() && state.combos.isEmpty()) STATES.remove(wearer);
    }

    private static AttributeInstance neededAttribute(LivingEntity wearer, Attribute attribute, UUID id, boolean active)
    {
        // Looking for our residual UUID does not create an otherwise unused AttributeInstance.
        return active || wearer.getAttributes().hasModifier(attribute, id) ? wearer.getAttribute(attribute) : null;
    }

    private static void conditionalMultiplier(LivingEntity wearer, Attribute type, UUID id, String name,
                                              boolean equipped, double threshold, double bonus)
    {
        AttributeInstance attribute = neededAttribute(wearer, type, id, equipped);
        boolean active = equipped && attribute != null
                && BlessingBenefitMath.valueWithout(attribute, id, false) <= threshold;
        setModifier(attribute, id, name, AttributeModifier.Operation.MULTIPLY_TOTAL, bonus, active);
    }

    static void setModifier(AttributeInstance attribute, UUID id, String name,
                            AttributeModifier.Operation operation, double amount, boolean active)
    {
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(id);
        if (active && old != null && old.getAmount() == amount && old.getOperation() == operation) return;
        if (old != null) attribute.removeModifier(id);
        if (active)
        {
            AttributeModifier modifier = new AttributeModifier(id, "blessingofpandora:" + name, amount, operation);
            // Preserve the earned health cap while the game loads saved Health, before the first tick.
            // Only our own UUID is persisted; tick revalidates equipment, owner and food before retaining it.
            if (id.equals(SPIRIT_HEALTH)) attribute.addPermanentModifier(modifier);
            else attribute.addTransientModifier(modifier);
        }
    }

    /** Runs after the Forge hurt event but before armor/absorption, like the original positive effect. */
    public static float modifyDamage(LivingEntity target, DamageSource source, float amount)
    {
        if (target.level().isClientSide || !Float.isFinite(amount) || amount <= 0 || source.is(NO_SCALE)
                || !(source.getEntity() instanceof LivingEntity wearer) || wearer == target)
            return amount;
        int active = equippedMask(wearer, false);
        State state = STATES.get(wearer);
        long now = wearer.level().getGameTime();
        double result = amount;
        if (contains(active, BlessingDefinitions.FEARLESSNESS))
        {
            if (state == null) state = STATES.computeIfAbsent(wearer, key -> new State());
            FearlessnessMarks marks = state.marks.computeIfAbsent(target, key -> new FearlessnessMarks());
            result *= 1.0 + marks.mature(now) * 0.2;
            marks.hit(now);
        }
        else if (state != null) state.marks.clear();
        if (state != null)
        {
            if (contains(active, BlessingDefinitions.REQUIEM))
            {
                RequiemCombo combo = state.combos.get(target);
                if (combo != null) result *= 1.0 + combo.layers(now) * 0.1;
            }
            else state.combos.clear();
        }
        return (float) Math.min(Float.MAX_VALUE, result);
    }

    /** At vanilla's health-damage commit, after armor, absorption and Forge/L2 damage listeners. */
    public static float finalizeDamage(LivingEntity target, DamageSource source, float amount)
    {
        if (target.level().isClientSide || !Float.isFinite(amount) || amount <= 0) return amount;
        LivingEntity attacker = source.getEntity() instanceof LivingEntity entity && entity != target ? entity : null;
        boolean wearingRequiem = attacker != null && contains(equippedMask(attacker, false), BlessingDefinitions.REQUIEM);
        boolean requiem = wearingRequiem && !source.is(NO_SCALE);
        int protection = contains(equippedMask(target, false), BlessingDefinitions.LIBERATION)
                ? BlessingEnchantmentLevels.total(target) : 0;
        float result = BlessingBenefitMath.finalDamage(amount, requiem ? target.getMaxHealth() : 0, requiem, protection);
        if (requiem && result > 0)
        {
            STATES.computeIfAbsent(attacker, key -> new State()).combos
                    .computeIfAbsent(target, key -> new RequiemCombo()).hit(attacker.level().getGameTime());
        }
        else if (attacker != null && !wearingRequiem)
        {
            State state = STATES.get(attacker);
            if (state != null) state.combos.clear();
        }
        com.blessingofpandora.compat.love.LoveProgressEvents.recordDamage(target, source, result);
        return result;
    }

    public static void clear(LivingEntity wearer)
    {
        STATES.remove(wearer);
        CompoundTag data = wearer.getPersistentData();
        if (data.contains(SPIRIT_FED_TICKS)) data.remove(SPIRIT_FED_TICKS);
        AttributeInstance health = neededAttribute(wearer, Attributes.MAX_HEALTH, SPIRIT_HEALTH, false);
        boolean hadSpiritHealth = health != null && health.getModifier(SPIRIT_HEALTH) != null;
        remove(wearer, Attributes.ATTACK_SPEED, FREEDOM_SPEED);
        remove(wearer, ForgeMod.ENTITY_REACH.get(), INFINITY_REACH);
        if (health != null) health.removeModifier(SPIRIT_HEALTH);
        remove(wearer, Attributes.ATTACK_DAMAGE, STRENGTH_ATTACK);
        remove(wearer, Attributes.MOVEMENT_SPEED, STRENGTH_SPEED);
        remove(wearer, Attributes.MOVEMENT_SPEED, IGNORE_SLOWNESS);
        if (hadSpiritHealth && wearer.isAlive() && wearer.getHealth() > wearer.getMaxHealth())
            wearer.setHealth(wearer.getMaxHealth());
    }

    private static void remove(LivingEntity wearer, Attribute attribute, UUID id)
    {
        AttributeInstance instance = neededAttribute(wearer, attribute, id, false);
        if (instance != null) instance.removeModifier(id);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) public static void onDeath(LivingDeathEvent event)
    {
        if (!event.getEntity().level().isClientSide) clear(event.getEntity());
    }
    @SubscribeEvent public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) { STATES.remove(event.getEntity()); }
    @SubscribeEvent public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) { STATES.remove(event.getEntity()); }
    @SubscribeEvent public static void onServerStopped(ServerStoppedEvent event) { STATES.clear(); }
}
