/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate.generator;

import au.gov.aims.aws.s3.entity.S3Client;
import au.gov.aims.ereefs.Utils;
import au.gov.aims.ereefs.bean.metadata.TimeIncrement;
import au.gov.aims.ereefs.bean.metadata.ncanimate.NcAnimateOutputFileMetadataBean;
import au.gov.aims.ereefs.bean.ncanimate.NcAnimateConfigBean;
import au.gov.aims.ereefs.bean.ncanimate.NcAnimateRegionBean;
import au.gov.aims.ereefs.bean.ncanimate.render.AbstractNcAnimateRenderFileBean;
import au.gov.aims.ereefs.bean.ncanimate.render.NcAnimateRenderMapBean;
import au.gov.aims.ereefs.bean.ncanimate.render.NcAnimateRenderVideoBean;
import au.gov.aims.ereefs.database.DatabaseClient;
import au.gov.aims.ncanimate.SystemCallThread;
import au.gov.aims.ncanimate.commons.NcAnimateGenerateFileBean;
import au.gov.aims.ncanimate.commons.NcAnimateUtils;
import au.gov.aims.ncanimate.commons.generator.context.GeneratorContext;
import au.gov.aims.ncanimate.commons.timetable.DateTimeRange;
import au.gov.aims.ncanimate.commons.timetable.FrameTimetableMap;
import au.gov.aims.ncanimate.commons.timetable.ProductTimetable;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class VideoGenerator extends AbstractMediaGenerator {
    private static final Logger LOGGER = Logger.getLogger(VideoGenerator.class);

    // Frame to use for video preview.
    // NOTE: If the number is larger than the number of frames in the video, the last frame is used.
    private static final int PREVIEW_FRAME_NUMBER = 0;

    public VideoGenerator(NcAnimateConfigBean ncAnimateConfig, ProductTimetable productTimetable, S3Client s3Client, DatabaseClient dbClient) {
        super(ncAnimateConfig, productTimetable, s3Client, dbClient);
    }

    public void generateVideo(
            NcAnimateGenerateFileBean videoOutputFileBean,
            Map<DateTimeRange, List<FrameTimetableMap>> videoFrameMap,
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

        DateTimeRange videoDateRange = videoOutputFileBean.getDateRange();

        Map<String, AbstractNcAnimateRenderFileBean> renderFiles = videoOutputFileBean.getRenderFiles();

        final NcAnimateRenderMapBean.MapFormat frameFormat = GeneratorContext.VIDEO_FRAME_FORMAT;
        if (regionMap != null) {

            Collection<NcAnimateRegionBean> regions = regionMap.values();
            // If regionId is specified, filter out regions
            if (regionId != null) {
                regions = new ArrayList<NcAnimateRegionBean>();
                regions.add(regionMap.get(regionId));
            }

            for (NcAnimateRegionBean region : regions) {
                for (Double targetHeight : targetHeights) {

                    // Create a GeneratorContext for all the video format (mp4, wmv)
                    GeneratorContext context = new GeneratorContext(ncAnimateConfig);
                    context.setFrameTimeIncrement(frameTimeIncrement);
                    context.setRegion(region);
                    context.setTargetHeight(targetHeight);
                    context.setDateRange(videoDateRange);
                    context.setOutputFilenamePrefix("video");

                    File previewFile = null;
                    for (AbstractNcAnimateRenderFileBean renderFile : renderFiles.values()) {
                        if (renderFile instanceof NcAnimateRenderVideoBean) {
                            NcAnimateRenderVideoBean videoRenderFile = (NcAnimateRenderVideoBean)renderFile;
                            context.setRenderFile(videoRenderFile);

                            List<FrameTimetableMap> frameTimetableMaps = videoFrameMap.get(videoDateRange);

                            if (frameTimetableMaps != null && !frameTimetableMaps.isEmpty()) {
                                // Prepare video frames
                                File frameDir = context.getFrameDirectory();
                                File videoFrameDir = new File(frameDir, GeneratorContext.VIDEO_FRAME_DIRECTORY);
                                if (videoFrameDir.exists()) {
                                    Utils.deleteDirectory(videoFrameDir);
                                }
                                if (Utils.prepareDirectory(videoFrameDir)) {
                                    // Find all frame files
                                    // ordered alphabetically, to make sure the video frames are in the right order.
                                    Set<File> sortedFrameFiles = new TreeSet<File>();
                                    Set<File> missingFrameFiles = new TreeSet<File>();
                                    for (FrameTimetableMap frameTimetableMap : frameTimetableMaps) {
                                        for (DateTimeRange frameDateRange : frameTimetableMap.keySet()) {

                                            File frameFile = AbstractMediaGenerator.getFrameFile(context, frameDateRange, frameFormat);
                                            if (frameFile != null && frameFile.exists()) {
                                                sortedFrameFiles.add(frameFile);
                                            } else {
                                                missingFrameFiles.add(frameFile);
                                            }
                                        }
                                    }

                                    if (!missingFrameFiles.isEmpty()) {
                                        StringBuilder errorMessage = new StringBuilder("NcAnimate frame didn't generate all necessary video frame files:");
                                        for (File missingFrameFile : missingFrameFiles) {
                                            errorMessage.append(String.format("%n- %s", missingFrameFile));
                                        }
                                        LOGGER.error(errorMessage);
                                        throw new RuntimeException("ERROR: NcAnimate frame didn't generate all necessary video frame files");
                                    }

                                    // Create symbolic link for each frames
                                    if (!sortedFrameFiles.isEmpty()) {
                                        LOGGER.debug("Creating symbolic links to the video frames");
                                        int fileCounter = 0;
                                        File tempPreviewFile = null;
                                        for (File frameFile : sortedFrameFiles) {
                                            // Use the video first frame as a preview
                                            if (fileCounter <= PREVIEW_FRAME_NUMBER) {
                                                tempPreviewFile = frameFile;
                                            }
                                            File symbolicLink = new File(videoFrameDir, GeneratorContext.FRAME_FILENAME_PREFIX + "_" + String.format("%05d", fileCounter) + "." + frameFormat.getExtension());
                                            if (symbolicLink.exists()) {
                                                LOGGER.warn("Deleting existing symbolic link: " + symbolicLink);
                                                symbolicLink.delete();
                                            }
                                            Files.createSymbolicLink(symbolicLink.toPath(), frameFile.toPath());
                                            fileCounter++;
                                        }

                                        File destinationFile = context.getOutputFile();

                                        if (NcAnimateRenderVideoBean.VideoFormat.ZIP.equals(videoRenderFile.getFormat())) {
                                            LOGGER.debug("Creating video frame zip archive");
                                            this.zipVideoFrames(destinationFile, sortedFrameFiles);
                                        } else {
                                            LOGGER.debug("Executing command lines to generate the video");
                                            this.callVideoCommandLines(context);
                                        }

                                        if (destinationFile.exists()) {
                                            // Upload to S3
                                            URI uploadUri = this.getFileURI(videoRenderFile, context);
                                            LOGGER.info(String.format("Uploading video %s to %s", destinationFile, uploadUri));
                                            this.uploadFile(destinationFile, uploadUri);

                                            // Delete generated video file (not the actual frame files, those might be needed for other products)
                                            if (!destinationFile.delete()) {
                                                LOGGER.error(String.format("Could not delete the generated video file: %s", destinationFile));
                                            } else {
                                                LOGGER.info(String.format("Deleted generated video file: %s", destinationFile));
                                            }

                                            previewFile = tempPreviewFile;
                                        } else {
                                            LOGGER.error(String.format("The video file %s was not generated", destinationFile));
                                        }
                                    }
                                } else {
                                    LOGGER.error(String.format("Could not create the video frame directory: %s", videoFrameDir));
                                }

                                // Cleanup - Delete the directory used to create symlinks to video frames
                                if (videoFrameDir.exists()) {
                                    Utils.deleteDirectory(videoFrameDir);
                                }
                            }
                        }
                    }

                    // Upload video preview to S3
                    if (previewFile != null) {
                        URI previewFileUri = VideoGenerator.getPreviewFileURI(videoOutputFileBean, context);
                        LOGGER.debug(String.format("Uploading video preview %s to %s", previewFile, previewFileUri));
                        this.uploadFile(previewFile, previewFileUri);
                    }

                    // Generate metadata
                    NcAnimateOutputFileMetadataBean metadata = metadataGenerator.generateProductMetadata(
                            context,
                            this.getInputFiles(videoDateRange, videoFrameMap),
                            videoOutputFileBean);
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

    public static URI getPreviewFileURI(NcAnimateGenerateFileBean videoOutputFile, GeneratorContext context) throws URISyntaxException {
        return new URI(NcAnimateUtils.parseString(
                videoOutputFile.getPreviewFileUri(GeneratorContext.VIDEO_FRAME_FORMAT.getExtension()),
                context));
    }

    private void zipVideoFrames(File zipFile, Collection<File> frameFiles) throws IOException {
        // Create a new zip archive (ZipOutputStream) using the parameter zipFile
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (File frameFile : frameFiles) {
                try (FileInputStream fileInputStream = new FileInputStream(frameFile)) {
                    // Create a new entry in the zip archive
                    // The entry name will be the same name as the file name (it doesn't have to be)
                    ZipEntry zipEntry = new ZipEntry(frameFile.getName());
                    zipOutputStream.putNextEntry(zipEntry);

                    // Stream the frameFile bytes to the zip archive.
                    // All bytes streamed are part of the zip entry, until zipOutputStream.closeEntry() is called.
                    byte[] buffer = new byte[4090];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        zipOutputStream.write(buffer, 0, bytesRead);
                    }
                } finally {
                    zipOutputStream.closeEntry();
                }
            }
        }
    }

    private void callVideoCommandLines(GeneratorContext context) throws Exception {
        AbstractNcAnimateRenderFileBean renderFile = context.getRenderFile();
        if (renderFile instanceof NcAnimateRenderVideoBean) {
            NcAnimateRenderVideoBean videoRenderFile = (NcAnimateRenderVideoBean)renderFile;

            List<String> commandLines = videoRenderFile.getCommandLines();
            if (commandLines != null && !commandLines.isEmpty()) {
                for (String rawCommandLine : commandLines) {
                    String commandLine = NcAnimateUtils.parseString(rawCommandLine, context);
                    LOGGER.debug(String.format("Calling command line: %s", commandLine));

                    SystemCallThread systemCall = new SystemCallThread(commandLine) {
                        @Override
                        public void onStart() {
                            LOGGER.info("RUNNING: " + this.getCommandLine());
                        }

                        @Override
                        public void onStop(boolean succeed) {
                            LOGGER.info("END OF: " + this.getCommandLine());
                            if (!succeed) {
                                LOGGER.fatal("The script returned errors in the standard error stream.");
                            }
                        }

                        @Override
                        public void onException(Exception ex) {
                            LOGGER.fatal(ex);
                        }
                    };

                    systemCall.start();
                    systemCall.join();

                    Integer exitCode = systemCall.getExitCode();
                    if (exitCode != null && exitCode != 0) {
                        throw new IOException(String.format("Exception occurred while making a video. Exit code: %d", exitCode));
                    }
                }
            }
        }
    }
}
