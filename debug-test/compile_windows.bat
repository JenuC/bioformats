@echo off
setlocal

REM Create build directory
if not exist build mkdir build

REM Find all the source files we need
set FORMATS_API_SRC=..\components\formats-api\src
set FORMATS_GPL_SRC=..\components\formats-gpl\src
set FORMATS_BSD_SRC=..\components\formats-bsd\src
set COMMON_SRC=..\components\ome-common\src

REM Set up classpath for compilation
set CLASSPATH=.;%FORMATS_API_SRC%;%FORMATS_GPL_SRC%;%FORMATS_BSD_SRC%;%COMMON_SRC%

REM Compile the debug tool
echo Compiling SDTReaderDebug...
javac -d build ^
  -cp %CLASSPATH% ^
  src\SDTReaderDebug.java

if %errorlevel% neq 0 (
  echo Compilation failed
  goto :end
)

echo Compilation successful

:end
endlocal 