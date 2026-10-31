package com.blessingofpandora;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Collection;
import java.util.UUID;

import static com.blessingofpandora.BlessingBenefits.SPIRIT_FED_TICKS;

/** Non-recursive benefit math and saved timers. Never queries equipment or AttributeInstance.getValue(). */
public final class BlessingBenefitMath
{
    private BlessingBenefitMath() {}

    public static double valueWithout(AttributeInstance attribute, UUID excluded, boolean ignorePenalties)
    {
        return attribute.getAttribute().sanitizeValue(calculate(
                attribute.getBaseValue(), attribute.getModifiers(), excluded, ignorePenalties));
    }

    public static double calculate(double base, Collection<AttributeModifier> modifiers,
                                   UUID excluded, boolean ignorePenalties)
    {
        double added = base;
        for (AttributeModifier modifier : modifiers)
        {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION
                    && included(modifier, excluded, ignorePenalties))
                added += modifier.getAmount();
        }
        double value = added;
        for (AttributeModifier modifier : modifiers)
        {
            if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE
                    && included(modifier, excluded, ignorePenalties))
                value += added * modifier.getAmount();
        }
        for (AttributeModifier modifier : modifiers)
        {
            if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL
                    && included(modifier, excluded, ignorePenalties))
                value *= 1.0 + modifier.getAmount();
        }
        return value;
    }

    private static boolean included(AttributeModifier modifier, UUID excluded, boolean ignorePenalties)
    {
        return !modifier.getId().equals(excluded) && (!ignorePenalties || modifier.getAmount() >= 0.0);
    }

    public static double foodBonus(int food, double reality)
    {
        if (reality < 5.0) return 0.0;
        int clamped = Math.min(food, 20);
        return Math.max(0, clamped - 16) * 0.05 + (clamped == 20 ? 0.2 : 0.0);
    }

    /** Matches the dependency's >= 14 food, >= 2400 consecutive tick requirement. */
    public static int fedTicks(int previous, boolean eligible)
    {
        return eligible ? Math.min(previous + 1, 2400) : 0;
    }

    static int updateSpiritTimer(CompoundTag data, boolean eligible)
    {
        int previous = data.getInt(SPIRIT_FED_TICKS);
        int ticks = fedTicks(Math.max(0, Math.min(2400, previous)), eligible);
        if (ticks > 0)
        {
            // getInt also accepts other numeric tags; normalize those just as the old putInt did.
            if (previous != ticks || !data.contains(SPIRIT_FED_TICKS, Tag.TAG_INT))
                data.putInt(SPIRIT_FED_TICKS, ticks);
        }
        else if (data.contains(SPIRIT_FED_TICKS)) data.remove(SPIRIT_FED_TICKS);
        return ticks;
    }

    public static double liberationReduction(int enchantmentLevels)
    {
        return Math.max(0, Math.min(400, enchantmentLevels)) * 0.002;
    }

    /** The offensive minimum is resolved before the defender's blessing, without reviving zero hits. */
    public static float finalDamage(float amount, float targetMaxHealth, boolean requiem, int protectionLevels)
    {
        if (!Float.isFinite(amount) || amount <= 0) return amount;
        double result = amount;
        if (requiem && Float.isFinite(targetMaxHealth) && targetMaxHealth > 0)
            result = Math.max(result, targetMaxHealth * 0.005);
        result *= 1.0 - liberationReduction(protectionLevels);
        return (float) Math.min(Float.MAX_VALUE, result);
    }
}
