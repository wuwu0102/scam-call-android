# ADR 0002: Phase 1 Uses GitHub Pages Shared JSON Database

## Status
Accepted

## Context
Phase 1 目標是快速可用、低維運成本，先完成查詢流程與 fallback 行為，不導入複雜後端。

## Decision
Phase 1 先讀取 GitHub Pages/Raw 提供的 shared JSON database，並保留 fallback URL、timeout、retry 與錯誤訊息顯示。

## Consequences
- 可快速驗證 Caller ID/號碼查詢流程。
- API 失敗時可 fallback，降低不可用風險。
- 後續可在不破壞現行流程下替換為更完整資料服務。
