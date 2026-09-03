# 🌟 Nell Downloader

**Nell Downloader** adalah aplikasi Android native berkinerja tinggi untuk mengunduh media universal (HLS/M3U8, Video, Audio) yang dirancang dengan antarmuka **Luminous Glassmorphism** modern berbasis Jetpack Compose, Media3 ExoPlayer, Room Database, dan arsitektur MVVM.

---

## 🚀 Panduan Setup & Build di Android (Termux PRoot Distro)

Anda dapat meng-compile dan mem-build file APK **Nell Downloader** langsung dari smartphone Android Anda menggunakan **Termux** dan **PRoot Distro** (Ubuntu / Debian).

---

### 📋 Prasyarat di Termux

1. **Aplikasi Termux**: Disarankan menginstal Termux dari [F-Droid](https://f-droid.org/packages/com.termux/) atau GitHub Releases (jangan gunakan versi Google Play Store karena sudah usang).
2. **Koneksi Internet** yang stabil untuk mengunduh OpenJDK, Android SDK Command-line Tools, dan dependensi Gradle.
3. **Ruang Penyimpanan (Storage)**: Minimal 3–5 GB ruang kosong di perangkat Anda.

---

### 🛠️ Langkah 1: Persiapan Termux & Instalasi PRoot Distro

Buka aplikasi **Termux** di HP Anda, lalu jalankan perintah berikut:

```bash
# 1. Izinkan akses penyimpanan Android
termux-setup-storage

# 2. Update package Termux dan install proot-distro & git
pkg update -y && pkg upgrade -y
pkg install -y proot-distro git wget curl

# 3. Install distro Linux (disarankan Ubuntu)
proot-distro install ubuntu
```

---

### 🐧 Langkah 2: Masuk ke PRoot Ubuntu dengan Akses Storage

Masuk ke lingkungan PRoot Ubuntu dengan bind-mount direktori penyimpanan internal HP (`/sdcard`):

```bash
proot-distro login ubuntu --shared-tmp --bind /sdcard:/sdcard
```

*(Sekarang prompt terminal Anda akan berubah menjadi `root@localhost:~#`)*

---

### 📂 Langkah 3: Masuk ke Direktori Project

Jika file project sudah ada di penyimpanan HP Anda (misal di folder Download), Anda bisa langsung masuk ke foldernya:

```bash
# Contoh jika ditaruh di folder internal HP:
cd /sdcard/Download/NellDownloader

# Atau jika Anda clone langsung di dalam PRoot:
# git clone <repository-url>
# cd NellDownloader
```

---

### ⚡ Langkah 4: Jalankan Skrip Setup Otomatis

Kami telah menyediakan skrip **`setup-termux-proot.sh`** yang otomatis mengonfigurasi OpenJDK 17, Android SDK, Command-line Tools, dan lisensi Google SDK:

```bash
# Berikan izin eksekusi skrip
chmod +x setup-termux-proot.sh build-termux.sh gradlew

# Jalankan setup
bash setup-termux-proot.sh
```

**Skrip ini akan secara otomatis:**
1. Menginstal `openjdk-17-jdk`, `wget`, `curl`, `unzip`, `git`, `tar`, dan `ca-certificates`.
2. Mengunduh & mengonfigurasi **Android SDK Command-line Tools** di `$HOME/android-sdk`.
3. Menyetujui semua **Android SDK Licenses** secara otomatis (`sdkmanager --licenses`).
4. Mengunduh platform SDK `android-36` dan `build-tools;34.0.0`.
5. Menambahkan `ANDROID_HOME`, `JAVA_HOME`, dan `PATH` ke file `~/.bashrc`.

---

### 🔨 Langkah 5: Build File APK

Setelah setup selesai, jalankan skrip build berikut:

```bash
# Build Debug APK (Rekomendasi untuk testing & instalasi)
bash build-termux.sh debug

# Atau untuk Release APK:
# bash build-termux.sh release
```

Skrip akan secara otomatis:
- Menjalankan build dengan optimasi memori RAM (mencegah Termux crash / OOM killer).
- Meng-compile project ke format APK.
- Menyalin file APK hasil build ke folder `./build-output/NellDownloader-debug.apk`.
- Jika folder `/sdcard/Download` terdeteksi, file APK juga otomatis disalin ke folder **Download** di HP Anda agar bisa langsung diinstal melalui File Manager!

---

### 📱 Langkah 6: Install APK di Smartphone Anda

1. Buka aplikasi **File Manager** / Pengelola File bawaan HP Anda.
2. Buka folder **Download**.
3. Ketuk file **`NellDownloader-debug.apk`** lalu pilih **Install**.
4. Selesai! Aplikasi Nell Downloader siap digunakan.

---

## ⚙️ Build Manual Menggunakan `./gradlew` (Alternatif)

Jika Anda ingin menjalankan perintah Gradle secara manual di dalam PRoot Distro:

```bash
# Muat environment variable Android SDK
source ~/.bashrc

# Jalankan build debug APK tanpa daemon
./gradlew assembleDebug --no-daemon -Dorg.gradle.jvmargs="-Xmx2048m -XX:MaxMetaspaceSize=512m"
```

File APK yang dihasilkan akan berada di:
`app/build/outputs/apk/debug/app-debug.apk`

---

## ❓ Troubleshooting (Pemecahan Masalah)

### 1. `Killed` atau `Out of Memory (OOM)` saat compile
**Penyebab:** HP kekurangan RAM bebas saat Gradle menjalankan multi-threading compiler.  
**Solusi:**
- Jalankan build dengan parameter `--max-workers=1` atau `--max-workers=2`:
  ```bash
  ./gradlew assembleDebug --no-daemon --max-workers=1 -Dorg.gradle.jvmargs="-Xmx1536m"
  ```
- Tutup aplikasi lain yang berjalan di background HP Anda sebelum melakukan build.

### 2. `Permission denied` saat menjalankan `./gradlew` atau `.sh`
**Solusi:**
```bash
chmod +x ./gradlew setup-termux-proot.sh build-termux.sh
```

### 3. `Failed to find Build Tools revision 34.0.0`
**Solusi:**
Pastikan skrip setup sudah dijalankan, atau unduh manual melalui sdkmanager:
```bash
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "build-tools;34.0.0" "platforms;android-36"
```

### 4. `AAPT2 aarch64 execution error`
**Solusi:**
Gradle modern otomatis mengunduh binary AAPT2 yang kompatibel dengan arsitektur Linux ARM64 (aarch64). Pastikan paket `libc6` terinstal dan environment PRoot Anda berjalan di `aarch64` (dapat dicek dengan perintah `uname -m`).

---

## 📦 Struktur Skrip Build Termux

- **`setup-termux-proot.sh`**: Skrip instalasi satu langkah untuk OpenJDK 17, Android SDK Tools, platform tools, dan license auto-accept.
- **`build-termux.sh`**: Skrip runner build APK dengan alokasi RAM optimal, pengecekan env, dan auto-copy APK ke penyimpanan Android.
- **`gradlew`**: Gradle Wrapper POSIX shell script.

---

## 💡 Fitur Utama Nell Downloader

- 🧊 **Luminous Glassmorphism UI**: Tampilan transparan frosted glass dengan tema dinamis dark/light mode.
- 📡 **Universal HLS / M3U8 Master Parser**: Resolusi nested multi-variant stream, bandwidth, dan segment extraction.
- 🔐 **AES-128 Stream Decryptor**: Otomatis mendekripsi chunk HLS terproteksi secara on-the-fly.
- ⚡ **Multi-Segment Downloader**: Download paralel segment HLS & direct Range-chunking dengan retry otomatis.
- 🎬 **Built-in Media3 ExoPlayer**: Preview langsung video/audio hasil unduhan dalam aplikasi.
- 🌐 **Integrated Browser**: Deteksi dan inspeksi tautan media streaming langsung dari web page.
- 🗄️ **Room Database**: Riwayat pencarian dan download manager persistent.
