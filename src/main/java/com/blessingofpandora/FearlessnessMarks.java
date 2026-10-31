package com.blessingofpandora;

import java.util.ArrayDeque;

/** At most 401 tick buckets; excess hits in one tick cannot change a five-layer bonus. */
public final class FearlessnessMarks
{
    private record Bucket(long tick, int hits) {}
    private final ArrayDeque<Bucket> buckets = new ArrayDeque<>();

    public int mature(long now)
    {
        prune(now);
        int count = 0;
        for (Bucket bucket : buckets)
        {
            // The original curse uses strict > for maturity and expiry.
            long age = now - bucket.tick();
            // Later buckets are younger. Do not short-circuit a signed overflow in synthetic timelines.
            if (age >= 0 && age <= 200) break;
            if (age > 200) count += bucket.hits();
            if (count >= 5) return 5;
        }
        return count;
    }

    public void hit(long now)
    {
        prune(now);
        Bucket last = buckets.peekLast();
        if (last != null && last.tick() == now)
        {
            if (last.hits() == 5) return;
            buckets.removeLast();
            buckets.addLast(new Bucket(now, last.hits() + 1));
        }
        else buckets.addLast(new Bucket(now, 1));
    }

    public boolean empty(long now)
    {
        prune(now);
        return buckets.isEmpty();
    }

    private void prune(long now)
    {
        // Reset if a test/world time moves backwards; old marks must not leak into another timeline.
        if (!buckets.isEmpty() && now < buckets.peekLast().tick()) buckets.clear();
        while (!buckets.isEmpty() && now - buckets.peekFirst().tick() > 400) buckets.removeFirst();
    }

    int bucketCount() { return buckets.size(); }
}
