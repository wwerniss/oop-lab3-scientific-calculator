@echo off
setlocal
set "BASE=%~dp0"
set "WRAPPER=%BASE%gradle\wrapper\gradle-wrapper.jar"
if not exist "%WRAPPER%" (
    echo Downloading the official Gradle 9.6.0 wrapper launcher...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Invoke-WebRequest -UseBasicParsing -Uri 'https://raw.githubusercontent.com/gradle/gradle/v9.6.0/gradle/wrapper/gradle-wrapper.jar' -OutFile '%WRAPPER%'"
    if errorlevel 1 (
        echo Could not download Gradle wrapper jar. Check internet / GitHub access.
        exit /b 1
    )
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$actual=(Get-FileHash -Algorithm SHA256 -LiteralPath '%WRAPPER%').Hash.ToLowerInvariant(); if($actual -ne '497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7'){ throw 'Gradle wrapper jar checksum mismatch' }"
    if errorlevel 1 exit /b 1
)
if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_EXE=java.exe"
)
"%JAVA_EXE%" -classpath "%WRAPPER%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
