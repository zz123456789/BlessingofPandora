package com.blessingofpandora.client;

/** Monotonic, frame-time gesture state; independent of tooltips, key mappings and screens. */
public final class LoveHudHoldGesture
{
    public static final double HOLD_SECONDS = 0.5;
    private long previous;
    private double elapsed;
    private boolean consumed;

    public boolean update(long now, boolean valid, boolean held)
    {
        if (!held)
        {
            consumed = false;
            resetProgress();
            return false;
        }
        if (!valid || consumed) { resetProgress(); return false; }
        if (previous != 0) elapsed += Math.min(0.1, Math.max(0, (now - previous) / 1_000_000_000.0));
        previous = now;
        elapsed = Math.min(HOLD_SECONDS, elapsed);
        return elapsed >= HOLD_SECONDS;
    }

    public double fraction() { return elapsed / HOLD_SECONDS; }

    /** Used after opening or losing window focus: a physical release must precede another gesture. */
    public void consume() { consumed = true; resetProgress(); }

    public void resetProgress() { previous = 0; elapsed = 0; }
}
