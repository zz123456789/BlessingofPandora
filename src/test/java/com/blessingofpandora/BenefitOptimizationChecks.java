package com.blessingofpandora;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Differential checks against the 1.6.1 implementations, independent of a running world. */
public final class BenefitOptimizationChecks
{
    private static int checks;

    public static void main(String[] args)
    {
        checks = 0;
        attributeArithmeticMatchesBaseline();
        marksMatchBaseline();
        spiritTimerMatchesBaseline();
        System.out.println("PASS: " + checks + " benefit optimization checks");
    }

    private static void attributeArithmeticMatchesBaseline()
    {
        Random random = new Random(162);
        double[] special = {0, -0.0, 0.5, -1, 1, 1.0e16, -1.0e16,
                Double.MIN_VALUE, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (int sample = 0; sample < 2000; sample++)
        {
            List<AttributeModifier> modifiers = new ArrayList<>();
            for (int index = 0, size = random.nextInt(35); index < size; index++)
            {
                double amount = random.nextBoolean() ? special[random.nextInt(special.length)] : random.nextDouble() * 8 - 4;
                modifiers.add(new AttributeModifier(new UUID(random.nextLong(), random.nextLong()), "sample", amount,
                        AttributeModifier.Operation.values()[random.nextInt(3)]));
            }
            UUID excluded = modifiers.isEmpty() || random.nextBoolean()
                    ? null : modifiers.get(random.nextInt(modifiers.size())).getId();
            double base = special[random.nextInt(special.length)];
            for (boolean ignorePenalties : new boolean[] {false, true})
            {
                double before = baselineCalculate(base, modifiers, excluded, ignorePenalties);
                double after = BlessingBenefitMath.calculate(base, modifiers, excluded, ignorePenalties);
                check(Double.doubleToLongBits(before) == Double.doubleToLongBits(after),
                        "identical arithmetic including rounding and invalid values, sample " + sample);
            }
        }
    }

    // Deliberately preserve the previous condition and iteration order as the comparison oracle.
    private static double baselineCalculate(double base, Collection<AttributeModifier> modifiers,
                                            UUID excluded, boolean ignorePenalties)
    {
        double added = base;
        for (AttributeModifier modifier : modifiers)
        {
            if (!modifier.getId().equals(excluded) && (!ignorePenalties || modifier.getAmount() >= 0.0)
                    && modifier.getOperation() == AttributeModifier.Operation.ADDITION)
                added += modifier.getAmount();
        }
        double value = added;
        for (AttributeModifier modifier : modifiers)
        {
            if (!modifier.getId().equals(excluded) && (!ignorePenalties || modifier.getAmount() >= 0.0)
                    && modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE)
                value += added * modifier.getAmount();
        }
        for (AttributeModifier modifier : modifiers)
        {
            if (!modifier.getId().equals(excluded) && (!ignorePenalties || modifier.getAmount() >= 0.0)
                    && modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL)
                value *= 1.0 + modifier.getAmount();
        }
        return value;
    }

    private static void marksMatchBaseline()
    {
        Random random = new Random(16205);
        FearlessnessMarks after = new FearlessnessMarks();
        BaselineMarks before = new BaselineMarks();
        long now = 0;
        for (int step = 0; step < 6000; step++)
        {
            now += random.nextInt(6);
            if (step % 173 == 0) now -= 500;
            compareMarks(before, after, now);
            int hits = random.nextInt(30);
            for (int hit = 0; hit < hits; hit++)
            {
                before.hit(now);
                after.hit(now);
            }
            compareMarks(before, after, now);
        }
        // Include boundaries, multiple buckets, rollback and signed-overflow synthetic timelines.
        for (long[] timeline : new long[][] {
                {0, 0, 0, 0, 0, 0, 1, 200, 201, 400, 401, 402},
                {100, 99, 100, 300, 301, 500, 501},
                {Long.MIN_VALUE, Long.MIN_VALUE + 1, -401, -201, -1, 0, 1, 201, Long.MAX_VALUE}})
        {
            before = new BaselineMarks();
            after = new FearlessnessMarks();
            for (long tick : timeline)
            {
                compareMarks(before, after, tick);
                before.hit(tick);
                after.hit(tick);
                compareMarks(before, after, tick);
            }
        }
    }

    private static void compareMarks(BaselineMarks before, FearlessnessMarks after, long now)
    {
        check(before.mature(now) == after.mature(now), "mature layers match at " + now);
        check(before.empty(now) == after.empty(now), "empty state matches at " + now);
        check(before.buckets.size() == after.bucketCount(), "retained buckets match at " + now);
    }

    private static void spiritTimerMatchesBaseline()
    {
        List<CompoundTag> inputs = new ArrayList<>();
        inputs.add(new CompoundTag());
        for (int value : new int[] {Integer.MIN_VALUE, -1, 0, 1, 2398, 2399, 2400, 2401, Integer.MAX_VALUE})
        {
            CompoundTag input = new CompoundTag();
            input.putInt(BlessingBenefits.SPIRIT_FED_TICKS, value);
            inputs.add(input);
        }
        CompoundTag longTag = new CompoundTag();
        longTag.putLong(BlessingBenefits.SPIRIT_FED_TICKS, 2400);
        inputs.add(longTag);
        CompoundTag doubleTag = new CompoundTag();
        doubleTag.putDouble(BlessingBenefits.SPIRIT_FED_TICKS, 2400.75);
        inputs.add(doubleTag);
        CompoundTag stringTag = new CompoundTag();
        stringTag.putString(BlessingBenefits.SPIRIT_FED_TICKS, "2400");
        inputs.add(stringTag);
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put(BlessingBenefits.SPIRIT_FED_TICKS, new CompoundTag());
        inputs.add(compoundTag);

        for (CompoundTag input : inputs)
        {
            input.putString("other_mod:retained", "unchanged");
            for (boolean initialEligibility : new boolean[] {false, true})
            {
                CompoundTag before = input.copy();
                CompoundTag after = input.copy();
                for (int tick = 0; tick < 50; tick++)
                {
                    boolean eligible = tick < 25 ? initialEligibility : !initialEligibility;
                    int previous = Math.max(0, Math.min(2400, before.getInt(BlessingBenefits.SPIRIT_FED_TICKS)));
                    int expected = eligible ? Math.min(previous + 1, 2400) : 0;
                    if (expected > 0) before.putInt(BlessingBenefits.SPIRIT_FED_TICKS, expected);
                    else before.remove(BlessingBenefits.SPIRIT_FED_TICKS);
                    check(BlessingBenefitMath.updateSpiritTimer(after, eligible) == expected, "timer return matches baseline");
                    check(before.equals(after), "timer NBT and numeric normalization match baseline");
                }
            }
        }
        CompoundTag matured = new CompoundTag();
        matured.putInt(BlessingBenefits.SPIRIT_FED_TICKS, 2400);
        Tag original = matured.get(BlessingBenefits.SPIRIT_FED_TICKS);
        for (int tick = 0; tick < 200; tick++) BlessingBenefitMath.updateSpiritTimer(matured, true);
        check(matured.get(BlessingBenefits.SPIRIT_FED_TICKS) == original, "mature timer retains the existing IntTag");
        matured.putLong(BlessingBenefits.SPIRIT_FED_TICKS, 2400);
        BlessingBenefitMath.updateSpiritTimer(matured, true);
        check(matured.contains(BlessingBenefits.SPIRIT_FED_TICKS, Tag.TAG_INT), "same-value wrong numeric type is repaired");
    }

    private static final class BaselineMarks
    {
        private record Bucket(long tick, int hits) {}
        private final ArrayDeque<Bucket> buckets = new ArrayDeque<>();

        int mature(long now)
        {
            prune(now);
            int count = 0;
            for (Bucket bucket : buckets)
            {
                if (now - bucket.tick() > 200) count += bucket.hits();
                if (count >= 5) return 5;
            }
            return count;
        }

        void hit(long now)
        {
            prune(now);
            Bucket last = buckets.peekLast();
            if (last != null && last.tick() == now)
            {
                buckets.removeLast();
                buckets.addLast(new Bucket(now, Math.min(5, last.hits() + 1)));
            }
            else buckets.addLast(new Bucket(now, 1));
        }

        boolean empty(long now)
        {
            prune(now);
            return buckets.isEmpty();
        }

        private void prune(long now)
        {
            if (!buckets.isEmpty() && now < buckets.peekLast().tick()) buckets.clear();
            while (!buckets.isEmpty() && now - buckets.peekFirst().tick() > 400) buckets.removeFirst();
        }
    }

    private static void check(boolean condition, String label)
    {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
