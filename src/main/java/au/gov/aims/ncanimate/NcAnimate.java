/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate;

import au.gov.aims.aws.s3.entity.S3Client;
import au.gov.aims.ereefs.Utils;
import au.gov.aims.ereefs.bean.metadata.TimeIncrement;
import au.gov.aims.ereefs.bean.metadata.netcdf.NetCDFMetadataBean;
import au.gov.aims.ereefs.bean.ncanimate.NcAnimateConfigBean;
import au.gov.aims.ereefs.bean.ncanimate.NcAnimateRegionBean;
import au.gov.aims.ereefs.bean.ncanimate.render.AbstractNcAnimateRenderFileBean;
import au.gov.aims.ereefs.bean.ncanimate.render.NcAnimateRenderMapBean;
import au.gov.aims.ereefs.bean.ncanimate.render.NcAnimateRenderVideoBean;
import au.gov.aims.ereefs.bean.task.TaskBean;
import au.gov.aims.ereefs.database.CacheStrategy;
import au.gov.aims.ereefs.database.DatabaseClient;
import au.gov.aims.ereefs.helper.NcAnimateConfigHelper;
import au.gov.aims.ereefs.helper.TaskHelper;
import au.gov.aims.ncanimate.commons.NcAnimateGenerateFileBean;
import au.gov.aims.ncanimate.commons.NcAnimateUtils;
import au.gov.aims.ncanimate.commons.generator.context.GeneratorContext;
import au.gov.aims.ncanimate.commons.timetable.DateTimeRange;
import au.gov.aims.ncanimate.commons.timetable.FrameTimetable;
import au.gov.aims.ncanimate.commons.timetable.FrameTimetableMap;
import au.gov.aims.ncanimate.commons.timetable.NetCDFMetadataFrame;
import au.gov.aims.ncanimate.commons.timetable.NetCDFMetadataSet;
import au.gov.aims.ncanimate.commons.timetable.ProductTimetable;
import au.gov.aims.ncanimate.generator.AbstractMediaGenerator;
import au.gov.aims.ncanimate.generator.FrameGenerator;
import au.gov.aims.ncanimate.generator.MapGenerator;
import au.gov.aims.ncanimate.generator.VideoGenerator;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class NcAnimate {
    private static final Logger LOGGER = Logger.getLogger(NcAnimate.class);

    private static final String TASKID_ENV_VARIABLE = "TASK_ID";
    private static final String NCANIMATE_TASK_TYPE = "ncanimate";

    private static final int LOG_OUTDATED_LIMIT = 3;

    private DatabaseClient dbClient;
    private FrameGenerator frameGenerator;

    private S3Client s3Client;

    private String regionId;

    public static void main(String ... args) throws Exception {
        String taskId = NcAnimate.getTaskId(args);

        if ("__FIX_NCANIMATE_PRODUCT_METADATA_ID__".equals(taskId)) {
            NcAnimateMetadataIdFixer.fixMetadataIds(
                    new DatabaseClient(NcAnimateUtils.APP_NAME), CacheStrategy.NONE);
            return;
        }
        /* WARNING! This task will trigger a re-generation of all nc-aggregate tasks. */
        if ("__FIX_DOWNLOAD_METADATA_ID__".equals(taskId)) {
            NcAnimateMetadataIdFixer.fixDownloadMetadataIds(
                    new DatabaseClient(NcAnimateUtils.APP_NAME), CacheStrategy.NONE);
            return;
        }
        if ("__FIX_METADATA_DUPLICATE_ID__".equals(taskId)) {
            NcAnimateMetadataIdFixer.fixDuplicatedMetadataIds(
                    new DatabaseClient(NcAnimateUtils.APP_NAME), CacheStrategy.NONE);
            return;
        }
        // 2023-01-17: CSIRO changed all the last modified dates of the files on NCI, but didn't change any data.
        //     That triggered a re-download of all the old files, creating duplicate of all the old metadata:
        //     - The metadata for the download files which have a "." in them now have a duplicate entry with a "_".
        //     To fix this issue permanently, we create this task ID which do the following:
        //     1. Remove new duplicate ID with a "_" in them.
        //     2. Change the last modified date of the old metadata to some date far in the future (3000-01-01)
        //         to prevent this issue from re-occurring.
        if ("__LOCK_OLD_EREEFS_METADATA_IDS__".equals(taskId)) {
            NcAnimateMetadataIdFixer.lockOldMetadataIds(
                    new DatabaseClient(NcAnimateUtils.APP_NAME), CacheStrategy.NONE);
            return;
        }

        NcAnimate ncAnimate = new NcAnimate();
        ncAnimate.generateFromTaskId(taskId);
    }

    public NcAnimate() {
        this(new DatabaseClient(NcAnimateUtils.APP_NAME), NcAnimate.createS3Client());
    }

    private static S3Client createS3Client() {
        try {
            return new S3Client();
        } catch(Exception ex) {
            LOGGER.warn("Exception occurred while initialising the S3Client. Please ignore if encountered in unit tests.", ex);
            return null;
        }
    }

    public NcAnimate(DatabaseClient dbClient, S3Client s3Client) {
        this.dbClient = dbClient;
        this.s3Client = s3Client;

        this.regionId = null;
        this.frameGenerator = new FrameGenerator();
    }

    /**
     * Set a custom service end point, for MongoDB.
     * Used with unit tests.
     */
    public void setCustomDatabaseServerAddress(String customDatabaseServerAddress, int customDatabaseServerPort) {
        this.frameGenerator.setCustomDatabaseServerAddress(customDatabaseServerAddress, customDatabaseServerPort);
    }

    /**
     * Set custom database name, for MongoDB.
     * Used with unit tests.
     */
    public void setCustomDatabaseName(String customDatabaseName) {
        this.frameGenerator.setCustomDatabaseName(customDatabaseName);
    }


    /**
     * Set the region ID
     * @param regionId ID of the region to generate. Set to null to generate all regions.
     */
    public void setRegionId(String regionId) {
        this.frameGenerator.setRegionId(regionId);
        this.regionId = regionId;
    }

    private static String getTaskId(String ... args) {
        // Look for a task ID from parameters.
        if (args != null && args.length > 0) {
            return args[0];
        }

        // Get "TASK_ID" from environmental variables (used when ran on AWS infrastructure)
        return System.getenv(TASKID_ENV_VARIABLE);
    }

    public TaskBean getTask(String taskId) throws Exception {
        if (taskId != null) {
            TaskHelper taskHelper = new TaskHelper(this.dbClient, CacheStrategy.DISK);
            return taskHelper.getTask(taskId);
        }

        return null;
    }

    public void generateFromTaskId(String taskId) throws Exception {
        LOGGER.info(String.format("Generate for task ID: %s", taskId));

        NcAnimateUtils.printMemoryUsage("NcAnimate init");
        long maxMemory = Runtime.getRuntime().maxMemory();
        if (maxMemory == Long.MAX_VALUE) {
            LOGGER.info(String.format("%n    Max memory: UNLIMITED"));
        } else {
            LOGGER.info(String.format("%n    Max memory: LIMITED TO %.2f MB", (maxMemory / (1024 * 1024.0))));
        }

        TaskBean task = this.getTask(taskId);
        if (task != null) {
            LOGGER.info(String.format("Task: %s", task));

            String taskType = task.getType();
            String regionId = task.getRegionId();
            if (regionId != null && !regionId.isEmpty()) {
                this.setRegionId(regionId);
            } else {
                this.setRegionId(null);
            }

            if (NCANIMATE_TASK_TYPE.equals(taskType)) {
                String productId = task.getProductId();
                if (productId != null && !productId.isEmpty()) {
                    this.generateFromProductId(productId);
                } else {
                    throw new IllegalArgumentException(String.format("The task has no productDefinitionId. Task ID: %s", taskId));
                }
            } else {
                throw new IllegalArgumentException(
                        String.format("Wrong task type. Expected \"%s\" Found \"%s\". Task ID: %s", NCANIMATE_TASK_TYPE, taskType, taskId));
            }
        } else {
            throw new IllegalArgumentException(String.format("Task not found. Task ID: %s", taskId));
        }
    }

    /**
     * Generate frames for a product.
     * NOTE: This method does not generate all frames blindly!
     * 1. Loop through all products, find the one which are outdated
     * 2. Build a list of frames which needs to be generated (don't care about outdated or missing frames, NcAnimate frame take care of that)
     * 3. Merge all frame date ranges into large continuous date ranges (defragmentation of available dates from input data files)
     * 4. Split the large continuous date ranges into date range of frames sharing the same input files
     * 5. Generate the frames (using the grouping above)
     * 6. Generate outdated products (videos and maps)
     *
     * @param productId ID of the NcAnimate configuration to generate.
     * @throws Exception
     */
    public void generateFromProductId(String productId) throws Exception {
        LOGGER.info(String.format("Generate for product ID: %s", productId));

        NcAnimateConfigHelper configHelper = new NcAnimateConfigHelper(this.dbClient, CacheStrategy.DISK);
        NcAnimateConfigBean ncAnimateConfig = configHelper.getNcAnimateConfig(productId);
        ProductTimetable productTimetable = new ProductTimetable(ncAnimateConfig, this.dbClient);

        NcAnimateUtils.printMemoryUsage("NcAnimate ProductTimetable");

        MapGenerator mapGenerator = new MapGenerator(ncAnimateConfig, productTimetable, this.s3Client, this.dbClient);
        VideoGenerator videoGenerator = new VideoGenerator(ncAnimateConfig, productTimetable, this.s3Client, this.dbClient);

        // Validate regions
        Map<String, NcAnimateRegionBean> regionMap = ncAnimateConfig.getRegions();
        if (regionMap == null || regionMap.isEmpty()) {
            throw new IllegalStateException(String.format("Invalid NcAnimate configuration ID %s. The configuration contains no region.", productId));
        }

        if (this.regionId != null) {
            NcAnimateRegionBean selectedRegion = regionMap.get(this.regionId);
            if (selectedRegion == null) {
                throw new IllegalStateException(String.format("Can not generate NcAnimate products for region ID %s, configuration ID %s. The region is invalid.", regionId, productId));
            }
        }

        try {
            LOGGER.info(String.format("Region ID: %s", (this.regionId == null ? "unset (generating for all regions)" : this.regionId)));

            // Get all video frames and files
            LOGGER.info("Generate list of outdated videos");
            Map<DateTimeRange, List<FrameTimetableMap>> videoFrameMap = productTimetable.getVideoFrames();
            List<NcAnimateGenerateFileBean> videoOutputFileBeans = productTimetable.getVideoOutputFiles();

            // Filter outdated video files
            List<NcAnimateGenerateFileBean> outdatedVideoOutputFileBeans = new ArrayList<NcAnimateGenerateFileBean>();
            for (NcAnimateGenerateFileBean videoOutputFileBean : videoOutputFileBeans) {
                if (NcAnimateUtils.isOutdated(this.s3Client, videoOutputFileBean, videoFrameMap, ncAnimateConfig, this.regionId, outdatedVideoOutputFileBeans.size() < LOG_OUTDATED_LIMIT)) {
                    outdatedVideoOutputFileBeans.add(videoOutputFileBean);
                }
            }
            LOGGER.info(String.format("Found %d outdated videos", outdatedVideoOutputFileBeans.size()));


            // Get all map frames and files
            LOGGER.info("Generate list of outdated maps");
            Map<DateTimeRange, List<FrameTimetableMap>> mapFrameMap = productTimetable.getMapFrames();
            List<NcAnimateGenerateFileBean> mapOutputFileBeans = productTimetable.getMapOutputFiles();

            // Filter outdated map files
            List<NcAnimateGenerateFileBean> outdatedMapOutputFileBeans = new ArrayList<NcAnimateGenerateFileBean>();
            for (NcAnimateGenerateFileBean mapOutputFileBean : mapOutputFileBeans) {
                if (NcAnimateUtils.isOutdated(this.s3Client, mapOutputFileBean, mapFrameMap, ncAnimateConfig, this.regionId, outdatedMapOutputFileBeans.size() < LOG_OUTDATED_LIMIT)) {
                    outdatedMapOutputFileBeans.add(mapOutputFileBean);
                }
            }
            LOGGER.info(String.format("Found %d outdated maps", outdatedMapOutputFileBeans.size()));

            NcAnimateUtils.printMemoryUsage("NcAnimate outdated products");


            if (!outdatedVideoOutputFileBeans.isEmpty() || !outdatedMapOutputFileBeans.isEmpty()) {
                TimeIncrement frameTimeIncrement = ncAnimateConfig.getFrameTimeIncrement();

                // Create a (very big) list of all the frame files that will be generated
                LOGGER.info("Plan out how frame files needs to be generated");
                Map<String, File> allFrameFiles = new HashMap<String, File>();
                if (!outdatedVideoOutputFileBeans.isEmpty()) {
                    for (NcAnimateGenerateFileBean outdatedVideoOutdatedFileBean : outdatedVideoOutputFileBeans) {
                        allFrameFiles.putAll(this.getFrameFiles(ncAnimateConfig, outdatedVideoOutdatedFileBean, videoFrameMap, frameTimeIncrement));
                    }
                }
                if (!outdatedMapOutputFileBeans.isEmpty()) {
                    for (NcAnimateGenerateFileBean outdatedMapOutdatedFileBean : outdatedMapOutputFileBeans) {
                        allFrameFiles.putAll(this.getFrameFiles(ncAnimateConfig, outdatedMapOutdatedFileBean, mapFrameMap, frameTimeIncrement));
                    }
                }

                // Group all video and map frames into one big collection (since the process to generate video frame and map is the same)
                Map<DateTimeRange, List<FrameTimetableMap>> allFrames = combineFrames(videoFrameMap, mapFrameMap);

                NcAnimateUtils.printMemoryUsage("NcAnimate allFrames map");


                // Create a list of all product date range that needs to be generated
                List<DateTimeRange> unmergedDateRanges = new ArrayList<DateTimeRange>();
                for (NcAnimateGenerateFileBean outdatedVideoOutputFileBean : outdatedVideoOutputFileBeans) {
                    unmergedDateRanges.add(outdatedVideoOutputFileBean.getDateRange());
                }
                for (NcAnimateGenerateFileBean outdatedMapOutputFileBean : outdatedMapOutputFileBeans) {
                    unmergedDateRanges.add(outdatedMapOutputFileBean.getDateRange());
                }

                if (!unmergedDateRanges.isEmpty()) {
                    // Merge product date range into long continuous date ranges (defragmentation)
                    SortedSet<DateTimeRange> mergedDateRanges = DateTimeRange.mergeDateRanges(unmergedDateRanges);
                    if (mergedDateRanges != null && !mergedDateRanges.isEmpty()) {
                        SortedSet<DateTimeRange> generatedDateRanges = new TreeSet<DateTimeRange>();

                        for (DateTimeRange mergedDateRange : mergedDateRanges) {
                            // Split the long continuous date range into smaller date range containing frames that use the same input files
                            Map<Set<String>, SortedSet<DateTimeRange>> groupedFrames = this.groupFrames(mergedDateRange, allFrames);

                            // Sort all date ranges in a single collection
                            SortedSet<DateTimeRange> sortedDateRanges = new TreeSet<DateTimeRange>();
                            for (SortedSet<DateTimeRange> fileGroupDateRange : groupedFrames.values()) {
                                sortedDateRanges.addAll(fileGroupDateRange);
                            }

                            if (!sortedDateRanges.isEmpty()) {
                                // Add missing frames, to generate "No data" frames where there is no data available
                                SortedSet<DateTimeRange> noGapSortedDateRanges = new TreeSet<DateTimeRange>();
                                DateTime lastEndDate = null;
                                for (DateTimeRange dateRange : sortedDateRanges) {
                                    noGapSortedDateRanges.add(dateRange);
                                    if (lastEndDate != null) {
                                        if (lastEndDate.compareTo(dateRange.getStartDate()) < 0) {
                                            DateTimeRange noDataDateRange = DateTimeRange.create(lastEndDate, dateRange.getStartDate());
                                            noGapSortedDateRanges.add(noDataDateRange);
                                        }
                                    }
                                    lastEndDate = dateRange.getEndDate();
                                }
                                // Add missing frames at the beginning
                                if (!DateTimeRange.ALL_TIME.equals(mergedDateRange)) {
                                    DateTimeRange firstDateRange = sortedDateRanges.first();
                                    if (mergedDateRange.getStartDate().compareTo(firstDateRange.getStartDate()) < 0) {
                                        noGapSortedDateRanges.add(DateTimeRange.create(mergedDateRange.getStartDate(), firstDateRange.getStartDate()));
                                    }
                                    // Add missing frames at the end
                                    DateTimeRange lastDateRange = sortedDateRanges.last();
                                    if (mergedDateRange.getEndDate().compareTo(lastDateRange.getEndDate()) > 0) {
                                        noGapSortedDateRanges.add(DateTimeRange.create(lastDateRange.getEndDate(), mergedDateRange.getEndDate()));
                                    }
                                }

                                for (DateTimeRange dateRange : noGapSortedDateRanges) {
                                    LOGGER.info(String.format("Generate frame files for date range [%s - %s]", dateRange.getStartDate(), dateRange.getEndDate()));


                                    NcAnimateUtils.printMemoryUsage("NcAnimate before generateFrames");

                                    // Generate video frames & map frames per group of dates that share the same input files
                                    this.frameGenerator.generateFrames(ncAnimateConfig, dateRange);

                                    NcAnimateUtils.printMemoryUsage("NcAnimate after generateFrames");


                                    generatedDateRanges.add(dateRange);
                                    generatedDateRanges = DateTimeRange.mergeDateRanges(generatedDateRanges);

                                    // Generate maps and videos that can be generated with the frames we currently have

                                    // Generate outdated videos
                                    if (!outdatedVideoOutputFileBeans.isEmpty()) {
                                        List<NcAnimateGenerateFileBean> newOutdatedVideoOutputFiles = new ArrayList<NcAnimateGenerateFileBean>();

                                        for (NcAnimateGenerateFileBean outdatedVideoOutputFileBean : outdatedVideoOutputFileBeans) {
                                            DateTimeRange videoDateRange = outdatedVideoOutputFileBean.getDateRange();
                                            if (this.isReady(generatedDateRanges, videoDateRange)) {
                                                videoGenerator.generateVideo(outdatedVideoOutputFileBean, videoFrameMap, frameTimeIncrement, this.regionId);
                                            } else {
                                                newOutdatedVideoOutputFiles.add(outdatedVideoOutputFileBean);
                                            }
                                        }

                                        outdatedVideoOutputFileBeans = newOutdatedVideoOutputFiles;
                                    }

                                    // Generate outdated maps
                                    if (!outdatedMapOutputFileBeans.isEmpty()) {
                                        List<NcAnimateGenerateFileBean> newOutdatedMapOutputFiles = new ArrayList<NcAnimateGenerateFileBean>();

                                        for (NcAnimateGenerateFileBean outdatedMapOutputFileBean : outdatedMapOutputFileBeans) {
                                            DateTimeRange mapDateRange = outdatedMapOutputFileBean.getDateRange();
                                            if (this.isReady(generatedDateRanges, mapDateRange)) {
                                                mapGenerator.generateMap(outdatedMapOutputFileBean, mapFrameMap, frameTimeIncrement, this.regionId);
                                            } else {
                                                newOutdatedMapOutputFiles.add(outdatedMapOutputFileBean);
                                            }
                                        }

                                        outdatedMapOutputFileBeans = newOutdatedMapOutputFiles;
                                    }

                                    // Delete generated frame files that are not needed anymore to generate other products (video or map)
                                    this.cleanupFrames(
                                            ncAnimateConfig,
                                            videoFrameMap,
                                            frameTimeIncrement,
                                            mapFrameMap,
                                            frameTimeIncrement,
                                            outdatedVideoOutputFileBeans,
                                            outdatedMapOutputFileBeans,
                                            allFrameFiles);
                                }
                            }
                        }
                    }
                }
            }

            LOGGER.info("--------------------------------------------------");
            LOGGER.info("---------------- End of NcAnimate ----------------");
            LOGGER.info("--------------------------------------------------");

            if (!outdatedVideoOutputFileBeans.isEmpty()) {
                LOGGER.error("Some videos could not be generated:");
                for (NcAnimateGenerateFileBean outdatedVideoOutputFileBean : outdatedVideoOutputFileBeans) {
                    DateTimeRange dateRange = outdatedVideoOutputFileBean.getDateRange();
                    LOGGER.error(String.format("    - %s (%s - %s)",
                            outdatedVideoOutputFileBean.getFileId(),
                            dateRange == null ? null : dateRange.getStartDate(),
                            dateRange == null ? null : dateRange.getEndDate()));
                }
            }

            if (!outdatedMapOutputFileBeans.isEmpty()) {
                LOGGER.error("Some maps could not be generated:");
                for (NcAnimateGenerateFileBean outdatedMapOutputFileBean : outdatedMapOutputFileBeans) {
                    DateTimeRange dateRange = outdatedMapOutputFileBean.getDateRange();
                    LOGGER.error(String.format("    - %s (%s - %s)",
                            outdatedMapOutputFileBean.getFileId(),
                            dateRange == null ? null : dateRange.getStartDate(),
                            dateRange == null ? null : dateRange.getEndDate()));
                }
            }

            if (!outdatedVideoOutputFileBeans.isEmpty() || !outdatedMapOutputFileBeans.isEmpty()) {
                throw new IllegalStateException("Some output products could not be generated.");
            }

        } finally {
            NcAnimateUtils.clearCache();

            // Delete temporary working directory before exiting
            File workingDirectory = ncAnimateConfig.getRender().getWorkingDirectoryFile();
            if (workingDirectory.exists()) {
                Utils.deleteDirectory(workingDirectory);
            }
        }
    }

    /**
     * Check if a product (video or map) has all the frame files needed to generate it.
     * @param generatedDateRanges
     * @param productDateRange
     * @return
     */
    private boolean isReady(SortedSet<DateTimeRange> generatedDateRanges, DateTimeRange productDateRange) {
        for (DateTimeRange generatedDateRange : generatedDateRanges) {
            if (generatedDateRange.contains(productDateRange)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Delete generated frames files that are no longer needed.
     * NOTE: This method needs to be called after the generation of maps / videos.
     *     NcAnimate frame generate many GB of frames. If those frames stay on disk
     *     until the end, on a long generation, the server may run out of disk space.
     * @param ncAnimateConfig
     * @param videoFrameMap
     * @param videoFrameTimeIncrement
     * @param mapFrameMap
     * @param mapFrameTimeIncrement
     * @param remainingVideoOutdatedFileBeans
     * @param remainingMapOutdatedFileBeans
     * @param allFrameFiles
     */
    private void cleanupFrames(
            NcAnimateConfigBean ncAnimateConfig,
            Map<DateTimeRange, List<FrameTimetableMap>> videoFrameMap,
            TimeIncrement videoFrameTimeIncrement,
            Map<DateTimeRange, List<FrameTimetableMap>> mapFrameMap,
            TimeIncrement mapFrameTimeIncrement,
            List<NcAnimateGenerateFileBean> remainingVideoOutdatedFileBeans,
            List<NcAnimateGenerateFileBean> remainingMapOutdatedFileBeans,
            Map<String, File> allFrameFiles) {

        LOGGER.info("Cleanup old frame files");

        // Create a (very big) list of all the frame files needed for the remaining products (maps & videos) to create
        Map<String, File> frameFileNeeded = new HashMap<String, File>();
        if (remainingVideoOutdatedFileBeans != null && !remainingVideoOutdatedFileBeans.isEmpty()) {
            for (NcAnimateGenerateFileBean remainingVideoOutdatedFileBean : remainingVideoOutdatedFileBeans) {
                frameFileNeeded.putAll(this.getFrameFiles(ncAnimateConfig, remainingVideoOutdatedFileBean, videoFrameMap, videoFrameTimeIncrement));
            }
        }
        if (remainingMapOutdatedFileBeans != null && !remainingMapOutdatedFileBeans.isEmpty()) {
            for (NcAnimateGenerateFileBean remainingMapOutdatedFileBean : remainingMapOutdatedFileBeans) {
                frameFileNeeded.putAll(this.getFrameFiles(ncAnimateConfig, remainingMapOutdatedFileBean, mapFrameMap, mapFrameTimeIncrement));
            }
        }

        // Loop through all frame files and delete the one which are not required anymore
        Set<File> deletedFiles = new TreeSet<File>();
        if (allFrameFiles != null && !allFrameFiles.isEmpty()) {
            for (Map.Entry<String, File> allFrameFileEntry : allFrameFiles.entrySet()) {
                if (!frameFileNeeded.containsKey(allFrameFileEntry.getKey())) {
                    File unneededFrameFile = allFrameFileEntry.getValue();
                    if (unneededFrameFile != null && unneededFrameFile.exists()) {
                        if (!unneededFrameFile.delete()) {
                            LOGGER.warn(String.format("Could not delete old frame file: %s", unneededFrameFile));
                        } else {
                            deletedFiles.add(unneededFrameFile);
                        }
                    }
                }
            }
        }

        if (!deletedFiles.isEmpty()) {
            LOGGER.info(String.format("Deleted %d old frame files", deletedFiles.size()));

            // Only generate the following debug message is the LOGGER level is DEBUG or higher
            if (LOGGER.isDebugEnabled()) {
                StringBuilder debugMessage = new StringBuilder("Deleted old frame files:");
                for (File deletedFile : deletedFiles) {
                    debugMessage.append(String.format("%n- %s", deletedFile));
                }
                LOGGER.debug(debugMessage);
            }
        }
    }

    private Map<String, File> getFrameFiles(
            NcAnimateConfigBean ncAnimateConfig,
            NcAnimateGenerateFileBean outputFile,
            Map<DateTimeRange, List<FrameTimetableMap>> frameMap,
            TimeIncrement frameTimeIncrement) {

        Map<String, File> frameFiles = new HashMap<String, File>();

        if (outputFile == null || frameMap == null || frameMap.isEmpty()) {
            return frameFiles;
        }

        DateTimeRange dateRange = outputFile.getDateRange();

        List<FrameTimetableMap> productFrameTimetableMaps = frameMap.get(dateRange);
        if (productFrameTimetableMaps == null || productFrameTimetableMaps.isEmpty()) {
            return frameFiles;
        }

        Map<String, NcAnimateRegionBean> regionMap = ncAnimateConfig.getRegions();
        List<Double> targetHeights =  ncAnimateConfig.getTargetHeights();
        if (targetHeights == null || targetHeights.isEmpty()) {
            targetHeights = new ArrayList<Double>();
            targetHeights.add(null);
        }

        GeneratorContext context = new GeneratorContext(ncAnimateConfig);

        Map<String, AbstractNcAnimateRenderFileBean> renderFiles = outputFile.getRenderFiles();

        if (regionMap != null) {

            Collection<NcAnimateRegionBean> regions = regionMap.values();
            // If regionId is specified, filter out regions
            if (this.regionId != null) {
                regions = new ArrayList<NcAnimateRegionBean>();
                regions.add(regionMap.get(this.regionId));
            }

            for (NcAnimateRegionBean region : regions) {
                for (Double targetHeight : targetHeights) {

                    for (FrameTimetableMap productFrameTimetableMap : productFrameTimetableMaps) {
                        for (DateTimeRange frameDateRange : productFrameTimetableMap.keySet()) {
                            context.setFrameTimeIncrement(frameTimeIncrement);
                            context.setRegion(region);
                            context.setTargetHeight(targetHeight);
                            context.setDateRange(frameDateRange);

                            for (AbstractNcAnimateRenderFileBean renderFile : renderFiles.values()) {
                                context.setRenderFile(renderFile);

                                NcAnimateRenderMapBean.MapFormat frameFormat = null;
                                if (renderFile instanceof NcAnimateRenderVideoBean) {
                                    frameFormat = GeneratorContext.VIDEO_FRAME_FORMAT;
                                } else if (renderFile instanceof NcAnimateRenderMapBean) {
                                    frameFormat = NcAnimateRenderMapBean.MapFormat.fromExtension(renderFile.getFileExtension());
                                }

                                File frameFile = AbstractMediaGenerator.getFrameFile(context, frameDateRange, frameFormat);
                                if (frameFile != null) {
                                    frameFiles.put(frameFile.getAbsolutePath(), frameFile);
                                }
                            }
                        }
                    }
                }
            }
        }

        return frameFiles;
    }

    /**
     * Create a Map containing the FrameTimetableMap of videoFrameMap and mapFrameMap.
     * This method do not combine DateTimeRange, it just put them all in List.
     * @param videoFrameMap
     * @param mapFrameMap
     * @return A new Map containing all the FrameTimetableMap for the videos and maps.
     */
    private Map<DateTimeRange, List<FrameTimetableMap>> combineFrames(Map<DateTimeRange, List<FrameTimetableMap>> videoFrameMap, Map<DateTimeRange, List<FrameTimetableMap>> mapFrameMap) {
        Map<DateTimeRange, List<FrameTimetableMap>> allFrames = new HashMap<DateTimeRange, List<FrameTimetableMap>>();

        if (videoFrameMap != null) {
            for (Map.Entry<DateTimeRange, List<FrameTimetableMap>> videoFrameMapEntry : videoFrameMap.entrySet()) {
                DateTimeRange dateRange = videoFrameMapEntry.getKey();
                List<FrameTimetableMap> frames = videoFrameMapEntry.getValue();

                allFrames.put(dateRange, new ArrayList<FrameTimetableMap>(frames));
            }
        }

        if (mapFrameMap != null) {
            for (Map.Entry<DateTimeRange, List<FrameTimetableMap>> mapFrameMapEntry : mapFrameMap.entrySet()) {
                DateTimeRange dateRange = mapFrameMapEntry.getKey();
                List<FrameTimetableMap> frames = mapFrameMapEntry.getValue();

                List<FrameTimetableMap> collectedFrames = allFrames.get(dateRange);
                if (collectedFrames == null) {
                    collectedFrames = new ArrayList<FrameTimetableMap>();
                    allFrames.put(dateRange, collectedFrames);
                }

                collectedFrames.addAll(frames);
            }
        }

        return allFrames;
    }

    private Map<Set<String>, SortedSet<DateTimeRange>> groupFrames(DateTimeRange mergedDateRange, Map<DateTimeRange, List<FrameTimetableMap>> frameMap) {
        Map<Set<String>, SortedSet<DateTimeRange>> dateRangeMap = new HashMap<Set<String>, SortedSet<DateTimeRange>>();

        if (frameMap != null) {
            for (Map.Entry<DateTimeRange, List<FrameTimetableMap>> frameMapEntry : frameMap.entrySet()) {
                DateTimeRange frameDateRange = frameMapEntry.getKey();
                if (mergedDateRange.contains(frameDateRange)) {
                    List<FrameTimetableMap> frameList = frameMapEntry.getValue();
                    for (FrameTimetableMap frameTimetableMap : frameList) {
                        for (Map.Entry<DateTimeRange, FrameTimetable> frameTimetableMapEntry : frameTimetableMap.entrySet()) {
                            FrameTimetable frameTimetable = frameTimetableMapEntry.getValue();
                            Set<String> metadataIds = this.getMetadataIds(frameTimetable);
                            if (!metadataIds.isEmpty()) {
                                // NOTE: Date ranges can not simply be merged here.
                                //     That could be a problem if we have gaps in the data:
                                //     Input:
                                //         File 1:           [----------]      [------------]
                                //         File 2:           [------------------------------]
                                //     Output:
                                //         [File 1, File 2]: [------------------------------]
                                //         [File 2]:                    [------]
                                //     Expected:
                                //         [File 1, File 2]: [----------]      [------------]
                                //         [File 2]:                    [------]
                                //     To do this, store all frameTimetableMapEntry.getKey() dateRange into a list, then merge the list at the end
                                DateTimeRange newDateRange = frameTimetableMapEntry.getKey();
                                if (newDateRange != null) {
                                    SortedSet<DateTimeRange> dateRangeList = dateRangeMap.get(metadataIds);
                                    if (dateRangeList == null) {
                                        dateRangeList = new TreeSet<DateTimeRange>();
                                        dateRangeMap.put(metadataIds, dateRangeList);
                                    }
                                    dateRangeList.add(newDateRange);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Merge date ranges
        for (Map.Entry<Set<String>, SortedSet<DateTimeRange>> dateRangeMapEntry : dateRangeMap.entrySet()) {
            SortedSet<DateTimeRange> unmergedDateRanges = dateRangeMapEntry.getValue();
            SortedSet<DateTimeRange> mergedDateRanges = DateTimeRange.mergeDateRanges(unmergedDateRanges);
            dateRangeMap.put(dateRangeMapEntry.getKey(), mergedDateRanges);
        }

        return dateRangeMap;
    }

    private Set<String> getMetadataIds(FrameTimetable frameTimetable) {
        Set<String> metadataIds = new HashSet<String>();

        if (frameTimetable != null) {
            for (NetCDFMetadataSet netCDFMetadataSet : frameTimetable.values()) {
                for (NetCDFMetadataFrame netCDFMetadataFrame : netCDFMetadataSet) {
                    NetCDFMetadataBean netCDFMetadataBean = netCDFMetadataFrame.getMetadata();
                    if (netCDFMetadataBean != null) {
                        String metadataId = netCDFMetadataBean.getId();
                        if (metadataId != null) {
                            metadataIds.add(metadataId);
                        }
                    }
                }
            }
        }

        return metadataIds;
    }
}
