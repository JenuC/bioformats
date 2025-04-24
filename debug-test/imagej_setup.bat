@echo off
setlocal enabledelayedexpansion

REM ImageJ setup script for debugging SDT files
REM This script downloads ImageJ and sets up Bio-Formats

echo Setting up ImageJ debugging environment...

REM Create directories
if not exist "lib" mkdir lib
if not exist "imagej" mkdir imagej

REM Download dependencies (if not already present)
if not exist "lib\bioformats_package.jar" (
    echo Downloading Bio-Formats package...
    powershell -Command "& {Invoke-WebRequest -Uri 'https://downloads.openmicroscopy.org/bio-formats/6.8.0/artifacts/bioformats_package.jar' -OutFile 'lib\bioformats_package.jar'}"
)

if not exist "imagej\ij.jar" (
    echo Downloading ImageJ...
    powershell -Command "& {Invoke-WebRequest -Uri 'https://wsr.imagej.net/distros/win/ij153-win-java8.zip' -OutFile 'imagej\imagej.zip'}"
    echo Extracting ImageJ...
    powershell -Command "& {Expand-Archive -Path 'imagej\imagej.zip' -DestinationPath 'imagej' -Force}"
)

REM Compile our custom classes
echo Compiling SDT utilities...
if not exist "build" mkdir build

javac -cp "lib\bioformats_package.jar;imagej\ij.jar" -d build src\SDTUnzipper.java src\BioFormatsSDTWrapper.java src\ImageJSDTPlugin.java

REM Create plugins directory for ImageJ and copy our plugin
if not exist "imagej\plugins" mkdir imagej\plugins
copy build\ImageJSDTPlugin.class imagej\plugins\
copy build\BioFormatsSDTWrapper.class imagej\plugins\
copy build\SDTUnzipper.class imagej\plugins\

REM Create a batch file to run ImageJ with our plugin
echo @echo off > run_imagej.bat
echo set CLASSPATH=lib\bioformats_package.jar;imagej\ij.jar;build >> run_imagej.bat
echo java -Xmx1024m -cp "%%CLASSPATH%%" ij.ImageJ >> run_imagej.bat

REM Create a batch file to test our SDT unzipper
echo @echo off > test_unzipper.bat
echo set CLASSPATH=lib\bioformats_package.jar;build >> test_unzipper.bat
echo java -cp "%%CLASSPATH%%" SDTUnzipper %%* >> test_unzipper.bat

REM Create a batch file to test our BioFormats wrapper
echo @echo off > test_wrapper.bat
echo set CLASSPATH=lib\bioformats_package.jar;build >> test_wrapper.bat
echo java -cp "%%CLASSPATH%%" BioFormatsSDTWrapper %%* >> test_wrapper.bat

echo Setup completed!
echo.
echo Usage:
echo   run_imagej.bat        - Start ImageJ with our plugin
echo   test_unzipper.bat     - Test the SDT unzipper utility
echo   test_wrapper.bat      - Test the BioFormats wrapper
echo.
echo You can drag and drop SDT files onto these batch files. 