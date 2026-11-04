#!/usr/bin/env bash
# Undo everything: re-enable Amazon apps and remove Vibe.  Usage: ./restore.sh 192.168.1.50
cd "$(dirname "$0")"
IP="${1:?Usage: ./restore.sh <firestick-ip-address>}"
DEV="$IP:5555"
adb connect "$DEV"
grep -v '^\s*#' debloat-packages.txt | sed '/^\s*$/d' | while read -r P; do
  adb -s "$DEV" shell pm enable --user 0 "$P" >/dev/null 2>&1 && echo "  on: $P"
done
# Also re-enable the update packages in case you uncommented them.
for P in com.amazon.device.software.ota com.amazon.device.software.ota.override com.amazon.tv.forcedotaupdater.v2; do
  adb -s "$DEV" shell pm enable --user 0 "$P" >/dev/null 2>&1
done
adb -s "$DEV" uninstall com.tvvibe.launcher
echo "Restored. Your Firestick is back to stock."
