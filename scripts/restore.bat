@echo off
REM Undo everything: re-enable Amazon apps and remove Vibe.  Usage: restore.bat 192.168.1.50
cd /d "%~dp0"
if "%~1"=="" (echo Usage: restore.bat FIRESTICK-IP-ADDRESS & exit /b 1)
set DEV=%~1:5555
adb connect %DEV%
for /f "usebackq eol=# tokens=*" %%p in ("debloat-packages.txt") do (
  adb -s %DEV% shell pm enable --user 0 %%p >nul 2>&1 && echo   on: %%p
)
for %%p in (com.amazon.device.software.ota com.amazon.device.software.ota.override com.amazon.tv.forcedotaupdater.v2) do adb -s %DEV% shell pm enable --user 0 %%p >nul 2>&1
adb -s %DEV% uninstall com.tvvibe.launcher
echo Restored. Your Firestick is back to stock.
pause
