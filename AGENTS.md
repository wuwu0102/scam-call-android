# AGENTS Guidelines for scam-call-android

## Core Safety Rules

所有修改必須：
- 不破壞既有功能
- 不直接刪除功能
- 優先 patch
- 保持向下相容

禁止：
- 大規模重構
- 任意更換架構
- 任意升級 major dependency
- 任意改 package name（固定為 `com.alertanumero.mx`）

## UI Rules

所有 UI 修改：
- 必須 mobile-first
- 必須支援 dark mode
- 必須避免文字超出
- 必須適合西班牙文長字串

## Data & Networking Rules

所有資料來源：
- 必須可 fallback
- API fail 不可 crash
- 必須有 timeout
- 必須有 retry

## Git & Change Management

所有 commit：
- 必須可 rollback
- commit message 必須清楚
- 不可直接 force push main

## GitHub Actions

- build fail 必須停止 merge
- release build 與 debug build 分離
