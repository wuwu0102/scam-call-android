# AGENTS Guidelines for scam-call-android

## Core Safety Rules

所有修改必須：
- 不破壞既有功能
- 不直接刪除功能
- 優先 patch
- 保持向下相容
- 所有 AI / Codex 修改必須以小步 PR 進行

禁止：
- 大規模重構
- 任意更換架構
- 任意升級 major dependency
- 任意改 package name（固定為 `com.alertanumero.mx`）
- 直接 force push `main`
- 移除既有 GitHub Actions
- 移除安全、隱私、資料來源 fallback 機制

## UI Rules

所有 UI 修改：
- 必須 mobile-first
- 必須支援 dark mode
- 必須避免文字超出
- 必須適合西班牙文長字串（不可爆版）

## Data & Networking Rules

所有資料來源：
- 必須可 fallback
- API fail 不可 crash
- 必須有 timeout
- 必須有 retry
- database fetch 必須有 fallback / error message
- caller ID / 電話號碼比對不得上傳私人通話紀錄

## Git & Change Management

所有 commit：
- 必須可 rollback
- commit message 必須清楚
- 不可直接 force push main

所有功能修改必須通過：
- `./gradlew build`
- `./gradlew lint`
- `./gradlew assembleDebug`

每次 PR 必須說明：
1. 修改目的
2. 修改檔案
3. 測試結果
4. 是否影響現有功能
5. rollback 方法

## GitHub Actions

- build fail 必須停止 merge
- release build 與 debug build 分離
