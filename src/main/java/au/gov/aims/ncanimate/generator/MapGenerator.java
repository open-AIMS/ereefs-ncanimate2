/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate.generator;

import au.gov.aims.aws.s3.entity.S3Client;
import au.gov.aims.ereefs.bean.metadata.TimeIncrement;
import au.gov.aims.ereefs.bean.metadata.ncanimate.NcAnimateOutputFileMetadataBean;
import au.gov.aims.ereefs.bean.ncanimate.NcAnimateConfigBean;
import au.gov.aims.ereefs.bean.ncanimate.NcAnimateRegionBean;
import au.gov.aims.ereefs.bean.ncanimate.render.AbstractNcAnimateRenderFileBean;
import au.gov.aims.ereefs.bean.ncanimate.render.NcAnimateRenderMapBean;
import au.gov.aims.ereefs.database.DatabaseClient;
import au.gov.aims.ncanimate.ImageResizer;
import au.gov.aims.ncanimate.commons.NcAnimateGenerateFileBean;
import au.gov.aims.ncanimate.commons.generator.context.GeneratorContext;
import au.gov.aims.ncanimate.commons.timetable.DateTimeRange;
import au.gov.aims.ncanimate.commons.timetable.FrameTimetableMap;
import au.gov.aims.ncanimate.commons.timetable.ProductTimetable;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MapGenerator extends AbstractMediaGenerator {
    private static final Logger LOGGER = Logger.getLogger(MapGenerator.class);

    public MapGenerator(NcAnimateConfigBean ncAnimateConfig, ProductTimetable productTimetable, S3Client s3Client, DatabaseClient dbClient) {
        super(ncAnimateConfig, productTimetable, s3Client, dbClient);
    }

    public void generateMap(
            NcAnimateGenerateFileBean mapOutputFileBean,
            Map<DateTimeRange, List<FrameTimetableMap>> mapFrameMap,
            TimeIncrement frameTimeIncrement,
            String regionId) throws Exception {

        NcAnimateConfigBean ncAnimateConfig = this.getNcAnimateConfig();
        MetadataGenerator metadataGenerator = this.getMetadataGenerator();

        // Extract data needed to make the GeneratorContext
        Map<String, NcAnimateRegionBean> regionMap = ncAnimateConfig.getRegions();
        List<Double> targetHeights =  ncAnimateConfig.getTargetHeights();
        if (targetHeights == null || targetHeights.isEmpty()) {
            targetHeights = new ArrayList<Double>();
            targetHeights.add(null);
        }

        DateTimeRange mapDateRange = mapOutputFileBean.getDateRange();

        Map<String, AbstractNcAnimateRenderFileBean> renderFiles = mapOutputFileBean.getRenderFiles();

        if (regionMap != null) {

            Collection<NcAnimateRegionBean> regions = regionMap.values();
            // If regionId is specified, filter out regions
            if (regionId != null) {
                regions = new ArrayList<NcAnimateRegionBean>();
                regions.add(regionMap.get(regionId));
            }

            for (NcAnimateRegionBean region : regions) {
                for (Double targetHeight : targetHeights) {

                    // Create a GeneratorContext for all the map format (png, svg, etc)
                    GeneratorContext context = new GeneratorContext(ncAnimateConfig);
                    context.setFrameTimeIncrement(frameTimeIncrement);
                    context.setRegion(region);
                    context.setTargetHeight(targetHeight);
                    context.setDateRange(mapDateRange);
                    context.setOutputFilenamePrefix("map");

                    for (AbstractNcAnimateRenderFileBean renderFile : renderFiles.values()) {

                        context.setRenderFile(renderFile);

                        // The time range of the map must match the time range of the frame
                        NcAnimateRenderMapBean.MapFormat frameFormat = NcAnimateRenderMapBean.MapFormat.fromExtension(renderFile.getFileExtension());

                        File frameFile = AbstractMediaGenerator.getFrameFile(context, mapDateRange, frameFormat);
                        if (frameFile != null) {
                            if (!frameFile.exists()) {
                                StringBuilder errorMessage = new StringBuilder("NcAnimate frame didn't generate all necessary video frame files:");
                                errorMessage.append(String.format("%n- %s", frameFile));
                                LOGGER.error(errorMessage);
                                throw new RuntimeException("ERROR: NcAnimate frame didn't generate all necessary map frame files");

                            } else {
                                File destinationFile = context.getOutputFile();

                                boolean resized = false;
                                // Resize the image if needed
                                // Only resize raster images (SVG are not resized)
                                if (frameFormat.isRaster()) {
                                    // Get the expected image dimensions
                                    // (could be null if something went wrong during the generation of the file)
                                    Integer productWidth = context.getProductWidth();
                                    Integer productHeight = context.getProductHeight();

                                    if (productWidth != null && productHeight != null) {
                                        // Only resize if the new image dimensions are different from actual image dimensions
                                        // (original image dimensions should never be null at this point)
                                        Integer originalWidth = context.getScaledCanvasWidth();
                                        Integer originalHeight = context.getScaledCanvasHeight();
                                        if (originalWidth == null || !originalWidth.equals(productWidth) ||
                                            originalHeight == null || !originalHeight.equals(productHeight)) {

                                            try {
                                                LOGGER.debug(String.format("Resizing image %s to %s (%dx%d to %dx%d)",
                                                        frameFile, destinationFile, originalWidth, originalHeight, productWidth, productHeight));

                                                ImageResizer.resize(frameFile, destinationFile, frameFormat, productWidth, productHeight);
                                                resized = true;
                                            } catch (Exception ex) {
                                                LOGGER.error(String.format("Error occurred while resizing image %s to %s (%dx%d to %dx%d).",
                                                        frameFile, destinationFile, originalWidth, originalHeight, productWidth, productHeight), ex);
                                            }
                                        }
                                    }
                                }

                                if (!resized) {
                                    try {
                                        LOGGER.debug(String.format("Creating symbolic link %s to %s", frameFile, destinationFile));
                                        if (destinationFile.exists()) {
                                            LOGGER.warn("Deleting existing symbolic link: " + destinationFile);
                                            destinationFile.delete();
                                        }
                                        Files.createSymbolicLink(destinationFile.toPath(), frameFile.toPath());
                                    } catch (Exception ex) {
                                        LOGGER.error(String.format("Error occurred while copying %s to %s.", frameFile, destinationFile), ex);
                                    }
                                }

                                if (destinationFile.exists()) {
                                    // Upload to S3
                                    URI uploadUri = this.getFileURI(renderFile, context);
                                    LOGGER.info(String.format("Uploading %s to %s", destinationFile, uploadUri));
                                    this.uploadFile(destinationFile, uploadUri);

                                    // Delete generated map file (not the actual frame file, it might be needed for other products such as videos)
                                    if (!destinationFile.delete()) {
                                        LOGGER.error(String.format("Could not delete the generated map file: %s", destinationFile));
                                    } else {
                                        LOGGER.info(String.format("Deleted generated map file: %s", destinationFile));
                                    }
                                } else {
                                    LOGGER.error(String.format("The map file %s was not generated", destinationFile));
                                }
                            }
                        }
                    }

                    // Generate metadata
                    NcAnimateOutputFileMetadataBean metadata = metadataGenerator.generateProductMetadata(
                            context,
                            this.getInputFiles(mapDateRange, mapFrameMap),
                            mapOutputFileBean);
                    if (metadata == null) {
                        LOGGER.error("Metadata is null");
                    } else {
                        LOGGER.debug("Saving metadata to the database");
                        this.saveMetadata(metadata.toJSON());
                    }
                }
            }
        }
    }
}
