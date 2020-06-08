package com.lbynet.connect;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.core.DataPool;
import com.lbynet.connect.backend.core.FileManager;
import com.lbynet.connect.frontend.FolderViewAdapter;
import com.lbynet.connect.frontend.Visualizer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import jp.wasabeef.blurry.Blurry;

public class ReceivedActivity extends AppCompatActivity {

    ArrayList<File> files;
    FolderViewAdapter adapter;
    boolean isFirstOnResume = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        DataPool.activity = this;

        if(Utils.isDarkMode(this)) {
            setTheme(R.style.AppTheme_Dark);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.received);


        //Process data
        ArrayList<String> fileData = getIntent().getStringArrayListExtra("received_files");

        //Setup RecyclerView
        RecyclerView rv = findViewById(R.id.rv_filelist);
        rv.setLayoutManager(new LinearLayoutManager(this));

        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        adapter = new FolderViewAdapter(Utils.getOutputPath(), this::onFileClick);

        if(fileData != null) {
            adapter.setNumFilesReceived(fileData.size());
        }

        rv.setAdapter(adapter);

        Blurry.with(this).radius(10).sampling(10).from(Utils.getWallpaper(this)).into(findViewById(R.id.iv_master_background));
        TextView tvDirNote = findViewById(R.id.tv_download_directory);

        tvDirNote.setText(String.format(tvDirNote.getText().toString(),Utils.getOutputPath()));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(adapter != null && !isFirstOnResume) {
            adapter.refresh();
        }
        else {
            isFirstOnResume = false;
        }
    }

    public boolean onFileClick(File file) {


        try {
            Uri uri = FileProvider.getUriForFile(this,  "com.lbynet.connect.fileprovider", file);

            /*
            String name = file.getName(),
                    path = file.getPath();
            SAL.print(name + ", " + path);
             */

            ArrayList<Uri> uris = new ArrayList<>();

            uris.add(uri);

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM,uri);
            sendIntent.setType(getContentResolver().getType(uri));
            sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent shareIntent = Intent.createChooser(sendIntent,null);

            startActivityForResult(shareIntent,1);
            //Utils.printToast(this,name + "," + path,false);

        } catch (Exception e) {
            SAL.print(e);
        }
        return true;
    }


}