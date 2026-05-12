#!/usr/bin/env bash
set -u

PACKAGE_NAME="com.alertanumero.mx"

run_section() {
  local title="$1"
  local cmd="$2"

  echo
  echo "== ${title} =="
  if ! eval "$cmd"; then
    echo "[WARNING] 指令失敗：${cmd}"
  fi
}

echo "開始檢查角色與服務狀態：${PACKAGE_NAME}"

run_section "[1/6] 檢查 package 是否安裝" "adb shell pm list packages | sed 's/\r$//' | grep '${PACKAGE_NAME}'"

run_section "[2/6] default dialer（dumpsys telecom）" "adb shell dumpsys telecom | sed 's/\r$//' | grep -iE 'default dialer|com.google.android.dialer|${PACKAGE_NAME}'"

run_section "[3/6] role holders: CALL_SCREENING" "adb shell cmd role holders android.app.role.CALL_SCREENING | sed 's/\r$//'"

run_section "[4/6] dumpsys role（篩關鍵字）" "adb shell dumpsys role | sed 's/\r$//' | grep -iE '${PACKAGE_NAME}|call_screening|default dialer|com.google.android.dialer'"

run_section "[5/6] dumpsys telecom（篩關鍵字）" "adb shell dumpsys telecom | sed 's/\r$//' | grep -iE '${PACKAGE_NAME}|CallScreeningService|default dialer|com.google.android.dialer'"

run_section "[6/6] dumpsys package（篩關鍵字）" "adb shell dumpsys package ${PACKAGE_NAME} | sed 's/\r$//' | grep -iE '${PACKAGE_NAME}|CallScreeningService|BIND_SCREENING_SERVICE'"

echo
echo "完成：角色與服務狀態檢查結束。"
