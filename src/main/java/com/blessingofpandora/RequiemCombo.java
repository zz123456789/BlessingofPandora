package com.blessingofpandora;

/** One attacker's confirmed damage streak against one target. */
public final class RequiemCombo
{
    private int layers;
    private long lastHit;

    public int layers(long now)
    {
        if (layers > 0 && (now < lastHit || now - lastHit > 40)) layers = 0;
        return layers;
    }

    public void hit(long now)
    {
        layers = Math.min(5, layers(now) + 1);
        lastHit = now;
    }
}
