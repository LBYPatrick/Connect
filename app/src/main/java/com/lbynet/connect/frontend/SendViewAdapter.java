package com.lbynet.connect.frontend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.lbynet.connect.R;
import com.lbynet.connect.backend.core.DataPool;
import com.lbynet.connect.backend.networking.Pairing;

import java.util.ArrayList;

public class SendViewAdapter extends RecyclerView.Adapter<SendViewAdapter.DeviceHolder> {

    ArrayList<Pairing.Device> pairedDevices = new ArrayList<>();

    @NonNull
    @Override
    public DeviceHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutId = R.layout.target_button;

        View v = inflater.inflate(layoutId,parent,false);

        DeviceHolder vh = new SendViewAdapter.DeviceHolder(v);

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceHolder holder, int position) {

    }

    public void refresh() {

        boolean isChanged = Pairing.getFilteredDevices(pairedDevices, DataPool.DEVICE_LIST_REFRESH_INTERVAL * 2);

        if(isChanged) {
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return pairedDevices.size();
    }

    public static class DeviceHolder extends RecyclerView.ViewHolder {

        private CardView parent;
        private TextView uidView,
                         ipView,
                         speedView;
        private ProgressBar progressBar;
        private Button cancelButton;

        public DeviceHolder(@NonNull View itemView) {
            super(itemView);

            parent = (CardView)itemView;

        }
    }
}
