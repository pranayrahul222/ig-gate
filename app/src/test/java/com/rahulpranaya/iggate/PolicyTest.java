package com.rahulpranaya.iggate;

/** Plain Java checks for the screen rules. Run with javac, no phone required. */
public final class PolicyTest {
    public static void main(String[] args) {
        homeStays();
        reelsTabStaysUntilTheCounter();
        searchTabGoesToInbox();
        searchPageGoesToInbox();
        chatStays();
        reelFromHomeStaysUntilTheCounter();
        reelFromChatPlays();
        inlineFeedHasNoViewerFlag();
        youtubeHomeIsNotShorts();
        shortsScreenIsDetected();
        fiveReelsStay();
        sixthReelTrips();
        quickRepeatCountsOnce();
        leavingStartsOver();
        rateLimitQuietsAfterThree();
        System.out.println("ok");
    }

    private static void homeStays() {
        Policy.View view = new Policy.View();
        view.homeSelected = true;
        Policy.Result result = Policy.decide(view, Policy.Place.NONE);
        check(result.action == Policy.Action.STAY, "home stays");
        check(result.place == Policy.Place.HOME, "home is remembered");
    }

    private static void reelsTabStaysUntilTheCounter() {
        Policy.View view = new Policy.View();
        view.reelsTabSelected = true;
        Policy.Result result = Policy.decide(view, Policy.Place.HOME);
        check(result.action == Policy.Action.STAY, "reels tab waits for the counter");
    }

    private static void searchTabGoesToInbox() {
        Policy.View view = new Policy.View();
        view.searchTabSelected = true;
        Policy.Result result = Policy.decide(view, Policy.Place.HOME);
        check(result.action == Policy.Action.GO_TO_INBOX, "search tab");
    }

    private static void searchPageGoesToInbox() {
        Policy.View view = new Policy.View();
        view.searchPageOpen = true;
        Policy.Result result = Policy.decide(view, Policy.Place.HOME);
        check(result.action == Policy.Action.GO_TO_INBOX, "search page");
    }

    private static void chatStays() {
        Policy.View view = new Policy.View();
        view.messagesOpen = true;
        Policy.Result result = Policy.decide(view, Policy.Place.HOME);
        check(result.action == Policy.Action.STAY, "chat stays");
        check(result.place == Policy.Place.CHAT, "chat is remembered");
    }

    private static void reelFromHomeStaysUntilTheCounter() {
        Policy.View home = new Policy.View();
        home.homeSelected = true;
        Policy.Result remembered = Policy.decide(home, Policy.Place.NONE);
        Policy.View viewer = new Policy.View();
        viewer.fullScreenReel = true;
        Policy.Result result = Policy.decide(viewer, remembered.place);
        check(result.action == Policy.Action.STAY, "reel from home waits for the counter");
        check(result.place == Policy.Place.HOME, "home memory holds");
    }

    private static void reelFromChatPlays() {
        Policy.View chat = new Policy.View();
        chat.messagesOpen = true;
        Policy.Result remembered = Policy.decide(chat, Policy.Place.HOME);
        Policy.View viewer = new Policy.View();
        viewer.fullScreenReel = true;
        Policy.Result result = Policy.decide(viewer, remembered.place);
        check(result.action == Policy.Action.STAY, "reel from chat plays");
        check(result.place == Policy.Place.CHAT, "chat memory holds during the reel");
    }

    private static void inlineFeedHasNoViewerFlag() {
        Policy.View view = new Policy.View();
        view.homeSelected = true;
        view.fullScreenReel = false;
        Policy.Result result = Policy.decide(view, Policy.Place.HOME);
        check(result.action == Policy.Action.STAY, "feed cell stays");
    }

    private static void youtubeHomeIsNotShorts() {
        check(!Youtube.isShorts(new Youtube.View()), "youtube home is not shorts");
    }

    private static void shortsScreenIsDetected() {
        Youtube.View tab = new Youtube.View();
        tab.shortsTab = true;
        Youtube.View player = new Youtube.View();
        player.shortsPlayer = true;
        check(Youtube.isShorts(tab), "shorts tab");
        check(Youtube.isShorts(player), "shorts player");
    }

    private static void fiveReelsStay() {
        ReelSession session = new ReelSession();
        for (int i = 1; i <= ReelSession.LIMIT; i++) {
            check(!session.onReel(i * 1000L), "reel " + i);
        }
    }

    private static void sixthReelTrips() {
        ReelSession session = new ReelSession();
        for (int i = 1; i <= ReelSession.LIMIT; i++) {
            session.onReel(i * 1000L);
        }
        check(session.onReel(6_000L), "sixth reel");
    }

    private static void quickRepeatCountsOnce() {
        ReelSession session = new ReelSession();
        check(!session.onReel(0), "first");
        check(!session.onReel(200), "same swipe");
        check(!session.onReel(1_000), "second reel");
    }

    private static void leavingStartsOver() {
        ReelSession session = new ReelSession();
        for (int i = 1; i <= ReelSession.LIMIT; i++) {
            session.onReel(i * 1000L);
        }
        session.leave();
        check(!session.onReel(20_000L), "a new go starts at one");
    }

    private static void rateLimitQuietsAfterThree() {
        RateLimit limit = new RateLimit();
        check(limit.allow(0), "first redirect");
        check(!limit.mark(0), "first does not quiet");
        check(!limit.allow(500), "gap");
        check(limit.allow(1000), "second redirect");
        check(!limit.mark(1000), "second does not quiet");
        check(limit.allow(2000), "third redirect");
        check(limit.mark(2000), "third starts quiet");
        check(!limit.allow(3000), "quiet holds");
        check(limit.allow(17_000), "quiet ends");
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError(name);
        }
    }
}
