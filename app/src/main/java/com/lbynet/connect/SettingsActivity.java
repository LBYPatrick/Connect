package com.lbynet.connect;

import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import com.lbynet.connect.backend.Utils;

import jp.wasabeef.blurry.Blurry;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        ImageView bkgr = findViewById(R.id.iv_bkgnd);

        ((ImageView)findViewById(R.id.iv_bkgnd)).setImageBitmap(Utils.getWallpaper(this));

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Blurry.with(this).async().radius(30).sampling(5).color(Color.argb(30, 0, 0, 0)).from(Utils.getWallpaper(this)).into(bkgr);

    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.connect_settings, rootKey);
        }
    }
}