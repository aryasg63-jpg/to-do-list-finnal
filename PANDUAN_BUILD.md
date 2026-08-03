# TodoKu Alarm — Panduan Build APK

Project Android Studio siap pakai. Semua kode sudah lengkap dan tervalidasi sintaksnya — Anda tinggal build, tidak perlu menulis kode apapun.

## Yang sudah dibuat untukmu

**Fitur utama:**
- 6 kategori: Olahraga, Makanan, Minuman, Aktivitas, Tugas Rumah, Tugas Sekolah
- Jam digital real-time + tanggal di layar utama
- **2 alarm per tugas**: alarm "Bersiap-siap" (5/10/15/30/60 menit sebelum) dan alarm "Waktu Mulai" (tepat waktu)
- Alarm tetap bunyi walau app ditutup, HP dikunci, atau di-restart (pakai AlarmManager + BootReceiver asli Android)
- **Custom suara alarm dari file .mp3/.opus lokal** — pilih file dari penyimpanan HP, ada tombol coba dengar sebelum disimpan
- Layar alarm full-screen (seperti alarm clock bawaan) dengan tombol Matikan / Tunda 5 menit
- Fitur efisiensi waktu: countdown "mulai dalam X menit" di tiap tugas, ringkasan total estimasi waktu semua tugas, dan kartu "Berikutnya" yang otomatis menampilkan tugas terdekat
- Data tersimpan permanen di database lokal HP (SQLite), tidak butuh internet sama sekali

## Cara build jadi APK (pilih salah satu)

### Opsi A — Android Studio (paling gampang, gratis)
1. Download & install [Android Studio](https://developer.android.com/studio)
2. Extract file `TodoAlarm.zip` yang saya berikan
3. Buka Android Studio → **Open** → pilih folder `TodoAlarm`
4. Tunggu proses "Gradle Sync" selesai otomatis (5–15 menit tergantung koneksi, karena mendownload Android SDK di komputer Anda)
5. Klik menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**
6. File APK akan muncul di `app/build/outputs/apk/debug/app-debug.apk`
7. Kirim file itu ke HP (lewat kabel USB, WhatsApp ke diri sendiri, atau Google Drive), lalu install seperti biasa

### Opsi B — Command line (kalau sudah punya Android SDK terinstall)
```bash
cd TodoAlarm
./gradlew assembleDebug
```
APK akan ada di `app/build/outputs/apk/debug/app-debug.apk`

### Opsi C — Minta tolong orang lain / jasa online
Kalau tidak familiar dengan Android Studio, project ZIP ini bisa dikirim ke teman yang ngerti coding Android, atau di-build lewat layanan CI online seperti GitHub Actions (gratis) — saya bisa bantu buatkan konfigurasinya kalau tertarik, cukup bilang saja.

## Penting saat install APK di HP

Karena APK ini tidak dari Google Play Store, Android akan minta izin **"Install dari sumber tidak dikenal"** — ini normal untuk APK custom, bukan tanda virus. Tinggal tap "Izinkan" saat muncul.

Setelah install, buka aplikasinya sekali agar bisa:
1. Memberi izin **notifikasi** (Android 13+)
2. Memberi izin **"Alarm & pengingat"** khusus — akan muncul dialog otomatis, tap "Buka Pengaturan" → aktifkan toggle-nya. Ini WAJIB agar alarm presisi tepat waktu, kalau tidak, alarm bisa telat beberapa menit karena pembatasan baterai Android.

## Kalau ingin ganti warna/nama aplikasi

- Nama app: edit `app/src/main/res/values/strings.xml`
- Warna tema (ungu #6C5CE7): edit `app/src/main/res/values/colors.xml`
- Ikon aplikasi: ganti file di folder `mipmap-*` (sudah saya buatkan ikon default bertema checklist)

## Struktur kode (kalau Anda atau orang lain mau modifikasi)

| File | Fungsi |
|---|---|
| `MainActivity.java` | Layar utama, jam real-time, tambah/edit tugas |
| `AlarmScheduler.java` | Menjadwalkan alarm prep & start ke sistem Android |
| `AlarmReceiver.java` | Penerima sinyal saat waktu alarm tiba |
| `AlarmSoundService.java` | Memutar file mp3/opus custom, loop sampai dimatikan |
| `AlarmRingActivity.java` | Layar full-screen saat alarm bunyi |
| `BootReceiver.java` | Menjadwalkan ulang semua alarm setelah HP restart |
| `TaskDb.java` | Database SQLite penyimpanan tugas |
| `Task.java` | Model data satu tugas |
| `CategoryHelper.java` | Emoji, label, warna 6 kategori |
