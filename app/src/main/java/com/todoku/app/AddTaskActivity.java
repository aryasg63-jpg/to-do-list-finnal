package com.todoku.app;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Layar TAMBAH / EDIT kegiatan full-screen dengan opsi lengkap:
 * tanggal & jam, kategori custom, durasi, catatan, pengulangan (harian/mingguan/
 * bulanan/tahunan/kustom hari/interval), pengingat bersiap custom, suara custom.
 */
public class AddTaskActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "extra_task_id";

    private final String[] repeatLabels = {
            "Tidak berulang",
            "Setiap hari",
            "Hari kerja (Sen–Jum)",
            "Mingguan",
            "Bulanan",
            "Tahunan",
            "Pilih hari tertentu...",
            "Setiap N hari"
    };
    private final String[] repeatValues = {
            Task.REPEAT_NONE, Task.REPEAT_DAILY, Task.REPEAT_WEEKDAYS, Task.REPEAT_WEEKLY,
            Task.REPEAT_MONTHLY, Task.REPEAT_YEARLY, Task.REPEAT_CUSTOM_DAYS, Task.REPEAT_INTERVAL_DAYS
    };
    private final String[] prepLabels = {"5 menit", "10 menit", "15 menit", "30 menit", "1 jam", "Kustom..."};
    private final Integer[] prepValues = {5, 10, 15, 30, 60, -1};

    private Calendar pendingStart = Calendar.getInstance();
    private String pendingSoundUri = null;
    private MediaPlayer previewPlayer;

    private EditText etTitle, etCustomCategory, etDuration, etNote, etPrepCustom, etRepeatInterval;
    private Spinner spCategory, spRepeat, spPrepMinutes;
    private Button btnPickDate, btnPickTime, btnPickSound, btnSave, btnPreviewSound;
    private ImageButton btnBack;
    private TextView tvSelectedSound;
    private CheckBox cbPrepAlarm, cbStartAlarm;
    private LinearLayout layoutRepeatInterval, layoutWeekChips;
    private TextView tvIntervalLabel;
    private List<Chip> weekChips = new ArrayList<>();

    private final ActivityResultLauncher<String[]> audioPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) { }
                    pendingSoundUri = uri.toString();
                    tvSelectedSound.setText("🎵 " + queryFileName(uri));
                    btnPreviewSound.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        etTitle = findViewById(R.id.etTitle);
        spCategory = findViewById(R.id.spCategory);
        etCustomCategory = findViewById(R.id.etCustomCategory);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickTime = findViewById(R.id.btnPickTime);
        etDuration = findViewById(R.id.etDuration);
        etNote = findViewById(R.id.etNote);
        spRepeat = findViewById(R.id.spRepeat);
        layoutRepeatInterval = findViewById(R.id.layoutRepeatInterval);
        tvIntervalLabel = findViewById(R.id.tvIntervalLabel);
        etRepeatInterval = findViewById(R.id.etRepeatInterval);
        layoutWeekChips = findViewById(R.id.layoutWeekChips);
        cbPrepAlarm = findViewById(R.id.cbPrepAlarm);
        spPrepMinutes = findViewById(R.id.spPrepMinutes);
        etPrepCustom = findViewById(R.id.etPrepCustom);
        cbStartAlarm = findViewById(R.id.cbStartAlarm);
        btnPickSound = findViewById(R.id.btnPickSound);
        tvSelectedSound = findViewById(R.id.tvSelectedSound);
        btnPreviewSound = findViewById(R.id.btnPreviewSound);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvTitle);

        weekChips.addAll(Arrays.asList(
                findViewById(R.id.chipMon), findViewById(R.id.chipTue), findViewById(R.id.chipWed),
                findViewById(R.id.chipThu), findViewById(R.id.chipFri), findViewById(R.id.chipSat),
                findViewById(R.id.chipSun)));

        // Kategori: built-in + custom
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, CategoryHelper.builtInCategoryLabels());
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);
        spCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                etCustomCategory.setVisibility(CategoryHelper.isCustomSlot(pos) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });

        // Pengulangan
        ArrayAdapter<String> repAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, repeatLabels);
        repAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRepeat.setAdapter(repAdapter);
        spRepeat.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                updateRepeatOptions(repeatValues[pos]);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });

        // Prep minutes
        ArrayAdapter<String> prepAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, prepLabels);
        prepAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPrepMinutes.setAdapter(prepAdapter);
        spPrepMinutes.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                etPrepCustom.setVisibility(prepValues[pos] == -1 ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });

        // Default: 1 jam dari sekarang
        pendingStart = Calendar.getInstance();
        pendingStart.add(Calendar.HOUR_OF_DAY, 1);
        pendingStart.set(Calendar.SECOND, 0);
        pendingStart.set(Calendar.MILLISECOND, 0);
        spPrepMinutes.setSelection(1); // 10 menit

        long editId = getIntent().getLongExtra(EXTRA_TASK_ID, 0);
        if (editId != 0) {
            loadForEdit(editId, tvTitle);
        } else {
            tvTitle.setText("Tambah Kegiatan");
            // default pilih hari ini (Senin) untuk mode custom
            updateStartButtons();
        }

        btnPickDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (dp, y, m, d) -> {
                pendingStart.set(Calendar.YEAR, y);
                pendingStart.set(Calendar.MONTH, m);
                pendingStart.set(Calendar.DAY_OF_MONTH, d);
                updateStartButtons();
            }, pendingStart.get(Calendar.YEAR), pendingStart.get(Calendar.MONTH),
                    pendingStart.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnPickTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (tp, hour, minute) -> {
                pendingStart.set(Calendar.HOUR_OF_DAY, hour);
                pendingStart.set(Calendar.MINUTE, minute);
                pendingStart.set(Calendar.SECOND, 0);
                pendingStart.set(Calendar.MILLISECOND, 0);
                updateStartButtons();
            }, pendingStart.get(Calendar.HOUR_OF_DAY), pendingStart.get(Calendar.MINUTE), true).show();
        });

        btnPickSound.setOnClickListener(v ->
                audioPickerLauncher.launch(new String[]{"audio/mpeg", "audio/opus", "audio/ogg", "audio/*"}));

        btnPreviewSound.setOnClickListener(v -> {
            if (pendingSoundUri == null) return;
            try {
                if (previewPlayer != null) { previewPlayer.release(); previewPlayer = null; }
                previewPlayer = new MediaPlayer();
                previewPlayer.setDataSource(this, Uri.parse(pendingSoundUri));
                previewPlayer.setOnPreparedListener(MediaPlayer::start);
                previewPlayer.setOnCompletionListener(mp -> { mp.release(); previewPlayer = null; });
                previewPlayer.prepareAsync();
            } catch (Exception e) {
                Toast.makeText(this, "Tidak bisa memutar file ini", Toast.LENGTH_SHORT).show();
            }
        });

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> save());
    }

    private void updateRepeatOptions(String repeatType) {
        boolean intervalVisible = Task.REPEAT_WEEKLY.equals(repeatType)
                || Task.REPEAT_MONTHLY.equals(repeatType)
                || Task.REPEAT_INTERVAL_DAYS.equals(repeatType);
        layoutRepeatInterval.setVisibility(intervalVisible ? View.VISIBLE : View.GONE);
        layoutWeekChips.setVisibility(Task.REPEAT_CUSTOM_DAYS.equals(repeatType) ? View.VISIBLE : View.GONE);

        if (Task.REPEAT_WEEKLY.equals(repeatType)) tvIntervalLabel.setText("Ulangi setiap berapa minggu?");
        else if (Task.REPEAT_MONTHLY.equals(repeatType)) tvIntervalLabel.setText("Ulangi setiap berapa bulan?");
        else if (Task.REPEAT_INTERVAL_DAYS.equals(repeatType)) tvIntervalLabel.setText("Ulangi setiap berapa hari?");
    }

    private void loadForEdit(long id, TextView tvTitle) {
        Task t = new TaskDb(this).getById(id);
        if (t == null) { finish(); return; }

        tvTitle.setText("Edit Kegiatan");
        etTitle.setText(t.title);

        int catIndex = Arrays.asList(CategoryHelper.builtInCategories()).indexOf(t.category);
        if (catIndex >= 0) {
            spCategory.setSelection(catIndex);
        } else {
            spCategory.setSelection(CategoryHelper.builtInCategories().length); // slot custom
            etCustomCategory.setVisibility(View.VISIBLE);
            etCustomCategory.setText(t.category);
        }

        pendingStart.setTimeInMillis(t.startTimeMillis);
        etDuration.setText(t.estimatedMinutes == 0 ? "" : String.valueOf(t.estimatedMinutes));
        etNote.setText(t.note);

        int repIdx = Arrays.asList(repeatValues).indexOf(t.repeatType);
        spRepeat.setSelection(Math.max(0, repIdx));
        etRepeatInterval.setText(String.valueOf(t.repeatInterval));
        for (int i = 0; i < 7; i++) {
            weekChips.get(i).setChecked(Task.weekdayBitSet(i, t.repeatWeekdays));
        }

        cbPrepAlarm.setChecked(t.prepAlarmEnabled);
        int prepIdx = Arrays.asList(prepValues).indexOf(t.prepMinutesBefore);
        if (prepIdx >= 0) {
            spPrepMinutes.setSelection(prepIdx);
        } else {
            spPrepMinutes.setSelection(prepValues.length - 1); // kustom
            etPrepCustom.setVisibility(View.VISIBLE);
            etPrepCustom.setText(String.valueOf(t.prepMinutesBefore));
        }
        cbStartAlarm.setChecked(t.alarmEnabled);

        pendingSoundUri = t.soundUri;
        if (pendingSoundUri != null) {
            tvSelectedSound.setText("🎵 " + queryFileName(Uri.parse(pendingSoundUri)));
            btnPreviewSound.setVisibility(View.VISIBLE);
        }
        updateStartButtons();
    }

    private void updateStartButtons() {
        btnPickDate.setText(new SimpleDateFormat("EEE, d MMM yyyy", new Locale("id", "ID")).format(pendingStart.getTime()));
        btnPickTime.setText("🕐 " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(pendingStart.getTime()));
    }

    private void save() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Nama kegiatan tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        String category;
        if (CategoryHelper.isCustomSlot(spCategory.getSelectedItemPosition())) {
            category = etCustomCategory.getText().toString().trim();
            if (category.isEmpty()) {
                Toast.makeText(this, "Tulis nama kategori custom", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            category = CategoryHelper.builtInCategories()[spCategory.getSelectedItemPosition()];
        }

        int durationMin = 0;
        try { durationMin = Integer.parseInt(etDuration.getText().toString().trim()); } catch (Exception ignored) {}

        String repeatType = repeatValues[spRepeat.getSelectedItemPosition()];
        int repeatInterval = 1;
        try { repeatInterval = Math.max(1, Integer.parseInt(etRepeatInterval.getText().toString().trim())); } catch (Exception ignored) {}

        int repeatWeekdays = 0;
        if (Task.REPEAT_CUSTOM_DAYS.equals(repeatType)) {
            for (int i = 0; i < 7; i++) {
                if (weekChips.get(i).isChecked()) repeatWeekdays |= (1 << i);
            }
            if (repeatWeekdays == 0) {
                Toast.makeText(this, "Pilih minimal satu hari", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        boolean prepEnabled = cbPrepAlarm.isChecked();
        int prepMinutes = 0;
        if (prepEnabled) {
            int sel = spPrepMinutes.getSelectedItemPosition();
            if (prepValues[sel] >= 0) {
                prepMinutes = prepValues[sel];
            } else {
                try { prepMinutes = Integer.parseInt(etPrepCustom.getText().toString().trim()); }
                catch (Exception e) { prepMinutes = 0; }
                if (prepMinutes <= 0) {
                    Toast.makeText(this, "Isi menit pengingat bersiap", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        long editId = getIntent().getLongExtra(EXTRA_TASK_ID, 0);
        Task t = new Task();
        t.id = editId;
        t.title = title;
        t.category = category;
        t.startTimeMillis = pendingStart.getTimeInMillis();
        t.prepMinutesBefore = prepMinutes;
        t.prepAlarmEnabled = prepEnabled;
        t.alarmEnabled = cbStartAlarm.isChecked();
        t.soundUri = pendingSoundUri;
        t.estimatedMinutes = durationMin;
        t.note = etNote.getText().toString().trim();
        t.repeatType = repeatType;
        t.repeatInterval = repeatInterval;
        t.repeatWeekdays = repeatWeekdays;
        t.done = false;

        TaskDb db = new TaskDb(this);
        long newId = db.insertOrUpdate(t);
        t.id = newId;
        AlarmScheduler.scheduleForTask(this, t);

        Toast.makeText(this, repeatType.equals(Task.REPEAT_NONE) ? "Kegiatan disimpan 💾" : "Kegiatan berulang disimpan 🔁", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String queryFileName(Uri uri) {
        String result = "file audio";
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) result = cursor.getString(idx);
            }
        } catch (Exception ignored) { }
        return result;
    }

    @Override
    protected void onDestroy() {
        if (previewPlayer != null) {
            previewPlayer.release();
            previewPlayer = null;
        }
        super.onDestroy();
    }
}