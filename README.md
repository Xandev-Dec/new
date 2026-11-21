# JagaAnak — Aplikasi Kontrol Orang Tua (Android, Kotlin + Compose)

Fitur:
- PIN orang tua (disimpan sebagai hash SHA-256)
- Batas waktu layar harian
- Blokir aplikasi tertentu
- Statistik pemakaian per aplikasi hari ini
- Layar blokir + buka sementara 15 menit dengan PIN
- Berjalan otomatis setelah perangkat restart
- Transparan: notifikasi permanen "JagaAnak aktif" selalu tampil bagi anak

Cara build:
1. Buka folder ini di Android Studio (Koala atau lebih baru), tunggu Gradle sync.
2. Run di HP anak (Android 8.0+).
3. Buat PIN, aktifkan 2 izin (akses penggunaan & tampil di atas aplikasi lain), nyalakan "Pengawasan aktif".
4. Build > Build APK(s) untuk menghasilkan file .apk.

Pengembangan lanjutan: jam tidur, sinkron ke HP orang tua (Firebase), berbagi lokasi
dengan persetujuan anak, filter web, laporan mingguan, perlindungan uninstall (Device Admin).
