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

## 建議測試流程

請依序執行：

a. `./scripts/android-diagnostics/01_check_device.sh`

b. `./scripts/android-diagnostics/02_check_roles.sh`

c. `./scripts/android-diagnostics/03_watch_call_logs.sh`

d. 用另一支**實體手機**打進來（不要用 LINE/WhatsApp/VoIP）

e. `./scripts/android-diagnostics/04_collect_report.sh`

## 其他輔助腳本

- `05_reset_and_rebind_hint.sh`：提供手動重新綁定提示（不做危險自動操作）。
- `06_test_notification_intent.sh`：用 `adb` 啟動 app，方便你在 app 內手動按「Probar notificación」。
