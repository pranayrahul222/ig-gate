package com.rahulpranaya.iggate;

/** At most one redirect a second. After three in ten seconds, stay quiet for fifteen. */
final class RateLimit {
    private static final long GAP_MS = 1000;
    private static final long WINDOW_MS = 10_000;
    private static final long QUIET_MS = 15_000;

    private long lastRedirect = -1;
    private long quietUntil;
    private final long[] stamps = new long[8];
    private int count;

    boolean allow(long now) {
        if (now < quietUntil) {
            return false;
        }
        if (lastRedirect >= 0 && now - lastRedirect < GAP_MS) {
            return false;
        }
        return recent(now) < 3;
    }

    /** Records a redirect. Returns true the moment the quiet period starts. */
    boolean mark(long now) {
        lastRedirect = now;
        if (count == stamps.length) {
            System.arraycopy(stamps, 1, stamps, 0, count - 1);
            count--;
        }
        stamps[count++] = now;
        if (quietUntil <= now && recent(now) >= 3) {
            quietUntil = now + QUIET_MS;
            return true;
        }
        return false;
    }

    private int recent(long now) {
        int recent = 0;
        for (int i = 0; i < count; i++) {
            if (now - stamps[i] < WINDOW_MS) {
                recent++;
            }
        }
        return recent;
    }
}
