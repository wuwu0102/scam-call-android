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

本專案包含兩個 workflow：

1. `android-build.yml`（CI）
   - 觸發時機：`push`、`pull_request`
   - 執行項目：
     - `gradle build`
     - `gradle lint`
     - `gradle assembleDebug`
   - 成功後上傳 Debug APK Artifact：`AlertaNumeroMX-debug-apk`
   - 任一階段失敗即標記 workflow failed（PR 需通過檢查才可 merge）

2. `release-apk.yml`（手動 Release APK Build）
   - 觸發時機：`workflow_dispatch`（手動執行）
   - 執行項目：
     - `gradle assembleDebug`
   - 成功後上傳 Release APK Artifact：`AlertaNumeroMX-release-apk`
   - 不包含簽名金鑰，不進行 Play Store 發佈

## Build 方法

```bash
gradle clean build
gradle lint
gradle assembleDebug
```

## APK 輸出位置

- Debug APK（本地）：`app/build/outputs/apk/debug/app-debug.apk`
- Release APK（本地）：`app/build/outputs/apk/release/`
- CI Artifact（Debug）：`AlertaNumeroMX-debug-apk`
- CI Artifact（Release）：`AlertaNumeroMX-release-apk`

## 如何下載 APK（GitHub Actions Artifact）

1. 進入 GitHub Repo 的 **Actions** 頁面。
2. 選擇 workflow：
   - Debug：`Android Build`
   - Release：`Release APK Build`
3. 點選成功的 workflow run。
4. 在頁面下方 **Artifacts** 區塊下載：
   - `AlertaNumeroMX-debug-apk` 或
   - `AlertaNumeroMX-release-apk`

## 如何測試 Android App

### 本地測試（Android Studio）

1. 開啟專案並完成 Gradle Sync。
2. 執行 `app` 到模擬器或實機。
3. 測試基本流程：
   - 首頁載入
   - 手動查詢號碼
   - 更新資料按鈕

### CLI 測試命令

```bash
./gradlew build
./gradlew assembleDebug
```

### 安裝 APK 測試（可選）

可將下載的 debug APK 安裝到測試裝置進行驗證。


### Phase 1（可測試版本）
- App 首頁顯示：`Alerta Número MX`、資料筆數、最後更新時間、手動更新按鈕。
- App 會從 GitHub Pages / GitHub raw JSON 下載 scam database（含 fallback URL 與 retry）。
- 支援手動輸入電話並查詢結果：`Seguro / Sospechoso / No encontrado`。
- 電話格式比對支援：`+52`、`52`、10 位墨西哥號碼，並自動清理空格 / 括號 / 橫線。
- API 失敗時不會 crash，會在畫面顯示錯誤訊息。
- 已保留後續擴充 Room local cache 的資料層結構。

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

## AI Development Workflow

- AI/Codex 變更必須使用小步 PR（single-topic, rollback-friendly）。
- 嚴禁大規模重構、嚴禁修改 package name `com.alertanumero.mx`。
- 功能變更必須通過：
  - `./gradlew build`
  - `./gradlew lint`
  - `./gradlew assembleDebug`
- 不可移除安全、隱私、fallback 保護。

## Branch Protection Recommendation

請在 GitHub 網頁手動設定：
`Settings → Branches → Add rule`

建議啟用：
- Require pull request before merging
- Require status checks to pass before merging
- Require branches to be up to date before merging

此設定可確保狀態檢查（例如 build/lint）通過後才可 merge，避免壞掉的 PR 進入 `main`。

## How to Download Debug APK

1. 進入 **Actions**。
2. 開啟 `Android Build` 的成功 workflow run。
3. 在 **Artifacts** 下載 `AlertaNumeroMX-debug-apk`。

## How to Run Manual Release APK Workflow

1. 進入 **Actions**。
2. 選擇 `Release APK Build` workflow。
3. 點擊 **Run workflow**（`workflow_dispatch`）。
4. 成功後在 **Artifacts** 下載 `AlertaNumeroMX-release-apk`。

> 注意：目前不包含 signing keystore，不會發佈 Play Store。手動 Release workflow 先產出 debug APK 並統一上傳 `app/build/outputs/apk/**/*.apk`。

## PR Rules

每個 PR 必須包含：
1. 修改目的
2. 修改檔案
3. 測試結果
4. 是否影響現有功能
5. rollback 方法

另外每個 PR 僅允許一個主題，並附上 CI 結果。

## Issue Templates

已新增：
- `bug_report.yml`
- `feature_request.yml`
- `ai_task.yml`

可用來強制收集重現資訊、需求邊界、風險等級與驗收條件。

## ADR 文件說明

已新增 `docs/adr`：
- `0001-independent-android-repo.md`
- `0002-github-pages-shared-database.md`
- `0003-phase-1-no-firebase.md`
- `0004-ai-guardrails-and-small-prs.md`

用於記錄重要架構決策與 AI 治理準則，避免後續協作出現方向漂移。
