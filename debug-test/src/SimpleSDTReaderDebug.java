import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple program to debug SDT file headers directly.
 * This doesn't depend on Bio-Formats libraries.
 */
public class SimpleSDTReaderDebug {
    
    // Common patterns in SDT files
    private static final String X_STRING = "SP_SCAN_X";
    private static final String Y_STRING = "SP_SCAN_Y";
    private static final String T_STRING = "SP_ADC_RE";
    private static final String C_STRING = "SP_SCAN_RX";
    private static final String X_IMG_STRING = "SP_IMG_X";
    private static final String Y_IMG_STRING = "SP_IMG_Y";
    
    // Constants for SDT file header validation
    private static final int HEADER_VALID = 0x5555;
    private static final int HEADER_NOT_VALID = 0x1111;
    private static final int HEADER_CHKSUM = 0x55aa;
    
    public static void main(String[] args) {
        // Check if a file path was provided
        if (args.length < 1) {
            System.out.println("Please provide the path to an SDT file as a command-line argument.");
            System.exit(1);
        }
        
        // Get the file path from command-line arguments
        String inputFile = args[0];
        File file = new File(inputFile);
        
        if (!file.exists()) {
            System.out.println("File does not exist: " + inputFile);
            System.exit(1);
        }
        
        try {
            // Print basic information about the file
            System.out.println("File: " + inputFile);
            System.out.println("File size: " + file.length() + " bytes");
            
            // Open the file with RandomAccessFile
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            FileChannel channel = raf.getChannel();
            
            // Map the beginning of the file into memory
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, Math.min(file.length(), 4096));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            // Read basic header fields
            short revision = buffer.getShort();
            int infoOffs = buffer.getInt();
            short infoLength = buffer.getShort();
            int setupOffs = buffer.getInt();
            int setupLength = buffer.getChar(); // unsigned short
            int dataBlockOffs = buffer.getInt();
            short noOfDataBlocks = buffer.getShort();
            int dataBlockLength = buffer.getInt();
            int measDescBlockOffs = buffer.getInt();
            short noOfMeasDescBlocks = buffer.getShort();
            short measDescBlockLength = buffer.getShort();
            int headerValid = buffer.getInt();
            long reserved1 = buffer.getInt() & 0xFFFFFFFFL; // Read as unsigned
            int reserved2 = buffer.getInt();
            int chksum = buffer.getInt();
            
            // Print header information
            System.out.println("\nSDT Header Information:");
            System.out.println("Revision: " + revision);
            System.out.println("Info Offset: " + infoOffs);
            System.out.println("Info Length: " + infoLength);
            System.out.println("Setup Offset: " + setupOffs);
            System.out.println("Setup Length: " + setupLength);
            System.out.println("Data Block Offset: " + dataBlockOffs);
            System.out.println("Number of Data Blocks: " + noOfDataBlocks + 
                               (noOfDataBlocks == 0x7fff ? " (Use reserved1 instead)" : ""));
            System.out.println("Data Block Length: " + dataBlockLength);
            System.out.println("Measurement Description Block Offset: " + measDescBlockOffs);
            System.out.println("Number of Measurement Description Blocks: " + noOfMeasDescBlocks);
            System.out.println("Measurement Description Block Length: " + measDescBlockLength);
            System.out.println("Header Valid: 0x" + Integer.toHexString(headerValid) + 
                              (headerValid == HEADER_VALID ? " (Valid)" : 
                               headerValid == HEADER_NOT_VALID ? " (Not Valid)" : " (Unknown status)"));
            System.out.println("Reserved1: " + reserved1 + 
                               (noOfDataBlocks == 0x7fff ? " (Actual number of data blocks)" : ""));
            System.out.println("Reserved2: " + reserved2);
            System.out.println("Checksum: 0x" + Integer.toHexString(chksum) + 
                              (chksum == HEADER_CHKSUM ? " (Valid)" : " (Invalid)"));
            
            String info = "";
            String setup = "";
            
            // Try to read the info string
            if (infoOffs > 0 && infoLength > 0) {
                raf.seek(infoOffs);
                byte[] infoBytes = new byte[infoLength];
                raf.read(infoBytes);
                info = new String(infoBytes);
                System.out.println("\nInfo Section:");
                System.out.println(info.substring(0, Math.min(200, info.length())) + 
                                  (info.length() > 200 ? "..." : ""));
                
                // Check for file identification
                System.out.println("\nFile Type:");
                if (info.contains("SPC Setup Script File")) {
                    System.out.println("SPC Setup Script File (.set)");
                } else if (info.contains("SPC Setup & Data File")) {
                    System.out.println("SPC Setup & Data File (.sdt)");
                } else if (info.contains("SPC Flow Data File")) {
                    System.out.println("SPC Flow Data File (.sdt)");
                } else if (info.contains("SPC DLL Data File")) {
                    System.out.println("SPC DLL Data File (.sdt)");
                } else if (info.contains("SPC FCS Data File")) {
                    System.out.println("SPC FCS Data File (.sdt)");
                } else {
                    System.out.println("Unknown file type");
                }
            }
            
            // Try to read the setup string
            if (setupOffs > 0 && setupLength > 0) {
                raf.seek(setupOffs);
                byte[] setupBytes = new byte[setupLength];
                raf.read(setupBytes);
                setup = new String(setupBytes);
                System.out.println("\nSetup Section (first 200 chars):");
                System.out.println(setup.substring(0, Math.min(200, setup.length())) + 
                                  (setup.length() > 200 ? "..." : ""));
            }
            
            // Try to extract dimensions using multiple methods
            extractDimensions(info, setup);
            
            // Try to read binary setup data - this might contain dimension information
            String binarySetup = findBinarySetup(setup);
            if (binarySetup != null && !binarySetup.isEmpty()) {
                System.out.println("\nFound binary setup data");
                // Binary data could be parsed here
            }
            
            // Check for the data block
            if (dataBlockOffs > 0) {
                System.out.println("\nData Block Information:");
                raf.seek(dataBlockOffs);
                
                // Read block header fields if available
                try {
                    short blockNo = raf.readShort();
                    int dataOffs = raf.readInt();
                    int nextBlockOffs = raf.readInt();
                    short blockType = raf.readShort();
                    short measDescBlockNo = raf.readShort();
                    int lblockNo = raf.readInt();
                    int blockLength = raf.readInt();
                    
                    System.out.println("Block Number: " + blockNo);
                    System.out.println("Data Offset: " + dataOffs);
                    System.out.println("Next Block Offset: " + nextBlockOffs);
                    System.out.println("Block Type: " + blockType);
                    System.out.println("Measurement Description Block Number: " + measDescBlockNo);
                    System.out.println("Long Block Number: " + lblockNo);
                    System.out.println("Block Length: " + blockLength);
                    
                    // Try to read some of the actual pixel data
                    if (dataOffs > 0 && blockLength > 0) {
                        System.out.println("\nFirst few bytes of data (hex):");
                        raf.seek(dataOffs);
                        byte[] dataBytes = new byte[Math.min(32, blockLength)];
                        raf.read(dataBytes);
                        
                        StringBuilder hexDump = new StringBuilder();
                        for (int i = 0; i < dataBytes.length; i++) {
                            if (i > 0 && i % 16 == 0) {
                                hexDump.append("\n");
                            }
                            hexDump.append(String.format("%02X ", dataBytes[i] & 0xFF));
                        }
                        System.out.println(hexDump.toString());
                    }
                } catch (Exception e) {
                    System.out.println("Error reading data block header: " + e.getMessage());
                }
            }
            
            // Clean up
            channel.close();
            raf.close();
            
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void extractDimensions(String info, String setup) {
        System.out.println("\nAttempting to extract dimensions:");
        
        // Method 1: Extract using string patterns
        int width = extractIntValue(info, "#SP [" + X_STRING + ",I,", "]");
        if (width == -1) {
            width = extractIntValue(info, "#SP [" + X_IMG_STRING + ",I,", "]");
        }
        
        int height = extractIntValue(info, "#SP [" + Y_STRING + ",I,", "]");
        if (height == -1) {
            height = extractIntValue(info, "#SP [" + Y_IMG_STRING + ",I,", "]");
        }
        
        int timeBins = extractIntValue(info, "#SP [" + T_STRING + ",I,", "]");
        int channels = extractIntValue(info, "#SP [" + C_STRING + ",I,", "]");
        
        System.out.println("String pattern extraction results:");
        System.out.println("Width: " + (width != -1 ? width : "Not found"));
        System.out.println("Height: " + (height != -1 ? height : "Not found"));
        System.out.println("Time Bins: " + (timeBins != -1 ? timeBins : "Not found"));
        System.out.println("Channels: " + (channels != -1 ? channels : "Not found"));
        
        // Method 2: Extract using regular expressions
        System.out.println("\nRegex pattern extraction results:");
        width = findDimensionWithRegex(info + " " + setup, X_STRING);
        if (width == -1) {
            width = findDimensionWithRegex(info + " " + setup, X_IMG_STRING);
        }
        System.out.println("Width: " + (width != -1 ? width : "Not found"));
        
        height = findDimensionWithRegex(info + " " + setup, Y_STRING);
        if (height == -1) {
            height = findDimensionWithRegex(info + " " + setup, Y_IMG_STRING);
        }
        System.out.println("Height: " + (height != -1 ? height : "Not found"));
        
        timeBins = findDimensionWithRegex(info + " " + setup, T_STRING);
        System.out.println("Time Bins: " + (timeBins != -1 ? timeBins : "Not found"));
        
        channels = findDimensionWithRegex(info + " " + setup, C_STRING);
        System.out.println("Channels: " + (channels != -1 ? channels : "Not found"));
        
        // Look for dimension information in different formats
        System.out.println("\nSearching for dimension information in different formats:");
        
        // Simple key-value pairs that might be present
        String[] dimensionKeys = {
            "no_of_x", "number_of_x", "width", "x_pixels", "pixels_x", "nx", "sp_scan_x",
            "no_of_y", "number_of_y", "height", "y_pixels", "pixels_y", "ny", "sp_scan_y",
            "adc_resolution", "adc_re", "time_bins", "no_of_time_channels", "nt", "sp_adc_re",
            "no_of_channels", "channels", "spectral_channels", "nc", "sp_scan_rx"
        };
        
        // Print full info and setup strings for manual inspection
        System.out.println("\nMatching lines in Info String:");
        if (info != null && !info.isEmpty()) {
            printMatchingLines(info, dimensionKeys);
        }
        
        System.out.println("\nMatching lines in Setup String:");
        if (setup != null && !setup.isEmpty()) {
            printMatchingLines(setup, dimensionKeys);
        }
    }
    
    private static void printMatchingLines(String text, String[] keywords) {
        String[] lines = text.split("\\r?\\n");
        boolean foundAny = false;
        
        for (String line : lines) {
            String lowerLine = line.toLowerCase();
            for (String keyword : keywords) {
                if (lowerLine.contains(keyword.toLowerCase())) {
                    System.out.println(line.trim());
                    foundAny = true;
                    break;
                }
            }
        }
        
        if (!foundAny) {
            System.out.println("No matching lines found");
        }
    }
    
    private static int findDimensionWithRegex(String text, String dimensionKey) {
        // This regex looks for patterns like:
        // dimensionKey=123 or dimensionKey,123 or [dimensionKey,I,123]
        Pattern pattern = Pattern.compile(
            dimensionKey + "\\s*[=,]\\s*(\\d+)|" +     // dimensionKey=123 or dimensionKey,123
            "\\[" + dimensionKey + "[,\\s]+\\w+[,\\s]+(\\d+)\\]"  // [dimensionKey,I,123]
        );
        
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1);
            if (value == null) {
                value = matcher.group(2);
            }
            if (value != null) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    // Ignore parse errors
                }
            }
        }
        return -1;
    }
    
    private static int extractIntValue(String text, String prefix, String suffix) {
        int startPos = text.indexOf(prefix);
        if (startPos == -1) return -1;
        
        startPos += prefix.length();
        int endPos = text.indexOf(suffix, startPos);
        if (endPos == -1) return -1;
        
        try {
            return Integer.parseInt(text.substring(startPos, endPos).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    private static String findBinarySetup(String setupText) {
        final String BINARY_MARKER = "BIN_PARA_BEGIN:";
        int index = setupText.indexOf(BINARY_MARKER);
        if (index >= 0) {
            return setupText.substring(index);
        }
        return null;
    }
} 