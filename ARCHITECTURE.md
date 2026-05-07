# ARCHITECTURE.md

## Overview

`Alerta Número MX` 採用 Android 官方推薦的分層 MVVM 架構，並保持 Android Repo 與 iOS/Web 系統完全解耦。

## Layers

- **UI Layer**: Jetpack Compose + ViewModel
- **Domain Layer**: Use cases（Phase 2/3 擴充）
- **Data Layer**: Repository + Retrofit API + Room Local DB

## Package Structure

- `com.alertanumero.mx.ui`：畫面與狀態管理
- `com.alertanumero.mx.domain`：商業邏輯（預留）
- `com.alertanumero.mx.data.remote`：遠端 API
- `com.alertanumero.mx.data.local`：Room entities/dao/database
- `com.alertanumero.mx.data.repository`：資料整合與 fallback

## Extensibility

- **Retrofit**：可接 GitHub Raw JSON / 自建 API
- **Room**：可作離線查詢與快取
- **Coroutines**：統一非同步流程
- **Firebase（future）**：可於 Phase 3 加入，不破壞核心架構

## CI/CD

GitHub Actions 於每次 push/PR 執行 build + lint + debug APK，確保協作品質。
