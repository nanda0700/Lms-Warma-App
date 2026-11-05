# LMS Warmadewa (aplikasi Android)

LMS Universitas Warmadewa dalam bentuk aplikasi, lengkap dengan tema kustom
(dark mode, background gambar/GIF, header gradient + partikel, font, translucent)
dan **notifikasi pengingat absen & pengumpulan tugas**.

## Cara membuat APK (tanpa Android Studio) - lewat GitHub, gratis

1. Buat akun di https://github.com (gratis), lalu klik **New repository**.
   Beri nama `lms-warmadewa`, pilih **Private**, klik **Create repository**.
2. Ekstrak ZIP proyek ini. Di halaman repo klik **uploading an existing file**,
   lalu seret SEMUA isi folder (folder `app`, file `build.gradle`, `settings.gradle`,
   `gradle.properties`, `README.md`, dst.). Klik **Commit changes**.
3. Folder `.github` kadang tidak ikut terunggah. Cek: jika tidak ada, klik
   **Add file > Create new file**, ketik nama `.github/workflows/build.yml`,
   lalu tempel isi file `build.yml` dari ZIP, dan **Commit**.
4. Buka tab **Actions**. Pilih **Build APK**, klik **Run workflow**
   (atau tunggu otomatis berjalan setelah commit). Tunggu sekitar 3-6 menit sampai centang hijau.
5. Klik proses yang selesai, turun ke bagian **Artifacts**, unduh **LMS-Warmadewa-apk**.
   Ekstrak ZIP-nya: isinya `app-debug.apk`.
6. Kirim `app-debug.apk` ke HP, buka, izinkan **Install dari sumber tidak dikenal**.
   Jika Play Protect memperingatkan, pilih **Tetap instal** (aplikasi ini buatan sendiri).

Jika proses di Actions gagal (tanda silang merah), buka proses itu, salin pesan error
pada langkah **Build debug APK**, dan kirim ke asisten untuk diperbaiki.

## Cara membuat APK dengan Android Studio (laptop)

1. Pasang Android Studio, pilih **Open**, arahkan ke folder proyek ini.
2. Tunggu Gradle selesai sinkron, lalu menu **Build > Build APK(s)**.
3. Hasil: `app/build/outputs/apk/debug/app-debug.apk`.

## Pemakaian pertama

1. Buka aplikasi, masuk dengan akun LMS seperti biasa.
2. Izinkan notifikasi ketika diminta.
3. Ketuk ikon kucing di kiri bawah untuk membuka pengaturan tema.
4. Tab **Ingat** > **Hubungkan kalender LMS**. Aplikasi membuka halaman ekspor kalender
   LMS dan mengambil link kalender Anda otomatis. Tunggu pesan hijau "Kalender terhubung".
   (Jika link tidak muncul, ketuk **Get calendar URL** sendiri.)
5. Atur kapan pengingat berbunyi (misalnya absen 30 dan 10 menit sebelum, tugas 1 hari dan 3 jam sebelum).

## Catatan penting

- Kalender disinkron otomatis kira-kira tiap 3 jam dan setiap aplikasi dibuka. Jadwal yang
  ditambahkan dosen mendadak baru terbaca pada sinkron berikutnya (atau ketuk **Sinkron sekarang**).
- Beberapa HP (Xiaomi, Oppo, Vivo, Realme, Samsung hemat baterai) menghentikan aplikasi di latar.
  Agar pengingat tepat waktu: Pengaturan HP > Aplikasi > LMS Warmadewa > Baterai > **Tidak dibatasi**,
  dan aktifkan **Mulai otomatis** (autostart) jika ada.
- Link kalender berisi token pribadi. Jangan dibagikan ke orang lain.
- Login lewat tombol Google/Microsoft di dalam aplikasi bisa ditolak oleh penyedia login
  (kebijakan keamanan WebView). Gunakan login username dan kata sandi LMS.
- APK ini bertanda tangan debug, cukup untuk pemakaian pribadi (bukan untuk Play Store).
