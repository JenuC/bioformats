@echo off
setlocal enabledelayedexpansion

REM This script compiles and runs a test for the EnhancedSDTReader

echo Setting up EnhancedSDTReader test...

REM Create directories
if not exist "lib" mkdir lib
if not exist "build" mkdir build

REM Download Bio-Formats if needed
if not exist "lib\bioformats_package.jar" (
    echo Downloading Bio-Formats package...
    powershell -Command "& {Invoke-WebRequest -Uri 'https://downloads.openmicroscopy.org/bio-formats/6.8.0/artifacts/bioformats_package.jar' -OutFile 'lib\bioformats_package.jar'}"
)

REM Create a test class for the EnhancedSDTReader
echo Creating test class...
echo import java.io.File; > src\EnhancedSDTReaderTest.java
echo import loci.formats.IFormatReader; >> src\EnhancedSDTReaderTest.java
echo import loci.formats.in.SDTReader; >> src\EnhancedSDTReaderTest.java
echo. >> src\EnhancedSDTReaderTest.java
echo public class EnhancedSDTReaderTest { >> src\EnhancedSDTReaderTest.java
echo     public static void main(String[] args) { >> src\EnhancedSDTReaderTest.java
echo         if (args.length ^< 1) { >> src\EnhancedSDTReaderTest.java
echo             System.out.println("Usage: java EnhancedSDTReaderTest ^<sdt_file_path^>"); >> src\EnhancedSDTReaderTest.java
echo             System.exit(1); >> src\EnhancedSDTReaderTest.java
echo         } >> src\EnhancedSDTReaderTest.java
echo. >> src\EnhancedSDTReaderTest.java
echo         String filePath = args[0]; >> src\EnhancedSDTReaderTest.java
echo         File file = new File(filePath); >> src\EnhancedSDTReaderTest.java
echo. >> src\EnhancedSDTReaderTest.java
echo         if (!file.exists()) { >> src\EnhancedSDTReaderTest.java
echo             System.out.println("File not found: " + filePath); >> src\EnhancedSDTReaderTest.java
echo             System.exit(1); >> src\EnhancedSDTReaderTest.java
echo         } >> src\EnhancedSDTReaderTest.java
echo. >> src\EnhancedSDTReaderTest.java
echo         System.out.println("Testing with original SDTReader:"); >> src\EnhancedSDTReaderTest.java
echo         testReader(new SDTReader(), filePath); >> src\EnhancedSDTReaderTest.java
echo. >> src\EnhancedSDTReaderTest.java
echo         System.out.println("\nTesting with EnhancedSDTReader:"); >> src\EnhancedSDTReaderTest.java
echo         testReader(new EnhancedSDTReader(), filePath); >> src\EnhancedSDTReaderTest.java
echo     } >> src\EnhancedSDTReaderTest.java
echo. >> src\EnhancedSDTReaderTest.java
echo     private static void testReader(IFormatReader reader, String filePath) { >> src\EnhancedSDTReaderTest.java
echo         try { >> src\EnhancedSDTReaderTest.java
echo             long startTime = System.currentTimeMillis(); >> src\EnhancedSDTReaderTest.java
echo             reader.setId(filePath); >> src\EnhancedSDTReaderTest.java
echo             long endTime = System.currentTimeMillis(); >> src\EnhancedSDTReaderTest.java
echo. >> src\EnhancedSDTReaderTest.java
echo             System.out.println("Successfully opened file with " + reader.getClass().getSimpleName()); >> src\EnhancedSDTReaderTest.java
echo             System.out.println("Dimensions: " + reader.getSizeX() + "x" + reader.getSizeY() + "x" + >> src\EnhancedSDTReaderTest.java
echo                               reader.getSizeZ() + "x" + reader.getSizeC() + "x" + reader.getSizeT()); >> src\EnhancedSDTReaderTest.java
echo             System.out.println("Time taken: " + (endTime - startTime) + " ms"); >> src\EnhancedSDTReaderTest.java
echo. >> src\EnhancedSDTReaderTest.java
echo             // Try to read the first plane >> src\EnhancedSDTReaderTest.java
echo             if (reader.getImageCount() ^> 0) { >> src\EnhancedSDTReaderTest.java
echo                 startTime = System.currentTimeMillis(); >> src\EnhancedSDTReaderTest.java
echo                 byte[] data = reader.openBytes(0); >> src\EnhancedSDTReaderTest.java
echo                 endTime = System.currentTimeMillis(); >> src\EnhancedSDTReaderTest.java
echo                 System.out.println("Successfully read first plane (" + data.length + " bytes)"); >> src\EnhancedSDTReaderTest.java
echo                 System.out.println("Time taken: " + (endTime - startTime) + " ms"); >> src\EnhancedSDTReaderTest.java
echo             } >> src\EnhancedSDTReaderTest.java
echo. >> src\EnhancedSDTReaderTest.java
echo             // Print some metadata >> src\EnhancedSDTReaderTest.java
echo             System.out.println("\nMetadata (first 10 entries):"); >> src\EnhancedSDTReaderTest.java
echo             String[] keys = reader.getMetadataKeys(); >> src\EnhancedSDTReaderTest.java
echo             for (int i = 0; i ^< Math.min(10, keys.length); i++) { >> src\EnhancedSDTReaderTest.java
echo                 System.out.println(keys[i] + ": " + reader.getMetadataValue(keys[i])); >> src\EnhancedSDTReaderTest.java
echo             } >> src\EnhancedSDTReaderTest.java
echo. >> src\EnhancedSDTReaderTest.java
echo         } catch (Exception e) { >> src\EnhancedSDTReaderTest.java
echo             System.out.println("Error: " + e.getMessage()); >> src\EnhancedSDTReaderTest.java
echo             e.printStackTrace(); >> src\EnhancedSDTReaderTest.java
echo         } finally { >> src\EnhancedSDTReaderTest.java
echo             try { >> src\EnhancedSDTReaderTest.java
echo                 reader.close(); >> src\EnhancedSDTReaderTest.java
echo             } catch (Exception e) { >> src\EnhancedSDTReaderTest.java
echo                 e.printStackTrace(); >> src\EnhancedSDTReaderTest.java
echo             } >> src\EnhancedSDTReaderTest.java
echo         } >> src\EnhancedSDTReaderTest.java
echo     } >> src\EnhancedSDTReaderTest.java
echo } >> src\EnhancedSDTReaderTest.java

REM Compile the enhanced reader and test class
echo Compiling EnhancedSDTReader and test class...
javac -cp "lib\bioformats_package.jar" -d build src\EnhancedSDTReader.java src\EnhancedSDTReaderTest.java

REM Run the test if file path was provided
if "%~1"=="" (
    echo Usage: run_enhanced_reader.bat ^<sdt_file_path^>
    echo.
    echo Example:
    echo   run_enhanced_reader.bat test_bochum.sdt
) else (
    echo Running test on %1
    java -cp "lib\bioformats_package.jar;build" EnhancedSDTReaderTest "%~1"
)

endlocal 