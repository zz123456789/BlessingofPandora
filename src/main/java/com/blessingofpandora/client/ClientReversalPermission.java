package com.blessingofpandora.client;

import com.blessingofpandora.BlessingDefinitions;

import java.util.Arrays;

public final class ClientReversalPermission
{
    private static final int BLESSING_COUNT = 7;
    private static boolean[] allowed = new boolean[BLESSING_COUNT];
    private static int sixthCurseState;
    private static long sixthAbsorbedExperience;
    private static long firstKillFailures;
    private static long fifthKillFailures;

    static
    {
        Arrays.fill(allowed, true);
    }

    private ClientReversalPermission()
    {
    }

    public static boolean isAllowed(int index)
    {
        return index < 0 || index >= allowed.length || allowed[index];
    }

    public static long sixthAbsorbedExperience()
    {
        return sixthAbsorbedExperience;
    }

    public static int sixthCurseState()
    {
        return sixthCurseState;
    }

    public static long killFailures(BlessingDefinitions definition)
    {
        if (definition == BlessingDefinitions.FREEDOM) return firstKillFailures;
        if (definition == BlessingDefinitions.FEARLESSNESS) return fifthKillFailures;
        return 0L;
    }

    public static void setStates(
            boolean[] states, int curseState, long absorbedExperience,
            long firstFailures, long fifthFailures)
    {
        if (states != null && states.length == BLESSING_COUNT)
        {
            allowed = states.clone();
        }
        sixthCurseState = Math.max(0, curseState);
        sixthAbsorbedExperience = Math.max(0L, absorbedExperience);
        firstKillFailures = Math.max(0L, firstFailures);
        fifthKillFailures = Math.max(0L, fifthFailures);
    }
}
