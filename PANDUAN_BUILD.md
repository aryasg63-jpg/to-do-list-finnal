# TodoKu Alarm — Panduan Build APK (Versi Lengkap)

Project Android Studio siap build. Semua kode sudah divalidasi (sintaks, referensi resource, referensi antar-class) — lihat bagian "Soal validasi" di bawah untuk detail apa yang sudah & belum bisa saya pastikan dari sisi saya.

## Fitur yang ada di versi ini

**Dasar (dari sebelumnya):**
- 6 kategori bawaan, jam real-time, 2 alarm per tugas (bersiap-siap + waktu mulai), custom suara .mp3/.opus dari file lokal, layar alarm full-screen, data tersimpan lokal 100% offline.

**Ditambahkan sekarang:**
- **Tugas harian & mingguan otomatis** — tandai "Ulangi", pilih Harian atau Mingguan (pilih hari spesifik Sen–Min), sistem otomatis generate 7 hari ke depan setiap kali app dibuka, tanpa perlu input ulang manual.
- **Pengingat custom bertingkat** — untuk tugas sekali-jalan (mis. deadline tugas sekolah), tambahkan pengingat H-1 sampai H-10 sebelum waktu mulai, bisa diulang beberapa kali dengan jarak jam yang diatur sendiri (mis. "ingatkan tiap hari mulai H-5", atau "ingatkan tiap 2 jam di H-1").
- **Kalender mingguan** — layar baru dengan tab 7 hari, titik penanda hari yang ada tugasnya, navigasi minggu sebelum/sesudah.
- **Statistik & grafik** — grafik batang 7/30 hari terakhir (buatan sendiri, tanpa library eksternal supaya build tetap ringan), breakdown persentase selesai per kategori.
- **Progress bar harian** — persentase tugas hari ini yang sudah selesai, langsung di layar utama.
- **Prioritas tugas** — Rendah/Sedang/Tinggi, ditandai titik warna di tiap kartu tugas.
- **Kategori custom** — tambah/edit/hapus kategori sendiri di luar 6 bawaan, lengkap dengan emoji dan pilihan warna.
- **Pelacak streak/kebiasaan** — badge "🔥 N hari berturut-turut" otomatis muncul di layar utama untuk tugas berulang yang konsisten dikerjakan.
- **Ringkasan notifikasi pagi** — notifikasi harian jam yang bisa diatur (default 06:00) berisi ringkasan kegiatan hari itu; ini sekaligus menjawab permintaan "pengingat harian" secara umum.
- **Widget layar utama** — tampilan ringkas (ringkasan hari ini + tugas berikutnya) langsung di homescreen HP tanpa buka app; tap widget untuk buka app.
- **Mode gelap** — Ikuti Sistem / Selalu Terang / Selalu Gelap, diatur lewat ikon ⚙️ di layar utama.

## Cara build jadi APK

### Opsi A — GitHub Actions (paling simpel, tidak perlu install apa-apa)
1. Upload folder ini ke repo GitHub Anda (ganti seluruh isi repo lama dengan isi folder ini)
2. Buka tab **Actions** di repo, tunggu proses "Build APK" selesai (~5 menit)
3. Klik proses yang sudah centang hijau → scroll ke **Artifacts** → download **app-debug-apk**

### Opsi B — Android Studio
1. Buka Android Studio → **Open** → pilih folder `TodoAlarm` ini
2. Tunggu Gradle Sync selesai
3. **Build → Build Bundle(s)/APK(s) → Build APK(s)**
4. APK ada di `app/build/outputs/apk/debug/app-debug.apk`

### Opsi C — Command line (kalau sudah ada Android SDK)
```bash
cd TodoAlarm
./gradlew assembleDebug
```

## Setelah install di HP

Sama seperti sebelumnya: izinkan "Install dari sumber tidak dikenal", lalu di dalam app izinkan **notifikasi** dan **Alarm & pengingat** (dialog otomatis muncul di pembukaan pertama).

**Khusus widget**: setelah install, tekan lama di layar homescreen kosong → Widget → cari "TodoKu Alarm" → tarik ke homescreen.

## Soal validasi — apa yang sudah & belum saya pastikan

Saya tidak punya Android SDK/Gradle di lingkungan kerja saya, jadi saya **tidak bisa menjalankan compile Java/Gradle sungguhan** di sini. Yang sudah saya lakukan sebagai gantinya, dengan script otomatis, ke **seluruh 31 file Java dan 23 file XML**:
- Kurung `()`/`{}`/`[]` seimbang di semua file Java (parser yang paham komentar `/* */`, `//`, dan string literal, bukan sekadar hitung karakter)
- Semua file XML valid secara struktur (well-formed, tidak ada tag yang lupa ditutup)
- Setiap `R.id.xxx`, `R.layout.xxx`, `R.drawable.xxx`, `R.string.xxx`, dll yang dipakai di Java benar-benar ada di file XML
- Setiap `@style/`, `@drawable/`, `@color/`, `@layout/` yang dipakai di dalam XML benar-benar didefinisikan
- Setiap `new NamaClass(...)` merujuk ke class yang benar-benar ada (di project ini atau library Android standar)

Ini menangkap kelas error paling umum (typo nama resource, method yang tidak ada, kurung tidak seimbang — beberapa memang saya temukan dan perbaiki selama proses ini). Yang **tidak bisa** saya jamin tanpa compiler sungguhan: kecocokan tipe data yang sangat spesifik, resolusi versi dependency di server Maven saat build jalan, dan hal-hal lain yang hanya terlihat saat javac/aapt2 benar-benar jalan. Kalau GitHub Actions/Code Magic tetap menunjukkan error, kirim pesan error lengkapnya ke saya — dengan info persis file & baris mana yang bermasalah, saya bisa perbaiki jauh lebih cepat & tepat daripada menebak.
