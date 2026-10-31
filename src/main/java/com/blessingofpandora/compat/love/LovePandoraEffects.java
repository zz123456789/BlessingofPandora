package com.blessingofpandora.compat.love;

import dev.xkmc.curseofpandora.init.data.CoPConfig;
import dev.xkmc.curseofpandora.init.data.CoPDamageTypeGen;
import dev.xkmc.curseofpandora.init.data.CoPTagGen;
import dev.xkmc.curseofpandora.init.registrate.CoPAttrs;
import dev.xkmc.curseofpandora.init.registrate.CoPEffects;
import dev.xkmc.l2complements.content.feature.EntityFeature;
import dev.xkmc.l2complements.init.data.DamageTypeGen;
import dev.xkmc.l2complements.init.registrate.LCEffects;
import dev.xkmc.l2damagetracker.contents.attack.*;
import dev.xkmc.l2damagetracker.contents.damage.DefaultDamageState;
import dev.xkmc.l2damagetracker.init.L2DamageTracker;
import dev.xkmc.l2damagetracker.init.data.L2DamageTypes;
import dev.xkmc.l2library.base.effects.EffectUtil;
import dev.xkmc.l2library.base.effects.EffectUtil.AddReason;
import dev.xkmc.l2library.base.effects.EffectBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.lang.ref.WeakReference;
import java.util.*;

import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.*;

/**
 * LivingEntity implementations of the audited CoP effects. Never invokes Player tokens,
 * modifies an owner's equipment, creates a fake player, or discovers methods reflectively.
 * Inventory membership is read exclusively from LoveSharing's immutable owner snapshot.
 */
