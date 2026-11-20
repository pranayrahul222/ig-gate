package com.rahulpranaya.iggate;

import java.net.URI;

/** Decides which website pages this app may open. No other app is involved. */
final class UrlPolicy {
    static final String INSTAGRAM_HOME = "https://www.instagram.com/";
    static final String INSTAGRAM_INBOX = "https://www.instagram.com/direct/inbox/";
    static final String YOUTUBE_HOME = "https://www.youtube.com/";

    enum Decision {
        ALLOW,
        INSTAGRAM_HOME,
        INSTAGRAM_INBOX,
        YOUTUBE_HOME,
        BLOCK
    }

    private UrlPolicy() {
    }

    static Decision decide(String url) {
        if (url == null) {
            return Decision.BLOCK;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("javascript:") || trimmed.startsWith("file:")
                || trimmed.startsWith("intent:") || trimmed.startsWith("data:")) {
            return Decision.BLOCK;
        }
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException bad) {
            return Decision.BLOCK;
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!scheme.equals("https") && !scheme.equals("http")) {
            return Decision.BLOCK;
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase();

        if (isInstagram(host)) {
            if (containsSegment(path, "reel") || containsSegment(path, "reels")) {
                return Decision.INSTAGRAM_HOME;
            }
            if (containsSegment(path, "explore") || containsSegment(path, "search")) {
                return Decision.INSTAGRAM_INBOX;
            }
            return Decision.ALLOW;
        }
        if (isYouTube(host)) {
            if (containsSegment(path, "shorts")) {
                return Decision.YOUTUBE_HOME;
            }
            return Decision.ALLOW;
        }
        if (isLoginHost(host, path)) {
            return Decision.ALLOW;
        }
        return Decision.BLOCK;
    }

    private static boolean isInstagram(String host) {
        return host.equals("instagram.com") || host.endsWith(".instagram.com");
    }

    private static boolean isYouTube(String host) {
        return host.equals("youtube.com")
                || host.endsWith(".youtube.com")
                || host.equals("youtu.be")
                || host.equals("youtube-nocookie.com")
                || host.endsWith(".youtube-nocookie.com");
    }

    private static boolean isLoginHost(String host, String path) {
        if (host.equals("accounts.google.com") || host.equals("myaccount.google.com")) {
            return true;
        }
        boolean facebook = host.equals("facebook.com") || host.endsWith(".facebook.com");
        if (!facebook) {
            return false;
        }
        return path.contains("login")
                || path.contains("checkpoint")
                || path.contains("oauth")
                || path.contains("dialog")
                || path.contains("signin");
    }

    private static boolean containsSegment(String path, String segment) {
        return path.equals("/" + segment)
                || path.startsWith("/" + segment + "/")
                || path.endsWith("/" + segment)
                || path.contains("/" + segment + "/");
    }
}
