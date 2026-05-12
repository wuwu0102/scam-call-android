#!/usr/bin/env bash
set -u

PACKAGE_NAME="com.alertanumero.mx"
FILTER_PATTERN="ScamCallScreening|ScamCall|CallScreeningService|PHONE_STATE|IncomingCallReceiver|AlertNotificationHelper|Telecom|RoleManager"
TIMESTAMP="$(date -u +%Y%m%d-%H%M%S)"
REPORT_DIR="reports"
REPORT_PATH="${REPORT_DIR}/android-call-test-${TIMESTAMP}.txt"

mkdir -p "${REPORT_DIR}"

run_to_report() {
  local title="$1"
  local cmd="$2"

  {
    echo
    echo "===== ${title} ====="
    if ! eval "$cmd"; then
      echo "[WARNING] 指令失敗：${cmd}"
    fi
  } >> "${REPORT_PATH}" 2>&1
}

echo "建立診斷報告：${REPORT_PATH}"

echo "Android Caller ID 測試報告" > "${REPORT_PATH}"
echo "UTC Timestamp: ${TIMESTAMP}" >> "${REPORT_PATH}"

run_to_report "device info" "adb shell getprop ro.product.manufacturer && adb shell getprop ro.product.model && adb shell getprop ro.build.version.release && adb shell getprop ro.build.version.sdk"

run_to_report "package install info" "adb shell pm list packages | sed 's/\r$//' | grep '${PACKAGE_NAME}'"

run_to_report "default dialer" "adb shell dumpsys telecom | sed 's/\r$//' | grep -iE 'default dialer|com.google.android.dialer|${PACKAGE_NAME}'"

run_to_report "role holders" "adb shell cmd role holders android.app.role.CALL_SCREENING"

run_to_report "dumpsys telecom relevant lines" "adb shell dumpsys telecom | sed 's/\r$//' | grep -iE '${PACKAGE_NAME}|CallScreeningService|default dialer|com.google.android.dialer'"

run_to_report "dumpsys package ${PACKAGE_NAME} relevant lines" "adb shell dumpsys package ${PACKAGE_NAME} | sed 's/\r$//' | grep -iE '${PACKAGE_NAME}|CallScreeningService|BIND_SCREENING_SERVICE'"

run_to_report "recent logcat 500 lines filtered" "adb logcat -d -t 500 | sed 's/\r$//' | grep -E '${FILTER_PATTERN}'"

echo "完成：報告已輸出到 ${REPORT_PATH}"