public final class LovePandoraEffects implements AttackListener
{
    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();
    private static final Map<LivingEntity, long[]> COOLDOWNS = new WeakHashMap<>();
    private static final Map<String, UUID> IDS = new HashMap<>();
    private static final Set<UUID> WEAPON_SPEED = Set.of(UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3"));
    private static boolean registered;

    private static final class Applied
    {
        final Attribute attribute;
        final UUID id;
        int generation;
        Applied(Attribute attribute, UUID id) { this.attribute = attribute; this.id = id; }
    }

    private static final class Marks
    {
        final ArrayList<Long> brave = new ArrayList<>();
        final Set<Long> fear = new HashSet<>();
        long terrorUntil = Long.MIN_VALUE;
    }

    private record Marker(MobEffectInstance instance, int amplifier, long expires) {}

    private static final class State
    {
        final Map<String, Applied> modifiers = new HashMap<>();
        final Map<UUID, Marks> marks = new HashMap<>();
        final ArrayList<Long> awakening = new ArrayList<>();
        final Map<Vec3, Float> shadowHits = new HashMap<>();
        final Map<MobEffect, Marker> markers = new HashMap<>();
        final ArrayList<WeakReference<Mob>> peons = new ArrayList<>();
        final Set<UUID> weaponReach = new HashSet<>();
        int generation;
        long instinctReady, eyeReady, crystalReady, punishmentReady;
        long angelicReady, abyssReady, walkReady, shadowReady, shadowExecute;
        long peonsRefresh;
        double heal;
        boolean equipmentDirty = true;
        final double[] enchantWeights = new double[EquipmentSlot.values().length];
        final int[] enchantValues = new int[EquipmentSlot.values().length];
        final int[] enchantKinds = new int[EquipmentSlot.values().length];
        int armorCurses, cursedArmor;
    }

    private static State state(LivingEntity e)
    {
        State existing = STATES.get(e);
        if (existing != null) return existing;
        State created = new State();
        long[] cooldowns = COOLDOWNS.remove(e);
        if (cooldowns != null)
        {
            created.angelicReady = cooldowns[0];
            created.abyssReady = cooldowns[1];
            created.walkReady = cooldowns[2];
            created.crystalReady = cooldowns[3];
        }
        STATES.put(e, created);
        return created;
    }
    private static long now(LivingEntity e) { return e.level().getGameTime(); }
    private static boolean has(LivingEntity e, String item) { return LoveSharing.has(e, item); }
    private static double reality(LivingEntity e) { return value(e, CoPAttrs.REALITY.get()); }
    private static double value(LivingEntity e, Attribute attribute)
    {
        AttributeInstance ins = e.getAttribute(attribute);
        return ins == null ? 0 : ins.getValue();
    }
    private static boolean ready(LivingEntity e, String item, int required)
    {
        return has(e, item) && reality(e) >= required;
    }
    private static boolean angelic(LivingEntity e, String item, int required)
    {
        return ready(e, item, required) && (e.level().canSeeSky(e.blockPosition())
                || ready(e, "angelic_punishment", CoPConfig.COMMON.angelic.angelicPunishmentRealityIndex.get()));
    }

    public static void register()
    {
        if (registered) return;
        registered = true;
        AttackEventHandler.register(5201, new LovePandoraEffects());
        MinecraftForge.EVENT_BUS.register(LovePandoraEffects.class);
        EntityFeature.STABLE_BODY.add(e -> has(e, "orb_of_stability"));
        EntityFeature.PROJECTILE_REJECT.add(e -> has(e, "orb_of_projectile_rejection"));
        EntityFeature.EXPLOSION_REJECT.add(e -> has(e, "orb_of_explosion_rejection"));
        EntityFeature.FIRE_REJECT.add(e -> has(e, "orb_of_fire_rejection"));
        EntityFeature.ENVIRONMENTAL_REJECT.add(e -> has(e, "orb_of_nature"));
        EntityFeature.MAGIC_REJECT.add(e -> has(e, "orb_of_magic_rejection"));
        EntityFeature.OWNER_PROTECTION.add(e -> has(e, "orb_of_master"));
        EntityFeature.SNOW_WALKER.add(e -> has(e, "bless_of_snow_walker"));
        EntityFeature.LAVA_WALKER.add(e -> has(e, "bless_of_lava_walker"));
    }

    /** Called once per active maid server tick, after the static snapshot modifiers are applied. */
    public static void tick(LivingEntity e)
    {
        if (e.level().isClientSide || !LoveSharing.active(e) || !e.isAlive()) return;
        State s = state(e);
        s.generation++;
        long time = now(e);
        if (has(e, "orb_of_fire_rejection")) e.clearFire();
        if (s.generation == 1 || time % 20 == Math.floorMod(e.getId(), 20)) refreshPotions(e);
        if (s.equipmentDirty) readEquipment(e, s);
        dynamicAttributes(e, s);
        if (has(e, "curse_of_spell") && time % 20 == Math.floorMod(e.getId(), 20) && spellPenalty(e, s) > 0)
            e.hurt(source(e, CoPDamageTypeGen.SPELL_CURSE, null), 1);
        if (angelic(e, "angelic_wing", CoPConfig.COMMON.angelic.angelicWingRealityIndex.get()) && e.isFallFlying())
        {
            Vec3 motion = e.getDeltaMovement();
            double length = motion.length();
            if (length > 0.2 && length < CoPConfig.COMMON.angelic.angelicWingMaxSpeed.get())
                e.setDeltaMovement(motion.scale(1 + CoPConfig.COMMON.angelic.angelicWingBoost.get() / length));
        }
        if (time % 20 == Math.floorMod(e.getId(), 20)
                && ready(e, "abyssal_watcher", CoPConfig.COMMON.abyssal.abyssalWatcherRealityIndex.get()))
            e.heal((float)(depth(e) * CoPConfig.COMMON.abyssal.abyssalWatcherRegen.get() * e.getMaxHealth()));
        if (s.heal > 0 && Double.isFinite(s.heal)) e.heal((float)Math.min(Float.MAX_VALUE, s.heal));
        s.heal = 0;
        if (!has(e, "shadow_consolidation")) s.shadowHits.clear();
        if (!s.shadowHits.isEmpty() && time >= s.shadowExecute) explodeShadow(e, s);
        if (!has(e, "curse_of_tension"))
            for (Marks m : s.marks.values()) { m.brave.clear(); m.terrorUntil = Long.MIN_VALUE; }
        if (!has(e, "curse_of_prudence")) for (Marks m : s.marks.values()) m.fear.clear();
        // Timer state is bounded by live combat rather than scanning surrounding entities every tick.
        if (time % 20 == Math.floorMod(e.getId(), 20)) pruneMarks(s, time);
        Iterator<Applied> iterator = s.modifiers.values().iterator();
        while (iterator.hasNext())
        {
            Applied applied = iterator.next();
            if (applied.generation != s.generation)
            {
                AttributeInstance ins = e.getAttribute(applied.attribute);
                if (ins != null) ins.removeModifier(applied.id);
                iterator.remove();
            }
        }
        if (e.getHealth() > e.getMaxHealth()) e.setHealth(e.getMaxHealth());
    }

    public static void clear(LivingEntity e)
    {
        State s = STATES.remove(e);
        if (s == null) return;
        // Upstream item cooldowns survive unequipping; do not allow toggling Love to reset a totem.
        if (e.isAlive() && Math.max(Math.max(s.angelicReady, s.abyssReady), Math.max(s.walkReady, s.crystalReady)) > now(e))
            COOLDOWNS.put(e, new long[] {s.angelicReady, s.abyssReady, s.walkReady, s.crystalReady});
        for (Applied applied : s.modifiers.values())
        {
            AttributeInstance ins = e.getAttribute(applied.attribute);
            if (ins != null) ins.removeModifier(applied.id);
        }
        // These are the two marker effects created by this adapter, not arbitrary owner potions.
        removeMarker(e, s, CoPEffects.AWAKENING.get());
        removeMarker(e, s, CoPEffects.SPIRIT_WALK.get());
        if (e.isAlive() && e.getHealth() > e.getMaxHealth()) e.setHealth(e.getMaxHealth());
    }

    private static void apply(LivingEntity e, State s, String name, Attribute attribute,
                              AttributeModifier.Operation operation, double amount)
    {
        if (!Double.isFinite(amount)) return;
        AttributeInstance ins = e.getAttribute(attribute);
        if (ins == null) return;
        Applied applied = s.modifiers.get(name);
        if (applied == null)
        {
            UUID id = IDS.computeIfAbsent(name, key -> UUID.nameUUIDFromBytes(
                    ("blessingofpandora:love/" + key).getBytes(StandardCharsets.UTF_8)));
            applied = new Applied(attribute, id);
            s.modifiers.put(name, applied);
        }
        applied.generation = s.generation;
        AttributeModifier old = ins.getModifier(applied.id);
        if (old != null && old.getAmount() == amount && old.getOperation() == operation) return;
        if (old != null) ins.removeModifier(applied.id);
        ins.addTransientModifier(new AttributeModifier(applied.id, "blessingofpandora:love/" + name, amount, operation));
    }

    private static void refreshPotions(LivingEntity e)
    {
        potion(e, "mini_beacon_speed", MobEffects.MOVEMENT_SPEED, 40, 1);
        potion(e, "mini_beacon_haste", MobEffects.DIG_SPEED, 40, 1);
        potion(e, "mini_beacon_strength", MobEffects.DAMAGE_BOOST, 40, 1);
        potion(e, "mini_beacon_jump", MobEffects.JUMP, 40, 1);
        potion(e, "mini_beacon_resistance", MobEffects.DAMAGE_RESISTANCE, 40, 1);
        potion(e, "mini_beacon_regen", MobEffects.REGENERATION, 60, 1);
        potion(e, "charm_of_night_vision", MobEffects.NIGHT_VISION, 440, 0);
        potion(e, "orb_of_fire_rejection", MobEffects.FIRE_RESISTANCE, 40, 1);
        if (ready(e, "evil_spirit_walk", CoPConfig.COMMON.evil.evilSpiritWalkRealityIndex.get()) && now(e) >= state(e).walkReady)
            refreshMarker(e, state(e), CoPEffects.SPIRIT_WALK.get(), 40, 0);
    }

    private static void potion(LivingEntity e, String item, MobEffect effect, int duration, int amplifier)
    {
        if (has(e, item)) refresh(e, effect, duration, amplifier);
    }
    private static void refresh(LivingEntity e, MobEffect effect, int duration, int amplifier)
    {
        MobEffectInstance old = e.getEffect(effect);
        if (old == null || old.getAmplifier() < amplifier || old.getDuration() < duration)
            e.addEffect(new MobEffectInstance(effect, duration, amplifier, true, true));
    }

    /** Do not claim or delete a marker already supplied by another item or command. */
    private static void refreshMarker(LivingEntity e, State s, MobEffect effect, int duration, int amplifier)
    {
        MobEffectInstance current = e.getEffect(effect);
        Marker owned = s.markers.get(effect);
        if (current != null && (owned == null || current != owned.instance()
                || current.isInfiniteDuration()
                || current.getAmplifier() != owned.amplifier()
                || current.getDuration() > owned.expires() - now(e) + 1))
        {
            s.markers.remove(effect);
            return;
        }
        if (current != null && current.getAmplifier() == amplifier && current.getDuration() >= duration) return;
        if (current != null && current.getAmplifier() != amplifier) e.removeEffect(effect);
        e.addEffect(new MobEffectInstance(effect, duration, amplifier, true, true));
        MobEffectInstance installed = e.getEffect(effect);
        if (installed != null && installed.getAmplifier() == amplifier)
            s.markers.put(effect, new Marker(installed, amplifier, now(e) + installed.getDuration()));
    }

    private static void removeMarker(LivingEntity e, State s, MobEffect effect)
    {
        Marker owned = s.markers.remove(effect);
        MobEffectInstance current = e.getEffect(effect);
        if (owned != null && current == owned.instance() && !current.isInfiniteDuration() && current.getAmplifier() == owned.amplifier()
                && current.getDuration() <= owned.expires() - now(e) + 1)
            e.removeEffect(effect);
    }

    private static void readEquipment(LivingEntity e, State s)
    {
        s.armorCurses = 0;
        s.cursedArmor = 0;
        s.weaponReach.clear();
        var weaponAttributes = e.getMainHandItem().getAttributeModifiers(EquipmentSlot.MAINHAND);
        for (AttributeModifier modifier : weaponAttributes.get(ForgeMod.ENTITY_REACH.get())) s.weaponReach.add(modifier.getId());
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            ItemStack stack = e.getItemBySlot(slot);
            double weight = 0;
            int curses = 0;
            Map<Enchantment, Integer> enchantments = stack.getAllEnchantments();
            for (var enchantment : enchantments.entrySet())
            {
                int level = enchantment.getValue();
                if (level > 0) weight += Math.pow(2, Math.min(1023, level - 1));
                if (enchantment.getKey().isCurse()) curses++;
            }
            int index = slot.ordinal();
            s.enchantWeights[index] = weight;
            s.enchantValues[index] = stack.isEmpty() ? 0 : stack.getEnchantmentValue();
            s.enchantKinds[index] = enchantments.size();
            if (slot.getType() == EquipmentSlot.Type.ARMOR)
            {
                s.armorCurses += curses;
                if (curses > 0) s.cursedArmor++;
            }
        }
        s.equipmentDirty = false;
    }

