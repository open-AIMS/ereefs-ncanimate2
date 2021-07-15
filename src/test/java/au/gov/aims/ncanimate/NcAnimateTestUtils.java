/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate;

import au.gov.aims.ereefs.bean.metadata.netcdf.NetCDFMetadataBean;
import au.gov.aims.ereefs.database.CacheStrategy;
import au.gov.aims.ereefs.database.DatabaseClient;
import au.gov.aims.ereefs.database.manager.MetadataManager;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;

/**
 * A few useful methods used with ncAnimate and ncAnimate-frame tests.
 */
public class NcAnimateTestUtils {
    private static boolean debug = false;

    public static void setDebug(boolean debug) {
        NcAnimateTestUtils.debug = debug;
    }

    public static void insertInputDataFromDirectory(
            DatabaseClient dbClient, String definitionId, File dir) throws Exception {

        NcAnimateTestUtils.insertInputDataFromDirectory(dbClient, definitionId, dir, ".nc");
    }

    public static void insertInputDataFromDirectory(
            DatabaseClient dbClient, String definitionId, File dir, String fileExtension) throws Exception {

        File[] netCDFFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String filename) {
                return filename != null && filename.endsWith(fileExtension);
            }
        });

        if (netCDFFiles != null) {
            NcAnimateTestUtils.insertInputDataFiles(dbClient, definitionId, netCDFFiles);
        }
    }

    public static void insertInputDataFiles(
            DatabaseClient dbClient, String definitionId, File ... netCDFFiles) throws Exception {

        for (File netCDFFile : netCDFFiles) {
            String datasetId = netCDFFile.getName();

            if (NcAnimateTestUtils.debug) {
                System.out.println("Adding NetCDF file: " + datasetId);
            }

            URI fileURI = netCDFFile.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(
                    definitionId, datasetId, fileURI, netCDFFile, netCDFFile.lastModified(), false);

            MetadataManager metadataManager = new MetadataManager(dbClient, CacheStrategy.DISK);

            if (NcAnimateTestUtils.debug) {
                System.out.println(metadata.toString());
            }
            metadataManager.save(metadata.toJSON());
        }
    }
}
