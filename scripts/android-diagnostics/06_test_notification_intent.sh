#!/usr/bin/env bash
set -u

PACKAGE_NAME="com.alertanumero.mx"

echo "== [1/2] 嘗試啟動 MainActivity（monkey）=="
if ! adb shell monkey -p "${PACKAGE_NAME}" 1; then
  echo "[WARNING] 無法透過 monkey 啟動 ${PACKAGE_NAME}，請確認裝置已連線且 app 已安裝。"
fi

echo "== [2/2] 手動通知測試提醒 =="
echo "請在 app 內按『Probar notificación』進行通知 / full-screen 顯示測試。"
echo "此腳本不會偽造來電，也不會要求高風險權限。"
