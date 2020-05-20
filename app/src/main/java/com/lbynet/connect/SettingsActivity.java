package com.lbynet.connect;

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

        //((ImageView)findViewById(R.id.iv_bkgnd)).setImageBitmap(Utils.getWallpaper(this));

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Blurry.with(this).radius(100).sampling(1).from(Utils.getWallpaper(this)).into(findViewById(R.id.iv_bkgnd));

    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.connect_settings, rootKey);
        }
    }
}