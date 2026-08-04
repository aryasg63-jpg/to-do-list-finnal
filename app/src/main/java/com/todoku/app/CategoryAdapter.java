package com.todoku.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

    public interface Listener {
        void onEdit(Category c);
        void onDelete(Category c);
    }

    private List<Category> items;
    private final Listener listener;

    public CategoryAdapter(List<Category> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateData(List<Category> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Category c = items.get(position);
        h.tvEmoji.setText(c.emoji);
        h.tvLabel.setText(c.label + (c.isDefault ? "" : "  ·  custom"));
        h.vColorDot.getBackground().mutate().setTint(android.graphics.Color.parseColor(c.colorHex));

        h.btnEditCat.setOnClickListener(v -> listener.onEdit(c));

        if (c.isDefault) {
            h.btnDeleteCat.setVisibility(View.INVISIBLE); // kategori bawaan tidak bisa dihapus
        } else {
            h.btnDeleteCat.setVisibility(View.VISIBLE);
            h.btnDeleteCat.setOnClickListener(v -> listener.onDelete(c));
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvLabel;
        View vColorDot;
        ImageButton btnEditCat, btnDeleteCat;

        VH(View v) {
            super(v);
            tvEmoji = v.findViewById(R.id.tvEmoji);
            tvLabel = v.findViewById(R.id.tvLabel);
            vColorDot = v.findViewById(R.id.vColorDot);
            btnEditCat = v.findViewById(R.id.btnEditCat);
            btnDeleteCat = v.findViewById(R.id.btnDeleteCat);
        }
    }
}