    private static double spellPenalty(LivingEntity e, State s)
    {
        if (s.equipmentDirty) readEquipment(e, s);
        double tolerance = Math.max(1, value(e, CoPAttrs.SPELL.get()));
        double total = 0;
        for (int i = 0; i < s.enchantWeights.length; i++)
            total += Math.max(0, s.enchantWeights[i] / (tolerance + s.enchantValues[i]) / tolerance
                    * CoPConfig.COMMON.curse.curseOfSpellLoadFactor.get() - 1);
        return Math.min(Float.MAX_VALUE, total);
    }

    private static int depth(LivingEntity e)
    {
        if (e.getY() > 0) return 0;
        int step = ready(e, "abyssal_will", CoPConfig.COMMON.abyssal.abyssalWillRealityIndex.get())
                ? CoPConfig.COMMON.abyssal.abyssalWillDepthStep.get() : CoPConfig.COMMON.abyssal.abyssalDepthStep.get();
        return (int)-Math.floor(e.getY() / Math.max(1, step));
    }

    private static DamageSource source(LivingEntity e, ResourceKey<DamageType> type, LivingEntity attacker)
    {
        return new DamageSource(CoPDamageTypeGen.forKey(e.level(), type), attacker);
    }

    private static boolean ordinary(DamageSource source)
    {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.is(DamageTypeTags.BYPASSES_EFFECTS);
    }

