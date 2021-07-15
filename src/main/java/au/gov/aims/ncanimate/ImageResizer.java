/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate;

import au.gov.aims.ereefs.bean.ncanimate.render.NcAnimateRenderMapBean;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Resize an image to a given width x height.
 * Inspired from:
 *     https://www.codejava.net/java-se/graphics/how-to-resize-images-in-java
 */
public class ImageResizer {

    /**
     * Resizes an image to a given width and height.
     * @param inputFile The original image file
     * @param outputFile The file to save the resized image
     * @param fileFormat The format of the image
     * @param scaledWidth absolute width in pixels
     * @param scaledHeight absolute height in pixels
     * @throws IOException
     */
    public static void resize(
            File inputFile,
            File outputFile,
            NcAnimateRenderMapBean.MapFormat fileFormat,
            int scaledWidth,
            int scaledHeight
    ) throws IOException {

        // reads input image
        BufferedImage inputImage = ImageIO.read(inputFile);

        // creates output image
        BufferedImage outputImage = new BufferedImage(scaledWidth,
                scaledHeight, inputImage.getType());

        // scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        // writes to output file
        ImageIO.write(outputImage, fileFormat.getExtension(), outputFile);
    }
}
