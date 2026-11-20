package com.rahulpranaya.iggate;

/** Plain Java checks for the in-app page rules. Run with javac, no phone required. */
public final class UrlPolicyTest {
    public static void main(String[] args) {
        allow("https://www.instagram.com/", "home");
        allow("https://www.instagram.com/direct/inbox/", "inbox");
        allow("https://www.instagram.com/stories/friend/", "story");
        allow("https://www.youtube.com/watch?v=abc", "video");
        allow("https://youtu.be/abc", "short link");
        allow("https://accounts.google.com/v3/signin/identifier", "google sign in");
        allow("https://www.facebook.com/login.php", "facebook login");
        check(UrlPolicy.decide("https://www.instagram.com/reels/") == UrlPolicy.Decision.INSTAGRAM_HOME, "reels");
        check(UrlPolicy.decide("https://www.instagram.com/reel/abc/") == UrlPolicy.Decision.INSTAGRAM_HOME, "reel");
        check(UrlPolicy.decide("https://www.instagram.com/explore/") == UrlPolicy.Decision.INSTAGRAM_INBOX, "explore");
        check(UrlPolicy.decide("https://www.instagram.com/explore/search/") == UrlPolicy.Decision.INSTAGRAM_INBOX, "search");
        check(UrlPolicy.decide("https://m.youtube.com/shorts/abc") == UrlPolicy.Decision.YOUTUBE_HOME, "shorts");
        check(UrlPolicy.decide("javascript:alert(1)") == UrlPolicy.Decision.BLOCK, "javascript");
        check(UrlPolicy.decide("file:///etc/passwd") == UrlPolicy.Decision.BLOCK, "file");
        check(UrlPolicy.decide("intent://scan/#Intent;end") == UrlPolicy.Decision.BLOCK, "intent");
        check(UrlPolicy.decide("https://www.facebook.com/") == UrlPolicy.Decision.BLOCK, "facebook home");
        check(UrlPolicy.decide("https://example.com/") == UrlPolicy.Decision.BLOCK, "other site");
        check(UrlPolicy.decide(null) == UrlPolicy.Decision.BLOCK, "null");
        System.out.println("ok");
    }

    private static void allow(String url, String name) {
        check(UrlPolicy.decide(url) == UrlPolicy.Decision.ALLOW, name);
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError(name);
        }
    }
}
