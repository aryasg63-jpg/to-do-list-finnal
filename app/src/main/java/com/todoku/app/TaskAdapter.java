package com.todoku.app;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {

    public interface Listener {
        void onToggleDone(Task t, boolean done);
        void onEdit(Task t);
        void onDelete(Task t);
    }

    private List<Task> items;
    private final Listener listener;
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public TaskAdapter(List<Task> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateData(List<Task> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Task t = items.get(position);

        h.tvTitle.setText(t.title);
        h.tvCategory.setText(CategoryHelper.emojiFor(t.category) + " " + CategoryHelper.labelFor(t.category));
        h.tvCategory.setBackgroundColor(CategoryHelper.colorFor(t.category));
        h.stripe.setBackgroundColor(CategoryHelper.colorFor(t.category));
        h.tvTime.setText("⏰ " + timeFmt.format(new java.util.Date(t.startTimeMillis)));

        h.priorityDot.getBackground().mutate().setTint(android.graphics.Color.parseColor(Priority.colorHex(t.priority)));
        h.tvRecurringBadge.setVisibility(t.isRecurring() ? View.VISIBLE : View.GONE);

        h.tvCountdown.setText(formatCountdown(t.startTimeMillis, t.done));

        h.cbDone.setOnCheckedChangeListener(null);
        h.cbDone.setChecked(t.done);
        if (t.done) {
            h.tvTitle.setPaintFlags(h.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            h.tvTitle.setPaintFlags(h.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
        h.cbDone.setOnCheckedChangeListener((btn, checked) -> listener.onToggleDone(t, checked));

        h.btnEdit.setOnClickListener(v -> listener.onEdit(t));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(t));
    }

    private String formatCountdown(long startMillis, boolean done) {
        if (done) return "✓ Selesai";
        long diff = startMillis - System.currentTimeMillis();
        if (diff < 0) return "⚠️ Sudah lewat waktu";
        long totalMin = diff / 60000;
        long h = totalMin / 60;
        long m = totalMin % 60;
        if (h > 0) return "mulai dalam " + h + " jam " + m + " menit";
        if (m > 0) return "mulai dalam " + m + " menit";
        return "mulai sebentar lagi!";
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CardView card;
        View stripe, priorityDot;
        CheckBox cbDone;
        TextView tvTitle, tvCategory, tvTime, tvCountdown, tvRecurringBadge;
        ImageButton btnEdit, btnDelete;

        VH(View v) {
            super(v);
            card = (CardView) v;
            stripe = v.findViewById(R.id.vCatStripe);
            priorityDot = v.findViewById(R.id.vPriorityDot);
            cbDone = v.findViewById(R.id.cbDone);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvCategory = v.findViewById(R.id.tvCategory);
            tvTime = v.findViewById(R.id.tvTime);
            tvCountdown = v.findViewById(R.id.tvCountdown);
            tvRecurringBadge = v.findViewById(R.id.tvRecurringBadge);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
