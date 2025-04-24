import java.io.File;
import java.util.ArrayList;
import java.util.List;

import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.in.SDTReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.formats.ome.OMEXMLMetadata;

/**
 * A wrapper for Bio-Formats that handles zipped SDT files.
 * It automatically unzips SDT files when needed before passing them to Bio-Formats.
 */
public class BioFormatsSDTWrapper {
    
    private IFormatReader reader;
    private List<String> tempFiles = new ArrayList<>();
    
    /**
     * Creates a new wrapper with a default ImageReader.
     */
    public BioFormatsSDTWrapper() throws Exception {
        // Create a new instance of ImageReader
        reader = new ImageReader();
        
        // Create and set metadata
        ServiceFactory factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        IMetadata metadata = service.createOMEXMLMetadata();
        reader.setMetadataStore(metadata);
    }
    
    /**
     * Creates a new wrapper with a specific reader.
     */
    public BioFormatsSDTWrapper(IFormatReader customReader) throws Exception {
        // Use the provided reader
        reader = customReader;
        
        // Create and set metadata
        ServiceFactory factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        IMetadata metadata = service.createOMEXMLMetadata();
        reader.setMetadataStore(metadata);
    }
    
    /**
     * Opens the specified SDT file.
     * If the file is zipped, it will be unzipped before opening.
     * 
     * @param filePath Path to the SDT file
     */
    public void openFile(String filePath) throws Exception {
        // Process the file using SDTUnzipper
        String processedPath = SDTUnzipper.processSDTFile(filePath);
        
        // If a new file was created, add it to the list for cleanup later
        if (!processedPath.equals(filePath)) {
            tempFiles.add(processedPath);
        }
        
        // Open the processed file with Bio-Formats
        reader.setId(processedPath);
        System.out.println("File opened successfully: " + processedPath);
        
        // Print out basic metadata
        System.out.println("File Information:");
        System.out.println("Dimensions: " + reader.getSizeX() + " x " + 
                          reader.getSizeY() + " x " + reader.getSizeZ() + " x " +
                          reader.getSizeC() + " x " + reader.getSizeT());
        System.out.println("Pixel Type: " + reader.getPixelType());
        System.out.println("Is Interleaved: " + reader.isInterleaved());
        System.out.println("Number of Series: " + reader.getSeriesCount());
    }
    
    /**
     * Gets the underlying Bio-Formats reader.
     */
    public IFormatReader getReader() {
        return reader;
    }
    
    /**
     * Closes the reader and cleans up any temporary files.
     */
    public void close() throws Exception {
        if (reader != null) {
            reader.close();
        }
        
        // Delete any temporary files that were created
        for (String tempFile : tempFiles) {
            File file = new File(tempFile);
            if (file.exists()) {
                file.delete();
                System.out.println("Deleted temporary file: " + tempFile);
            }
        }
        tempFiles.clear();
    }
    
    /**
     * Main method for testing the wrapper.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java BioFormatsSDTWrapper <sdt_file_path>");
            System.exit(1);
        }
        
        BioFormatsSDTWrapper wrapper = null;
        try {
            wrapper = new BioFormatsSDTWrapper();
            wrapper.openFile(args[0]);
            
            // Print all metadata keys
            System.out.println("\nMetadata:");
            String[] metadataKeys = wrapper.getReader().getMetadataKeys();
            for (String key : metadataKeys) {
                System.out.println(key + ": " + wrapper.getReader().getMetadataValue(key));
            }
        } catch (Exception e) {
            System.err.println("Error processing file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (wrapper != null) {
                try {
                    wrapper.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
} 