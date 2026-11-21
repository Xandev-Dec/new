# JagaAnak — 2 Aplikasi: Orang Tua & Anak

- **JagaAnak Orang Tua** (`id.jagaanak.parent`): buat kode keluarga, lihat pemakaian, atur batas waktu & blokir aplikasi.
- **JagaAnak Anak** (`id.jagaanak.child`): dipasang di HP anak, dihubungkan dengan kode, menerapkan aturan.

Sinkronisasi memakai Firebase (Firestore + Anonymous Auth).

## Setup Firebase (sekali saja)
1. https://console.firebase.google.com → Add project.
2. Tambahkan **dua** aplikasi Android: `id.jagaanak.parent` dan `id.jagaanak.child`.
3. Unduh **google-services.json** (satu file, berisi keduanya).
4. Build > Authentication > Sign-in method > aktifkan **Anonymous**.
5. Build > Firestore Database > Create database. Tab Rules, isi:
```
rules_version = '2';
service cloud.firestore {
  match /databases/{db}/documents {
    match /families/{code} {
      allow read, write: if request.auth != null;
    }
  }
}
```
6. GitHub repo > Settings > Secrets and variables > Actions > New secret:
   nama `GOOGLE_SERVICES_JSON`, isi = seluruh isi file google-services.json.
7. Push → tab Actions → unduh 2 artifact APK.

Build lokal: taruh google-services.json di folder `app/`, lalu `gradle assembleParentDebug assembleChildDebug`.

## Cara pakai
1. Pasang APK Orang Tua di HP orang tua, buat PIN → muncul kode 8 karakter.
2. Pasang APK Anak di HP anak, masukkan kode, centang persetujuan, aktifkan 2 izin.
3. Dari HP orang tua atur batas waktu dan blokir aplikasi; berlaku di HP anak.
