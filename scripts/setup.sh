#!/usr/bin/env bash
# Vibe Launcher setup for Mac / Linux.  Usage: ./setup.sh 192.168.1.50
set -e
cd "$(dirname "$0")"
IP="${1:?Usage: ./setup.sh <firestick-ip-address>}"
DEV="$IP:5555"
APK="VibeLauncher.apk"
SVC="com.tvvibe.launcher/com.tvvibe.launcher.HomeWatcherService"
[ -f "$APK" ] || { echo "Put VibeLauncher.apk in this folder first."; exit 1; }

adb connect "$DEV"
echo
echo "On the TV, if you see 'Allow USB debugging?', tick 'Always allow' and choose OK."
read -r -p "Press Enter when done... "

echo "Installing Vibe..."
adb -s "$DEV" install -r "$APK"

echo "Turning on the Home button redirect..."
CUR=$(adb -s "$DEV" shell settings get secure enabled_accessibility_services | tr -d '\r')
if [ -z "$CUR" ] || [ "$CUR" = "null" ]; then NEW="$SVC"
elif echo "$CUR" | grep -q "$SVC"; then NEW="$CUR"
else NEW="$CUR:$SVC"; fi
adb -s "$DEV" shell settings put secure enabled_accessibility_services "$NEW"
adb -s "$DEV" shell settings put secure accessibility_enabled 1

read -r -p "Also switch off Amazon ads & bloatware? (y/n) " ANS
if [ "$ANS" = "y" ]; then
  INSTALLED=$(adb -s "$DEV" shell pm list packages | tr -d '\r')
  grep -v '^\s*#' debloat-packages.txt | sed '/^\s*$/d' | while read -r P; do
    if echo "$INSTALLED" | grep -qx "package:$P"; then
      adb -s "$DEV" shell pm disable-user --user 0 "$P" >/dev/null && echo "  off: $P"
    else
      echo "  (not on this device): $P"
    fi
  done
fi

adb -s "$DEV" shell am start -n com.tvvibe.launcher/.MainActivity >/dev/null
echo "Done! Press Home on the remote to try it."
