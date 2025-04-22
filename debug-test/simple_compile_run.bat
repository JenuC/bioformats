@echo off
setlocal

if "%~1"=="" (
    echo Usage: simple_compile_run.bat ^<path-to-sdt-file^>
    goto :end
)

REM Create build directory
if not exist build mkdir build

REM Compile the debug tool
echo Compiling SimpleSDTReaderDebug...
javac -d build src\SimpleSDTReaderDebug.java

if %errorlevel% neq 0 (
    echo Compilation failed
    goto :end
)

echo Compilation successful

REM Run the tool
echo Running SimpleSDTReaderDebug on %~1...
java -cp build SimpleSDTReaderDebug "%~1"

:end
endlocal 