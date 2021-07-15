/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate.generator;

import au.gov.aims.aws.s3.S3Utils;
import au.gov.aims.ereefs.bean.AbstractBean;
import au.gov.aims.ereefs.bean.metadata.TimeIncrement;
import au.gov.aims.ereefs.bean.metadata.ncanimate.NcAnimateInputFileBean;
import au.gov.aims.ereefs.bean.metadata.ncanimate.NcAnimateOutputFileBean;
import au.gov.aims.ereefs.bean.metadata.ncanimate.NcAnimateOutputFileMetadataBean;
import au.gov.aims.ereefs.bean.metadata.netcdf.NetCDFMetadataBean;
import au.gov.aims.ereefs.bean.ncanimate.NcAnimateConfigBean;
import au.gov.aims.ereefs.bean.ncanimate.render.AbstractNcAnimateRenderFileBean;
import au.gov.aims.ereefs.bean.ncanimate.render.NcAnimateRenderBean;
import au.gov.aims.ereefs.bean.ncanimate.render.NcAnimateRenderMapBean;
import au.gov.aims.ereefs.bean.ncanimate.render.NcAnimateRenderMetadataBean;
import au.gov.aims.ereefs.bean.ncanimate.render.NcAnimateRenderVideoBean;
import au.gov.aims.ereefs.database.manager.MetadataManager;
import au.gov.aims.ncanimate.commons.NcAnimateGenerateFileBean;
import au.gov.aims.ncanimate.commons.NcAnimateUtils;
import au.gov.aims.ncanimate.commons.generator.context.GeneratorContext;
import au.gov.aims.ncanimate.commons.timetable.DateTimeRange;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONObject;

import java.net.URI;
import java.net.URL;
import java.util.Map;

public class MetadataGenerator {
    private static final Logger LOGGER = Logger.getLogger(MetadataGenerator.class);

    public static final String OUTPUT_FILE_MAP_TYPE = "MAP";
    public static final String OUTPUT_FILE_VIDEO_TYPE = "VIDEO";

    private NcAnimateConfigBean ncAnimateConfig;

    private TimeIncrement mapTimeIncrement;
    private TimeIncrement videoTimeIncrement;

    public MetadataGenerator(NcAnimateConfigBean ncAnimateConfig, TimeIncrement mapTimeIncrement, TimeIncrement videoTimeIncrement) {
        this.ncAnimateConfig = ncAnimateConfig;
        this.mapTimeIncrement = mapTimeIncrement;
        this.videoTimeIncrement = videoTimeIncrement;
    }

