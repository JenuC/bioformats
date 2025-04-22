import java.io.File;
import java.io.IOException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.in.SDTReader;
import loci.formats.in.SDTInfo;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;

/**
 * A simple program to debug the SDTReader class.
 */
public class SDTReaderDebug {
    
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
        
        // Initialize the reader
        SDTReader reader = new SDTReader();
        
        try {
            // Print basic information about the file
            System.out.println("File: " + inputFile);
            System.out.println("File size: " + file.length() + " bytes");
            
            // Set up the reader
            reader.setId(inputFile);
            
            // Print information about the file format
            System.out.println("\nFile format information:");
            System.out.println("Format name: " + reader.getFormat());
            System.out.println("Suffix: " + reader.getSuffixes()[0]);
            
            // Print information about the SDT header
            System.out.println("\nSDT header information:");
            System.out.println("Time bins: " + reader.getTimeBinCount());
            System.out.println("Channels: " + reader.getChannelCount());
            
            // Print detailed SDT info
            System.out.println("\nDetailed SDT information:");
            
            // Get the SDTInfo object
            SDTInfo info = reader.getInfo();
            if (info != null) {
                System.out.println("Width: " + info.width);
                System.out.println("Height: " + info.height);
                System.out.println("Time bins: " + info.timeBins);
                System.out.println("Channels: " + info.channels);
                System.out.println("Timepoints: " + info.timepoints);
                System.out.println("Data block offset: " + info.dataBlockOffs);
                System.out.println("Data block length: " + info.dataBlockLength);
                System.out.println("Number of data blocks: " + info.noOfDataBlocks);
                System.out.println("Header valid: " + info.headerValid);
                System.out.println("Info: " + (info.info != null ? info.info.substring(0, Math.min(100, info.info.length())) + "..." : "null"));
                
                // Print array information
                if (info.allBlockOffsets != null) {
                    System.out.println("Block offsets count: " + info.allBlockOffsets.length);
                    for (int i = 0; i < Math.min(5, info.allBlockOffsets.length); i++) {
                        System.out.println("  Block offset [" + i + "]: " + info.allBlockOffsets[i]);
                    }
                }
                
                if (info.allBlockLengths != null) {
                    System.out.println("Block lengths count: " + info.allBlockLengths.length);
                    for (int i = 0; i < Math.min(5, info.allBlockLengths.length); i++) {
                        System.out.println("  Block length [" + i + "]: " + info.allBlockLengths[i]);
                    }
                }
            } else {
                System.out.println("SDTInfo is null");
            }
            
            // Print basic image dimensions
            System.out.println("\nImage dimensions:");
            System.out.println("Width: " + reader.getSizeX());
            System.out.println("Height: " + reader.getSizeY());
            System.out.println("Z-sections: " + reader.getSizeZ());
            System.out.println("Timepoints: " + reader.getSizeT());
            System.out.println("Channels: " + reader.getSizeC());
            System.out.println("Pixel type: " + FormatTools.getPixelTypeString(reader.getPixelType()));
            System.out.println("Little endian: " + reader.isLittleEndian());
            System.out.println("RGB: " + reader.isRGB());
            System.out.println("Interleaved: " + reader.isInterleaved());
            System.out.println("Image count: " + reader.getImageCount());
            
            // Try to read a plane
            try {
                System.out.println("\nAttempting to read first plane...");
                byte[] plane = reader.openBytes(0);
                System.out.println("Successfully read first plane. Plane size: " + plane.length + " bytes");
            } catch (Exception e) {
                System.out.println("Error reading plane: " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (FormatException e) {
            System.out.println("Format error: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                System.out.println("Error closing reader: " + e.getMessage());
            }
        }
    }
} 