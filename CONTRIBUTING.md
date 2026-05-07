# CONTRIBUTING.md

## Branching

- 建議使用 feature branches：`feature/<topic>`
- 禁止 force push `main`

## Development Flow

1. Fork / branch
2. 實作最小可回滾變更
3. 本地跑 `gradle build lint assembleDebug`
4. 發 PR 並通過 GitHub Actions

## Code Standards

- Kotlin 優先
- 遵循 MVVM 與單一職責
- 不做大型重構，優先 patch
- UI 必須支援深色模式與長字串

## Commit Message

請使用清楚、可追溯的訊息，例如：

- `chore: scaffold Android MVVM baseline`
- `feat: add phase1 search input state`
