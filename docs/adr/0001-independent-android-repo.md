# ADR 0001: Android Repo Independent from scam-call-database

## Status
Accepted

## Context
Android App 與資料來源維護節奏不同，且 App 需要獨立 CI/CD、發版與權限管理。若與 `scam-call-database` 混合，會提高變更耦合與風險。

## Decision
`scam-call-android` 維持獨立 repository，不直接修改或耦合 `scam-call-database` 的程式流程。

## Consequences
- Android 可以獨立做 Build、Lint、APK Artifact。
- App 改動不會直接影響資料庫 repo。
- 便於使用 branch protection 與小步 PR 管理風險。
