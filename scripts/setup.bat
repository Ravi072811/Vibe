@echo off
REM Vibe Launcher setup for Windows.  Usage: setup.bat 192.168.1.50
cd /d "%~dp0"
if "%~1"=="" (echo Usage: setup.bat FIRESTICK-IP-ADDRESS & exit /b 1)
set DEV=%~1:5555
set SVC=com.tvvibe.launcher/com.tvvibe.launcher.HomeWatcherService
if not exist VibeLauncher.apk (echo Put VibeLauncher.apk in this folder first. & exit /b 1)

adb connect %DEV%
echo.
echo On the TV, if you see "Allow USB debugging?", tick "Always allow" and choose OK.
pause

echo Installing Vibe...
adb -s %DEV% install -r VibeLauncher.apk

echo Turning on the Home button redirect...
set CUR=
for /f "delims=" %%a in ('adb -s %DEV% shell settings get secure enabled_accessibility_services') do set CUR=%%a
if "%CUR%"=="null" set CUR=
set NEW=%SVC%
if not "%CUR%"=="" set NEW=%CUR%:%SVC%
echo %CUR% | find "%SVC%" >nul && set NEW=%CUR%
adb -s %DEV% shell settings put secure enabled_accessibility_services "%NEW%"
adb -s %DEV% shell settings put secure accessibility_enabled 1

set /p ANS=Also switch off Amazon ads and bloatware? (y/n) 
if /i "%ANS%"=="y" (
  for /f "usebackq eol=# tokens=*" %%p in ("debloat-packages.txt") do (
    adb -s %DEV% shell pm disable-user --user 0 %%p >nul 2>&1 && echo   off: %%p
  )
)

adb -s %DEV% shell am start -n com.tvvibe.launcher/.MainActivity >nul
echo Done! Press Home on the remote to try it.
pause