    private static void dynamicAttributes(LivingEntity e, State s)
    {
        var c = CoPConfig.COMMON;
        if (has(e, "curse_of_inertia")) limit(e, s, "inertia", Attributes.ATTACK_SPEED,
                c.curse.curseOfInertiaCap.get(), c.curse.curseOfInertiaBase.get(), c.curse.curseOfInertiaBonus.get(), true);
        if (has(e, "curse_of_proximity")) limit(e, s, "proximity", ForgeMod.ENTITY_REACH.get(),
                (int)c.curse.curseOfProximityCap.get().doubleValue(), c.curse.curseOfProximityBase.get(), c.curse.curseOfProximityBonus.get(), true);
        if (has(e, "curse_of_flesh")) limit(e, s, "flesh", Attributes.MAX_HEALTH,
                Double.MAX_VALUE, -1, 0, false);
        // Food-dependent parts of Flesh/Metabolism deliberately do not access Player FoodData.
        if (angelic(e, "angelic_bless", c.angelic.angelicBlessRealityIndex.get()))
        {
            apply(e, s, "angelic_bless_absorb", L2DamageTracker.ABSORB.get(), ADDITION, c.angelic.angelicBlessAbsorption.get());
            apply(e, s, "angelic_bless_reduction", L2DamageTracker.REDUCTION.get(), MULTIPLY_TOTAL, -c.angelic.angelicBlessDamageReduction.get());
        }
        if (angelic(e, "angelic_descent", c.angelic.angelicDescentRealityIndex.get()))
            apply(e, s, "angelic_descent", Attributes.ATTACK_DAMAGE, MULTIPLY_BASE, c.angelic.angelicDescentMeleeBonus.get());
        int depth = depth(e);
        if (depth > 0 && ready(e, "abyssal_treasure", c.abyssal.abyssalTreasureRealityIndex.get()))
            apply(e, s, "abyssal_treasure", Attributes.LUCK, ADDITION, depth);
        if (depth > 0 && ready(e, "abyssal_shell", c.abyssal.abyssalShellRealityIndex.get()))
        {
            apply(e, s, "abyssal_shell_armor", Attributes.ARMOR, MULTIPLY_BASE, depth * c.abyssal.abyssalShellBonus.get());
            apply(e, s, "abyssal_shell_toughness", Attributes.ARMOR_TOUGHNESS, MULTIPLY_BASE, depth * c.abyssal.abyssalShellBonus.get());
        }
        if (ready(e, "barbaric_instinct", c.barbaric.barbaricInstinctRealityIndex.get())) barbaric(e, s, "instinct");
        if (ready(e, "barbaric_wrath", c.barbaric.barbaricWrathRealityIndex.get()))
        {
            barbaric(e, s, "wrath");
            apply(e, s, "barbaric_wrath_crit", L2DamageTracker.CRIT_DMG.get(), ADDITION, c.barbaric.barbaricWrathCritBonus.get());
            apply(e, s, "barbaric_wrath_toughness", Attributes.ARMOR_TOUGHNESS, ADDITION, c.barbaric.barbaricWrathToughBonus.get());
        }
        if (ready(e, "barbaric_roar", c.barbaric.barbaricRoarRealityIndex.get()))
        {
            barbaric(e, s, "roar");
            apply(e, s, "barbaric_roar_attack", Attributes.ATTACK_DAMAGE, MULTIPLY_BASE, c.barbaric.barbaricRoarAttack.get());
            apply(e, s, "barbaric_roar_reduction", L2DamageTracker.REDUCTION.get(), MULTIPLY_TOTAL, -c.barbaric.barbaricRoarReduction.get());
        }
        if (ready(e, "barbaric_blade", c.barbaric.barbaricBladeRealityIndex.get()))
        {
            barbaric(e, s, "blade");
            apply(e, s, "barbaric_blade_attack", Attributes.ATTACK_DAMAGE, MULTIPLY_BASE, c.barbaric.barbaricBladeAttack.get());
            apply(e, s, "barbaric_blade_projectile", L2DamageTracker.BOW_STRENGTH.get(), ADDITION, c.barbaric.barbaricBladeProjectile.get());
        }
        if (ready(e, "primal_force", c.barbaric.primalForceRealityIndex.get()))
        {
            barbaric(e, s, "primal_force");
            apply(e, s, "primal_force_armor", Attributes.ARMOR, ADDITION, e.getMaxHealth() * c.barbaric.primalForceSelfArmor.get());
        }
        if (ready(e, "evil_spirit_awakening", c.evil.evilSpiritAwakeningRealityIndex.get()))
        {
            long threshold = now(e) - c.evil.evilSpiritAwakeningDuration.get();
            s.awakening.removeIf(time -> time < threshold);
            // Upstream caps the retained stack history at five, then caps its visible level.
            while (s.awakening.size() > 5) s.awakening.remove(0);
            if (!s.awakening.isEmpty())
            {
                int size = s.awakening.size();
                apply(e, s, "awakening_magic", L2DamageTracker.MAGIC_FACTOR.get(), ADDITION, size * c.evil.evilSpiritAwakeningMagicBonus.get());
                apply(e, s, "awakening_reduction", L2DamageTracker.REDUCTION.get(), MULTIPLY_TOTAL, -size * c.evil.evilSpiritAwakeningReduction.get());
                int duration = (int)Math.max(1, s.awakening.get(0) - threshold);
                refreshMarker(e, s, CoPEffects.AWAKENING.get(), duration, Math.max(0, Math.min(size, c.evil.evilSpiritAwakeningMaxLevel.get()) - 1));
            }
            else removeMarker(e, s, CoPEffects.AWAKENING.get());
        }
        else
        {
            s.awakening.clear();
            removeMarker(e, s, CoPEffects.AWAKENING.get());
        }
        if (ready(e, "evil_spirit_walk", c.evil.evilSpiritWalkRealityIndex.get()) && now(e) >= s.walkReady)
        {
            apply(e, s, "spirit_walk_magic", L2DamageTracker.MAGIC_FACTOR.get(), ADDITION, c.evil.evilSpiritWalkMagicBonus.get());
            apply(e, s, "spirit_walk_attack", Attributes.ATTACK_DAMAGE, MULTIPLY_BASE, c.evil.evilSpiritWalkAtkBonus.get());
        }
        else removeMarker(e, s, CoPEffects.SPIRIT_WALK.get());
        if (e.isSprinting() && ready(e, "wind_thrust", c.elemental.windThrustRealityIndex.get()))
        {
            apply(e, s, "wind_thrust_speed", Attributes.MOVEMENT_SPEED, MULTIPLY_BASE, c.elemental.windThrustSpeed.get());
            apply(e, s, "wind_thrust_attack", Attributes.ATTACK_DAMAGE, MULTIPLY_BASE, c.elemental.windThrustDamage.get());
        }
        if (ready(e, "earth_crush", c.elemental.earthCrushRealityIndex.get())
                && value(e, Attributes.ATTACK_SPEED) <= c.elemental.earthCrushThreshold.get())
            apply(e, s, "earth_crush", L2DamageTracker.CRIT_DMG.get(), ADDITION, c.elemental.earthCrushBonus.get());
        if (ready(e, "flaming_explosion", c.elemental.flamingExplosionRealityIndex.get()))
            apply(e, s, "flaming_explosion", L2DamageTracker.EXPLOSION_FACTOR.get(), ADDITION, c.elemental.flamingExplosionBonus.get());
        if (e.isInWaterOrBubble() && ready(e, "waving_spell", c.elemental.wavingSpellRealityIndex.get()))
            apply(e, s, "waving_spell", L2DamageTracker.MAGIC_FACTOR.get(), ADDITION, c.elemental.wavingSpellBonus.get());
        if (ready(e, "curse_redirection", c.elemental.curseRedirectionRealityIndex.get()))
        {
            apply(e, s, "curse_redirection_magic", L2DamageTracker.MAGIC_FACTOR.get(), ADDITION, s.armorCurses * c.elemental.curseRedirectionBonus.get());
            apply(e, s, "curse_redirection_spell", CoPAttrs.SPELL.get(), ADDITION, s.cursedArmor);
        }
        if (has(e, "spell_singularity"))
        {
            int r = (int)reality(e);
            boolean pass = true;
            for (int kinds : s.enchantKinds) if (kinds > r) { pass = false; break; }
            if (pass)
            {
                apply(e, s, "spell_singularity_spell", CoPAttrs.SPELL.get(), ADDITION, r * c.compat.spellSingularitySpellBonusPerReality.get());
                apply(e, s, "spell_singularity_magic", L2DamageTracker.MAGIC_FACTOR.get(), ADDITION, r * c.compat.spellSingularityMagicDamageBonusPerReality.get());
            }
        }
    }

