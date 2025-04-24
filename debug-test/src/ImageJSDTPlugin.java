import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.io.OpenDialog;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.io.File;
import java.io.IOException;

import loci.formats.FormatException;
import loci.formats.IFormatReader;

/**
 * An ImageJ plugin that can open both zipped and unzipped SDT files.
 * It uses our BioFormatsSDTWrapper to handle the files appropriately.
 */
public class ImageJSDTPlugin implements PlugIn {
    
    @Override
    public void run(String arg) {
        // Open a file dialog to select an SDT file
        OpenDialog od = new OpenDialog("Open SDT File...", arg);
        String directory = od.getDirectory();
        String fileName = od.getFileName();
        
        if (fileName == null) {
            return; // User canceled the dialog
        }
        
        String filePath = directory + fileName;
        openSDTFile(filePath);
    }
    
    /**
     * Opens an SDT file and displays it in ImageJ.
     */
    public void openSDTFile(String filePath) {
        BioFormatsSDTWrapper wrapper = null;
        
        try {
            IJ.showStatus("Opening SDT file: " + filePath);
            
            // Create a wrapper for Bio-Formats
            wrapper = new BioFormatsSDTWrapper();
            wrapper.openFile(filePath);
            
            IFormatReader reader = wrapper.getReader();
            
            // Get file dimensions
            int width = reader.getSizeX();
            int height = reader.getSizeY();
            int channels = reader.getSizeC();
            int timepoints = reader.getSizeT();
            int slices = reader.getSizeZ();
            
            // Display file information
            IJ.log("File: " + filePath);
            IJ.log("Dimensions: " + width + "x" + height + "x" + slices + 
                   " (" + channels + " channels, " + timepoints + " timepoints)");
            
            // Read the pixel data and create an ImageJ image
            int totalImages = reader.getImageCount();
            ImagePlus[] images = new ImagePlus[totalImages];
            
            for (int i = 0; i < totalImages; i++) {
                IJ.showStatus("Reading image " + (i + 1) + "/" + totalImages);
                IJ.showProgress(i + 1, totalImages);
                
                // Read the pixel data
                byte[] pixels = reader.openBytes(i);
                
                // Create an ImageProcessor for the current image
                ImageProcessor ip = null;
                if (reader.getPixelType() == loci.formats.FormatTools.UINT16) {
                    short[] shortPixels = new short[pixels.length / 2];
                    for (int j = 0; j < shortPixels.length; j++) {
                        shortPixels[j] = (short) ((pixels[j * 2 + 1] & 0xff) << 8 | (pixels[j * 2] & 0xff));
                    }
                    ip = new ShortProcessor(width, height, shortPixels, null);
                } else {
                    // Handle other pixel types if needed
                    IJ.error("Unsupported pixel type");
                    return;
                }
                
                // Create an ImagePlus
                String title = filePath;
                if (totalImages > 1) {
                    title += " - " + (i + 1) + "/" + totalImages;
                }
                images[i] = new ImagePlus(title, ip);
            }
            
            // Display the images
            if (totalImages == 1) {
                images[0].show();
            } else {
                // Create a stack or hyperstack
                IJ.showStatus("Creating hyperstack...");
                
                // Get the first image
                ImagePlus firstImage = images[0];
                
                // Create a hyperstack
                ImagePlus hyperstack = IJ.createHyperStack(
                    fileName,
                    width,
                    height,
                    channels,
                    slices,
                    timepoints,
                    reader.getPixelType() == loci.formats.FormatTools.UINT16 ? 16 : 8
                );
                
                // Fill the hyperstack with data
                for (int i = 0; i < totalImages; i++) {
                    hyperstack.setProcessor(images[i].getProcessor(), i + 1);
                }
                
                hyperstack.show();
            }
            
            IJ.showStatus("SDT file opened successfully");
            
        } catch (Exception e) {
            IJ.error("Error opening SDT file", e.getMessage());
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
    
    /**
     * Main method for testing the plugin.
     */
    public static void main(String[] args) {
        // Start ImageJ
        new ImageJ();
        
        // Run the plugin
        if (args.length > 0) {
            new ImageJSDTPlugin().openSDTFile(args[0]);
        } else {
            new ImageJSDTPlugin().run("");
        }
    }
} 