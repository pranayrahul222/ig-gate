package com.rahulpranaya.iggate;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

/**
 * Leaves Instagram Reels and Search, and sends YouTube Shorts back to Home.
 * The lists at the top are the behavior. Edit one, rebuild, reinstall.
 */
public final class GateService extends AccessibilityService {
    private static final String TAG = "IGGate";
    private static final String INSTAGRAM = "com.instagram.android";
    private static final String YOUTUBE = "com.google.android.youtube";
    private static final int MAX_NODES = 900;
    private static final int MAX_DEPTH = 60;
    private static final long SCAN_GAP_MS = 350;

    /** Bottom-tab ids Instagram has used for Reels and Search. */
    private static final String[] BLOCKED_TAB_IDS = {
            "clips_tab",
            "search_tab"
    };

    /** Full-screen reel player. Story ids start with reel_viewer and are absent here. */
    private static final String[] VIEWER_ID_PARTS = {
            "clips_viewer"
    };

    /** Search page fields. A home screen with no search field stays open. */
    private static final String[] SEARCH_FIELD_IDS = {
            "action_bar_search_edit_text",
            "row_search_edit_text"
    };

    /**
     * An open chat or the inbox list. The messages tab icon is always on screen,
     * so it is handled only when that tab is selected. Matching uses the id, never the message text.
     */
    private static final String[] CHAT_IDS = {
            "row_thread_composer_edittext",
            "row_thread_composer_button_send",
            "direct_text_message_text_view",
            "inbox_refreshable_thread_list_recyclerview"
    };

    private static final String[] CHAT_ID_PARTS = {
            "direct_inbox",
            "direct_thread"
    };

    private static final String[] HOME_TAB_IDS = {
            "feed_tab",
            "home_tab"
    };

    private static final String[] LANDING_TAB_IDS = {
            "direct_tab",
            "direct_inbox_tab"
    };

    private static final String[] LANDING_LABELS = {
            "messages",
            "message",
            "direct",
            "direct messages",
            "dms",
            "messenger",
            "inbox"
    };

    private static final String[] HOME_LABELS = {
            "home"
    };

    /** YouTube's bottom bar. The Shorts tab label is "Shorts". */
    private static final String[] YT_BAR_IDS = {
            "pivot_bar"
    };

    private static final String[] YT_SHORTS_LABELS = {
            "shorts"
    };

    /** Full-screen Shorts player. The shorts row on Home uses different ids. */
    private static final String[] YT_PLAYER_PARTS = {
            "reel_watch",
            "reel_player_overlay",
            "reel_player_page",
            "shorts_player",
            "reel_recycler"
    };

    private static final String[] REELS_LABELS = {
            "reels",
            "reel"
    };

    private static final String[] SEARCH_LABELS = {
            "search",
            "search and explore",
            "explore"
    };

