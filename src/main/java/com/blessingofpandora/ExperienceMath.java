package com.blessingofpandora;

/** Minecraft 1.20.1's level curve, independent of the historical totalExperience field. */
public final class ExperienceMath
{
    private ExperienceMath() {}

    public static long atLevel(int level)
    {
        long l = Math.max(0, level);
        if (l <= 16) return l * l + 6 * l;
        if (l <= 31) return (5 * l * l - 81 * l + 720) / 2;
        // Guard hostile/modded levels against long overflow.
        if (l > 1_000_000_000L) return Long.MAX_VALUE;
        return (9 * l * l - 325 * l + 4440) / 2;
    }

    public static long available(int level, float progress, int nextLevelCost)
    {
        long base = atLevel(level);
        double fraction = Float.isFinite(progress) ? Math.max(0, Math.min(1, progress)) : 0;
        long partial = Math.round(fraction * Math.max(0, nextLevelCost));
        return base > Long.MAX_VALUE - partial ? Long.MAX_VALUE : base + partial;
    }

    public static int drain(long available, long progress, long target)
    {
        return (int) Math.max(0, Math.min(50_000L,
                Math.min(available, Math.max(0, target - progress))));
    }
}
