# Alerta Número MX (Android)

Alerta Número MX 是一個獨立的 Android Caller ID 應用程式專案，專門提供墨西哥電話號碼的可疑風險查詢。此 Repo 與既有 iOS / Web 主系統分離，未來所有 Android 功能都在本專案中維護。

## 專案目標

- 獨立 Android 應用（不影響既有 iOS / Web）
- Kotlin + Android Studio 官方架構（MVVM）
- 支援長期 AI / Codex 維護與多人協作
- 可由 GitHub Actions 自動建置 APK
- 預留 Firebase / API / Room 本地資料庫擴充

## 架構圖

```text
scam-call-android/
├── app/                         # Android app module
│   ├── src/main/java/com/alertanumero/mx/
│   │   ├── data/                # API, Room, Repository
│   │   ├── domain/              # Use cases (future)
│   │   ├── ui/                  # Compose UI + ViewModel
│   │   └── MainActivity.kt
│   ├── src/main/res/
│   └── build.gradle.kts
├── docs/                        # 架構與流程文件
├── scripts/                     # 開發輔助腳本
└── .github/workflows/           # CI/CD workflows
```

## 安裝方式

1. 安裝 Android Studio（建議最新版 Stable）。
2. 以 Android Studio 開啟本專案根目錄。
3. 等待 Gradle Sync 完成。
4. 選擇模擬器或實機後執行 `app`。

## Android Studio / SDK 需求

- Android Studio：最新版 Stable
- JDK：17
- Kotlin：2.0.x
- Min SDK：26
- Target SDK：34（最新穩定）
- Compile SDK：34

## GitHub Actions 說明

本專案包含 `android-build.yml`：

- 觸發時機：`push`、`pull_request`
- 執行項目：
  - `gradle build`
  - `gradle lint`
  - `gradle assembleDebug`
- 成功後上傳 Debug APK Artifact
- 任一階段失敗即標記 workflow failed（PR 需通過檢查才可 merge）

## Build 方法

```bash
gradle clean build
gradle lint
gradle assembleDebug
```

## APK 輸出位置

- 本地：`app/build/outputs/apk/debug/app-debug.apk`
- CI Artifact：`app-debug-apk`

## Roadmap

### Phase 1
- App 可啟動與主畫面
- 從 GitHub JSON 下載 scam database
- 顯示資料更新時間
- 手動搜尋號碼
- 顯示是否為可疑號碼

### Phase 2
- Android Caller ID API
- 來電辨識
- 本地快取與自動更新

### Phase 3
- Firebase
- 社群回報與雲端同步
- AI 風險分析
