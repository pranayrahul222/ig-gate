package com.rahulpranaya.iggate;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class MainActivity extends Activity {
    private static final String INSTAGRAM = "com.instagram.android";

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.status);
        Button allow = findViewById(R.id.allow_access);
        Button open = findViewById(R.id.open_instagram);
        Button share = findViewById(R.id.share_app);
        allow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAccessibilitySettings();
            }
        });
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openInstagram();
            }
        });
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareApp();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        status.setText(guardEnabled() ? R.string.status_on : R.string.status_off);
    }

    private boolean guardEnabled() {
        String enabled = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) {
            return false;
        }
        String flat = new ComponentName(this, GateService.class).flattenToString();
        return enabled.toLowerCase().contains(flat.toLowerCase());
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (ActivityNotFoundException notFound) {
            Toast.makeText(this, R.string.accessibility_missing, Toast.LENGTH_LONG).show();
        }
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

    private void openInstagram() {
        PackageManager packages = getPackageManager();
        Intent launch = packages.getLaunchIntentForPackage(INSTAGRAM);
        if (launch == null) {
            Toast.makeText(this, R.string.instagram_missing, Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(launch);
    }
}
