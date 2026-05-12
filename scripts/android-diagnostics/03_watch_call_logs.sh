#!/usr/bin/env bash
set -u

FILTER_PATTERN="ScamCallScreening|ScamCall|CallScreeningService|PHONE_STATE|IncomingCallReceiver|AlertNotificationHelper|Telecom|RoleManager"

echo "== [1/3] 檢查 adb 是否存在 =="
if ! command -v adb >/dev/null 2>&1; then
  echo "[ERROR] 找不到 adb。請先安裝 Android platform-tools。"
  exit 1
fi

echo "== [2/3] 清空 logcat =="
if ! adb logcat -c; then
  echo "[WARNING] 無法清空 logcat，將繼續監看。"
fi

echo "== [3/3] 開始監看相關 log =="
echo "現在請用另一支實體手機打進來，不要用 LINE/WhatsApp/VoIP，不要用 Wi-Fi Calling"
echo "過濾關鍵字：${FILTER_PATTERN}"

adb logcat | grep -E "${FILTER_PATTERN}"
