import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

/**
 * A utility class to handle zipped SDT files.
 * This class can detect if an SDT file contains zipped data,
 * unzip it to a temporary file, and provide the path to the processed file.
 */
public class SDTUnzipper {
    
    /**
     * Checks if the provided SDT file contains zipped data.
     * It looks for the "PK" signature at the data block offset.
     * 
     * @param sdtFile The SDT file to check
     * @return true if the file appears to be a zipped SDT file
     */
    public static boolean isZippedSDT(File sdtFile) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(sdtFile, "r")) {
            // Read the data block offset from the header
            raf.seek(16); // dataBlockOffs is at position 16 in the header
            int dataBlockOffs = raf.readInt();
            
            // Check for PK signature at the data block offset
            raf.seek(dataBlockOffs);
            byte[] signature = new byte[2];
            raf.read(signature);
            
            return signature[0] == 'P' && signature[1] == 'K';
        }
    }
    
    /**
     * Processes the given SDT file.
     * If the file is zipped, it will be unzipped to a temporary file.
     * Otherwise, the original file path is returned.
     * 
     * @param sdtFilePath Path to the SDT file
     * @return Path to the processed file (either the original or unzipped temp file)
     */
    public static String processSDTFile(String sdtFilePath) throws IOException {
        File sdtFile = new File(sdtFilePath);
        
        if (!sdtFile.exists()) {
            throw new FileNotFoundException("File not found: " + sdtFilePath);
        }
        
        if (!isZippedSDT(sdtFile)) {
            System.out.println("File is not zipped or not in a recognized format.");
            return sdtFilePath; // Return original path if not zipped
        }
        
        System.out.println("Detected zipped SDT file. Creating unzipped version...");
        
        // Create a temporary file to hold the unzipped content
        String baseName = sdtFile.getName();
        String unzippedName = baseName.replaceFirst("\\.sdt$", "_unzipped.sdt");
        File tempFile = new File(sdtFile.getParent(), unzippedName);
        
        // Read the header info to determine where the ZIP data starts
        try (RandomAccessFile raf = new RandomAccessFile(sdtFile, "r")) {
            // Read and preserve the header
            raf.seek(0);
            byte[] header = new byte[1024]; // Assuming the header is within the first 1KB
            int headerSize = raf.read(header);
            
            // Read the data block offset
            raf.seek(16); // dataBlockOffs position
            int dataBlockOffs = raf.readInt();
            
            // Create the output file with the header
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                // Write the header first
                fos.write(header, 0, headerSize);
                
                // Now read from the ZIP data and uncompress it
                raf.seek(dataBlockOffs);
                
                // Create a ZipInputStream to read the compressed data
                raf.seek(dataBlockOffs);
                byte[] testBytes = new byte[4];
                raf.read(testBytes);
                
                // Reset position to data block offset
                raf.seek(dataBlockOffs);
                
                if (testBytes[0] == 'P' && testBytes[1] == 'K') {
                    // Create a ZipInputStream using the RandomAccessFile
                    try (ZipInputStream zis = new ZipInputStream(
                            new BufferedInputStream(new FileInputStream(sdtFile)) {
                                {
                                    // Skip to the data block offset
                                    long skipped = 0;
                                    while (skipped < dataBlockOffs) {
                                        skipped += skip(dataBlockOffs - skipped);
                                    }
                                }
                            })) {
                        
                        // Read the ZIP entry
                        ZipEntry entry = zis.getNextEntry();
                        if (entry != null) {
                            // Copy the uncompressed data
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                            zis.closeEntry();
                        } else {
                            throw new IOException("No entries found in the ZIP data");
                        }
                    }
                } else {
                    throw new IOException("Expected ZIP signature not found at data block offset");
                }
            }
        }
        
        System.out.println("Unzipped file created: " + tempFile.getAbsolutePath());
        return tempFile.getAbsolutePath();
    }
    
    /**
     * Main method for testing the unzipper.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java SDTUnzipper <sdt_file_path>");
            System.exit(1);
        }
        
        try {
            String processedFile = processSDTFile(args[0]);
            System.out.println("Processed file: " + processedFile);
        } catch (Exception e) {
            System.err.println("Error processing file: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 