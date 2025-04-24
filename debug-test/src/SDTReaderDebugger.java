import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;

import loci.common.RandomAccessInputStream;
import loci.formats.FormatException;
import loci.formats.in.SDTInfo;
import loci.formats.in.SDTReader;

/**
 * A utility to debug the SDT file format and Bio-Formats SDTReader implementation.
 * This is designed to understand why zipped SDT files aren't being read correctly.
 */
public class SDTReaderDebugger {
    
    /**
     * Analyzes the provided SDT file and checks for various issues.
     */
    public static void analyzeSDTFile(String filePath) {
        File file = new File(filePath);
        
        if (!file.exists()) {
            System.out.println("File not found: " + filePath);
            return;
        }
        
        System.out.println("Analyzing SDT file: " + filePath);
        System.out.println("File size: " + file.length() + " bytes");
        
        try {
            // Check basic header
            System.out.println("\n--- BASIC HEADER ANALYSIS ---");
            checkBasicHeader(file);
            
            // Check for ZIP signature
            System.out.println("\n--- ZIP SIGNATURE CHECK ---");
            checkForZipSignature(file);
            
            // Try to use the SDTReader
            System.out.println("\n--- ATTEMPTING TO USE SDTREADER ---");
            trySDTReader(filePath);
            
            // Debug ZIP data structure
            System.out.println("\n--- DEBUGGING ZIP DATA STRUCTURE ---");
            debugZipStructure(file);
            
        } catch (Exception e) {
            System.err.println("Error analyzing file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Checks the basic SDT header.
     */
    private static void checkBasicHeader(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(0);
            short revision = raf.readShort();
            int infoOffs = raf.readInt();
            short infoLength = raf.readShort();
            int setupOffs = raf.readInt();
            int setupLength = raf.readUnsignedShort();
            int dataBlockOffs = raf.readInt();
            short noOfDataBlocks = raf.readShort();
            int dataBlockLength = raf.readInt();
            
            System.out.println("Revision: " + revision);
            System.out.println("Info Offset: " + infoOffs);
            System.out.println("Info Length: " + infoLength);
            System.out.println("Setup Offset: " + setupOffs);
            System.out.println("Setup Length: " + setupLength);
            System.out.println("Data Block Offset: " + dataBlockOffs);
            System.out.println("Number of Data Blocks: " + noOfDataBlocks);
            System.out.println("Data Block Length: " + dataBlockLength);
            
            // Read info section if available
            if (infoOffs > 0 && infoLength > 0) {
                raf.seek(infoOffs);
                byte[] infoBytes = new byte[infoLength];
                raf.read(infoBytes);
                String info = new String(infoBytes);
                
                System.out.println("\nInfo Section (first 200 chars):");
                System.out.println(info.substring(0, Math.min(200, info.length())));
            }
        }
    }
    
    /**
     * Checks if the file has a ZIP signature at the data block offset.
     */
    private static void checkForZipSignature(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            // Get data block offset
            raf.seek(16); // dataBlockOffs is at position 16
            int dataBlockOffs = raf.readInt();
            
            // Check for PK signature
            raf.seek(dataBlockOffs);
            byte[] signature = new byte[4];
            raf.read(signature);
            
            System.out.println("Data block offset: " + dataBlockOffs);
            System.out.printf("Signature bytes: 0x%02X 0x%02X 0x%02X 0x%02X\n", 
                             signature[0] & 0xFF, signature[1] & 0xFF, 
                             signature[2] & 0xFF, signature[3] & 0xFF);
            
            boolean isPKSignature = signature[0] == 'P' && signature[1] == 'K';
            System.out.println("Is PK ZIP signature: " + isPKSignature);
            
            if (isPKSignature) {
                // Read more of the ZIP header
                raf.seek(dataBlockOffs);
                byte[] zipHeader = new byte[30]; // Local file header is at least 30 bytes
                raf.read(zipHeader);
                
                System.out.println("\nZIP Local File Header:");
                System.out.printf("Signature: 0x%02X%02X%02X%02X\n", 
                                 zipHeader[0] & 0xFF, zipHeader[1] & 0xFF,
                                 zipHeader[2] & 0xFF, zipHeader[3] & 0xFF);
                
                // Extract version needed to extract
                ByteBuffer buffer = ByteBuffer.wrap(zipHeader);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                
                short versionNeeded = buffer.getShort(4);
                short generalPurpose = buffer.getShort(6);
                short compressionMethod = buffer.getShort(8);
                short lastModTime = buffer.getShort(10);
                short lastModDate = buffer.getShort(12);
                int crc32 = buffer.getInt(14);
                int compressedSize = buffer.getInt(18);
                int uncompressedSize = buffer.getInt(22);
                short fileNameLength = buffer.getShort(26);
                short extraFieldLength = buffer.getShort(28);
                
                System.out.println("Version needed: " + versionNeeded);
                System.out.println("General purpose bit flag: 0x" + Integer.toHexString(generalPurpose & 0xFFFF));
                System.out.println("Compression method: " + compressionMethod);
                System.out.println("Last mod time: " + lastModTime);
                System.out.println("Last mod date: " + lastModDate);
                System.out.println("CRC-32: 0x" + Integer.toHexString(crc32));
                System.out.println("Compressed size: " + compressedSize);
                System.out.println("Uncompressed size: " + uncompressedSize);
                System.out.println("File name length: " + fileNameLength);
                System.out.println("Extra field length: " + extraFieldLength);
                
                // Read filename if present
                if (fileNameLength > 0) {
                    raf.seek(dataBlockOffs + 30);
                    byte[] fileNameBytes = new byte[fileNameLength];
                    raf.read(fileNameBytes);
                    String fileName = new String(fileNameBytes);
                    System.out.println("File name: " + fileName);
                }
            }
        }
    }
    
    /**
     * Attempts to use the SDTReader to read the file.
     */
    private static void trySDTReader(String filePath) {
        SDTReader reader = new SDTReader();
        try {
            reader.setId(filePath);
            
            System.out.println("SDTReader successfully opened the file");
            System.out.println("Dimensions: " + reader.getSizeX() + "x" + 
                              reader.getSizeY() + "x" + reader.getSizeZ() + "x" + 
                              reader.getSizeC() + "x" + reader.getSizeT());
            
            // Read more metadata
            System.out.println("\nMetadata:");
            String[] metadataKeys = reader.getMetadataKeys();
            for (String key : metadataKeys) {
                System.out.println(key + ": " + reader.getMetadataValue(key));
            }
            
        } catch (FormatException | IOException e) {
            System.err.println("SDTReader failed to read the file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Debugs the ZIP structure in the file.
     */
    private static void debugZipStructure(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            // Get data block offset
            raf.seek(16); // dataBlockOffs is at position 16
            int dataBlockOffs = raf.readInt();
            
            // Check for PK signature
            raf.seek(dataBlockOffs);
            byte[] signature = new byte[4];
            raf.read(signature);
            
            if (signature[0] == 'P' && signature[1] == 'K') {
                // Try using a ZipInputStream
                System.out.println("Trying to read with ZipInputStream...");
                
                try (FileInputStream fis = new FileInputStream(file);
                     BufferedInputStream bis = new BufferedInputStream(fis) {
                         { 
                             // Skip to the data block offset
                             long skipped = 0;
                             while (skipped < dataBlockOffs) {
                                 skipped += skip(dataBlockOffs - skipped);
                             }
                         }
                     };
                     ZipInputStream zis = new ZipInputStream(bis)) {
                    
                    // Try to read entries
                    ZipEntry entry = zis.getNextEntry();
                    if (entry != null) {
                        System.out.println("Found ZIP entry: " + entry.getName());
                        System.out.println("Entry size: " + entry.getSize() + " bytes");
                        System.out.println("Compressed size: " + entry.getCompressedSize() + " bytes");
                        
                        // Read some of the data
                        byte[] buffer = new byte[1024];
                        int bytesRead = zis.read(buffer);
                        System.out.println("Read " + bytesRead + " bytes of data");
                        
                        // Display first few bytes
                        System.out.println("First 32 bytes (hex):");
                        for (int i = 0; i < Math.min(32, bytesRead); i++) {
                            System.out.printf("%02X ", buffer[i] & 0xFF);
                            if ((i + 1) % 16 == 0) System.out.println();
                        }
                        System.out.println();
                        
                        zis.closeEntry();
                    } else {
                        System.out.println("No ZIP entries found");
                    }
                } catch (IOException e) {
                    System.err.println("Error reading ZIP structure: " + e.getMessage());
                    System.out.println("\nTrying alternative approach with low-level analysis...");
                    
                    // Analyze the ZIP structure manually
                    raf.seek(dataBlockOffs);
                    byte[] zipData = new byte[Math.min(1024, (int)(file.length() - dataBlockOffs))];
                    raf.read(zipData);
                    
                    System.out.println("Raw data at data block offset (hex):");
                    for (int i = 0; i < Math.min(64, zipData.length); i++) {
                        System.out.printf("%02X ", zipData[i] & 0xFF);
                        if ((i + 1) % 16 == 0) System.out.println();
                    }
                }
            } else {
                System.out.println("No ZIP signature found at data block offset");
            }
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java SDTReaderDebugger <sdt_file_path>");
            System.exit(1);
        }
        
        analyzeSDTFile(args[0]);
    }
} 