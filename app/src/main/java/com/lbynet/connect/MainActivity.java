package com.lbynet.connect;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lbynet.connect.backend.DataPool;
import com.lbynet.connect.backend.IO;
import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;

public class MainActivity extends AppCompatActivity {

    private TextView tvMime;
    private ProgressBar pb;
    private ImageView ivImage;

    void grantPermissions() {
        String [] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        };

        for(String p : permissions) {
            if(checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String [] {p}, 1);
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

    @Override
    protected void onResume() {
        super.onResume();
        DataPool.timesRun += 1;
        //new LoadResult(this).execute(getIntent());
    }

    public class LoadResult extends AsyncTask<Intent,Void, Boolean> {

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
        protected Boolean doInBackground(Intent... intents) {

            Intent intent = intents[0];

            if(intent.getType() == null) {

                DataPool.timesRun += 1;

                msg_ = "Share Intent not detected. Do not launch this app directly." + DataPool.timesRun;
            } else {
                msg_ = "Type: " + getIntent().getType() + "\n";

                //Multiple Files
                if(intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
                    msg_ += "Info:\n";

                    ArrayList<Uri> arr = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

                    for(Uri uri : arr) {
                        msg_ += "URI: " + uri.toString() + "\n" +
                                    "\tScheme: " + uri.getScheme() + "\n" +
                                    "\tReal Path:" + Utils.getPath(context_,uri) + "\n\n";
                    }
                }
                //Single File
                else {

                    Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

                    SAL.print("Scheme: " + uri.getScheme() + "\n"
                            + "Query: " + uri.getQuery() + "\n"
                            + "Path: " + Utils.getPath(context_,uri));


                    if(intent.getType().startsWith("image/")) {

                        image_ = uri;
                        msg_ = "";

                        return true;
                    }
                    else {
                        msg_ += "URI: " + uri.toString() + "\n" +
                                "\tScheme: " + uri.getScheme() + "\n" +
                                "\tReal Path:" + Utils.getPath(context_,uri) + "\n\n";
                    }
                }
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean s) {

            try {

                if (s.equals(true)) {
                    ivImage.setImageURI(image_);
                    ivImage.setVisibility(View.VISIBLE);
                } else {
                    tvMime.setText(msg_);
                    tvMime.setVisibility(View.VISIBLE);
                }

                pb.setVisibility(View.INVISIBLE);
            } catch (Exception e) {
                SAL.print(e);
            }

        }
    }

}
