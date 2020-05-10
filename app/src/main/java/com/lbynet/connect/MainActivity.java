package com.lbynet.connect;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lbynet.connect.backend.networking.FileSender;
import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TextView tvMime;
    private ProgressBar pb;
    private ImageView ivImage;

    void grantPermissions() {
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        };

        for (String p : permissions) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{p}, 1);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        grantPermissions();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvMime = findViewById(R.id.tv_mime);
        pb = findViewById(R.id.pb_loading);

        ivImage = findViewById(R.id.iv_image);

        new LoadResult(this).execute(getIntent());

    }

    public class LoadResult extends AsyncTask<Intent, Void, Void> {

        String msg_ = "";
        Uri image_;
        Context context_;


        public LoadResult(Context context) {
            context_ = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tvMime.setVisibility(View.INVISIBLE);
            pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Intent... intents) {

            Intent intent = intents[0];

            if (intent.getType() == null) {

                msg_ = "Share Intent not detected. Do not launch this app directly.";

            } else if (intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {

                msg_ = "Type: " + getIntent().getType() + "\n\n" +
                        "Info:\n";

                ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                String[] paths = new String[uris.size()];

                for (int i = 0; i < uris.size(); ++i) {
                    msg_ += "URI: " + uris.get(i).toString() + "\n" +
                            "\tScheme: " + uris.get(i).getScheme() + "\n" +
                            "\tReal Path:" + Utils.getPath(context_, uris.get(i)) + "\n\n";

                    paths[i] = Utils.getPath(context_, uris.get(i));
                }
                runOnUiThread(() -> {
                    new FileSender("192.168.1.182", paths).start();
                });

            }
            //Single File
            else if (intent.getAction().equals(Intent.ACTION_SEND)) {

                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

                SAL.print("Scheme: " + uri.getScheme() + "\n"
                        + "Query: " + uri.getQuery() + "\n"
                        + "Path: " + Utils.getPath(context_, uri));

                runOnUiThread(() -> {
                    new FileSender("192.168.1.182", Utils.getPath(context_, uri)).start();
                });

                msg_ += "URI: " + uri.toString() + "\n" +
                        "\tScheme: " + uri.getScheme() + "\n" +
                        "\tReal Path:" + Utils.getPath(context_, uri) + "\n\n";
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            try {

                tvMime.setText(msg_);
                tvMime.setVisibility(View.VISIBLE);

                pb.setVisibility(View.INVISIBLE);
            } catch (Exception e) {
                SAL.print(e);
            }

        }
    }

}
