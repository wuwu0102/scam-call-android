# SECURITY.md

## Privacy & Data Handling

- 不蒐集使用者聯絡人資料。
- 不上傳私人通話紀錄。
- 所有號碼資料以匿名化方式處理。
- 支援離線使用（依本地快取版本）。
- Caller ID 比對以本地資料為主。

## Network Safety

- API 呼叫需設定 timeout 保護。
- API 呼叫需具備 retry/fallback 機制。
- 需具備 rate limit 保護，避免濫用或過量請求。

## Release & Incident

- 所有版本需經 CI 驗證。
- 發現漏洞時以最小影響 patch 方式修復並更新 changelog。
