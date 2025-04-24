# SDT Reader Debug Tools

This folder contains tools for debugging and analyzing SDT files, particularly for understanding the differences between zipped and unzipped SDT files.

## Available Tools

### 1. SDT File Analysis and Debugging

- **SimpleSDTReaderDebug**: Analyzes the header structure of SDT files without Bio-Formats dependency
- **SDTReaderDebugger**: Advanced debugging tool that analyzes both header and data sections, including ZIP structure
- **EnhancedSDTReader**: Modified version of Bio-Formats' SDTReader with improved handling of zipped SDT files

### 2. Preprocessor Tools

- **SDTUnzipper**: Detects and unzips compressed SDT files to make them compatible with Bio-Formats
- **BioFormatsSDTWrapper**: A wrapper that automatically preprocesses SDT files before passing them to Bio-Formats

### 3. ImageJ Integration

- **ImageJSDTPlugin**: An ImageJ plugin that can open both zipped and unzipped SDT files

## Setup Instructions

### Prerequisites

- Java Development Kit (JDK) 8 or higher
- Windows operating system (for running batch files)

### Building and Running the Tools

1. **Setup Environment**

   Run the setup script to download dependencies and prepare the environment:

   ```
   imagej_setup.bat
   ```

2. **Test the SDT Unzipper**

   To test the SDT unzipper utility:

   ```
   test_unzipper.bat test_bochum.sdt
   ```

3. **Test the Bio-Formats Wrapper**

   To test the Bio-Formats wrapper:

   ```
   test_wrapper.bat test_bochum.sdt
   ```

4. **Run ImageJ with the Plugin**

   To start ImageJ with our plugin installed:

   ```
   run_imagej.bat
   ```

5. **Debug SDT Reader**

   To analyze an SDT file structure:

   ```
   debug_sdtreader.bat test_bochum.sdt
   ```

6. **Test Enhanced SDT Reader**

   To compare the original and enhanced SDT readers:

   ```
   run_enhanced_reader.bat test_bochum.sdt
   ```

## Files Included

- **test_bochum.sdt**: Zipped SDT file (2.0MB)
- **test_bochum_uz.sdt**: Unzipped SDT file (128MB)
- **uz_metadata.txt**: Metadata extracted from unzipped file
- **zip_medata.txt**: Metadata extracted from zipped file

## Understanding the Problem

The BioFormats library's SDTReader is correctly identifying the zipped SDT file by checking for the "PK" signature bytes at the beginning of the data block. When it finds these bytes, it uses a ZipInputStream to decompress the data.

However, there are issues with the zipped version:

1. The unzipped file (`test_bochum_uz.sdt`) works because it's already decompressed and can be read directly.
2. The zipped file (`test_bochum.sdt`) is not working correctly with BioFormats, which could be due to:
   - Issues with the ZIP header structure
   - The compression format not being exactly what the BioFormats library expects
   - Possible corruption in the ZIP file

Our enhanced tools help overcome these issues by:

1. **The SDTUnzipper**: Preprocesses the file by extracting the zipped content
2. **The EnhancedSDTReader**: Improves the original SDTReader with better ZIP handling
3. **The BioFormatsSDTWrapper**: Automatically determines the file type and handles it accordingly

## Further Investigation

For further debugging:

1. Use `SDTReaderDebugger` to analyze the structure of zipped SDT files
2. Compare the metadata between zipped and unzipped files
3. Examine the ZIP entry headers for any inconsistencies 