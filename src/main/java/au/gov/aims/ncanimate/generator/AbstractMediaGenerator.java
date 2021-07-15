/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate.generator;

import au.gov.aims.aws.s3.FileWrapper;
import au.gov.aims.aws.s3.entity.S3Client;
import au.gov.aims.ereefs.bean.metadata.netcdf.NetCDFMetadataBean;
import au.gov.aims.ereefs.bean.ncanimate.NcAnimateConfigBean;
import au.gov.aims.ereefs.bean.ncanimate.render.AbstractNcAnimateRenderFileBean;
import au.gov.aims.ereefs.bean.ncanimate.render.NcAnimateRenderMapBean;
import au.gov.aims.ereefs.database.CacheStrategy;
import au.gov.aims.ereefs.database.DatabaseClient;
import au.gov.aims.ereefs.database.manager.MetadataManager;
import au.gov.aims.ncanimate.commons.NcAnimateUtils;
import au.gov.aims.ncanimate.commons.generator.context.GeneratorContext;
import au.gov.aims.ncanimate.commons.timetable.DateTimeRange;
import au.gov.aims.ncanimate.commons.timetable.FrameTimetable;
import au.gov.aims.ncanimate.commons.timetable.FrameTimetableMap;
import au.gov.aims.ncanimate.commons.timetable.NetCDFMetadataFrame;
import au.gov.aims.ncanimate.commons.timetable.NetCDFMetadataSet;
import au.gov.aims.ncanimate.commons.timetable.ProductTimetable;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractMediaGenerator {
    private S3Client s3Client;
    private DatabaseClient dbClient;
    private NcAnimateConfigBean ncAnimateConfig;
    private MetadataGenerator metadataGenerator;

    public AbstractMediaGenerator(
            NcAnimateConfigBean ncAnimateConfig,
            ProductTimetable productTimetable,
            S3Client s3Client,
            DatabaseClient dbClient) {

        this.s3Client = s3Client;
        this.dbClient = dbClient;
        this.ncAnimateConfig = ncAnimateConfig;
        this.metadataGenerator = new MetadataGenerator(ncAnimateConfig, productTimetable.getMapTimeIncrement(), productTimetable.getVideoTimeIncrement());
    }

    public NcAnimateConfigBean getNcAnimateConfig() {
        return this.ncAnimateConfig;
    }

    public MetadataGenerator getMetadataGenerator() {
        return this.metadataGenerator;
    }

    public static File getFrameFile(GeneratorContext context, DateTimeRange frameDateRange, NcAnimateRenderMapBean.MapFormat frameFormat) {
        Map<NcAnimateRenderMapBean.MapFormat, FileWrapper> frameFiles = context.getFrameFileWrapperMap(frameDateRange);

        FileWrapper frameFileWrapper = frameFiles.get(frameFormat);
        if (frameFileWrapper != null) {
            return frameFileWrapper.getFile();
        }

        return null;
    }

    public URI getFileURI(AbstractNcAnimateRenderFileBean renderFile, GeneratorContext context) throws URISyntaxException {
        return new URI(NcAnimateUtils.parseString(renderFile.getFileURI(), context));
    }

    public void uploadFile(File file, URI destination) throws IOException, InterruptedException {
        FileWrapper fileWrapper = new FileWrapper(destination, file);
        fileWrapper.uploadFile(this.s3Client);
    }

    public void saveMetadata(JSONObject metadata) throws Exception {
        if (metadata != null) {
            MetadataManager metadataManager = new MetadataManager(this.dbClient, CacheStrategy.DISK);
            metadataManager.save(metadata);
        }
    }

    public Map<String, NetCDFMetadataBean> getInputFiles(DateTimeRange productDateRange, Map<DateTimeRange, List<FrameTimetableMap>> frameMap) {
        Map<String, NetCDFMetadataBean> inputFiles = new HashMap<String, NetCDFMetadataBean>();

        List<FrameTimetableMap> frameTimetableMapList = frameMap.get(productDateRange);
        if (frameTimetableMapList != null) {
            for (FrameTimetableMap frameTimetableMap : frameTimetableMapList) {
                for (FrameTimetable frameTimetable : frameTimetableMap.values()) {
                    for (NetCDFMetadataSet netCDFMetadataSet : frameTimetable.values()) {
                        NetCDFMetadataFrame netCDFMetadataFrame = netCDFMetadataSet.first();
                        if (netCDFMetadataFrame != null) {
                            NetCDFMetadataBean netCDFMetadata = netCDFMetadataFrame.getMetadata();
                            if (netCDFMetadata != null) {
                                inputFiles.put(netCDFMetadata.getId(), netCDFMetadata);
                            }
                        }
                    }
                }
            }
        }

        return inputFiles;
    }
}
