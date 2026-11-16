package com.rahulpranaya.iggate;

/** YouTube screen flags. The reel counter decides when Shorts has gone on too long. */
final class Youtube {
    static final class View {
        boolean shortsTab;
        boolean shortsPlayer;
    }

    private Youtube() {
    }

    static boolean isShorts(View view) {
        return view.shortsTab || view.shortsPlayer;
    }
}
