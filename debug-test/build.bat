@echo off
setlocal enabledelayedexpansion

REM Find the Bio-Formats JAR file
set "BIOFORMATS_JAR="
for /r ..\components %%i in (bioformats_package*.jar) do (
    set "BIOFORMATS_JAR=%%i"
    goto :found_jar
)

:check_target
REM If not found, look in the target directory
for /r ..\components\bundles\bioformats_package\target %%i in (bioformats_package*.jar) do (
    echo "%%i" | findstr /v "sources" | findstr /v "javadoc" > nul
    if !errorlevel! equ 0 (
        set "BIOFORMATS_JAR=%%i"
        goto :found_jar
    )
)

echo ERROR: Bio-Formats JAR file not found. Please build the Bio-Formats project first.
goto :end

:found_jar
echo Using Bio-Formats JAR: %BIOFORMATS_JAR%

REM Create build directory
if not exist build mkdir build

REM Compile the debug tool
echo Building SDTReaderDebug...
javac -cp "%BIOFORMATS_JAR%" -d build src\SDTReaderDebug.java
if %errorlevel% neq 0 (
    echo ERROR: Compilation failed.
    goto :end
)
echo Build complete.

:end
endlocal  