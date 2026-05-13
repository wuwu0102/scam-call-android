# Android Caller ID 診斷腳本

這組腳本用於 `com.alertanumero.mx`（Alerta Número MX）在**真實電話來電情境**下的 Android Caller ID / Call Screening 行為檢查。

## 事前準備

1. 安裝 Android platform-tools（需包含 `adb`）。
2. 手機開啟 **Developer options** 與 **USB debugging**。
3. USB 連線後執行：

```bash
adb devices
```

確認裝置狀態為 `device`。

## 正式測試流程

1. 安裝 app。
2. 啟用通知。
3. 啟用 Caller ID / Call Screening。
4. 關閉 Wi-Fi Calling。
5. 重開機。
6. 第一通可能較慢，先記錄。
7. 第二通通常更準，作為正式結果。
8. 用 `adb logcat` 確認是否有：
   - `onScreenCall entered`
   - `respondAllow`
   - `showCallAlert`
   - `CallScreeningServiceFilter: com.alertanumero.mx scheduled/done`

## 建議腳本執行順序

a. `./scripts/android-diagnostics/01_check_device.sh`

b. `./scripts/android-diagnostics/02_check_roles.sh`

c. `./scripts/android-diagnostics/03_watch_call_logs.sh`

d. 用另一支**實體手機**打進來（不要用 LINE/WhatsApp/VoIP）

e. `./scripts/android-diagnostics/04_collect_report.sh`

## 其他輔助腳本

- `05_reset_and_rebind_hint.sh`：提供手動重新綁定提示（不做危險自動操作）。
- `06_test_notification_intent.sh`：用 `adb` 啟動 app，方便你在 app 內手動按「Probar notificación」。

## Pixel / Google Phone 補充判定

- 在 Pixel / Google Phone 上，**heads-up notification** 是主要成功標準。
- 不再要求 overlay / 浮動視窗一定要蓋在原生電話畫面上。
- 重開機後第一通可先記錄，**第二通測試結果作為正式判定**。

## 新測試標準（兩層模式）

1. Ideal mode
   - `CallScreeningService` detectado
   - 有號碼可用
   - 可比對資料庫並顯示正式分類告警

2. Compatible mode
   - Solo `PHONE_STATE`
   - Android 不提供號碼（`missing_EXTRA_INCOMING_NUMBER`）
   - App 顯示 **「📞 Llamada detectada」** heads-up notification

### Pixel / Google Phone 成功條件更新

若 Pixel / Google Phone 只提供 `PHONE_STATE`，不再要求一直更換全新陌生門號。此情境下測試成功標準改為：
- 來電時出現「Llamada detectada」heads-up notification
- App 內顯示 `PHONE_STATE detected: yes`
