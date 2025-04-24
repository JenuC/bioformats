import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.ZipInputStream;

import loci.common.DataTools;
import loci.common.RandomAccessInputStream;
import loci.formats.CoreMetadata;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.MetadataTools;
import loci.formats.in.SDTInfo;
import loci.formats.in.SDTReader;
import loci.formats.meta.MetadataStore;

/**
 * An enhanced version of the SDTReader class that better handles zipped SDT files.
 * This class extends the original SDTReader to add improved handling for ZIP compressed data.
 */
public class EnhancedSDTReader extends SDTReader {
    
    private boolean isZipped = false;
    private byte[] zipHeader = new byte[4];
    
    /**
     * Constructs a new EnhancedSDTReader.
     */
    public EnhancedSDTReader() {
        super();
    }
    
    /**
     * Checks if a file has a ZIP signature at its data block offset.
     */
    private boolean isZippedFile(RandomAccessInputStream stream) throws IOException {
        long currentPos = stream.getFilePointer();
        
        // Read the data block offset
        stream.seek(16); // dataBlockOffs is at position 16 in the header
        int dataBlockOffs = stream.readInt();
        
        // Check for PK signature at the data block offset
        stream.seek(dataBlockOffs);
        stream.read(zipHeader);
        boolean result = zipHeader[0] == 'P' && zipHeader[1] == 'K';
        
        // Return to the original position
        stream.seek(currentPos);
        
        return result;
    }
    
    /**
     * Enhanced version of the openBytes method that better handles zipped SDT files.
     */
    @Override
    public byte[] openBytes(int no, byte[] buf, int x, int y, int w, int h)
        throws FormatException, IOException
    {
        FormatTools.checkPlaneParameters(this, no, buf.length, x, y, w, h);

        int sizeX = getSizeX();
        int sizeY = getSizeY();
        int bpp = FormatTools.getBytesPerPixel(getPixelType());
        boolean little = isLittleEndian();

        // This is the Becker Hickl block not the pre-loaded data block
        long blockSize = info.allBlockLengths[getSeries()];

        int paddedWidth = sizeX + ((4 - (sizeX % 4)) % 4);
        int times = timeBins;
        if (info.mcstaPoints == getSizeT()) {
            times = getSizeT();
        }
        long planeSize = (long) paddedWidth * sizeY * times * bpp;

        // remove width padding if we can be reasonably certain
        // that the unpadded width is correct
        if (paddedWidth > sizeX && planeSize * getSizeC() > blockSize &&
            (planeSize / paddedWidth) * sizeX * getSizeC() <= blockSize)
        {
            paddedWidth = sizeX;
            planeSize = sizeX * sizeY * times * bpp;
        }

        if (preLoad && !intensity) {
            int channel = no / times;
            int timeBin = no % times;

            byte[] rowBuf = new byte[bpp * times * paddedWidth];

            int binSize = paddedWidth * sizeY * bpp;

            int preBlockSize = binSize * blockLength;

            // pre-load data for performance
            if (dataStore == null || storedSeries != getSeries()) {
                dataStore = new byte[preBlockSize];
                currentBlock = -1;
            }

            if (timeBin / blockLength != currentBlock || storedChannel != channel) {
                currentBlock = timeBin / blockLength;

                // A subset of whole timebins (a preBlock) is copied into storage
                // to allow different sub-plane sizes to be used for different timebin
                in.seek(info.allBlockOffsets[getSeries()]);
                ZipInputStream codec = null;

                // Enhanced ZIP detection and handling
                if (isZipped) {
                    codec = createZipInputStream();
                    codec.getNextEntry();
                    codec.skip(channel * planeSize);
                } else {
                    // Check for ZIP signature
                    String check = in.readString(2);
                    in.seek(in.getFilePointer() - 2);
                    
                    if (check.equals("PK")) {
                        isZipped = true;
                        codec = createZipInputStream();
                        codec.getNextEntry();
                        codec.skip(channel * planeSize);
                    } else {
                        in.skipBytes(channel * planeSize);
                    }
                }

                int endOfBlock = (currentBlock + 1) * blockLength;
                int storeLength;
                if (endOfBlock > times) {
                    storeLength = times - (currentBlock * blockLength);
                } else {
                    storeLength = blockLength;
                }

                for (int row = 0; row < sizeY; row++) {
                    readPixels(rowBuf, in, codec, 0);

                    for (int col = 0; col < paddedWidth; col++) {
                        // set output to first pixel of this row in 2D plane
                        // corresponding to zeroth timeBin
                        int output = (row * paddedWidth + col) * bpp;
                        int input = ((col * times) + (currentBlock * blockLength)) * bpp;
                        // copy subset of decay into buffer.

                        for (int t = 0; t < storeLength; t++) {
                            for (int bb = 0; bb < bpp; bb++) {
                                dataStore[output + bb] = rowBuf[input + bb];
                            }
                            output += binSize;
                            input += bpp;
                        }
                    }
                }
            }
            storedChannel = channel;
            storedSeries = getSeries();
            // dataStore loaded

            // copy 2D plane from dataStore into buf
            int iLineSize = paddedWidth * bpp;
            int oLineSize = w * bpp;
            // offset to correct timebin yth line and xth pixel
            int binInStore = timeBin - (currentBlock * blockLength);

            int input = (binSize * binInStore) + (y * iLineSize) + (x * bpp);
            int output = 0;

            for (int row = 0; row < h; row++) {
                System.arraycopy(dataStore, input, buf, output, oLineSize);
                input += iLineSize;
                output += oLineSize;
            }

            // allow for >1 count increments
            // the count increment is the amount by which the data is incremented for each event detected
            // normally this is 1 so each bit represents a photon
            // where it is >1 then divide the 16 bit data to get an answer in photon units
            if (info.incr > 1) {
                int incr = info.incr;

                ByteBuffer bb = ByteBuffer.wrap(buf);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                short s;

                for (int i = 0; i < buf.length; i += 2) {
                    s = (short) bb.getShort(i);
                    if (s > 0) { // sign bit is not set
                        bb.putShort(i, (short) (s / incr));
                    } else { // sign bit is set so extend to int to do the division
                        int ii = s & 0xffff;
                        bb.putShort(i, (short) (ii / incr));
                    }
                }
            }

            return buf;
        } else { // intensity mode so no pre-loading

            int channel = intensity ? no : no / times;
            int timeBin = intensity ? 0 : no % times;

            byte[] b = !intensity ? buf : new byte[sizeY * sizeX * times * bpp];

            byte[] rowBuf = new byte[bpp * times * w];

            in.seek(info.allBlockOffsets[getSeries()]);

            ZipInputStream codec = null;
            
            // Enhanced ZIP detection and handling
            if (isZipped) {
                codec = createZipInputStream();
                codec.getNextEntry();
                codec.skip(channel * planeSize + y * paddedWidth * bpp * times);
            } else {
                // Check for ZIP signature
                String check = in.readString(2);
                in.seek(in.getFilePointer() - 2);
                
                if (check.equals("PK")) {
                    isZipped = true;
                    codec = createZipInputStream();
                    codec.getNextEntry();
                    codec.skip(channel * planeSize + y * paddedWidth * bpp * times);
                } else {
                    in.skipBytes(channel * planeSize + (long) y * paddedWidth * bpp * times);
                }
            }

            for (int row = 0; row < h; row++) {
                readPixels(rowBuf, in, codec, (long) x * bpp * times);
                if (intensity) {
                    System.arraycopy(rowBuf, 0, b, row * bpp * times * w, rowBuf.length);
                } else {
                    for (int col = 0; col < w; col++) {
                        int output = (row * w + col) * bpp;
                        int input = (col * times + timeBin) * bpp;
                        for (int bb = 0; bb < bpp; bb++) {
                            b[output + bb] = rowBuf[input + bb];
                        }
                    }
                }
                if (codec == null) {
                    in.skipBytes((long) bpp * times * (paddedWidth - x - w));
                } else {
                    codec.skip(bpp * times * (paddedWidth - x - w));
                }
            }

            if (!intensity) {
                return buf;
            }

            // need to combine all lifetime bins into intensity image
            int binSize = w * bpp;
            for (int row = 0; row < h; row++) {
                int base = row * binSize;
                if (bpp == 2) {
                    for (int col = 0; col < w; col++) {
                        short sum = 0;
                        for (int t = 0; t < times; t++) {
                            int index = row * w * times * bpp + col * times * bpp + t * bpp;
                            sum += DataTools.bytesToShort(b, index, little);
                        }
                        DataTools.unpackBytes(sum, buf, base + col * 2, 2, little);
                    }
                } else {
                    for (int col = 0; col < w; col++) {
                        for (int bb = 0; bb < bpp; bb++) {
                            buf[base + col * bpp + bb] = 0;
                        }
                        for (int t = 0; t < times; t++) {
                            int index = row * w * times * bpp + col * times * bpp + t * bpp;
                            for (int bb = 0; bb < bpp; bb++) {
                                buf[base + col * bpp + bb] += b[index + bb];
                            }
                        }
                    }
                }
            }
            return buf;
        }
    }
    
