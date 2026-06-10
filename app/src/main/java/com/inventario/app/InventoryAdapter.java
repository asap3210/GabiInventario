package com.inventario.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.VH> {

    private final List<InventoryItem> items = new ArrayList<>();

    public void setItems(List<InventoryItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        InventoryItem it = items.get(position);
        h.object.setText(it.objectCode);
        h.room.setText("Sala: " + it.roomCode);
        String meta = "Últ.: " + it.lastSeen + "  ·  escaneos: " + it.scanCount;
        h.meta.setText(meta);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView object;
        final TextView room;
        final TextView meta;

        VH(@NonNull View itemView) {
            super(itemView);
            object = itemView.findViewById(R.id.tvObject);
            room = itemView.findViewById(R.id.tvRoom);
            meta = itemView.findViewById(R.id.tvMeta);
        }
    }
}