    private final RateLimit rateLimit = new RateLimit();
    private final ReelSession instagramReels = new ReelSession();
    private final ReelSession youtubeShorts = new ReelSession();
    private Policy.Place place = Policy.Place.NONE;
    private long lastScan;
    private boolean instagramViewer;
    private boolean youtubeViewer;
    private boolean instagramHeld;
    private boolean youtubeHeld;
    private int instagramFeedMark;
    private int youtubeFeedMark;
    private long lastReelScrollMs;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) {
            return;
        }
        if (!INSTAGRAM.contentEquals(event.getPackageName())
                && !YOUTUBE.contentEquals(event.getPackageName())) {
            return;
        }
        int type = event.getEventType();
        boolean scrolled = type == AccessibilityEvent.TYPE_VIEW_SCROLLED;
        if (scrolled) {
            lastReelScrollMs = SystemClock.uptimeMillis();
        }
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && !scrolled) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (!scrolled && type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && now - lastScan < SCAN_GAP_MS) {
            return;
        }
        lastScan = now;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }
        try {
            if (root.getPackageName() == null) {
                return;
            }
            if (INSTAGRAM.contentEquals(root.getPackageName())) {
                handleInstagram(root, now, scrolled);
            } else if (YOUTUBE.contentEquals(root.getPackageName())) {
                handleYouTube(root, now, scrolled);
            }
        } catch (RuntimeException ex) {
            Log.w(TAG, "scan failed: " + ex.getClass().getSimpleName());
        } finally {
            root.recycle();
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) {
            return;
        }
        info.eventTypes |= AccessibilityEvent.TYPE_VIEW_SCROLLED;
        info.packageNames = new String[] { INSTAGRAM, YOUTUBE };
        setServiceInfo(info);
    }

    private void handleInstagram(AccessibilityNodeInfo root, long now, boolean scrolled) {
        Policy.View view = read(root);
        Policy.Result result = Policy.decide(view, place);
        place = result.place;
        if (result.action == Policy.Action.GO_TO_INBOX) {
            instagramReels.leave();
            instagramViewer = false;
            instagramHeld = false;
            instagramFeedMark = 0;
            redirect(now, "redirect " + reason(view), root, Dest.INBOX);
            return;
        }
        boolean viewer = view.reelsTabSelected || view.fullScreenReel;
        if (watchSession(viewer, scrolled, now, instagramReels, instagramViewer, instagramHeld, instagramFeedMark, root, false)) {
            return;
        }
    }

    private void handleYouTube(AccessibilityNodeInfo root, long now, boolean scrolled) {
        Youtube.View view = readYoutube(root);
        boolean viewer = Youtube.isShorts(view);
        watchSession(viewer, scrolled, now, youtubeShorts, youtubeViewer, youtubeHeld, youtubeFeedMark, root, true);
    }

    /**
     * Counts one sitting of reels or shorts. Past five, opens that app's Home.
     * Returns true when the caller should stop.
     */
    private boolean watchSession(
            boolean viewer,
            boolean scrolled,
            long now,
            ReelSession session,
            boolean viewerOpen,
            boolean held,
            int feedMark,
            AccessibilityNodeInfo root,
            boolean youtube) {
        if (held) {
            if (!viewer) {
                setHeld(youtube, false);
                setViewer(youtube, false);
                setFeedMark(youtube, reelMark(root, youtube));
                session.leave();
            } else if (scrolled) {
                redirect(now, youtube ? "redirect yt-reel-limit" : "redirect ig-reel-limit", root,
                        youtube ? Dest.YT_HOME : Dest.IG_HOME);
            }
            return true;
        }
        boolean over = false;
        if (viewer) {
            if (!viewerOpen || scrolled) {
                over = session.onReel(now);
            }
            setViewer(youtube, true);
        } else {
            if (viewerOpen) {
                session.leave();
                setFeedMark(youtube, 0);
            }
            setViewer(youtube, false);
            int mark = reelMark(root, youtube);
            if (mark != 0 && mark != feedMark) {
                setFeedMark(youtube, mark);
                if (scrolled || now - lastReelScrollMs < 900) {
                    over = session.onReel(now);
                }
            }
        }
        if (!over) {
            return false;
        }
        session.leave();
        setHeld(youtube, true);
        redirect(now, youtube ? "redirect yt-reel-limit" : "redirect ig-reel-limit", root,
                youtube ? Dest.YT_HOME : Dest.IG_HOME);
        return true;
    }

    private void setViewer(boolean youtube, boolean open) {
        if (youtube) {
            youtubeViewer = open;
        } else {
            instagramViewer = open;
        }
    }

    private void setHeld(boolean youtube, boolean held) {
        if (youtube) {
            youtubeHeld = held;
        } else {
            instagramHeld = held;
        }
    }

    private void setFeedMark(boolean youtube, int mark) {
        if (youtube) {
            youtubeFeedMark = mark;
        } else {
            instagramFeedMark = mark;
        }
    }

    private enum Dest {
        INBOX,
        IG_HOME,
        YT_HOME
    }

    private void redirect(long now, String reason, AccessibilityNodeInfo root, Dest dest) {
        if (!rateLimit.allow(now)) {
            return;
        }
        boolean quiet = rateLimit.mark(now);
        Log.i(TAG, reason);
        if (dest == Dest.INBOX) {
            goToInbox(root);
        } else if (dest == Dest.IG_HOME) {
            goToInstagramHome(root);
        } else {
            goToYoutubeHome(root);
        }
        if (quiet) {
            Toast.makeText(this, R.string.paused, Toast.LENGTH_SHORT).show();
        }
    }

    private void goToInstagramHome(AccessibilityNodeInfo root) {
        if (clickMatch(root, HOME_TAB_IDS, HOME_LABELS, false, 0)) {
            return;
        }
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    private int reelMark(AccessibilityNodeInfo root, boolean youtube) {
        int minHeight = 500;
        if (getResources() != null && getResources().getDisplayMetrics() != null) {
            minHeight = Math.max(500, getResources().getDisplayMetrics().heightPixels / 3);
        }
        int[] best = new int[] {0, 0};
        int[] seen = new int[1];
        walkReelMark(root, youtube, minHeight, best, 0, seen);
        if (best[0] < minHeight) {
            return 0;
        }
        return best[1] == 0 ? 1 : best[1];
    }

    private void walkReelMark(
            AccessibilityNodeInfo node,
            boolean youtube,
            int minHeight,
            int[] best,
            int depth,
            int[] seen) {
        if (node == null || depth > MAX_DEPTH || seen[0] >= MAX_NODES) {
            return;
        }
        seen[0]++;
        if (node.isVisibleToUser()) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            int height = bounds.height();
            CharSequence desc = node.getContentDescription();
            String label = desc == null ? "" : desc.toString().toLowerCase();
            String id = node.getViewIdResourceName();
            if (height >= minHeight && isReelLabel(label, id, youtube) && height > best[0]) {
                best[0] = height;
                best[1] = label.hashCode();
            }
        }
        int children = node.getChildCount();
        for (int i = 0; i < children && seen[0] < MAX_NODES; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) {
                continue;
            }
            walkReelMark(child, youtube, minHeight, best, depth + 1, seen);
            child.recycle();
        }
    }

    private static boolean isReelLabel(String label, String id, boolean youtube) {
        if (youtube && id != null && (id.contains("reel_item") || id.contains("shorts_lockup"))) {
            return true;
        }
        if (label == null || label.length() < 4 || label.length() > 180) {
            return false;
        }
        if (youtube) {
            return label.equals("shorts") || label.startsWith("short ") || label.startsWith("shorts ");
        }
        return label.equals("reel") || label.startsWith("reel ") || label.startsWith("reel,");
    }

    private static String reason(Policy.View view) {
        if (view.reelsTabSelected) {
            return "reels-tab";
        }
        if (view.searchTabSelected) {
            return "search-tab";
        }
        if (view.searchPageOpen) {
            return "search-page";
        }
        return "viewer";
    }

    private void goToYoutubeHome(AccessibilityNodeInfo root) {
        if (clickMatch(root, new String[0], HOME_LABELS, false, 0)) {
            return;
        }
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    private Youtube.View readYoutube(AccessibilityNodeInfo root) {
        Youtube.View view = new Youtube.View();
        int[] seen = new int[1];
        walkYoutube(root, view, false, 0, seen);
        return view;
    }

    private void walkYoutube(AccessibilityNodeInfo node, Youtube.View view, boolean inBar, int depth, int[] seen) {
        if (node == null || depth > MAX_DEPTH || seen[0] >= MAX_NODES) {
            return;
        }
        seen[0]++;
        String id = node.getViewIdResourceName();
        boolean bar = inBar || idEndsAny(id, YT_BAR_IDS);
        if (bar && node.isSelected() && matches(labelOf(node), YT_SHORTS_LABELS)) {
            view.shortsTab = true;
        }
        if (node.isVisibleToUser() && idContainsAny(id, YT_PLAYER_PARTS)) {
            view.shortsPlayer = true;
        }
        int children = node.getChildCount();
        for (int i = 0; i < children && seen[0] < MAX_NODES; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) {
                continue;
            }
            walkYoutube(child, view, bar, depth + 1, seen);
            child.recycle();
        }
    }

    private Policy.View read(AccessibilityNodeInfo root) {
        Policy.View view = new Policy.View();
        int[] seen = new int[1];
        walk(root, view, false, 0, seen);
        boolean searchFieldVisible = view.searchPageOpen;
        view.searchPageOpen = view.searchTabSelected || (searchFieldVisible && !view.homeSelected);
        return view;
    }

    private void walk(AccessibilityNodeInfo node, Policy.View view, boolean inTabBar, int depth, int[] seen) {
        if (node == null || depth > MAX_DEPTH || seen[0] >= MAX_NODES) {
            return;
        }
        seen[0]++;
        String id = node.getViewIdResourceName();
        boolean bar = inTabBar || idEnds(id, "tab_bar") || idEnds(id, "fixed_tabbar_tabs_container")
                || idEndsAny(id, YT_BAR_IDS);
        boolean visible = node.isVisibleToUser();

        if (node.isSelected() && (bar || idEndsAny(id, BLOCKED_TAB_IDS) || idEndsAny(id, HOME_TAB_IDS) || idEndsAny(id, LANDING_TAB_IDS))) {
            String label = bar ? labelOf(node) : "";
            if (idEnds(id, "clips_tab") || matches(label, REELS_LABELS)) {
                view.reelsTabSelected = true;
            }
            if (idEnds(id, "search_tab") || matches(label, SEARCH_LABELS)) {
                view.searchTabSelected = true;
            }
            if (idEndsAny(id, HOME_TAB_IDS) || matches(label, HOME_LABELS)) {
                view.homeSelected = true;
            }
            if (idEndsAny(id, LANDING_TAB_IDS) || (bar && matches(label, LANDING_LABELS))) {
                view.messagesOpen = true;
            }
        }

        if (visible && idContainsAny(id, VIEWER_ID_PARTS)) {
            view.fullScreenReel = true;
        }
        if (visible && idEndsAny(id, SEARCH_FIELD_IDS)) {
            view.searchPageOpen = true;
        }
        if (visible && (idEndsAny(id, CHAT_IDS) || idContainsAny(id, CHAT_ID_PARTS))) {
            view.messagesOpen = true;
        }

        int children = node.getChildCount();
        for (int i = 0; i < children && seen[0] < MAX_NODES; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) {
                continue;
            }
            walk(child, view, bar, depth + 1, seen);
            child.recycle();
        }
    }

    private void goToInbox(AccessibilityNodeInfo root) {
        if (clickMatch(root, LANDING_TAB_IDS, LANDING_LABELS, false, 0)) {
            return;
        }
        if (clickLabel(root, LANDING_LABELS, 0)) {
            return;
        }
        if (clickMatch(root, HOME_TAB_IDS, HOME_LABELS, false, 0)) {
            return;
        }
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    private boolean clickMatch(
            AccessibilityNodeInfo node,
            String[] ids,
            String[] labels,
            boolean inTabBar,
            int depth) {
        if (node == null || depth > MAX_DEPTH) {
            return false;
        }
        String id = node.getViewIdResourceName();
        boolean bar = inTabBar || idEnds(id, "tab_bar") || idEnds(id, "fixed_tabbar_tabs_container")
                || idEndsAny(id, YT_BAR_IDS);
        boolean idHit = idEndsAny(id, ids);
        boolean labelHit = bar && matches(labelOf(node), labels);
        if (node.isVisibleToUser() && (idHit || labelHit) && performClick(node)) {
            return true;
        }
        int children = node.getChildCount();
        for (int i = 0; i < children; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) {
                continue;
            }
            boolean clicked = clickMatch(child, ids, labels, bar, depth + 1);
            child.recycle();
            if (clicked) {
                return true;
            }
        }
        return false;
    }

    /** Paper-plane button on older Instagram, which sits outside the bottom tab bar. */
    private boolean clickLabel(AccessibilityNodeInfo node, String[] labels, int depth) {
        if (node == null || depth > MAX_DEPTH) {
            return false;
        }
        if (node.isVisibleToUser() && node.isClickable() && matches(labelOf(node), labels)) {
            if (performClick(node)) {
                return true;
            }
        }
        int children = node.getChildCount();
        for (int i = 0; i < children; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) {
                continue;
            }
            boolean clicked = clickLabel(child, labels, depth + 1);
            child.recycle();
            if (clicked) {
                return true;
            }
        }
        return false;
    }

    private boolean performClick(AccessibilityNodeInfo start) {
        if (start.isClickable() && start.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true;
        }
        AccessibilityNodeInfo parent = start.getParent();
        int depth = 0;
        while (parent != null && depth < 4) {
            if (parent.isClickable()) {
                boolean clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                parent.recycle();
                return clicked;
            }
            AccessibilityNodeInfo next = parent.getParent();
            parent.recycle();
            parent = next;
            depth++;
        }
        if (parent != null) {
            parent.recycle();
        }
        return false;
    }

    private static String labelOf(AccessibilityNodeInfo node) {
        StringBuilder builder = new StringBuilder();
        appendLabel(builder, node.getContentDescription());
        appendLabel(builder, node.getText());
        int children = Math.min(node.getChildCount(), 6);
        for (int i = 0; i < children; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) {
                continue;
            }
            appendLabel(builder, child.getContentDescription());
            appendLabel(builder, child.getText());
            child.recycle();
            if (builder.length() > 40) {
                break;
            }
        }
        return builder.toString().trim();
    }

    private static void appendLabel(StringBuilder builder, CharSequence value) {
        if (value == null || builder.length() > 40) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value.toString().toLowerCase());
    }

    private static boolean matches(String label, String[] expected) {
        if (label == null || label.isEmpty() || label.length() > 40) {
            return false;
        }
        for (String word : expected) {
            if (label.equals(word) || label.startsWith(word + " ") || label.startsWith(word + ",")) {
                return true;
            }
        }
        return false;
    }

    private static boolean idEndsAny(String id, String[] suffixes) {
        if (id == null) {
            return false;
        }
        for (String suffix : suffixes) {
            if (idEnds(id, suffix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean idContainsAny(String id, String[] parts) {
        if (id == null) {
            return false;
        }
        for (String part : parts) {
            if (id.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private static boolean idEnds(String id, String suffix) {
        return id != null && (id.endsWith("/" + suffix) || id.endsWith(":" + suffix));
    }
}
