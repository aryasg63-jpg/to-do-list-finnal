package com.todoku.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TaskListFragment extends Fragment implements TaskAdapter.Listener {

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private TaskDb db;
    private TaskAdapter adapter;
    private List<Task> allTasks = new ArrayList<>();
    private String currentFilter = "all";

    private TextView tvEmptyState, tvClock, tvDate, tvNextTask;
    private TextView tvCountTotal, tvCountDone, tvTotalMinutes;
    private LinearLayout layoutFilterChips;
    private RecyclerView rvTasks;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new TaskDb(requireContext());

        rvTasks = view.findViewById(R.id.rvTasks);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        tvClock = view.findViewById(R.id.tvClock);
        tvDate = view.findViewById(R.id.tvDate);
        tvNextTask = view.findViewById(R.id.tvNextTask);
        tvCountTotal = view.findViewById(R.id.tvCountTotal);
        tvCountDone = view.findViewById(R.id.tvCountDone);
        tvTotalMinutes = view.findViewById(R.id.tvTotalMinutes);
        layoutFilterChips = view.findViewById(R.id.layoutFilterChips);

        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TaskAdapter(new ArrayList<>(), this);
        rvTasks.setAdapter(adapter);

        buildFilterChips();
        setupSwipeToDelete();

        startLiveClock();
        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        buildFilterChips();
        refresh();
    }

    @Override
    public void onDestroyView() {
        clockHandler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    // ---------- Jam real-time ----------
    private void startLiveClock() {
        Runnable tick = new Runnable() {
            @Override
            public void run() {
                Calendar now = Calendar.getInstance();
                tvClock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.getTime()));
                tvDate.setText(new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID")).format(now.getTime()));
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(tick);
    }

    // ---------- Filter chips kategori ----------
    private void buildFilterChips() {
        layoutFilterChips.removeAllViews();
        addChip("all", "🗂️ Semua");
        for (String cat : CategoryHelper.builtInCategories()) {
            addChip(cat, CategoryHelper.emojiFor(cat) + " " + CategoryHelper.labelFor(cat));
        }
        // Kategori custom yang muncul dari data
        for (Task t : allTasks) {
            if (CategoryHelper.isCustom(t.category)) addChip(t.category, "📌 " + t.category);
        }
    }

    private void addChip(String catKey, String label) {
        TextView chip = new TextView(requireContext());
        chip.setText(label);
        chip.setTextSize(13);
        chip.setPadding(32, 18, 32, 18);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(16);
        chip.setLayoutParams(lp);
        styleChip(chip, catKey.equals(currentFilter));
        chip.setOnClickListener(v -> {
            currentFilter = catKey;
            buildFilterChips();
            refresh();
        });
        layoutFilterChips.addView(chip);
    }

    private void styleChip(TextView chip, boolean active) {
        if (active) {
            chip.setBackgroundColor(0xFF6C5CE7);
            chip.setTextColor(0xFFFFFFFF);
        } else {
            chip.setBackgroundColor(requireContext().getColor(R.color.chip_bg));
            chip.setTextColor(requireContext().getColor(R.color.text_secondary));
        }
    }

    // ---------- Refresh data ----------
    private void refresh() {
        allTasks = db.getAll();

        // Majukan tugas berulang yang sudah lama terlewat
        for (Task t : allTasks) {
            long before = t.startTimeMillis;
            db.rollForwardRepeating(t);
            if (t.startTimeMillis != before) {
                AlarmScheduler.scheduleForTask(requireContext(), t);
            }
        }

        List<Task> filtered = new ArrayList<>();
        for (Task t : allTasks) {
            if (currentFilter.equals("all") || t.category.equals(currentFilter)) {
                filtered.add(t);
            }
        }
        adapter.updateData(filtered);
        tvEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvTasks.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);

        int done = 0, totalMinutes = 0;
        Task nextTask = null;
        long now = System.currentTimeMillis();
        for (Task t : allTasks) {
            if (t.done) done++;
            if (!t.done) totalMinutes += t.estimatedMinutes;
            long target = t.done ? Long.MAX_VALUE : t.startTimeMillis;
            if (t.isRepeating() && target < now) {
                target = t.firstOccurrenceOnOrAfter(now);
            }
            if (!t.done && target > now && (nextTask == null || target < nextTask.startTimeMillis)) {
                nextTask = t;
            }
        }
        tvCountTotal.setText(String.valueOf(allTasks.size()));
        tvCountDone.setText(String.valueOf(done));
        tvTotalMinutes.setText(formatMinutes(totalMinutes));

        if (nextTask != null) {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new java.util.Date(nextTask.startTimeMillis));
            tvNextTask.setText("📌 " + nextTask.title + " · " + time);
        } else {
            tvNextTask.setText("📌 Tidak ada tugas berikutnya");
        }
    }

    private String formatMinutes(int totalMin) {
        if (totalMin < 60) return totalMin + "m";
        return (totalMin / 60) + "j " + (totalMin % 60) + "m";
    }

    // ---------- Aksi list ----------
    @Override
    public void onToggleDone(Task t, boolean checked) {
        if (checked) {
            db.complete(t, true);
            if (t.isRepeating()) {
                Toast.makeText(requireContext(), "Selesai! 🔁 Lanjut ke jadwal berikutnya", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Kegiatan selesai 🎉", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (t.isRepeating()) {
                Toast.makeText(requireContext(), "Tugas berulang otomatis lanjut ke jadwal berikutnya", Toast.LENGTH_SHORT).show();
            } else {
                db.uncomplete(t);
                Toast.makeText(requireContext(), "Dibatalkan", Toast.LENGTH_SHORT).show();
            }
        }
        refresh();
    }

    @Override
    public void onEdit(Task t) {
        Intent i = new Intent(requireContext(), AddTaskActivity.class);
        i.putExtra(AddTaskActivity.EXTRA_TASK_ID, t.id);
        startActivity(i);
    }

    @Override
    public void onDelete(Task t) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Hapus kegiatan?")
                .setMessage(t.title)
                .setPositiveButton("Hapus", (d, w) -> removeTaskWithUndo(t))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void removeTaskWithUndo(Task t) {
        AlarmScheduler.cancelForTask(requireContext(), t.id);
        db.delete(t.id);
        refresh();

        Snackbar sb = Snackbar.make(rvTasks, "Kegiatan dihapus", Snackbar.LENGTH_LONG)
                .setAction("Batal", v -> {
                    t.id = db.insertOrUpdate(t);
                    AlarmScheduler.scheduleForTask(requireContext(), t);
                    refresh();
                });
        sb.show();
    }

    // ---------- Swipe untuk hapus ----------
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback cb = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getBindingAdapterPosition();
                Task t = adapter.getItem(pos);
                if (t != null) removeTaskWithUndo(t);
                else refresh();
            }
        };
        new ItemTouchHelper(cb).attachToRecyclerView(rvTasks);
    }
}