    public NcAnimateOutputFileMetadataBean generateProductMetadata(
            GeneratorContext context,
            Map<String, NetCDFMetadataBean> inputFiles,
            NcAnimateGenerateFileBean outputFile) throws Exception {

        NcAnimateRenderBean render = this.ncAnimateConfig.getRender();
        if (render == null) {
            LOGGER.warn("NcAnimate render is null. No output nor metadata can be generated.");
            return null;
        }
        if (outputFile == null) {
            LOGGER.warn("Output file is null. No metadata can be generated.");
            return null;
        }

        Map<String, AbstractNcAnimateRenderFileBean> renderFiles = outputFile.getRenderFiles();
        if (renderFiles == null || renderFiles.isEmpty()) {
            LOGGER.warn("Output file's render files are null. No metadata can be generated.");
            return null;
        }

        // Start with custom properties set in config (render.metadata.properties)
        NcAnimateRenderMetadataBean rawMetadata = render.getMetadata();
        if (rawMetadata == null) {
            LOGGER.warn("NcAnimate render's metadata is null. No metadata can be generated.");
            return null;
        }

        String id = AbstractBean.safeIdValue(NcAnimateUtils.parseString(outputFile.getId(), context));
        String definitionId = NcAnimateUtils.parseString(outputFile.getDefinitionId(), context);
        String datasetId = NcAnimateUtils.parseString(outputFile.getDatasetId(), context);


        NcAnimateOutputFileMetadataBean productMetadataBean = new NcAnimateOutputFileMetadataBean(
                new JSONObject(NcAnimateUtils.parseString(rawMetadata.toJSON().toString(), context)));

        productMetadataBean.setId(id);
        productMetadataBean.setDefinitionId(definitionId);
        productMetadataBean.setDatasetId(datasetId);
        productMetadataBean.setStatus(NetCDFMetadataBean.Status.VALID);
        productMetadataBean.setFrameDirectoryUrl(MetadataGenerator.getPublicURL(context.getS3FrameDirectory()));
        productMetadataBean.setType(MetadataManager.MetadataType.NCANIMATE_PRODUCT);
        productMetadataBean.setLastModified(new DateTime());

        DateTimeRange dateRange = context.getDateRange();
        if (dateRange != null) {
            productMetadataBean.setStartDate(dateRange.getStartDate());
            productMetadataBean.setEndDate(dateRange.getEndDate());
        }

        productMetadataBean.setRegion(context.getRegion());
        productMetadataBean.setTargetHeight(context.getTargetHeight());

        productMetadataBean.setFrameTimeIncrement(context.getFrameTimeIncrement());

        if (inputFiles != null) {
            for (NetCDFMetadataBean netCDFMetadataBean : inputFiles.values()) {
                NcAnimateInputFileBean inputFileBean = new NcAnimateInputFileBean();
                inputFileBean.setFileURI(netCDFMetadataBean.getFileURI());
                inputFileBean.setChecksum(netCDFMetadataBean.getChecksum());

                productMetadataBean.addInputFile(inputFileBean);
            }
        }

        boolean hasVideo = false, hasMap = false;
        for (Map.Entry<String, AbstractNcAnimateRenderFileBean> renderFileEntry : renderFiles.entrySet()) {
            AbstractNcAnimateRenderFileBean renderFile = renderFileEntry.getValue();
            context.setRenderFile(renderFile);

            NcAnimateOutputFileBean outputFileBean = new NcAnimateOutputFileBean();
            outputFileBean.setFileURI(MetadataGenerator.getPublicURL(renderFile.getFileURI(), context));
            outputFileBean.setWidth(context.getProductWidth());
            outputFileBean.setHeight(context.getProductHeight());

            if (renderFile instanceof NcAnimateRenderVideoBean) {
                NcAnimateRenderVideoBean videoRenderFile = (NcAnimateRenderVideoBean)renderFile;
                outputFileBean.setFps(videoRenderFile.getFps());
                outputFileBean.setType(OUTPUT_FILE_VIDEO_TYPE);
                outputFileBean.setFiletype(videoRenderFile.getFormat().toString());
                hasVideo = true;

            } else if (renderFile instanceof NcAnimateRenderMapBean) {
                NcAnimateRenderMapBean mapRenderFile = (NcAnimateRenderMapBean)renderFile;
                outputFileBean.setType(OUTPUT_FILE_MAP_TYPE);
                outputFileBean.setFiletype(mapRenderFile.getFormat().toString());
                hasMap = true;
            }

            productMetadataBean.addOutputFile(renderFileEntry.getKey(), outputFileBean);
        }

        if (hasVideo) {
            productMetadataBean.setVideoTimeIncrement(this.videoTimeIncrement);

            URI previewFileUri = VideoGenerator.getPreviewFileURI(outputFile, context);
            productMetadataBean.setPreview(MetadataGenerator.getPublicURL(previewFileUri));
        }

        if (hasMap) {
            productMetadataBean.setMapTimeIncrement(this.mapTimeIncrement);
        }

        return productMetadataBean;
    }

    private static String getPublicURL(String fileUriStr, GeneratorContext context) {
        String parsedUriStr = NcAnimateUtils.parseString(fileUriStr, context);
        if (parsedUriStr != null && !parsedUriStr.isEmpty()) {
            try {
                URI fileUri = new URI(parsedUriStr);
                return MetadataGenerator.getPublicURL(fileUri);
            } catch(Exception ex) {
                LOGGER.error(String.format("Invalid file URI: %s", parsedUriStr));
                return null;
            }
        }

        return null;
    }

    private static String getPublicURL(URI fileUri) {
        if (fileUri == null) {
            return null;
        }

        URL publicURL = S3Utils.getPublicURL(fileUri);
        if (publicURL != null) {
            return publicURL.toString();
        }
        return fileUri.toString();
    }
}
