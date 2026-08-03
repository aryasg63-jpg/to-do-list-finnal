package com.todoku.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Setelan: mode gelap, izin alarm presisi & baterai, backup/restore JSON, hapus data.
 */
public class SettingsFragment extends Fragment {

    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"),
                    uri -> {
                        if (uri == null) return;
                        try {
                            String json = BackupHelper.exportJson(new TaskDb(requireContext()));
                            OutputStream os = requireContext().getContentResolver().openOutputStream(uri);
                            if (os != null) {
                                os.write(json.getBytes("UTF-8"));
                                os.close();
                                Toast.makeText(requireContext(), "Backup tersimpan 💾", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Gagal backup: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri == null) return;
                        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
                            if (is == null) return;
                            StringBuilder sb = new StringBuilder();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                            String line;
                            while ((line = reader.readLine()) != null) sb.append(line);
                            int count = BackupHelper.restore(requireContext(), sb.toString());
                            Toast.makeText(requireContext(), "Berhasil memuat " + count + " tugas", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Gagal restore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    public android.view.View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container,
                                          android.os.Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(android.view.View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        androidx.appcompat.widget.SwitchCompat swDark = view.findViewById(R.id.swDarkMode);
        Button btnExact = view.findViewById(R.id.btnExactAlarm);
        Button btnBatt = view.findViewById(R.id.btnBatteryOpt);
        Button btnBackup = view.findViewById(R.id.btnBackup);
        Button btnRestore = view.findViewById(R.id.btnRestore);
        Button btnWipe = view.findViewById(R.id.btnWipe);

        int nightMode = AppCompatDelegate.getDefaultNightMode();
        swDark.setChecked(nightMode == AppCompatDelegate.MODE_NIGHT_YES);
        swDark.setOnCheckedChangeListener((v, checked) -> {
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
            if (getActivity() != null) getActivity().recreate();
        });

        btnExact.setOnClickListener(v -> {
            Uri pkg = Uri.parse("package:" + requireContext().getPackageName());
            try {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg));
            }
        });

        btnBatt.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + requireContext().getPackageName()));
            try {
                startActivity(i);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Buka Setelan → Baterai → optimasi", Toast.LENGTH_LONG).show();
            }
        });

        btnBackup.setOnClickListener(v -> exportLauncher.launch("todoku_backup_" + System.currentTimeMillis() + ".json"));
        btnRestore.setOnClickListener(v -> importLauncher.launch(new String[]{"application/json", "text/*"}));

        btnWipe.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Hapus semua data?")
                .setMessage("Semua tugas & riwayat akan hilang permanen. Tidak bisa dibatalkan.")
                .setPositiveButton("Ya, hapus", (d, w) -> {
                    TaskDb db = new TaskDb(requireContext());
                    db.getWritableDatabase().delete("tasks", null, null);
                    db.getWritableDatabase().delete("task_history", null, null);
                    for (Task t : new java.util.ArrayList<>(db.getActiveTasks())) {
                        AlarmScheduler.cancelForTask(requireContext(), t.id);
                    }
                    Toast.makeText(requireContext(), "Semua data dihapus", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show());
    }
}