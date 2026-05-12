#!/usr/bin/env bash
set -u

echo "== [1/4] 檢查 adb 是否存在 =="
if ! command -v adb >/dev/null 2>&1; then
  echo "[ERROR] 找不到 adb。請先安裝 Android platform-tools。"
  exit 1
fi

echo "== [2/4] 顯示 adb devices =="
adb devices

echo "== [3/4] 讀取裝置資訊（品牌 / 型號）=="
manufacturer="$(adb shell getprop ro.product.manufacturer 2>/dev/null | tr -d '\r')"
model="$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')"

echo "Manufacturer: ${manufacturer:-unknown}"
echo "Model: ${model:-unknown}"

echo "== [4/4] 讀取 Android 版本與 SDK =="
android_version="$(adb shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')"
sdk="$(adb shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')"

echo "Android version: ${android_version:-unknown}"
echo "SDK: ${sdk:-unknown}"

echo "完成：裝置檢查結束。"
