# SDTReaderDebug

A simple tool to debug and test the SDTReader.java file from Bio-Formats.

## Overview

This tool helps diagnose issues with reading .sdt files using the Bio-Formats SDTReader class. It prints detailed information about the file structure, metadata, and attempts to read image data.

## Building

First, you need to build the Bio-Formats project:

```bash
cd ..
mvn clean install
```

Then, build the debug tool:

```bash
cd debug-test
make
```

## Running

To run the tool on an SDT file:

```bash
make run FILE=/path/to/your/file.sdt
```

## Output

The tool will print detailed information about:

1. File format
2. SDT header information
3. Detailed SDT metadata
4. Image dimensions
5. An attempt to read the first plane of data

## Troubleshooting

If you encounter errors:

1. Make sure Bio-Formats has been built successfully
2. Check that your .sdt file exists and is accessible
3. Examine error messages for specific issues with the file format or reader implementation 