    private static void barbaric(LivingEntity e, State s, String part)
    {
        apply(e, s, "barbaric_" + part + "_magic", L2DamageTracker.MAGIC_FACTOR.get(), ADDITION, -CoPConfig.COMMON.barbaric.magicDamageDebuff.get());
    }

    /** Cancel only the positive modifiers targeted by the upstream curse; never remove another mod's UUID. */
    private static void limit(LivingEntity e, State s, String part, Attribute type,
                              double cap, double threshold, double bonus, boolean preserveWeapon)
    {
        AttributeInstance ins = e.getAttribute(type);
        if (ins == null) return;
        String prefix = "limit_" + part;
        Set<UUID> weapon = !preserveWeapon ? Collections.emptySet()
                : type == Attributes.ATTACK_SPEED ? WEAPON_SPEED : s.weaponReach;
        double add = 0, base = 0, total = 1;
        double remainingAdd = 0, remainingBase = 0, remainingTotal = 1;
        for (AttributeModifier modifier : ins.getModifiers())
        {
            if (modifier.getName().startsWith("blessingofpandora:love/" + prefix)) continue;
            double v = modifier.getAmount();
            switch (modifier.getOperation())
            {
                case ADDITION -> {
                    if (v > 0 && !weapon.contains(modifier.getId())) add += v;
                    else remainingAdd += v;
                }
                case MULTIPLY_BASE -> { if (v > 0) base += v; else remainingBase += v; }
                case MULTIPLY_TOTAL -> { if (v > 0) total *= 1 + v; else remainingTotal *= 1 + v; }
            }
        }
        apply(e, s, prefix + "_add", type, ADDITION, -add);
        apply(e, s, prefix + "_base", type, MULTIPLY_BASE, -base);
        // Never clamp the positive-multiplied value before cancelling that multiplier: at
        // large values that would underestimate the baseline and let the curse cap be bypassed.
        double baseline = (ins.getBaseValue() + remainingAdd) * (1 + remainingBase) * remainingTotal;
        double multiplier = baseline > cap && baseline > 0 ? cap / baseline : baseline <= threshold ? 1 + bonus : 1;
        apply(e, s, prefix + "_total", type, MULTIPLY_TOTAL, multiplier / total - 1);
    }

    private static void pruneMarks(State s, long time)
    {
        int braveAge = CoPConfig.COMMON.curse.curseOfTensionTokenMatureTime.get()
                + CoPConfig.COMMON.curse.curseOfTensionTokenEffectiveTime.get();
        int fearAge = CoPConfig.COMMON.curse.curseOfPrudenceDuration.get();
        s.marks.values().removeIf(m -> {
            m.brave.removeIf(t -> time > t + braveAge);
            m.fear.removeIf(t -> time >= t + fearAge);
            return m.brave.isEmpty() && m.fear.isEmpty() && m.terrorUntil < time;
        });
    }

    @Override public void onCreateSource(CreateSourceEvent event)
    {
        LivingEntity e = event.getAttacker();
        if (e != null && LoveSharing.active(e) && event.getResult() != null
                && event.getResult().toRoot() == L2DamageTypes.MOB_ATTACK
                && ready(e, "abyssal_crown", CoPConfig.COMMON.abyssal.abyssalCrownRealityIndex.get())
                && depth(e) * CoPConfig.COMMON.abyssal.abyssalCrownChance.get() > e.getRandom().nextDouble())
            event.enable(DefaultDamageState.BYPASS_MAGIC);
    }

