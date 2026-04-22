@ECHO OFF
SETLOCAL

SET "MAVEN_VERSION=3.9.9"
SET "WRAPPER_DIR=%~dp0.mvn\wrapper"
SET "ARCHIVE=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip"
SET "DIST_DIR=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%"
SET "MVN_CMD=%DIST_DIR%\bin\mvn.cmd"

IF NOT EXIST "%MVN_CMD%" (
  IF NOT EXIST "%WRAPPER_DIR%" (
    mkdir "%WRAPPER_DIR%"
  )

  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference='SilentlyContinue';" ^
    "$archive='%ARCHIVE%';" ^
    "$wrapperDir='%WRAPPER_DIR%';" ^
    "$distDir='%DIST_DIR%';" ^
    "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile $archive;" ^
    "if (Test-Path $distDir) { Remove-Item -Recurse -Force $distDir };" ^
    "Expand-Archive -Path $archive -DestinationPath $wrapperDir -Force"

  IF ERRORLEVEL 1 (
    EXIT /B %ERRORLEVEL%
  )
)

CALL "%MVN_CMD%" %*
SET "EXIT_CODE=%ERRORLEVEL%"
ENDLOCAL & EXIT /B %EXIT_CODE%
