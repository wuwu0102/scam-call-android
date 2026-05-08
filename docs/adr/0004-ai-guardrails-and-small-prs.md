# ADR 0004: AI Guardrails, Small PRs, and GitHub Actions Gates

## Status
Accepted

## Context
AI/Codex 可加速交付，但若缺乏邊界，容易出現大範圍變更、品質回歸或直接破壞 main。

## Decision
使用 AGENTS.md + PR Template + Issue Templates + GitHub Actions（build/lint/assembleDebug）做治理，並要求 AI 變更採小步 PR。

## Consequences
- 提高變更可追溯與可回滾性。
- PR 在合併前需先通過品質檢查。
- 降低 AI 改壞主幹與隱私風險。
