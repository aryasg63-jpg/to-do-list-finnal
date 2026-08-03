package com.todoku.app;

import android.content.Context;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {

    public interface Listener {
        void onToggleDone(Task t, boolean done);
        void onEdit(Task t);
        void onDelete(Task t);
    }

    private final List<Task> items;
    private final Listener listener;
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("EEE, d MMM", new Locale("id", "ID"));

    public TaskAdapter(List<Task> items, Listener listener) {
        this.items = new ArrayList<>(items);
        this.listener = listener;
    }

    public void updateData(List<Task> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public Task getItem(int position) {
        if (position < 0 || position >= items.size()) return null;
        return items.get(position);
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

        h.tvDateTime.setText(dayLabel(t.startTimeMillis) + " · " + timeFmt.format(new Date(t.startTimeMillis)));

        if (t.isRepeating()) {
            h.tvRepeat.setVisibility(View.VISIBLE);
            h.tvRepeat.setText("⟳ " + Task.repeatLabel(t));
        } else {
            h.tvRepeat.setVisibility(View.GONE);
        }

        h.tvCountdown.setText(formatCountdown(t));

        if (t.note != null && !t.note.isEmpty()) {
            h.tvNote.setVisibility(View.VISIBLE);
            h.tvNote.setText(t.note);
        } else {
            h.tvNote.setVisibility(View.GONE);
        }

        h.cbDone.setOnCheckedChangeListener(null);
        h.cbDone.setChecked(t.done);
        if (t.done) {
            h.tvTitle.setPaintFlags(h.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            h.tvTitle.setAlpha(0.55f);
        } else {
            h.tvTitle.setPaintFlags(h.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            h.tvTitle.setAlpha(1f);
        }
        h.cbDone.setOnCheckedChangeListener((btn, checked) -> listener.onToggleDone(t, checked));

        h.btnEdit.setOnClickListener(v -> listener.onEdit(t));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(t));
    }

    /** Label hari relatif: Hari ini / Besok / Kemarin / EEE, d MMM. */
    public static String dayLabel(long startMillis) {
        long today = Task.startOfDay(System.currentTimeMillis());
        long startDay = Task.startOfDay(startMillis);
        long diff = (startDay - today) / Task.DAY_MS;
        if (diff == 0) return "Hari ini";
        if (diff == 1) return "Besok";
        if (diff == -1) return "Kemarin";
        return new SimpleDateFormat("EEE, d MMM", new Locale("id", "ID")).format(new Date(startMillis));
    }

    private String formatCountdown(Task t) {
        if (t.done) return "✓ Selesai";
        long target = t.startTimeMillis;
        if (t.isRepeating() && target < System.currentTimeMillis()) {
            target = t.firstOccurrenceOnOrAfter(System.currentTimeMillis());
        }
        long diff = target - System.currentTimeMillis();
        if (diff < -60_000L) return "⚠️ Sudah lewat waktu";
        if (diff < 0) return "Mulai sekarang!";
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
        View stripe;
        CheckBox cbDone;
        TextView tvTitle, tvCategory, tvDateTime, tvRepeat, tvCountdown, tvNote;
        ImageButton btnEdit, btnDelete;

        VH(View v) {
            super(v);
            stripe = v.findViewById(R.id.vCatStripe);
            cbDone = v.findViewById(R.id.cbDone);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvCategory = v.findViewById(R.id.tvCategory);
            tvDateTime = v.findViewById(R.id.tvDateTime);
            tvRepeat = v.findViewById(R.id.tvRepeat);
            tvCountdown = v.findViewById(R.id.tvCountdown);
            tvNote = v.findViewById(R.id.tvNote);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}