package com.rahulpranaya.iggate;

/** Counts reels or shorts in one sitting. The sixth one is over the limit. */
final class ReelSession {
    static final int LIMIT = 5;
    private static final long SWIPE_GAP_MS = 400;

    private int count;
    private long lastCountMs;

    /** Returns true when this reel is past the limit. Quick repeats count as one reel. */
    boolean onReel(long now) {
        if (count > 0 && now - lastCountMs < SWIPE_GAP_MS) {
            return false;
        }
        lastCountMs = now;
        count++;
        return count > LIMIT;
    }

    void leave() {
        count = 0;
        lastCountMs = 0;
    }
}
