@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "GRADLE_HOME=%SCRIPT_DIR%gradle-8.8"
set "GRADLE_ZIP=%SCRIPT_DIR%gradle-8.8-bin.zip"

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%GRADLE_ZIP%" (
    echo [ERROR] Gradle distribution not found: "%GRADLE_ZIP%"
    exit /b 1
  )

  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%GRADLE_ZIP%' -DestinationPath '%SCRIPT_DIR%' -Force"
  if errorlevel 1 (
    echo [ERROR] Failed to extract Gradle distribution.
    exit /b 1
  )
)

call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
