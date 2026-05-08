# ADR 0003: No Firebase/Login/Backend in Phase 1

## Status
Accepted

## Context
導入 Firebase、帳號系統與後端會增加風險與時程，且本階段重點是提供可用查詢與穩定 APK 輸出。

## Decision
Phase 1 不導入 Firebase、login 或自建 backend，僅保留擴充介面。

## Consequences
- 降低初期開發與維運複雜度。
- 降低隱私與安全風險面。
- 後續 Phase 2/3 可透過 ADR 再評估導入策略。