    @Override public void onAttack(AttackCache cache, ItemStack weapon)
    {
        var event = cache.getLivingAttackEvent();
        if (event == null || event.isCanceled()) return;
        LivingEntity target = cache.getAttackTarget();
        LivingEntity attacker = cache.getAttacker();
        DamageSource source = event.getSource();
        var c = CoPConfig.COMMON;
        if (LoveSharing.active(target))
        {
            if (angelic(target, "angelic_wing", c.angelic.angelicWingRealityIndex.get())
                    && (source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL))) event.setCanceled(true);
            if (ready(target, "hellfire_reformation", c.hell.hellfireReformationRealityIndex.get())
                    && source.is(DamageTypeGen.SOUL_FLAME)) event.setCanceled(true);
            if (ready(target, "abyssal_will", c.abyssal.abyssalWillRealityIndex.get())
                    && target.hasEffect(CoPEffects.ABYSSAL_PROTECTION.get()) && source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS))
                event.setCanceled(true);
            if (event.isCanceled()) return;
            if (attacker instanceof Mob mob && ready(target, "primal_force", c.barbaric.primalForceRealityIndex.get()))
            {
                // Same upstream effect: the attacking mob gains armor based on its maximum health.
                AttributeInstance armor = mob.getAttribute(Attributes.ARMOR);
                UUID id = dev.xkmc.l2library.util.math.MathHelper.getUUIDFromString("primal_force");
                double amount = mob.getMaxHealth() * c.barbaric.primalForceTargetArmor.get();
                if (armor != null)
                {
                    AttributeModifier old = armor.getModifier(id);
                    if (old == null || old.getAmount() != amount)
                    {
                        if (old != null) armor.removeModifier(id);
                        armor.addTransientModifier(new AttributeModifier(id, "primal_force_bonus", amount, ADDITION));
                    }
                }
            }
            if (ordinary(source) && attacker instanceof Mob mob && mob.getMobType() != MobType.UNDEAD
                    && ready(target, "cursed_soul_crystal", c.hell.cursedSoulCrystalRealityIndex.get()))
            {
                State s = state(target);
                if (now(target) >= s.crystalReady)
                {
                    double range = c.hell.cursedSoulCrystalRange.get();
                    List<Mob> candidates = target.level().getEntitiesOfClass(Mob.class,
                            target.getBoundingBox().inflate(range), candidate -> candidate.isAlive()
                                    && candidate.getMobType() == MobType.UNDEAD && candidate.distanceToSqr(target) <= range * range
                                    && candidate != target && !allied(target, candidate));
                    if (!candidates.isEmpty())
                    {
                        Mob redirect = candidates.get(target.getRandom().nextInt(candidates.size()));
                        s.crystalReady = now(target) + c.hell.cursedSoulCrystalTriggerCoolDown.get();
                        event.setCanceled(true);
                        // Deferred until the current L2 attack frame has finished, avoiding recursive cache corruption.
                        float damage = event.getAmount();
                        dev.xkmc.l2library.init.events.GeneralEventHandler.schedule(() -> {
                            if (redirect.isAlive()) redirect.hurt(source, damage);
                        });
                        return;
                    }
                }
            }
        }
        if (attacker == null || !LoveSharing.active(attacker) || source.is(L2DamageTypes.NO_SCALE)) return;
        State s = state(attacker);
        if (has(attacker, "curse_of_tension"))
        {
            Marks marks = s.marks.get(target.getUUID());
            if (marks != null && marks.terrorUntil >= now(attacker)) { event.setCanceled(true); return; }
        }
        if (ready(attacker, "barbaric_instinct", c.barbaric.barbaricInstinctRealityIndex.get())
                && now(attacker) >= s.instinctReady && source.getDirectEntity() == attacker
                && attacker.getMainHandItem().getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE))
        {
            attacker.heal((float)(c.barbaric.barbaricInstinctHeal.get() * attacker.getMaxHealth()));
            s.instinctReady = now(attacker) + c.barbaric.barbaricInstinctCoolDown.get();
        }
        if (ready(attacker, "crown_of_demon", c.hell.crownOfDemonRealityIndex.get()))
        {
            int range = c.hell.crownOfDemonRange.get();
            // Query only in combat and at most once per second, even with rapid multi-hit attacks.
            if (now(attacker) >= s.peonsRefresh)
            {
                s.peonsRefresh = now(attacker) + 20;
                s.peons.clear();
                for (Mob mob : attacker.level().getEntitiesOfClass(Mob.class, attacker.getBoundingBox().inflate(range), LovePandoraEffects::peon))
                    s.peons.add(new WeakReference<>(mob));
            }
            for (WeakReference<Mob> reference : s.peons)
            {
                Mob mob = reference.get();
                if (mob != null && mob.isAlive() && mob.getTarget() == null && mob != target && !allied(mob, target)) mob.setTarget(target);
            }
        }
    }

    @Override public void onHurt(AttackCache cache, ItemStack weapon)
    {
        var event = cache.getLivingHurtEvent();
        if (event == null || event.isCanceled() || event.getSource().is(L2DamageTypes.NO_SCALE)) return;
        LivingEntity e = cache.getAttacker();
        if (e == null || !LoveSharing.active(e)) return;
        LivingEntity target = cache.getAttackTarget();
        var c = CoPConfig.COMMON;
        State s = state(e);
        long time = now(e);
        if (has(e, "curse_of_tension") && e != target)
        {
            Marks marks = s.marks.computeIfAbsent(target.getUUID(), ignored -> new Marks());
            int count = 0;
            for (long hit : marks.brave)
                if (time > hit + c.curse.curseOfTensionTokenMatureTime.get()
                        && time <= hit + c.curse.curseOfTensionTokenMatureTime.get() + c.curse.curseOfTensionTokenEffectiveTime.get()) count++;
            if (count > 0) cache.addHurtModifier(DamageModifier.multTotal((float)(1 + Math.min(count,
                    c.curse.curseOfTensionMaxLevel.get()) * c.curse.curseOfTensionDamageBonus.get())));
            marks.brave.add(time);
        }
        if (angelic(e, "angelic_descent", c.angelic.angelicDescentRealityIndex.get()) && target.getMobType() == MobType.UNDEAD)
            cache.addHurtModifier(DamageModifier.multTotal((float)(1 + c.angelic.angelicDescentUndeadBonus.get())));
        if (ready(e, "hellfire_skull", c.hell.hellfireSkullRealityIndex.get()))
        {
            MobEffectInstance flame = target.getEffect(LCEffects.FLAME.get());
            if (flame != null && flame.getDuration() >= c.hell.hellfireSkullMinimumDuration.get())
            {
                int amplifier = Math.min(flame.getAmplifier() + 1, (int)Math.round(reality(e)) - 1);
                if (amplifier > flame.getAmplifier())
                    EffectUtil.addEffect(target, new EffectBuilder(new MobEffectInstance(flame)).setAmplifier(amplifier).ins,
                            AddReason.FORCE, e);
            }
        }
        if (ready(e, "hellfire_reformation", c.hell.hellfireReformationRealityIndex.get()))
        {
            MobEffectInstance flame = e.getEffect(LCEffects.FLAME.get());
            if (flame != null) EffectUtil.addEffect(target, new MobEffectInstance(flame), AddReason.FORCE, e);
        }
        if (ready(e, "eye_of_cursed_souls", c.hell.eyeOfCursedSoulRealityIndex.get()) && time >= s.eyeReady)
        {
            s.eyeReady = time + c.hell.eyeOfCursedSoulCoolDown.get();
            double range = c.hell.eyeOfCursedSoulRange.get();
            for (Mob mob : target.level().getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(range), mob ->
                    mob instanceof Enemy && mob != target && mob.distanceToSqr(target) <= range * range && !allied(e, mob)))
            {
                mob.setLastHurtByMob(null);
                mob.setTarget(null);
                // Each enemy attributes this soul hit to itself, reproducing the upstream confusion behavior.
                dev.xkmc.l2library.init.events.GeneralEventHandler.schedule(() -> {
                    if (mob.isAlive()) mob.hurt(source(mob, CoPDamageTypeGen.SOUL_CURSE, mob), 1);
                });
            }
        }
        if (ready(e, "shadow_core", c.shadow.shadowCoreRealityIndex.get()))
        {
            int duration = c.shadow.shadowCoreDurationPerIndex.get() * (int)Math.round(reality(e));
            dev.xkmc.l2library.init.events.GeneralEventHandler.schedule(() -> {
                if (target.isAlive()) EffectUtil.addEffect(target,
                        new MobEffectInstance(CoPEffects.SHADOW.get(), duration), AddReason.FORCE, e);
            });
        }
        if (ready(e, "shadow_reformation", c.shadow.shadowReformationRealityIndex.get())
                && target.hasEffect(CoPEffects.SHADOW.get()) && event.getSource().is(L2DamageTypes.MAGIC))
            cache.addHurtModifier(DamageModifier.multTotal((float)(1 + c.shadow.shadowReformationBonus.get())));
        if (ready(e, "evil_spirit_curse", c.evil.evilSpiritCurseRealityIndex.get()) && event.getSource().is(L2DamageTypes.MAGIC)
                && target.getHealth() < target.getMaxHealth() * c.evil.evilSpiritCurseThreshold.get())
            cache.addHurtModifier(DamageModifier.multTotal((float)(1 + c.evil.evilSpiritCurseBonus.get())));
    }

    @Override public void onDamage(AttackCache cache, ItemStack weapon)
    {
        var event = cache.getLivingDamageEvent();
        if (event == null || event.isCanceled() || event.getSource().is(L2DamageTypes.NO_SCALE)) return;
        LivingEntity attacker = cache.getAttacker();
        LivingEntity target = cache.getAttackTarget();
        var c = CoPConfig.COMMON;
        if (attacker != null && LoveSharing.active(attacker))
        {
            State s = state(attacker);
            long time = now(attacker);
            if (has(attacker, "curse_of_prudence") && attacker != target)
            {
                Marks marks = s.marks.computeIfAbsent(target.getUUID(), ignored -> new Marks());
                marks.fear.removeIf(t -> time >= t + c.curse.curseOfPrudenceDuration.get());
                int count = Math.min(marks.fear.size(), c.curse.curseOfPrudenceMaxLevel.get());
                if (count > 0 && !event.getSource().is(DamageTypeTags.BYPASSES_COOLDOWN))
                    cache.addDealtModifier(DamageModifier.multTotal((float)Math.pow(c.curse.curseOfPrudenceDamageFactor.get(), count)));
                marks.fear.add(time);
                if (!target.getType().is(CoPTagGen.PRUDENCE_WHITELIST) && target.getHealth() > attacker.getHealth())
                    cache.addDealtModifier(DamageModifier.nonlinearFinal(9000,
                            damage -> Math.min(damage, (float)(target.getMaxHealth() * c.curse.curseOfPrudenceMaxDamage.get()))));
            }
            if (has(attacker, "curse_of_spell"))
            {
                double penalty = spellPenalty(attacker, s);
                if (penalty > 0) cache.addDealtModifier(DamageModifier.multTotal((float)(1 / (1 + penalty * c.curse.curseOfSpellWeakenFactor.get()))));
            }
            Marks terror = s.marks.get(target.getUUID());
            if ((terror == null || terror.terrorUntil < time)
                    && angelic(attacker, "angelic_punishment", c.angelic.angelicPunishmentRealityIndex.get()))
                cache.addDealtModifier(DamageModifier.nonlinearMiddle(71, damage -> {
                    float minimum = (float)(target.getHealth() * c.angelic.angelicPunishmentDamageBase.get());
                    if (damage < minimum && time >= s.punishmentReady)
                    {
                        s.punishmentReady = time + c.angelic.angelicPunishmentCoolDown.get();
                        return minimum;
                    }
                    return damage;
                }));
        }
        if (LoveSharing.active(target))
        {
            if (has(target, "curse_of_spell") && !event.getSource().is(CoPDamageTypeGen.SPELL_CURSE))
            {
                double penalty = spellPenalty(target, state(target));
                if (penalty > 0) cache.addDealtModifier(DamageModifier.multTotal((float)Math.min(Float.MAX_VALUE,
                        1 + penalty * c.curse.curseOfSpellDamageFactor.get())));
            }
            if (attacker != null && attacker.hasEffect(CoPEffects.SHADOW.get())
                    && ready(target, "shadow_reformation", c.shadow.shadowReformationRealityIndex.get())
                    && !event.getSource().is(L2DamageTypes.MAGIC) && ordinary(event.getSource()))
                cache.addDealtModifier(DamageModifier.multTotal((float)(1 - c.shadow.shadowReformationReduction.get())));
        }
    }

    @Override public void onDamageFinalized(AttackCache cache, ItemStack weapon)
    {
        var event = cache.getLivingDamageEvent();
        if (event == null || event.isCanceled() || event.getSource().is(L2DamageTypes.NO_SCALE)) return;
        LivingEntity attacker = cache.getAttacker();
        LivingEntity target = cache.getAttackTarget();
        var c = CoPConfig.COMMON;
        if (LoveSharing.active(target) && attacker != null && attacker != target && has(target, "curse_of_tension"))
        {
            Marks marks = state(target).marks.computeIfAbsent(attacker.getUUID(), ignored -> new Marks());
            if (cache.getDamageDealt() >= target.getMaxHealth() * c.curse.curseOfTensionPenaltyThreshold.get())
                marks.terrorUntil = now(target) + c.curse.curseOfTensionPenaltyDuration.get();
            marks.brave.clear();
        }
        if (attacker == null || !LoveSharing.active(attacker) || !target.hasEffect(CoPEffects.SHADOW.get())) return;
        State s = state(attacker);
        if (ready(attacker, "shadow_convergence", c.shadow.shadowConvergenceRealityIndex.get()))
            s.heal += cache.getDamageDealt() * c.shadow.shadowConvergenceHealFactor.get();
        if (!event.getSource().is(CoPDamageTypeGen.SHADOW)
                && ready(attacker, "shadow_consolidation", c.shadow.shadowConsolidationRealityIndex.get()))
        {
            if (s.shadowHits.isEmpty())
            {
                if (now(attacker) < s.shadowReady) return;
                s.shadowReady = now(attacker) + c.shadow.shadowConsolidationCoolDown.get();
                s.shadowExecute = now(attacker) + c.shadow.shadowConsolidationDelay.get();
            }
            s.shadowHits.merge(target.position(), cache.getDamageDealt(), Float::sum);
        }
    }

    private static void explodeShadow(LivingEntity e, State s)
    {
        var c = CoPConfig.COMMON.shadow;
        double range = c.shadowConsolidationRange.get();
        AABB bounds = null;
        for (Vec3 point : s.shadowHits.keySet())
        {
            AABB box = AABB.ofSize(point, range * 2, range * 2, range * 2);
            bounds = bounds == null ? box : bounds.minmax(box);
        }
        if (bounds == null) return;
        ResourceKey<DamageType> type = ready(e, "void_overflow", c.voidOverflowRealityIndex.get())
                ? CoPDamageTypeGen.VOID_CURSE : CoPDamageTypeGen.SHADOW_CURSE;
        for (Mob mob : e.level().getEntitiesOfClass(Mob.class, bounds, mob -> mob instanceof Enemy && !allied(e, mob)))
        {
            float damage = 0;
            for (var hit : s.shadowHits.entrySet())
                if (mob.distanceToSqr(hit.getKey()) <= range * range) damage += hit.getValue();
            if (damage > 0) mob.hurt(source(e, type, e), (float)(damage * c.shadowConsolidationFactor.get()));
        }
        s.shadowHits.clear();
    }

    private static boolean peon(Mob mob)
    {
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        return mob.getMobType() == MobType.UNDEAD && health != null
                && health.getBaseValue() <= CoPConfig.COMMON.hell.crownOfDemonBaseHealthThreshold.get();
    }

    private static boolean allied(LivingEntity a, LivingEntity b)
    {
        if (a == b || a.isAlliedTo(b)) return true;
        UUID ownerA = a instanceof OwnableEntity own ? own.getOwnerUUID() : null;
        UUID ownerB = b instanceof OwnableEntity own ? own.getOwnerUUID() : null;
        return ownerA != null && (ownerA.equals(b.getUUID()) || ownerA.equals(ownerB))
                || ownerB != null && ownerB.equals(a.getUUID());
    }

    @SubscribeEvent public static void onTarget(LivingChangeTargetEvent event)
    {
        LivingEntity target = event.getNewTarget();
        if (target == null || !LoveSharing.active(target)) return;
        LivingEntity mob = event.getEntity();
        if (mob.getLastHurtByMob() != target && (mob instanceof Piglin && has(target, "golden_heart")
                || mob instanceof EnderMan && has(target, "charm_of_calmness"))
                || mob instanceof Mob m && peon(m) && ready(target, "crown_of_demon", CoPConfig.COMMON.hell.crownOfDemonRealityIndex.get()))
            event.setNewTarget(null);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event)
    {
        if (event.isCanceled() || event.getEntity().level().isClientSide) return;
        LivingEntity e = event.getEntity();
        if (LoveSharing.activeAtDeath(e) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
        {
            State s = state(e);
            long time = now(e);
            var c = CoPConfig.COMMON;
            if (readyAtDeath(e, "angelic_protection", c.angelic.angelicProtectionRealityIndex.get()) && time >= s.angelicReady
                    && (e.level().canSeeSky(e.blockPosition()) || readyAtDeath(e, "angelic_punishment", c.angelic.angelicPunishmentRealityIndex.get())))
            {
                s.angelicReady = time + c.angelic.angelicProtectionCoolDown.get();
                revive(e, e.getMaxHealth());
                event.setCanceled(true);
            }
            else if (readyAtDeath(e, "abyssal_will", c.abyssal.abyssalWillRealityIndex.get()) && time >= s.abyssReady
                    && (e.getY() <= 0 || event.getSource().is(DamageTypeTags.BYPASSES_ENCHANTMENTS)))
            {
                s.abyssReady = time + c.abyssal.abyssalWillCoolDown.get();
                revive(e, 1);
                e.addEffect(new MobEffectInstance(CoPEffects.ABYSSAL_PROTECTION.get(), c.abyssal.abyssalWillDuration.get()));
                event.setCanceled(true);
            }
            else if (readyAtDeath(e, "evil_spirit_walk", c.evil.evilSpiritWalkRealityIndex.get()) && time >= s.walkReady)
            {
                s.walkReady = time + c.evil.evilSpiritWalkCoolDown.get();
                revive(e, e.getMaxHealth());
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                e.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                event.setCanceled(true);
            }
        }
        if (!event.isCanceled() && event.getSource().getEntity() instanceof LivingEntity attacker
                && LoveSharing.active(attacker) && ready(attacker, "evil_spirit_awakening", CoPConfig.COMMON.evil.evilSpiritAwakeningRealityIndex.get()))
            state(attacker).awakening.add(now(attacker));
        if (!event.isCanceled()) clear(e);
    }

    private static void revive(LivingEntity e, float health)
    {
        e.setHealth(health);
        e.removeAllEffects();
        e.hasImpulse = true;
        e.level().broadcastEntityEvent(e, (byte)35);
    }

    private static boolean readyAtDeath(LivingEntity e, String item, int required)
    {
        return LoveSharing.hasAtDeath(e, item) && reality(e) >= required;
    }

    @SubscribeEvent public static void onEquipment(LivingEquipmentChangeEvent event)
    {
        if (event.getEntity().level().isClientSide) return;
        State s = STATES.get(event.getEntity());
        if (s != null) s.equipmentDirty = true;
    }
    @SubscribeEvent public static void onServerStopped(ServerStoppedEvent event) { STATES.clear(); COOLDOWNS.clear(); }
}
