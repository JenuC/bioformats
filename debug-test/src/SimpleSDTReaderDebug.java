import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * A simple program to debug SDT file headers directly.
 * This doesn't depend on Bio-Formats libraries.
 */
public class SimpleSDTReaderDebug {
    
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
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, Math.min(file.length(), 1024));
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
            
            // Print header information
            System.out.println("\nSDT Header Information:");
            System.out.println("Revision: " + revision);
            System.out.println("Info Offset: " + infoOffs);
            System.out.println("Info Length: " + infoLength);
            System.out.println("Setup Offset: " + setupOffs);
            System.out.println("Setup Length: " + setupLength);
            System.out.println("Data Block Offset: " + dataBlockOffs);
            System.out.println("Number of Data Blocks: " + noOfDataBlocks);
            System.out.println("Data Block Length: " + dataBlockLength);
            System.out.println("Measurement Description Block Offset: " + measDescBlockOffs);
            System.out.println("Number of Measurement Description Blocks: " + noOfMeasDescBlocks);
            System.out.println("Measurement Description Block Length: " + measDescBlockLength);
            System.out.println("Header Valid: 0x" + Integer.toHexString(headerValid) + 
                              (headerValid == 0x5555 ? " (Valid)" : 
                               headerValid == 0x1111 ? " (Not Valid)" : " (Unknown status)"));
            
            // Try to read the info string
            if (infoOffs > 0 && infoLength > 0) {
                raf.seek(infoOffs);
                byte[] infoBytes = new byte[infoLength];
                raf.read(infoBytes);
                String info = new String(infoBytes);
                System.out.println("\nInfo Section:");
                System.out.println(info.substring(0, Math.min(200, info.length())) + 
                                  (info.length() > 200 ? "..." : ""));
                
                // Try to extract dimensions from the info
                extractDimensions(info);
            }
            
            // Clean up
            channel.close();
            raf.close();
            
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void extractDimensions(String info) {
        System.out.println("\nAttempting to extract dimensions:");
        
        // Look for width
        int width = extractIntValue(info, "#SP [SP_SCAN_X,I,", "]");
        if (width == -1) {
            width = extractIntValue(info, "#SP [SP_IMG_X,I,", "]");
        }
        
        // Look for height
        int height = extractIntValue(info, "#SP [SP_SCAN_Y,I,", "]");
        if (height == -1) {
            height = extractIntValue(info, "#SP [SP_IMG_Y,I,", "]");
        }
        
        // Look for time bins
        int timeBins = extractIntValue(info, "#SP [SP_ADC_RE,I,", "]");
        
        // Look for channels
        int channels = extractIntValue(info, "#SP [SP_SCAN_RX,I,", "]");
        
        System.out.println("Width: " + (width != -1 ? width : "Not found"));
        System.out.println("Height: " + (height != -1 ? height : "Not found"));
        System.out.println("Time Bins: " + (timeBins != -1 ? timeBins : "Not found"));
        System.out.println("Channels: " + (channels != -1 ? channels : "Not found"));
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
} 