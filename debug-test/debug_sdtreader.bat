@echo off
setlocal enabledelayedexpansion

REM This script compiles and runs the SDTReaderDebugger

echo Setting up SDTReaderDebugger...

REM Create directories
if not exist "lib" mkdir lib
if not exist "build" mkdir build

REM Download Bio-Formats if needed
if not exist "lib\bioformats_package.jar" (
    echo Downloading Bio-Formats package...
    powershell -Command "& {Invoke-WebRequest -Uri 'https://downloads.openmicroscopy.org/bio-formats/6.8.0/artifacts/bioformats_package.jar' -OutFile 'lib\bioformats_package.jar'}"
)

REM Compile the debugger
echo Compiling SDTReaderDebugger...
javac -cp "lib\bioformats_package.jar" -d build src\SDTReaderDebugger.java

REM Run the debugger if file path was provided
if "%~1"=="" (
    echo Usage: debug_sdtreader.bat ^<sdt_file_path^>
    echo.
    echo Example:
    echo   debug_sdtreader.bat test_bochum.sdt
) else (
    echo Running SDTReaderDebugger on %1
    java -cp "lib\bioformats_package.jar;build" SDTReaderDebugger "%~1"
)

endlocal 