    /**
     * Creates a new ZipInputStream from the current position of the input stream.
     */
    private ZipInputStream createZipInputStream() throws IOException {
        // Get the current position
        long currentPos = in.getFilePointer();
        
        // Create a ZipInputStream based on a portion of the input
        ZipInputStream zis = new ZipInputStream(in.getInputStream(currentPos));
        
        // Reset the cursor position
        in.seek(currentPos);
        
        return zis;
    }
    
    /**
     * Enhanced version of the initFile method that detects zipped SDT files.
     */
    @Override
    protected void initFile(String id) throws FormatException, IOException {
        super.initFile(id);
        
        // Check if the file is zipped
        isZipped = isZippedFile(in);
        if (isZipped) {
            LOGGER.info("Detected zipped SDT file");
        }
    }
    
    /**
     * Enhanced version of the readPixels method that better handles corrupted ZIP data.
     */
    @Override
    protected void readPixels(byte[] rowBuf, RandomAccessInputStream in, ZipInputStream codec, long skip)
        throws IOException 
    {
        if (codec == null) {
            in.skipBytes(skip);
            in.read(rowBuf);
        } else {
            try {
                codec.skip(skip);
                int nread = 0;
                while (nread < rowBuf.length) {
                    int n = codec.read(rowBuf, nread, rowBuf.length - nread);
                    if (n <= 0) {
                        // Handle corrupted zip data by filling the rest with zeros
                        if (nread < rowBuf.length) {
                            Arrays.fill(rowBuf, nread, rowBuf.length, (byte) 0);
                            LOGGER.warn("Filling {} bytes with zeros due to incomplete ZIP data", 
                                      rowBuf.length - nread);
                        }
                        break;
                    }
                    nread += n;
                }
            } catch (IOException e) {
                LOGGER.error("Error reading ZIP data: {}", e.getMessage());
                
                // Handle errors by filling with zeros
                Arrays.fill(rowBuf, (byte) 0);
                throw e;
            }
        }
    }
} 