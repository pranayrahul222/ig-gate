package com.rahulpranaya.iggate;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Opens Instagram or YouTube in this app and refuses Reels, Search, and Shorts pages. */
public final class BrowserActivity extends Activity {
    static final String EXTRA_START = "start";

    private static final String CHROME_MOBILE =
            "Mozilla/5.0 (Linux; Android 14; Pixel 9a) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36";

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(CHROME_MOBILE);
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(web, true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null || !request.isForMainFrame()) {
                    return false;
                }
                return steer(view, request.getUrl().toString());
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                steer(view, url);
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                steer(view, url);
            }
        });
        String start = getIntent().getStringExtra(EXTRA_START);
        if (start == null || UrlPolicy.decide(start) != UrlPolicy.Decision.ALLOW) {
            start = UrlPolicy.INSTAGRAM_HOME;
        }
        web.loadUrl(start);
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
            return;
        }
        super.onBackPressed();
    }

    /** @return true when this app refused the page */
    private boolean steer(WebView view, String url) {
        String next = replacement(url);
        if (next == null) {
            return false;
        }
        if (next.isEmpty()) {
            view.stopLoading();
            return true;
        }
        view.post(new Runnable() {
            @Override
            public void run() {
                if (web != null) {
                    web.loadUrl(next);
                }
            }
        });
        return true;
    }

    /** Allowed pages return null. A blocked page with nowhere to send the user returns empty. */
    private static String replacement(String url) {
        switch (UrlPolicy.decide(url)) {
            case ALLOW:
                return null;
            case INSTAGRAM_HOME:
                return UrlPolicy.INSTAGRAM_HOME;
            case INSTAGRAM_INBOX:
                return UrlPolicy.INSTAGRAM_INBOX;
            case YOUTUBE_HOME:
                return UrlPolicy.YOUTUBE_HOME;
            case BLOCK:
            default:
                return "";
        }
    }
}
