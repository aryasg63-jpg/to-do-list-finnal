package com.todoku.app;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CategoryManageActivity extends AppCompatActivity implements CategoryAdapter.Listener {

    private static final String[] PALETTE = {
            "#FF6B6B", "#FFA94D", "#FFD43B", "#69DB7C", "#4DABF7",
            "#748FFC", "#DA77F2", "#F783AC", "#20C997", "#868E96"
    };

    private CategoryDb db;
    private CategoryAdapter adapter;
    private String selectedColor = PALETTE[0];
    private LinearLayout paletteLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manage);

        db = new CategoryDb(this);

        RecyclerView rv = findViewById(R.id.rvCategories);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter(db.getAll(), this);
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddCategory).setOnClickListener(v -> openCategoryDialog(null));
    }

    private void reload() {
        adapter.updateData(db.getAll());
        CategoryHelper.refresh(this);
    }

    @Override
    public void onEdit(Category c) {
        openCategoryDialog(c);
    }

    @Override
    public void onDelete(Category c) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus kategori?")
                .setMessage("\"" + c.label + "\" akan dihapus. Tugas yang sudah pakai kategori ini tidak akan hilang.")
                .setPositiveButton("Hapus", (d, w) -> {
                    db.delete(c.key);
                    reload();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void openCategoryDialog(Category existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        EditText etLabel = view.findViewById(R.id.etCatLabel);
        EditText etEmoji = view.findViewById(R.id.etCatEmoji);
        paletteLayout = view.findViewById(R.id.layoutColorPalette);

        selectedColor = existing != null ? existing.colorHex : PALETTE[0];
        buildPalette();

        if (existing != null) {
            etLabel.setText(existing.label);
            etEmoji.setText(existing.emoji);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Kategori Baru" : "Edit Kategori")
                .setView(view)
                .create();

        view.findViewById(R.id.btnCatCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnCatSave).setOnClickListener(v -> {
            String label = etLabel.getText().toString().trim();
            String emoji = etEmoji.getText().toString().trim();
            if (label.isEmpty()) {
                Toast.makeText(this, "Nama kategori tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }
            if (emoji.isEmpty()) emoji = "📌";

            Category c = existing != null ? existing : new Category();
            if (existing == null) {
                c.key = "custom_" + System.currentTimeMillis();
                c.isDefault = false;
            }
            c.label = label;
            c.emoji = emoji;
            c.colorHex = selectedColor;

            db.addOrUpdate(c);
            reload();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void buildPalette() {
        paletteLayout.removeAllViews();
        int size = (int) (32 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);

        for (String hex : PALETTE) {
            View swatch = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd(margin);
            swatch.setLayoutParams(lp);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(Color.parseColor(hex));
            if (hex.equalsIgnoreCase(selectedColor)) {
                bg.setStroke((int) (3 * getResources().getDisplayMetrics().density), Color.BLACK);
            }
            swatch.setBackground(bg);

            swatch.setOnClickListener(v -> {
                selectedColor = hex;
                buildPalette();
            });

            paletteLayout.addView(swatch);
        }
    }
}
