package com.todoku.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TaskEditDialogFragment extends DialogFragment {

    public static final String RESULT_KEY = "task_edit_result";
    private static final String ARG_TASK_ID = "arg_task_id";

    public static TaskEditDialogFragment newInstance(long taskId) {
        TaskEditDialogFragment f = new TaskEditDialogFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_TASK_ID, taskId);
        f.setArguments(b);
        return f;
    }

    private TaskDb taskDb;
    private RecurringTemplateDb templateDb;
    private ReminderRuleDb reminderRuleDb;

    private final Calendar pendingDateTime = Calendar.getInstance();
    private String pendingSoundUri = null;
    private long editingTaskId = 0;
    private long editingTemplateId = 0;
    private boolean wasRecurringInstance = false;
    private int recurrenceDaysBitmask = 0;
    private final List<ReminderRule> pendingReminderRules = new ArrayList<>();
    private MediaPlayer previewPlayer;

    // View refs dipakai lintas listener
    private Spinner spCategory, spPrepMinutes;
    private RadioGroup rgPriority, rgRecurrenceType;
    private Switch switchRecurring;
    private LinearLayout layoutOneTimeDate, layoutRecurringOptions, layoutDayPicker, layoutReminderRules;
    private Button btnPickDate, btnPickStartTime, btnAddReminderRule;
    private TextView tvSelectedSound;
    private Button btnPreviewSound;
    private List<Category> categoryList;

    private final ActivityResultLauncher<String[]> audioPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    requireContext().getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) { }
                pendingSoundUri = uri.toString();
                tvSelectedSound.setText("🎵 " + queryFileName(uri));
                btnPreviewSound.setVisibility(View.VISIBLE);
            });

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        taskDb = new TaskDb(requireContext());
        templateDb = new RecurringTemplateDb(requireContext());
        reminderRuleDb = new ReminderRuleDb(requireContext());

        long taskId = getArguments() != null ? getArguments().getLong(ARG_TASK_ID, 0) : 0;

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null);

        EditText etTitle = view.findViewById(R.id.etTitle);
        spCategory = view.findViewById(R.id.spCategory);
        rgPriority = view.findViewById(R.id.rgPriority);
        switchRecurring = view.findViewById(R.id.switchRecurring);
        layoutOneTimeDate = view.findViewById(R.id.layoutOneTimeDate);
        layoutRecurringOptions = view.findViewById(R.id.layoutRecurringOptions);
        rgRecurrenceType = view.findViewById(R.id.rgRecurrenceType);
        layoutDayPicker = view.findViewById(R.id.layoutDayPicker);
        btnPickDate = view.findViewById(R.id.btnPickDate);
        btnPickStartTime = view.findViewById(R.id.btnPickStartTime);
        EditText etDuration = view.findViewById(R.id.etDuration);
        CheckBox cbPrepAlarm = view.findViewById(R.id.cbPrepAlarm);
        spPrepMinutes = view.findViewById(R.id.spPrepMinutes);
        CheckBox cbStartAlarm = view.findViewById(R.id.cbStartAlarm);
        layoutReminderRules = view.findViewById(R.id.layoutReminderRules);
        btnAddReminderRule = view.findViewById(R.id.btnAddReminderRule);
        Button btnPickSound = view.findViewById(R.id.btnPickSound);
        tvSelectedSound = view.findViewById(R.id.tvSelectedSound);
        btnPreviewSound = view.findViewById(R.id.btnPreviewSound);
        Button btnDeleteTask = view.findViewById(R.id.btnDeleteTask);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        // ---------- Kategori ----------
        categoryList = CategoryHelper.allCategoryObjects();
        List<String> catLabels = new ArrayList<>();
        for (Category c : categoryList) catLabels.add(c.emoji + " " + c.label);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, catLabels);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);

        // ---------- Prep minutes ----------
        List<String> prepOptions = List.of("5 menit", "10 menit", "15 menit", "30 menit", "60 menit");
        List<Integer> prepValues = List.of(5, 10, 15, 30, 60);
        ArrayAdapter<String> prepAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, prepOptions);
        prepAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPrepMinutes.setAdapter(prepAdapter);

        buildDayPickerChips();

        pendingDateTime.setTimeInMillis(System.currentTimeMillis());
        pendingDateTime.add(Calendar.HOUR_OF_DAY, 1);
        pendingDateTime.set(Calendar.SECOND, 0);

        String dialogTitle = "Tambah Kegiatan";

        // ---------- Muat data kalau mode edit ----------
        if (taskId > 0) {
            Task existingTask = taskDb.getById(taskId);
            if (existingTask != null) {
                if (existingTask.isRecurring()) {
                    // Edit sebuah instance recurring -> yang diedit sebenarnya TEMPLATE-nya
                    wasRecurringInstance = true;
                    editingTemplateId = existingTask.templateId;
                    RecurringTemplate tmpl = templateDb.getById(existingTask.templateId);
                    if (tmpl != null) {
                        dialogTitle = "Edit Kegiatan Berulang";
                        etTitle.setText(tmpl.title);
                        selectCategoryInSpinner(tmpl.category);
                        selectPriorityRadio(tmpl.priority);
                        etDuration.setText(String.valueOf(tmpl.estimatedMinutes));
                        cbPrepAlarm.setChecked(tmpl.prepAlarmEnabled);
                        cbStartAlarm.setChecked(tmpl.alarmEnabled);
                        selectPrepSpinner(prepValues, tmpl.prepMinutesBefore);
                        pendingSoundUri = tmpl.soundUri;
                        if (pendingSoundUri != null) {
                            tvSelectedSound.setText("🎵 " + queryFileName(Uri.parse(pendingSoundUri)));
                            btnPreviewSound.setVisibility(View.VISIBLE);
                        }
                        pendingDateTime.set(Calendar.HOUR_OF_DAY, tmpl.hour);
                        pendingDateTime.set(Calendar.MINUTE, tmpl.minute);

                        switchRecurring.setChecked(true);
                        layoutOneTimeDate.setVisibility(View.GONE);
                        layoutRecurringOptions.setVisibility(View.VISIBLE);
                        if ("WEEKLY".equals(tmpl.recurrenceType)) {
                            ((android.widget.RadioButton) view.findViewById(R.id.rbWeekly)).setChecked(true);
                            layoutDayPicker.setVisibility(View.VISIBLE);
                            recurrenceDaysBitmask = tmpl.recurrenceDays;
                            refreshDayPickerVisualState();
                        }
                    }
                } else {
                    editingTaskId = existingTask.id;
                    dialogTitle = "Edit Kegiatan";
                    etTitle.setText(existingTask.title);
                    selectCategoryInSpinner(existingTask.category);
                    selectPriorityRadio(existingTask.priority);
                    etDuration.setText(String.valueOf(existingTask.estimatedMinutes));
                    cbPrepAlarm.setChecked(existingTask.prepAlarmEnabled);
                    cbStartAlarm.setChecked(existingTask.alarmEnabled);
                    selectPrepSpinner(prepValues, existingTask.prepMinutesBefore);
                    pendingSoundUri = existingTask.soundUri;
                    if (pendingSoundUri != null) {
                        tvSelectedSound.setText("🎵 " + queryFileName(Uri.parse(pendingSoundUri)));
                        btnPreviewSound.setVisibility(View.VISIBLE);
                    }
                    pendingDateTime.setTimeInMillis(existingTask.startTimeMillis);

                    pendingReminderRules.clear();
                    pendingReminderRules.addAll(reminderRuleDb.getByTask(existingTask.id));
                    renderReminderRuleList();
                }
                btnDeleteTask.setVisibility(View.VISIBLE);
                btnDeleteTask.setText(wasRecurringInstance ? "Hapus Pengulangan" : "Hapus");
            }
        } else {
            spPrepMinutes.setSelection(1); // default 10 menit
            layoutOneTimeDate.setVisibility(View.VISIBLE);
            layoutRecurringOptions.setVisibility(View.GONE);
        }

        updateDateButtonLabel();
        updateTimeButtonLabel();
        updateReminderSectionVisibility();

        // ---------- Listener toggle recurring ----------
        switchRecurring.setOnCheckedChangeListener((btn, checked) -> {
            layoutOneTimeDate.setVisibility(checked ? View.GONE : View.VISIBLE);
            layoutRecurringOptions.setVisibility(checked ? View.VISIBLE : View.GONE);
            updateReminderSectionVisibility();
        });

        rgRecurrenceType.setOnCheckedChangeListener((group, checkedId) -> {
            layoutDayPicker.setVisibility(checkedId == R.id.rbWeekly ? View.VISIBLE : View.GONE);
        });

        // ---------- Date & time picker ----------
        btnPickDate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (dp, year, month, day) -> {
                pendingDateTime.set(year, month, day);
                updateDateButtonLabel();
            }, pendingDateTime.get(Calendar.YEAR), pendingDateTime.get(Calendar.MONTH), pendingDateTime.get(Calendar.DAY_OF_MONTH))
                    .show();
        });

        btnPickStartTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (tp, hour, minute) -> {
                pendingDateTime.set(Calendar.HOUR_OF_DAY, hour);
                pendingDateTime.set(Calendar.MINUTE, minute);
                updateTimeButtonLabel();
            }, pendingDateTime.get(Calendar.HOUR_OF_DAY), pendingDateTime.get(Calendar.MINUTE), true).show();
        });

        // ---------- Suara ----------
        btnPickSound.setOnClickListener(v ->
                audioPickerLauncher.launch(new String[]{"audio/mpeg", "audio/opus", "audio/ogg", "audio/*"}));

        btnPreviewSound.setOnClickListener(v -> {
            if (pendingSoundUri == null) return;
            try {
                if (previewPlayer != null) { previewPlayer.release(); previewPlayer = null; }
                previewPlayer = new MediaPlayer();
                previewPlayer.setDataSource(requireContext(), Uri.parse(pendingSoundUri));
                previewPlayer.setOnPreparedListener(MediaPlayer::start);
                previewPlayer.setOnCompletionListener(mp -> { mp.release(); previewPlayer = null; });
                previewPlayer.prepareAsync();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Tidak bisa memutar file ini", Toast.LENGTH_SHORT).show();
            }
        });

        // ---------- Pengingat custom ----------
        btnAddReminderRule.setOnClickListener(v -> openAddReminderRuleDialog());

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(dialogTitle)
                .setView(view)
                .create();

        btnCancel.setOnClickListener(v -> dismiss());

        btnDeleteTask.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(wasRecurringInstance ? "Hapus seluruh pengulangan?" : "Hapus kegiatan?")
                    .setMessage(wasRecurringInstance
                            ? "Semua kejadian mendatang dari kegiatan berulang ini akan dihapus. Riwayat yang sudah lewat tetap tersimpan."
                            : "Kegiatan ini akan dihapus permanen.")
                    .setPositiveButton("Hapus", (d, w) -> {
                        performDelete();
                        notifyResultAndDismiss();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Nama kegiatan tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }
            int durationMin = 0;
            try { durationMin = Integer.parseInt(etDuration.getText().toString().trim()); } catch (Exception ignored) { }

            String category = categoryList.get(spCategory.getSelectedItemPosition()).key;
            int priority = selectedPriority();
            int prepMinutes = prepValues.get(spPrepMinutes.getSelectedItemPosition());
            boolean prepEnabled = cbPrepAlarm.isChecked();
            boolean startEnabled = cbStartAlarm.isChecked();

            if (switchRecurring.isChecked()) {
                saveAsRecurring(title, category, priority, durationMin, prepMinutes, prepEnabled, startEnabled);
            } else {
                saveAsOneTime(title, category, priority, durationMin, prepMinutes, prepEnabled, startEnabled);
            }

            notifyResultAndDismiss();
        });

        return dialog;
    }

    // ---------- Simpan: sekali jalan ----------
    private void saveAsOneTime(String title, String category, int priority, int durationMin,
                                int prepMinutes, boolean prepEnabled, boolean startEnabled) {
        // Kalau sebelumnya recurring lalu diubah jadi sekali-jalan, bersihkan template lamanya
        if (wasRecurringInstance && editingTemplateId > 0) {
            RecurrenceGenerator.regenerateAfterTemplateChangeToNone(requireContext(), editingTemplateId);
            templateDb.delete(editingTemplateId);
        }

        Task t = new Task();
        t.id = editingTaskId;
        t.title = title;
        t.category = category;
        t.priority = priority;
        t.startTimeMillis = pendingDateTime.getTimeInMillis();
        t.instanceDateEpochDay = DateUtil.epochDayOf(t.startTimeMillis);
        t.prepMinutesBefore = prepMinutes;
        t.prepAlarmEnabled = prepEnabled;
        t.alarmEnabled = startEnabled;
        t.soundUri = pendingSoundUri;
        t.estimatedMinutes = durationMin;
        t.done = false;

        long newId = taskDb.insertOrUpdate(t);
        t.id = newId;

        reminderRuleDb.deleteByTask(newId);
        for (ReminderRule r : pendingReminderRules) {
            r.taskId = newId;
            reminderRuleDb.insert(r);
        }

        AlarmScheduler.scheduleForTask(requireContext(), t);
        maybeWarnExactAlarmPermission();
        TodoWidgetProvider.refreshAllWidgets(requireContext());
    }

    // ---------- Simpan: berulang ----------
    private void saveAsRecurring(String title, String category, int priority, int durationMin,
                                  int prepMinutes, boolean prepEnabled, boolean startEnabled) {
        // Kalau sebelumnya sekali-jalan lalu diubah jadi berulang, hapus baris tugas lamanya
        if (!wasRecurringInstance && editingTaskId > 0) {
            AlarmScheduler.cancelForTask(requireContext(), editingTaskId);
            reminderRuleDb.deleteByTask(editingTaskId);
            taskDb.delete(editingTaskId);
        }

        RecurringTemplate tmpl = new RecurringTemplate();
        tmpl.id = editingTemplateId;
        tmpl.title = title;
        tmpl.category = category;
        tmpl.priority = priority;
        tmpl.recurrenceType = rgRecurrenceType.getCheckedRadioButtonId() == R.id.rbWeekly ? "WEEKLY" : "DAILY";
        tmpl.recurrenceDays = recurrenceDaysBitmask == 0 ? 127 : recurrenceDaysBitmask; // default semua hari kalau belum pilih
        tmpl.hour = pendingDateTime.get(Calendar.HOUR_OF_DAY);
        tmpl.minute = pendingDateTime.get(Calendar.MINUTE);
        tmpl.prepMinutesBefore = prepMinutes;
        tmpl.prepAlarmEnabled = prepEnabled;
        tmpl.alarmEnabled = startEnabled;
        tmpl.soundUri = pendingSoundUri;
        tmpl.estimatedMinutes = durationMin;
        tmpl.active = true;

        long templateId = templateDb.insertOrUpdate(tmpl);
        RecurrenceGenerator.regenerateAfterTemplateChange(requireContext(), templateId);
        maybeWarnExactAlarmPermission();
    }

    private void performDelete() {
        if (wasRecurringInstance && editingTemplateId > 0) {
            RecurrenceGenerator.regenerateAfterTemplateChangeToNone(requireContext(), editingTemplateId);
            templateDb.delete(editingTemplateId);
        } else if (editingTaskId > 0) {
            AlarmScheduler.cancelForTask(requireContext(), editingTaskId);
            reminderRuleDb.deleteByTask(editingTaskId);
            taskDb.delete(editingTaskId);
        }
        TodoWidgetProvider.refreshAllWidgets(requireContext());
    }

    private void notifyResultAndDismiss() {
        getParentFragmentManager().setFragmentResult(RESULT_KEY, new Bundle());
        dismiss();
    }

    private void maybeWarnExactAlarmPermission() {
        android.app.AlarmManager am = (android.app.AlarmManager) requireContext().getSystemService(android.content.Context.ALARM_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && am != null && !am.canScheduleExactAlarms()) {
            Toast.makeText(requireContext(), "Aktifkan izin \"Alarm & pengingat\" di pengaturan HP agar alarm presisi bunyi tepat waktu", Toast.LENGTH_LONG).show();
        }
    }

    // ---------- Helper UI ----------
    private void selectCategoryInSpinner(String key) {
        for (int i = 0; i < categoryList.size(); i++) {
            if (categoryList.get(i).key.equals(key)) {
                spCategory.setSelection(i);
                return;
            }
        }
    }

    private void selectPriorityRadio(int priority) {
        if (priority == Priority.LOW) rgPriority.check(R.id.rbLow);
        else if (priority == Priority.HIGH) rgPriority.check(R.id.rbHigh);
        else rgPriority.check(R.id.rbMedium);
    }

    private int selectedPriority() {
        int id = rgPriority.getCheckedRadioButtonId();
        if (id == R.id.rbLow) return Priority.LOW;
        if (id == R.id.rbHigh) return Priority.HIGH;
        return Priority.MEDIUM;
    }

    private void selectPrepSpinner(List<Integer> values, int minutes) {
        int idx = values.indexOf(minutes);
        spPrepMinutes.setSelection(idx >= 0 ? idx : 1);
    }

    private void updateDateButtonLabel() {
        String d = new SimpleDateFormat("EEE, d MMM yyyy", new Locale("id", "ID")).format(pendingDateTime.getTime());
        btnPickDate.setText("📅 " + d);
    }

    private void updateTimeButtonLabel() {
        String t = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(pendingDateTime.getTime());
        btnPickStartTime.setText("🕐 " + t);
    }

    private void updateReminderSectionVisibility() {
        // Pengingat custom H-N hanya relevan untuk kegiatan SEKALI JALAN (mis. deadline tugas),
        // bukan untuk kegiatan berulang harian/mingguan.
        int vis = switchRecurring.isChecked() ? View.GONE : View.VISIBLE;
        layoutReminderRules.setVisibility(vis);
        btnAddReminderRule.setVisibility(vis);
    }

    private void buildDayPickerChips() {
        layoutDayPicker.removeAllViews();
        String[] labels = {"S", "S", "R", "K", "J", "S", "M"};
        for (int i = 0; i < 7; i++) {
            int bit = 1 << i;
            TextView chip = new TextView(requireContext());
            chip.setText(labels[i]);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setTextSize(13);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1f);
            lp.setMarginEnd(i < 6 ? dp(4) : 0);
            chip.setLayoutParams(lp);
            chip.setTag(bit);
            styleDayChip(chip, false);
            chip.setOnClickListener(v -> {
                recurrenceDaysBitmask ^= bit;
                refreshDayPickerVisualState();
            });
            layoutDayPicker.addView(chip);
        }
    }

    private void refreshDayPickerVisualState() {
        for (int i = 0; i < layoutDayPicker.getChildCount(); i++) {
            TextView chip = (TextView) layoutDayPicker.getChildAt(i);
            int bit = (int) chip.getTag();
            styleDayChip(chip, (recurrenceDaysBitmask & bit) != 0);
        }
    }

    private void styleDayChip(TextView chip, boolean selected) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(selected ? android.graphics.Color.parseColor("#6C5CE7") : android.graphics.Color.parseColor("#ECEBFA"));
        chip.setBackground(bg);
        chip.setTextColor(selected ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#8B8B9E"));
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // ---------- Pengingat custom: sub-dialog tambah ----------
    private void openAddReminderRuleDialog() {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_reminder_rule, null);
        Spinner spDaysBefore = v.findViewById(R.id.spDaysBefore);
        Spinner spRepeatCount = v.findViewById(R.id.spRepeatCount);
        Spinner spIntervalHours = v.findViewById(R.id.spIntervalHours);
        EditText etLabel = v.findViewById(R.id.etReminderLabel);

        List<String> days = new ArrayList<>();
        for (int i = 1; i <= 10; i++) days.add("H-" + i);
        ArrayAdapter<String> daysAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, days);
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDaysBefore.setAdapter(daysAdapter);

        List<String> repeats = new ArrayList<>();
        for (int i = 1; i <= 10; i++) repeats.add(i + "x");
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, repeats);
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRepeatCount.setAdapter(repeatAdapter);

        List<Integer> intervalValues = List.of(1, 2, 3, 4, 6, 8, 12, 24);
        List<String> intervalLabels = List.of("1 jam", "2 jam", "3 jam", "4 jam", "6 jam", "8 jam", "12 jam", "24 jam (harian)");
        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, intervalLabels);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spIntervalHours.setAdapter(intervalAdapter);
        spIntervalHours.setSelection(intervalValues.indexOf(24));

        AlertDialog sub = new AlertDialog.Builder(requireContext())
                .setTitle("Pengingat Custom")
                .setView(v)
                .create();

        v.findViewById(R.id.btnRuleCancel).setOnClickListener(x -> sub.dismiss());
        v.findViewById(R.id.btnRuleSave).setOnClickListener(x -> {
            ReminderRule r = new ReminderRule();
            r.daysBefore = spDaysBefore.getSelectedItemPosition() + 1;
            r.repeatCount = spRepeatCount.getSelectedItemPosition() + 1;
            r.repeatIntervalHours = intervalValues.get(spIntervalHours.getSelectedItemPosition());
            String label = etLabel.getText().toString().trim();
            r.label = label.isEmpty() ? null : label;

            pendingReminderRules.add(r);
            renderReminderRuleList();
            sub.dismiss();
        });

        sub.show();
    }

    private void renderReminderRuleList() {
        layoutReminderRules.removeAllViews();
        for (ReminderRule r : pendingReminderRules) {
            View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_reminder_rule, layoutReminderRules, false);
            TextView tvDesc = row.findViewById(R.id.tvRuleDesc);
            ImageButton btnRemove = row.findViewById(R.id.btnRemoveRule);

            StringBuilder desc = new StringBuilder("H-" + r.daysBefore);
            if (r.repeatCount > 1) {
                desc.append(", ulang ").append(r.repeatCount).append("x tiap ").append(r.repeatIntervalHours).append(" jam");
            } else {
                desc.append(", sekali");
            }
            if (r.label != null && !r.label.isEmpty()) desc.append(" — \"").append(r.label).append("\"");
            tvDesc.setText(desc.toString());

            btnRemove.setOnClickListener(v -> {
                pendingReminderRules.remove(r);
                renderReminderRuleList();
            });

            layoutReminderRules.addView(row);
        }
    }

    private String queryFileName(Uri uri) {
        String result = "file audio";
        try (android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) result = cursor.getString(idx);
            }
        } catch (Exception ignored) { }
        return result;
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        if (previewPlayer != null) { previewPlayer.release(); previewPlayer = null; }
    }
}
