@echo off
setlocal EnableExtensions EnableDelayedExpansion
set REQUIRED_GRADLE_VERSION=9.7.0
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%
set WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if exist "%WRAPPER_JAR%" goto wrapper

where gradle >NUL 2>&1
if %ERRORLEVEL% equ 0 (
  set INSTALLED_GRADLE_VERSION=
  for /f "tokens=2" %%G in ('gradle --version ^| findstr /B /C:"Gradle "') do set INSTALLED_GRADLE_VERSION=%%G
  if not "!INSTALLED_GRADLE_VERSION!"=="%REQUIRED_GRADLE_VERSION%" (
    echo TempoTrack requires Gradle %REQUIRED_GRADLE_VERSION% when gradle-wrapper.jar is absent; found !INSTALLED_GRADLE_VERSION!.
    echo Install Gradle %REQUIRED_GRADLE_VERSION% or generate gradle\wrapper\gradle-wrapper.jar with "gradle wrapper --gradle-version %REQUIRED_GRADLE_VERSION%".
    exit /b 1
  )
  gradle %*
  exit /b !ERRORLEVEL!
)

echo TempoTrack requires Gradle %REQUIRED_GRADLE_VERSION%. Install it or generate gradle\wrapper\gradle-wrapper.jar with "gradle wrapper --gradle-version %REQUIRED_GRADLE_VERSION%".
exit /b 1

:wrapper
if defined JAVA_HOME (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_EXE=java.exe
)
"%JAVA_EXE%" -Xmx64m -Xms64m -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
