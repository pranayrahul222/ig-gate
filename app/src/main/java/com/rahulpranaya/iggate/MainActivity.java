package com.rahulpranaya.iggate;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button openInstagram = findViewById(R.id.open_instagram);
        Button openYouTube = findViewById(R.id.open_youtube);
        Button share = findViewById(R.id.share_app);
        openInstagram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSite(UrlPolicy.INSTAGRAM_HOME);
            }
        });
        openYouTube.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSite(UrlPolicy.YOUTUBE_HOME);
            }
        });
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareApp();
            }
        });
    }

    private void openSite(String url) {
        Intent browser = new Intent(this, BrowserActivity.class);
        browser.putExtra(BrowserActivity.EXTRA_START, url);
        startActivity(browser);
    }

    private void shareApp() {
        File dest = new File(getCacheDir(), "IG-Gate.apk");
        try {
            copy(new File(getApplicationInfo().sourceDir), dest);
        } catch (IOException failed) {
            Toast.makeText(this, R.string.share_failed, Toast.LENGTH_LONG).show();
            return;
        }
        Uri uri = new Uri.Builder()
                .scheme("content")
                .authority(ApkProvider.AUTHORITY)
                .appendPath("IG-Gate.apk")
                .build();
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/vnd.android.package-archive");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_title));
        share.setClipData(ClipData.newRawUri(getString(R.string.share_title), uri));
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent chooser = Intent.createChooser(share, getString(R.string.share_title));
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(chooser);
    }

    private static void copy(File source, File dest) throws IOException {
        InputStream in = new FileInputStream(source);
        try {
            OutputStream out = new FileOutputStream(dest);
            try {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}
