/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate.generator;

import au.gov.aims.aws.s3.entity.S3Client;
import au.gov.aims.ereefs.bean.metadata.TimeIncrement;
import au.gov.aims.ereefs.bean.metadata.TimeIncrementUnit;
import au.gov.aims.ereefs.bean.metadata.ncanimate.NcAnimateInputFileBean;
import au.gov.aims.ereefs.bean.metadata.ncanimate.NcAnimateOutputFileBean;
import au.gov.aims.ereefs.bean.metadata.ncanimate.NcAnimateOutputFileMetadataBean;
import au.gov.aims.ereefs.bean.metadata.netcdf.NetCDFMetadataBean;
import au.gov.aims.ereefs.bean.ncanimate.NcAnimateBboxBean;
import au.gov.aims.ereefs.bean.ncanimate.NcAnimateConfigBean;
import au.gov.aims.ereefs.bean.ncanimate.NcAnimateRegionBean;
import au.gov.aims.ereefs.bean.ncanimate.render.AbstractNcAnimateRenderFileBean;
import au.gov.aims.ereefs.database.CacheStrategy;
import au.gov.aims.ereefs.database.DatabaseClient;
import au.gov.aims.ereefs.database.manager.MetadataManager;
import au.gov.aims.ereefs.helper.MetadataHelper;
import au.gov.aims.ereefs.helper.NcAnimateConfigHelper;
import au.gov.aims.ncanimate.DatabaseTestBase;
import au.gov.aims.ncanimate.commons.NcAnimateGenerateFileBean;
import au.gov.aims.ncanimate.commons.generator.context.GeneratorContext;
import au.gov.aims.ncanimate.commons.timetable.DateTimeRange;
import au.gov.aims.ncanimate.commons.timetable.FrameTimetableMap;
import au.gov.aims.ncanimate.commons.timetable.ProductTimetable;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class MetadataGeneratorTest extends DatabaseTestBase {
    private static Double DELTA = 0.000000001;

    @Test
    public void testGenerateMapMetadata() throws Exception {
        super.insertData();
        super.insertFakePartialGBR4NetCDFFile(true);

        String ncAnimateProductId = "gbr4_v2_temp-wind-salt-current";

        S3Client s3Client = null;
        DatabaseClient dbClient = this.getDatabaseClient();

        NcAnimateConfigHelper ncAnimateConfigHelper =
            new NcAnimateConfigHelper(dbClient, CacheStrategy.MEMORY);
        NcAnimateConfigBean ncAnimateConfig = ncAnimateConfigHelper.getNcAnimateConfig(ncAnimateProductId);

        TimeIncrement frameTimeIncrement = ncAnimateConfig.getFrameTimeIncrement();

        ProductTimetable productTimetable = new ProductTimetable(ncAnimateConfig, dbClient);
        Map<DateTimeRange, List<FrameTimetableMap>> mapFrameMap = productTimetable.getMapFrames();

        Map<String, NcAnimateRegionBean> regions = ncAnimateConfig.getRegions();
        NcAnimateRegionBean region = regions.get("qld");
        Assert.assertNotNull("Region \"qld\" not found.", region);

        // Get the list of all maps that can be generated using available input files
        List<NcAnimateGenerateFileBean> mapOutputFileBeans = productTimetable.getMapOutputFiles();
        Assert.assertEquals("Wrong number of output map files.", 2, mapOutputFileBeans.size());

        NcAnimateGenerateFileBean mapOutputFileBean = mapOutputFileBeans.get(0);

        Map<String, AbstractNcAnimateRenderFileBean> renderFiles = mapOutputFileBean.getRenderFiles();
        Assert.assertEquals("Wrong number of render files.", 3, renderFiles.size());
        AbstractNcAnimateRenderFileBean pngRenderFile = renderFiles.get("pngMap");
        Assert.assertNotNull("Render file \"pngMap\" not found.", pngRenderFile);

        DateTimeRange mapDateRange = mapOutputFileBean.getDateRange();

        GeneratorContext context = new GeneratorContext(ncAnimateConfig);
        context.setFrameTimeIncrement(frameTimeIncrement);
        context.setRegion(region);
        context.setTargetHeight(-1.5);
        context.setDateRange(mapDateRange);
        context.setOutputFilenamePrefix("map");
        context.setRenderFile(pngRenderFile);


        // Generate the map metadata and check if it's ok
        MapGenerator mapGenerator = new MapGenerator(ncAnimateConfig, productTimetable, s3Client, dbClient);
        Map<String, NetCDFMetadataBean> inputFiles = mapGenerator.getInputFiles(mapDateRange, mapFrameMap);

        MetadataGenerator metadataGenerator = mapGenerator.getMetadataGenerator();
        NcAnimateOutputFileMetadataBean metadataBean =
                metadataGenerator.generateProductMetadata(context, inputFiles, mapOutputFileBean);

        this.testMapMetadata(metadataBean);


        // Save the metadata into the DB, retrieve it and check it again
        mapGenerator.saveMetadata(metadataBean.toJSON());

        MetadataHelper metadataHelper = new MetadataHelper(dbClient, CacheStrategy.MEMORY);
        NcAnimateOutputFileMetadataBean savedMetadataBean =
                metadataHelper.getNcAnimateProductMetadata(ncAnimateProductId, metadataBean.getDatasetId());

        this.testMapMetadata(savedMetadataBean);
    }

    private void testMapMetadata(NcAnimateOutputFileMetadataBean metadataBean) {
        Assert.assertNotNull("Generated map metadata is null", metadataBean);

        //******************************
        //* Test map simple properties *
        //******************************

        // _id
        Assert.assertEquals("Map metadata id is wrong.",
                "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_qld_-1_5",
                metadataBean.getId());

        // definitionId
        Assert.assertEquals("Map metadata definitionId is wrong.",
                "gbr4_v2_temp-wind-salt-current",
                metadataBean.getDefinitionId());

        // datasetId
        Assert.assertEquals("Map metadata datasetId is wrong.",
                "gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_qld_-1.5",
                metadataBean.getDatasetId());

        // type
        Assert.assertEquals("Map metadata type is wrong.",
                MetadataManager.MetadataType.NCANIMATE_PRODUCT,
                metadataBean.getType());

        // status
        Assert.assertEquals("Map metadata status is wrong.",
                NetCDFMetadataBean.Status.VALID,
                metadataBean.getStatus());

        // lastModified
        Assert.assertNotNull("Map metadata lastModified is null.",
                metadataBean.getLastModified());
        long now = System.currentTimeMillis();
        long lastModified = metadataBean.getLastModified().getMillis();
        // Check if last modified is within now and 30 seconds ago.
        Assert.assertTrue("Map metadata lastModified is wrong.",
                lastModified <= now && lastModified >= now - 30000);

        // frameDirectoryUrl
        Assert.assertEquals("Map metadata frameDirectoryUrl is wrong.",
                "/tmp/ncanimateTests/s3/ncanimate/frames/gbr4_v2_temp-wind-salt-current/qld/height_-1.5/",
                metadataBean.getFrameDirectoryUrl());

        // preview
        Assert.assertNull("Map metadata preview is not null.",
                metadataBean.getPreview());

        // targetHeight
        Assert.assertEquals("Map metadata targetHeight is wrong.",
                -1.5,
                metadataBean.getTargetHeight(),
                DELTA);


        //*******************************
        //* Test map complex properties *
        //*******************************

        // outputFiles
        Map<String, NcAnimateOutputFileBean> outputFiles = metadataBean.getOutputFiles();
        Assert.assertNotNull("Map metadata outputFiles is null.",
                outputFiles);
        Assert.assertEquals("Map metadata outputFiles size is wrong.", 3, outputFiles.size());
        for (Map.Entry<String, NcAnimateOutputFileBean> outputFileEntry: outputFiles.entrySet()) {
            String outputFileId = outputFileEntry.getKey();
            NcAnimateOutputFileBean outputFile = outputFileEntry.getValue();
            switch(outputFileId) {
                case "gifMap":
                    Assert.assertEquals("Map metadata outputFile gifMap type is wrong.",
                            "MAP", outputFile.getType());

                    Assert.assertEquals("Map metadata outputFile gifMap fileType is wrong.",
                            "GIF", outputFile.getFiletype());

                    Assert.assertNull("Map metadata outputFile gifMap fps is not null.",
                            outputFile.getFps());

                    Assert.assertNotNull("Map metadata outputFile gifMap width is null.",
                            outputFile.getWidth());
                    Assert.assertEquals("Map metadata outputFile gifMap width is wrong.",
                            944, outputFile.getWidth().intValue());

                    Assert.assertNotNull("Map metadata outputFile gifMap height is null.",
                            outputFile.getHeight());
                    Assert.assertEquals("Map metadata outputFile gifMap height is wrong.",
                            427, outputFile.getHeight().intValue());

                    Assert.assertEquals("Map metadata outputFile gifMap fileURI is wrong.",
                            "/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_qld_-1.5.gif",
                            outputFile.getFileURI());
                    break;

                case "svgMap":
                    Assert.assertEquals("Map metadata outputFile svgMap type is wrong.",
                            "MAP", outputFile.getType());

                    Assert.assertEquals("Map metadata outputFile svgMap fileType is wrong.",
                            "SVG", outputFile.getFiletype());

                    Assert.assertNull("Map metadata outputFile svgMap fps is not null.",
                            outputFile.getFps());

                    Assert.assertNotNull("Map metadata outputFile svgMap width is null.",
                            outputFile.getWidth());
                    Assert.assertEquals("Map metadata outputFile svgMap width is wrong.",
                            944, outputFile.getWidth().intValue());

                    Assert.assertNotNull("Map metadata outputFile svgMap height is null.",
                            outputFile.getHeight());
                    Assert.assertEquals("Map metadata outputFile svgMap height is wrong.",
                            427, outputFile.getHeight().intValue());

                    Assert.assertEquals("Map metadata outputFile svgMap fileURI is wrong.",
                            "/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_qld_-1.5.svg",
                            outputFile.getFileURI());
                    break;

                case "pngMap":
                    Assert.assertEquals("Map metadata outputFile pngMap type is wrong.",
                            "MAP", outputFile.getType());

                    Assert.assertEquals("Map metadata outputFile pngMap fileType is wrong.",
                            "PNG", outputFile.getFiletype());

                    Assert.assertNull("Map metadata outputFile pngMap fps is not null.",
                            outputFile.getFps());

                    Assert.assertNotNull("Map metadata outputFile pngMap width is null.",
                            outputFile.getWidth());
                    Assert.assertEquals("Map metadata outputFile pngMap width is wrong.",
                            250, outputFile.getWidth().intValue());

                    Assert.assertNotNull("Map metadata outputFile pngMap height is null.",
                            outputFile.getHeight());
                    Assert.assertEquals("Map metadata outputFile pngMap height is wrong.",
                            113, outputFile.getHeight().intValue());

                    Assert.assertEquals("Map metadata outputFile pngMap fileURI is wrong.",
                            "/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_qld_-1.5.png",
                            outputFile.getFileURI());
                    break;

                default:
                    Assert.fail(String.format("Unexpect map outputFile ID: %s", outputFileId));
            }
        }

        // inputFiles
        List<NcAnimateInputFileBean> inputFiles = metadataBean.getInputFiles();
        Assert.assertNotNull("Map metadata inputFiles is null.",
                inputFiles);
        Assert.assertEquals("Map metadata inputFiles size is wrong.", 1, inputFiles.size());
        NcAnimateInputFileBean inputFile = inputFiles.get(0);
        // inputFile fileURI
        Assert.assertNotNull("Map metadata inputFile fileURI is null.",
                inputFile.getFileURI());
        Assert.assertEquals("Map metadata inputFile fileURI is wrong.",
                "file:/tmp/ncanimateTests/netcdfFiles/gbr4_v2_2010-09-01_00h00-02h00.nc",
                inputFile.getFileURI().toString());
        // inputFile checksum
        Assert.assertEquals("Map metadata inputFile checksum is wrong.",
                "MD5:5f238185be1919bfb3ad928b29802aa8",
                inputFile.getChecksum());

        // dateRange
        DateTime startDate = metadataBean.getStartDate();
        DateTime endDate = metadataBean.getEndDate();
        // dateRange startDate
        Assert.assertNotNull("Map metadata dateRange startDate is null.",
                startDate);
        Assert.assertEquals("Map metadata dateRange startDate is wrong.",
                "2010-09-01T00:00:00.000+10:00",
                startDate.toString());
        // dateRange endDate
        Assert.assertNotNull("Map metadata dateRange endDate is null.",
                endDate);
        Assert.assertEquals("Map metadata dateRange endDate is wrong.",
                "2010-09-01T01:00:00.000+10:00",
                endDate.toString());

        // videoTimeIncrement
        Assert.assertNull("Map metadata videoTimeIncrement is not null.",
                metadataBean.getVideoTimeIncrement());

        // mapTimeIncrement
        TimeIncrement mapTimeIncrement = metadataBean.getMapTimeIncrement();
        Assert.assertNotNull("Map metadata mapTimeIncrement is null.",
                mapTimeIncrement);
        Assert.assertNotNull("Map metadata mapTimeIncrement increment is null.",
                mapTimeIncrement.getIncrement());
        Assert.assertEquals("Map metadata mapTimeIncrement increment is wrong.",
                1, mapTimeIncrement.getIncrement().intValue());
        Assert.assertEquals("Map metadata mapTimeIncrement unit is wrong.",
                TimeIncrementUnit.HOUR, mapTimeIncrement.getUnit());

        // frameTimeIncrement
        TimeIncrement frameTimeIncrement = metadataBean.getFrameTimeIncrement();
        Assert.assertNotNull("Map metadata frameTimeIncrement is null.",
                frameTimeIncrement);
        Assert.assertNotNull("Map metadata frameTimeIncrement increment is null.",
                frameTimeIncrement.getIncrement());
        Assert.assertEquals("Map metadata frameTimeIncrement increment is wrong.",
                1, frameTimeIncrement.getIncrement().intValue());
        Assert.assertEquals("Map metadata frameTimeIncrement unit is wrong.",
                TimeIncrementUnit.HOUR, frameTimeIncrement.getUnit());

        // region
        // Region last modified is the date found in "resources/ncanimate/configParts/regions/qld.json"
        long regionLastModified = DateTime.parse("2019-08-15T12:25:00.000+08:00").getMillis();
        NcAnimateRegionBean region = metadataBean.getRegion();
        Assert.assertNotNull("Map metadata region is null.", region);
        Assert.assertNotNull("Map metadata region ID is null.", region.getId());
        Assert.assertEquals("Map metadata region ID is wrong.",
                "qld", region.getId().getValue());
        Assert.assertEquals("Map metadata region ID is wrong.",
                regionLastModified, region.getLastModified());
        Assert.assertEquals("Map metadata region label is wrong.",
                "Queensland", region.getLabel());
        NcAnimateBboxBean regionBboxBean = region.getBbox();
        Assert.assertNotNull("Map metadata region bbox is null.", regionBboxBean);
        Assert.assertEquals("Map metadata region bbox north is wrong.",
                -7.6, regionBboxBean.getNorth(), DELTA);
        Assert.assertEquals("Map metadata region bbox east is wrong.",
                156, regionBboxBean.getEast(), DELTA);
        Assert.assertEquals("Map metadata region bbox south is wrong.",
                -29.4, regionBboxBean.getSouth(), DELTA);
        Assert.assertEquals("Map metadata region bbox west is wrong.",
                142.4, regionBboxBean.getWest(), DELTA);

        // properties
        JSONObject jsonProperties = metadataBean.getProperties();
        Assert.assertNotNull("Map metadata properties is null.", jsonProperties);
        Assert.assertEquals("Map metadata number of properties is wrong.",
                2, jsonProperties.length());
        Assert.assertEquals("Map metadata properties framePeriod is wrong.",
                "Hourly", jsonProperties.optString("framePeriod", null));
        Assert.assertEquals("Map metadata properties framePeriod is wrong.",
                "-1.5", jsonProperties.optString("targetHeight", null));
    }


    @Test
    public void testGenerateVideoMetadata() throws Exception {
        super.insertData();
        super.insertFakePartialGBR4NetCDFFile(true);

        String ncAnimateProductId = "gbr4_v2_temp-wind-salt-current";

        S3Client s3Client = null;
        DatabaseClient dbClient = this.getDatabaseClient();

        NcAnimateConfigHelper ncAnimateConfigHelper =
            new NcAnimateConfigHelper(dbClient, CacheStrategy.MEMORY);
        NcAnimateConfigBean ncAnimateConfig = ncAnimateConfigHelper.getNcAnimateConfig(ncAnimateProductId);

        TimeIncrement frameTimeIncrement = ncAnimateConfig.getFrameTimeIncrement();

        ProductTimetable productTimetable = new ProductTimetable(ncAnimateConfig, dbClient);
        Map<DateTimeRange, List<FrameTimetableMap>> videoFrameMap = productTimetable.getVideoFrames();

        Map<String, NcAnimateRegionBean> regions = ncAnimateConfig.getRegions();
        NcAnimateRegionBean region = regions.get("qld");
        Assert.assertNotNull("Region \"qld\" not found.", region);

        // Get the list of all maps that can be generated using available input files
        List<NcAnimateGenerateFileBean> videoOutputFileBeans = productTimetable.getVideoOutputFiles();
        Assert.assertEquals("Wrong number of output video files.", 1, videoOutputFileBeans.size());

        NcAnimateGenerateFileBean videoOutputFileBean = videoOutputFileBeans.get(0);

        Map<String, AbstractNcAnimateRenderFileBean> renderFiles = videoOutputFileBean.getRenderFiles();
        Assert.assertEquals("Wrong number of render files.", 3, renderFiles.size());
        AbstractNcAnimateRenderFileBean mp4RenderFile = renderFiles.get("mp4Video");
        Assert.assertNotNull("Render file \"mp4Video\" not found.", mp4RenderFile);

        DateTimeRange mapDateRange = videoOutputFileBean.getDateRange();

        GeneratorContext context = new GeneratorContext(ncAnimateConfig);
        context.setFrameTimeIncrement(frameTimeIncrement);
        context.setRegion(region);
        context.setTargetHeight(-1.5);
        context.setDateRange(mapDateRange);
        context.setOutputFilenamePrefix("video");
        context.setRenderFile(mp4RenderFile);


        // Generate the video metadata and check if it's ok
        VideoGenerator videoGenerator = new VideoGenerator(ncAnimateConfig, productTimetable, s3Client, dbClient);
        Map<String, NetCDFMetadataBean> inputFiles = videoGenerator.getInputFiles(mapDateRange, videoFrameMap);

        MetadataGenerator metadataGenerator = videoGenerator.getMetadataGenerator();
        NcAnimateOutputFileMetadataBean metadataBean =
                metadataGenerator.generateProductMetadata(context, inputFiles, videoOutputFileBean);

        this.testVideoMetadata(metadataBean);


        // Save the metadata into the DB, retrieve it and check it again
        videoGenerator.saveMetadata(metadataBean.toJSON());

        MetadataHelper metadataHelper = new MetadataHelper(dbClient, CacheStrategy.MEMORY);
        NcAnimateOutputFileMetadataBean savedMetadataBean =
                metadataHelper.getNcAnimateProductMetadata(ncAnimateProductId, metadataBean.getDatasetId());

        this.testVideoMetadata(savedMetadataBean);
    }

    private void testVideoMetadata(NcAnimateOutputFileMetadataBean metadataBean) {
        Assert.assertNotNull("Generated video metadata is null", metadataBean);

        //********************************
        //* Test video simple properties *
        //********************************

        // _id
        Assert.assertEquals("Video metadata id is wrong.",
                "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_qld_-1_5",
                metadataBean.getId());

        // definitionId
        Assert.assertEquals("Video metadata definitionId is wrong.",
                "gbr4_v2_temp-wind-salt-current",
                metadataBean.getDefinitionId());

        // datasetId
        Assert.assertEquals("Video metadata datasetId is wrong.",
                "gbr4_v2_temp-wind-salt-current_video_yearly_2010_qld_-1.5",
                metadataBean.getDatasetId());

        // type
        Assert.assertEquals("Video metadata type is wrong.",
                MetadataManager.MetadataType.NCANIMATE_PRODUCT,
                metadataBean.getType());

        // status
        Assert.assertEquals("Video metadata status is wrong.",
                NetCDFMetadataBean.Status.VALID,
                metadataBean.getStatus());

        // lastModified
        Assert.assertNotNull("Video metadata lastModified is null.",
                metadataBean.getLastModified());
        long now = System.currentTimeMillis();
        long lastModified = metadataBean.getLastModified().getMillis();
        // Check if last modified is within now and 30 seconds ago.
        Assert.assertTrue("Video metadata lastModified is wrong.",
                lastModified <= now && lastModified >= now - 30000);

        // frameDirectoryUrl
        Assert.assertEquals("Video metadata frameDirectoryUrl is wrong.",
                "/tmp/ncanimateTests/s3/ncanimate/frames/gbr4_v2_temp-wind-salt-current/qld/height_-1.5/",
                metadataBean.getFrameDirectoryUrl());

        // preview
        Assert.assertEquals("Video metadata preview is wrong.",
                metadataBean.getPreview(),
                "/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_qld_-1.5_preview.png");

        // targetHeight
        Assert.assertEquals("Video metadata targetHeight is wrong.",
                -1.5,
                metadataBean.getTargetHeight(),
                DELTA);


        //*********************************
        //* Test video complex properties *
        //*********************************

        // outputFiles
        Map<String, NcAnimateOutputFileBean> outputFiles = metadataBean.getOutputFiles();
        Assert.assertNotNull("Video metadata outputFiles is null.",
                outputFiles);
        Assert.assertEquals("Video metadata outputFiles size is wrong.", 3, outputFiles.size());
        for (Map.Entry<String, NcAnimateOutputFileBean> outputFileEntry: outputFiles.entrySet()) {
            String outputFileId = outputFileEntry.getKey();
            NcAnimateOutputFileBean outputFile = outputFileEntry.getValue();
            switch(outputFileId) {
                case "wmvVideo":
                    Assert.assertEquals("Video metadata outputFile wmvVideo type is wrong.",
                            "VIDEO", outputFile.getType());

                    Assert.assertEquals("Video metadata outputFile wmvVideo fileType is wrong.",
                            "WMV", outputFile.getFiletype());

                    Assert.assertNotNull("Video metadata outputFile wmvVideo fps is null.",
                            outputFile.getFps());
                    Assert.assertEquals("Video metadata outputFile wmvVideo fps is wrong.",
                            10, outputFile.getFps().intValue());

                    Assert.assertNotNull("Video metadata outputFile wmvVideo width is null.",
                            outputFile.getWidth());
                    Assert.assertEquals("Video metadata outputFile wmvVideo width is wrong.",
                            640, outputFile.getWidth().intValue());

                    Assert.assertNotNull("Video metadata outputFile wmvVideo height is null.",
                            outputFile.getHeight());
                    Assert.assertEquals("Video metadata outputFile wmvVideo height is wrong.",
                            290, outputFile.getHeight().intValue());

                    Assert.assertEquals("Video metadata outputFile wmvVideo fileURI is wrong.",
                            "/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_qld_-1.5.wmv",
                            outputFile.getFileURI());
                    break;

                case "zipArchive":
                    Assert.assertEquals("Video metadata outputFile zipArchive type is wrong.",
                            "VIDEO", outputFile.getType());

                    Assert.assertEquals("Video metadata outputFile zipArchive fileType is wrong.",
                            "ZIP", outputFile.getFiletype());

                    Assert.assertNull("Video metadata outputFile zipArchive fps is not null.",
                            outputFile.getFps());

                    Assert.assertNotNull("Video metadata outputFile zipArchive width is null.",
                            outputFile.getWidth());
                    Assert.assertEquals("Video metadata outputFile zipArchive width is wrong.",
                            944, outputFile.getWidth().intValue());

                    Assert.assertNotNull("Video metadata outputFile zipArchive height is null.",
                            outputFile.getHeight());
                    Assert.assertEquals("Video metadata outputFile zipArchive height is wrong.",
                            427, outputFile.getHeight().intValue());

                    Assert.assertEquals("Video metadata outputFile zipArchive fileURI is wrong.",
                            "/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_qld_-1.5.zip",
                            outputFile.getFileURI());
                    break;

                case "mp4Video":
                    Assert.assertEquals("Video metadata outputFile mp4Video type is wrong.",
                            "VIDEO", outputFile.getType());

                    Assert.assertEquals("Video metadata outputFile mp4Video fileType is wrong.",
                            "MP4", outputFile.getFiletype());

                    Assert.assertNotNull("Video metadata outputFile mp4Video fps is null.",
                            outputFile.getFps());
                    Assert.assertEquals("Video metadata outputFile mp4Video fps is wrong.",
                            12, outputFile.getFps().intValue());

                    Assert.assertNotNull("Video metadata outputFile mp4Video width is null.",
                            outputFile.getWidth());
                    Assert.assertEquals("Video metadata outputFile mp4Video width is wrong.",
                            944, outputFile.getWidth().intValue());

                    Assert.assertNotNull("Video metadata outputFile mp4Video height is null.",
                            outputFile.getHeight());
                    Assert.assertEquals("Video metadata outputFile mp4Video height is wrong.",
                            432, outputFile.getHeight().intValue());

                    Assert.assertEquals("Video metadata outputFile mp4Video fileURI is wrong.",
                            "/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_qld_-1.5.mp4",
                            outputFile.getFileURI());
                    break;

                default:
                    Assert.fail(String.format("Unexpect video outputFile ID: %s", outputFileId));
            }
        }

        // inputFiles
        List<NcAnimateInputFileBean> inputFiles = metadataBean.getInputFiles();
        Assert.assertNotNull("Video metadata inputFiles is null.",
                inputFiles);
        Assert.assertEquals("Video metadata inputFiles size is wrong.", 1, inputFiles.size());
        NcAnimateInputFileBean inputFile = inputFiles.get(0);
        // inputFile fileURI
        Assert.assertNotNull("Video metadata inputFile fileURI is null.",
                inputFile.getFileURI());
        Assert.assertEquals("Video metadata inputFile fileURI is wrong.",
                "file:/tmp/ncanimateTests/netcdfFiles/gbr4_v2_2010-09-01_00h00-02h00.nc",
                inputFile.getFileURI().toString());
        // inputFile checksum
        Assert.assertEquals("Video metadata inputFile checksum is wrong.",
                "MD5:5f238185be1919bfb3ad928b29802aa8",
                inputFile.getChecksum());

        // dateRange
        DateTime startDate = metadataBean.getStartDate();
        DateTime endDate = metadataBean.getEndDate();
        // dateRange startDate
        Assert.assertNotNull("Video metadata dateRange startDate is null.",
                startDate);
        Assert.assertEquals("Video metadata dateRange startDate is wrong.",
                "2010-09-01T00:00:00.000+10:00",
                startDate.toString());
        // dateRange endDate
        Assert.assertNotNull("Video metadata dateRange endDate is null.",
                endDate);
        Assert.assertEquals("Video metadata dateRange endDate is wrong.",
                "2010-09-01T02:00:00.000+10:00",
                endDate.toString());

        // videoTimeIncrement
        TimeIncrement videoTimeIncrement = metadataBean.getVideoTimeIncrement();
        Assert.assertNotNull("Video metadata videoTimeIncrement is null.",
                videoTimeIncrement);
        Assert.assertNotNull("Video metadata videoTimeIncrement increment is null.",
                videoTimeIncrement.getIncrement());
        Assert.assertEquals("Video metadata videoTimeIncrement increment is wrong.",
                1, videoTimeIncrement.getIncrement().intValue());
        Assert.assertEquals("Video metadata videoTimeIncrement unit is wrong.",
                TimeIncrementUnit.YEAR, videoTimeIncrement.getUnit());

        // mapTimeIncrement
        Assert.assertNull("Video metadata mapTimeIncrement is not null.",
                metadataBean.getMapTimeIncrement());

        // frameTimeIncrement
        TimeIncrement frameTimeIncrement = metadataBean.getFrameTimeIncrement();
        Assert.assertNotNull("Video metadata frameTimeIncrement is null.",
                frameTimeIncrement);
        Assert.assertNotNull("Video metadata frameTimeIncrement increment is null.",
                frameTimeIncrement.getIncrement());
        Assert.assertEquals("Video metadata frameTimeIncrement increment is wrong.",
                1, frameTimeIncrement.getIncrement().intValue());
        Assert.assertEquals("Video metadata frameTimeIncrement unit is wrong.",
                TimeIncrementUnit.HOUR, frameTimeIncrement.getUnit());

        // region
        // Region last modified is the date found in "resources/ncanimate/configParts/regions/qld.json"
        long regionLastModified = DateTime.parse("2019-08-15T12:25:00.000+08:00").getMillis();
        NcAnimateRegionBean region = metadataBean.getRegion();
        Assert.assertNotNull("Video metadata region is null.", region);
        Assert.assertNotNull("Video metadata region ID is null.", region.getId());
        Assert.assertEquals("Video metadata region ID is wrong.",
                "qld", region.getId().getValue());
        Assert.assertEquals("Video metadata region ID is wrong.",
                regionLastModified, region.getLastModified());
        Assert.assertEquals("Video metadata region label is wrong.",
                "Queensland", region.getLabel());
        NcAnimateBboxBean regionBboxBean = region.getBbox();
        Assert.assertNotNull("Video metadata region bbox is null.", regionBboxBean);
        Assert.assertEquals("Video metadata region bbox north is wrong.",
                -7.6, regionBboxBean.getNorth(), DELTA);
        Assert.assertEquals("Video metadata region bbox east is wrong.",
                156, regionBboxBean.getEast(), DELTA);
        Assert.assertEquals("Video metadata region bbox south is wrong.",
                -29.4, regionBboxBean.getSouth(), DELTA);
        Assert.assertEquals("Video metadata region bbox west is wrong.",
                142.4, regionBboxBean.getWest(), DELTA);

        // properties
        JSONObject jsonProperties = metadataBean.getProperties();
        Assert.assertNotNull("Video metadata properties is null.", jsonProperties);
        Assert.assertEquals("Video metadata number of properties is wrong.",
                2, jsonProperties.length());
        Assert.assertEquals("Video metadata properties framePeriod is wrong.",
                "Hourly", jsonProperties.optString("framePeriod", null));
        Assert.assertEquals("Video metadata properties framePeriod is wrong.",
                "-1.5", jsonProperties.optString("targetHeight", null));
    }
}
