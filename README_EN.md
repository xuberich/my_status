# 📱 EgoGaze - Real-time Phone Status Mirror

[简体中文](./README.md) | [English](./README_EN.md)

**EgoGaze** is a sleek, elegant, and fully self-hosted personal digital portrait system. It mirrors your phone's physical state to a web interface in real-time, including battery level, location, step count, app usage duration, and music playback.

## 🔗 Live Preview

- **Access**: [https://status.dwill.top](https://status.dwill.top)

## 📸 Screenshots

### 🖥️ Web Interface (PC & Mobile)
| PC Browser View | Mobile Browser View |
| :---: | :---: |
| ![Web PC](./docs/images/web_pc.png) | ![Web Mobile](./docs/images/web_mobile.png) |

### 📱 Android Client (EgoGaze App)
| Status Sync App | Real-time Data Detail |
| :---: | :---: |
| ![App Main](./docs/images/app_main.jpg) | ![App Detail](./docs/images/app_detail.jpg) |

## ✨ Features

- **🚀 Real-time Sync**: Millisecond-level reporting latency with automatic web refresh (304 optimized).
- **📊 Precise Stats**: A re-implemented "Today's Usage Duration" algorithm based on Android `UsageEvents`, completely eliminating data drift.
- **🎵 Media Awareness**: Real-time display of currently playing song titles and artists.
- **🔋 Status Tracking**: Battery percentage, charging status, device temperature, and memory usage.
- **🛡️ Privacy First**: Fully self-hosted, data flows only between your private server and phone.

---

## 📥 Deployment Guide

### 1. Server Deployment (Docker)
Recommended for NAS or 24/7 PCs.

- **Quick Start**:
  ```bash
  cd phone-status/self-hosted
  docker-compose up -d --build
  ```
- **Config**: Default runs on port `9529`. [Click for detailed proxy and config guide](./self-hosted/README.md).

### 2. Phone App Deployment (Android) - Core
The source of all data. Must be correctly configured.

- **Build from Source**:
  1. Open `phone-status/android` in Android Studio.
  2. Select **`selfhostedDebug`** in the `Build Variants` panel.
  3. Build and install the APK.
- **Initial Setup**:
  1. Open App, enter your API URL (e.g., `https://status.dwill.top/api/update`).
  2. Enter API Key (default in `docker-compose.yml`).
  3. **[Important] Permissions**: Grant "Ignore Battery Optimization", "Usage Access", and "Notification Listener".
  4. Tap **[Start Service]**.
- **[Android Stay-Alive & Pitfalls Guide](./docs/android_pitfalls.md)**

### 3. Serverless: Cloudflare Deployment
For users without a public IP. [Cloudflare Deployment Guide](./docs/cloudflare_deploy.md).

---

## 🛠️ Development

### 1. Environment
- **Android**: Tested on **Vivo (Android 14 / OriginOS)**.
- **Server**: Node.js 18+ (TypeScript).

### 2. Docs
- **[Local Development Guide](./docs/local_debug.md)**


## 📜 License

Distributed under the MIT License. See `LICENSE` for more information.
