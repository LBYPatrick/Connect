package com.lbynet.connect;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.core.DataPool;

public class SettingsActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        DataPool.activity = this;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ((ImageView)findViewById(R.id.iv_bkgnd_clear)).setImageBitmap(Utils.getWallpaper(this));

        ImageView bkgr = findViewById(R.id.iv_background);
        View master = findViewById(R.id.master);

        //Utils.getBackground(this).into(bkgr);

        Utils.showView(this,bkgr,500);

        Utils.hideView(master,false,0);
        Utils.showView(master,500);

    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.connect_settings, rootKey);
        }
    }
}