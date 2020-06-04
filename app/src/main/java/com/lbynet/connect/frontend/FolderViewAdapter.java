package com.lbynet.connect.frontend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lbynet.connect.R;
import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.core.DataPool;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

public class FolderViewAdapter extends RecyclerView.Adapter<FolderViewAdapter.FileHolder> {

    ArrayList<File> filelist = new ArrayList<>();

    private String folderPath_;
    private boolean isFolderExist = true;
    private FileClickListener listener_;

    private int numFilesReceived = 0;

    public interface FileClickListener {
        boolean onFileClick(File file);
    }

    public FolderViewAdapter (String folderPath, FileClickListener listener) {

        if(listener != null) {
            listener_ = listener;
        }

        folderPath_ = folderPath;

        refresh();
    }

    public void refresh() {
        try {

            File [] files = new File(folderPath_).listFiles();

            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

            filelist = new ArrayList<>(Arrays.asList(files));


            isFolderExist = true;
        } catch (SecurityException e) {
            isFolderExist = false;
            //Folder does not exist, skip reading
        }

        notifyDataSetChanged();

        SAL.print("Refreshed");
    }

    public void setNumFilesReceived(int num) {
        numFilesReceived = num;
    }

    @NonNull
    @Override
    public FileHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutId = R.layout.file_item_view;

        View v = inflater.inflate(layoutId,parent,false);

        FileHolder vh = new FileHolder(v);

        return vh;
    }

    public boolean isIsFolderExist() {
        return isFolderExist;
    }

    @Override
    public void onBindViewHolder(@NonNull FileHolder holder, int position) {
        holder.bind(filelist.get(position));
    }

    @Override
    public int getItemCount() {
        return filelist.size();
    }

    class FileHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        FrameLayout parent;
        TextView tvFilename;
        TextView tvFullPath;
        TextView tvTimeModified;

        public FileHolder(@NonNull View itemView) {
            super(itemView);

            parent = (FrameLayout) itemView;
            tvFilename = itemView.findViewById(R.id.tv_filename);
            tvFullPath = itemView.findViewById(R.id.tv_hidden_full_path);
            tvTimeModified = itemView.findViewById(R.id.tv_file_modified_date);
            itemView.setOnClickListener(this);
        }

        public void bind(File file) {

            try {
                Date d = new Date(file.lastModified());
                String localizedDate = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Utils.getLocale()).format(d);

                tvFilename.setText(file.getName());
                tvFullPath.setText(file.getPath());
                tvTimeModified.setText(localizedDate);

                if(getAdapterPosition() < numFilesReceived) {

                   final View v = parent.findViewById(R.id.v_highlight);

                   new Thread ( () -> {

                       Utils.showView(DataPool.activity,v,1000);
                       Utils.sleepFor(1000);
                       Utils.hideView(DataPool.activity,v,true,1000);

                   }).start();
                }
                else {
                    //Reset it so that the highlighting animation only shows once
                    numFilesReceived = 0;
                }

            } catch (Exception e) {
                //Shhhhh
        }
        }

        @Override
        public void onClick(View v) {
            SAL.print("Clicked");

            int pos = getAdapterPosition();

            if(listener_ != null) {
                listener_.onFileClick(filelist.get(pos));
            }
        }
    }

}
