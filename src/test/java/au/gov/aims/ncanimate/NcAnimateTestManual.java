/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate;

import au.gov.aims.ereefs.bean.metadata.netcdf.NetCDFMetadataBean;
import au.gov.aims.ereefs.database.CacheStrategy;
import au.gov.aims.ereefs.database.manager.MetadataManager;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.URI;

/**
 * - NcAnimate manual tests -
 * Those tests are run manually to check NcAnimate behaviour
 * or to see the content of a given NetCDF file.
 */
public class NcAnimateTestManual extends DatabaseTestBase {
    private static final boolean WITH_MD5SUM = false;

    @Ignore
    @Test
    public void testGenerateGbr1() throws Exception {
        this.insertData();
        this.insertInputData_realData_hydro_gbr1();
        this.insertInputData_realData_hydro_gbr4();

        NcAnimate ncAnimate = this.getNcanimate();
        ncAnimate.generateFromProductId("gbr1_2-0_temp-wind-salt-current");
    }

    @Ignore
    @Test
    public void testGenerateGbr4() throws Exception {
        this.insertData();
        this.insertInputData_realData_hydro_gbr1();
        this.insertInputData_realData_hydro_gbr4();

        NcAnimate ncAnimate = this.getNcanimate();
        ncAnimate.generateFromProductId("gbr4_v2_temp-wind-salt-current");
    }

    @Ignore
    @Test
    public void testGenerateGbr4Future() throws Exception {
        this.insertData();
        this.insertInputData_realData_hydro_gbr4();

        NcAnimate ncAnimate = this.getNcanimate();
        ncAnimate.generateFromProductId("gbr4_v2_temp-wind-salt-current_future");
    }

    @Ignore
    @Test
    public void testGenerateGbr4InBetween() throws Exception {
        this.insertData();
        this.insertOldInputData_realData_hydro_gbr4();
        this.insertInputData_realData_hydro_gbr4();

        NcAnimate ncAnimate = this.getNcanimate();
        ncAnimate.generateFromProductId("gbr4_v2_temp-wind-salt-current_between");
    }

    @Ignore
    @Test
    public void testGenerateCombine() throws Exception {
        this.insertData();
        this.insertInputData_realData_hydro_gbr1();
        this.insertInputData_realData_hydro_gbr4();

        NcAnimate ncAnimate = this.getNcanimate();
        ncAnimate.setRegionId("brisbane");
        ncAnimate.generateFromProductId("gbr4_gbr1_combined_temp-wind-salt-current");
    }

    @Ignore
    @Test
    public void testGenerateNOAA() throws Exception {
        this.insertData();
        this.insertInputData_realData_hydro_gbr4();
        this.insertInputData_realData_noaa();
        // Add unrelated data to be sure they don't affect the result
        this.insertInputData_realData_hydro_gbr1();

        NcAnimate ncAnimate = this.getNcanimate();
        ncAnimate.generateFromProductId("noaa_wave");
    }

    @Ignore
    @Test
    public void testGenerateOnlyNOAA() throws Exception {
        this.insertData();
        this.insertInputData_realData_noaa();

        NcAnimate ncAnimate = this.getNcanimate();
        ncAnimate.generateFromProductId("only_noaa_wave");
    }

    @Ignore
    @Test
    public void testGenerateAllCurrentMultiDepth() throws Exception {
        this.insertData();
        this.insertInputData_realData_hydro_gbr4();
        this.insertInputData_realData_hydro_gbr4_ncaggregate();

        NcAnimate ncAnimate = this.getNcanimate();

        // Uncomment the product of interest.
        // You can uncomment multiple.

        ncAnimate.generateFromProductId("products__ncanimate__ereefs__gbr4_v2__current-multi-depth_shallow_hourly");
        //ncAnimate.generateFromProductId("products__ncanimate__ereefs__gbr4_v2__current-multi-depth_shallow_daily");
        //ncAnimate.generateFromProductId("products__ncanimate__ereefs__gbr4_v2__current-multi-depth_shallow_monthly");
        //ncAnimate.generateFromProductId("products__ncanimate__ereefs__gbr4_v2__current-multi-depth_shallow_annual");
        //ncAnimate.generateFromProductId("products__ncanimate__ereefs__gbr4_v2__current-multi-depth_shallow_all");

        //ncAnimate.generateFromProductId("products__ncanimate__ereefs__gbr4_v2__current-multi-depth_deep_hourly");
        //ncAnimate.generateFromProductId("products__ncanimate__ereefs__gbr4_v2__current-multi-depth_deep_daily");
        //ncAnimate.generateFromProductId("products__ncanimate__ereefs__gbr4_v2__current-multi-depth_deep_monthly");
        //ncAnimate.generateFromProductId("products__ncanimate__ereefs__gbr4_v2__current-multi-depth_deep_annual");
        //ncAnimate.generateFromProductId("products__ncanimate__ereefs__gbr4_v2__current-multi-depth_deep_all");
    }



    public void insertOldInputData_realData_hydro_gbr4() throws Exception {
        String definitionId = "downloads/gbr4_v2";

        File netCDFFile_2012_10 = new File("/home/glafond/Desktop/TMP_INPUT/netcdf/ereefs/gbr4_simple_2012-10.nc");
        {
            String datasetId = "gbr4_simple_2012-10.nc";
            URI fileURI = netCDFFile_2012_10.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(definitionId, datasetId, fileURI, netCDFFile_2012_10, netCDFFile_2012_10.lastModified(), WITH_MD5SUM);

            MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
            metadataManager.save(metadata.toJSON());
        }
    }

    public void insertInputData_realData_hydro_gbr4() throws Exception {
        String definitionId = "downloads/gbr4_v2";

        File netCDFFile_2014_12 = new File("/home/glafond/Desktop/TMP_INPUT/netcdf/ereefs/gbr4_simple_2014-12.nc");
        {
            String datasetId = "gbr4_simple_2014-12.nc";
            URI fileURI = netCDFFile_2014_12.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(definitionId, datasetId, fileURI, netCDFFile_2014_12, netCDFFile_2014_12.lastModified(), WITH_MD5SUM);

            MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
            metadataManager.save(metadata.toJSON());
        }
    }

    public void insertInputData_realData_hydro_gbr4_ncaggregate() throws Exception {
        String dailyDefinitionId = "products__ncaggregate__ereefs__gbr4_v2__ongoing__daily-monthly";
        String monthlyDefinitionId = "products__ncaggregate__ereefs__gbr4_v2__ongoing__monthly-monthly";
        String annualDefinitionId = "products__ncaggregate__ereefs__gbr4_v2__ongoing__annual-annual";
        String allDefinitionId = "products__ncaggregate__ereefs__gbr4_v2__ongoing__all-one";

        File allNetCDFFile = new File("/home/glafond/Desktop/TMP_INPUT/netcdf/ereefs/ncaggregate/gbr4_v2/gbr4_v2-all-one.nc");
        {
            String datasetId = "gbr4_v2-all-one.nc";
            URI fileURI = allNetCDFFile.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(allDefinitionId, datasetId, fileURI, allNetCDFFile, allNetCDFFile.lastModified(), WITH_MD5SUM);

            MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
            metadataManager.save(metadata.toJSON());
        }
    }

    public void insertInputData_realData_hydro_gbr1() throws Exception {
        String definitionId = "downloads/gbr1_2-0";

        File netCDFFile_2014_12_01 = new File("/home/glafond/Desktop/TMP_INPUT/netcdf/ereefs/gbr1_simple_2014-12-01.nc");
        {
            String datasetId = "gbr1_simple_2014-12-01.nc";
            URI fileURI = netCDFFile_2014_12_01.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(definitionId, datasetId, fileURI, netCDFFile_2014_12_01, netCDFFile_2014_12_01.lastModified(), WITH_MD5SUM);

            MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
            metadataManager.save(metadata.toJSON());
        }

        File netCDFFile_2014_12_02 = new File("/home/glafond/Desktop/TMP_INPUT/netcdf/ereefs/gbr1_simple_2014-12-02.nc");
        {
            String datasetId = "gbr1_simple_2014-12-02.nc";
            URI fileURI = netCDFFile_2014_12_02.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(definitionId, datasetId, fileURI, netCDFFile_2014_12_02, netCDFFile_2014_12_02.lastModified(), WITH_MD5SUM);

            MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
            metadataManager.save(metadata.toJSON());
        }

        File netCDFFile_2014_12_03 = new File("/home/glafond/Desktop/TMP_INPUT/netcdf/ereefs/gbr1_simple_2014-12-03.nc");
        {
            String datasetId = "gbr1_simple_2014-12-03.nc";
            URI fileURI = netCDFFile_2014_12_03.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(definitionId, datasetId, fileURI, netCDFFile_2014_12_03, netCDFFile_2014_12_03.lastModified(), WITH_MD5SUM);

            MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
            metadataManager.save(metadata.toJSON());
        }

        File netCDFFile_2017_07_21 = new File("/home/glafond/Desktop/TMP_INPUT/netcdf/ereefs/gbr1_simple_2017-07-21.nc");
        {
            String datasetId = "gbr1_simple_2017-07-21.nc";
            URI fileURI = netCDFFile_2017_07_21.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(definitionId, datasetId, fileURI, netCDFFile_2017_07_21, netCDFFile_2017_07_21.lastModified(), WITH_MD5SUM);

            MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
            metadataManager.save(metadata.toJSON());
        }
    }

    public void insertInputData_realData_bgc() throws Exception {
        String definitionId = "downloads/gbr4_bgc_924";

        // http://dapds00.nci.org.au/thredds/fileServer/fx3/gbr4_bgc_924/gbr4_bgc_simple_2014-12.nc
        File netCDFFile_2014_12 = new File("/home/glafond/Desktop/TMP_INPUT/netcdf/ereefs/gbr4_bgc_simple_2014-12.nc");
        {
            String datasetId = "gbr4_bgc_simple_2014-12.nc";
            URI fileURI = netCDFFile_2014_12.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(definitionId, datasetId, fileURI, netCDFFile_2014_12, netCDFFile_2014_12.lastModified(), WITH_MD5SUM);

            MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
            metadataManager.save(metadata.toJSON());
        }

        // http://dapds00.nci.org.au/thredds/fileServer/fx3/gbr4_bgc_924/gbr4_bgc_simple_2017-08.nc
        File netCDFFile_2017_08 = new File("/home/glafond/Desktop/TMP_INPUT/netcdf/ereefs/gbr4_bgc_simple_2017-08.nc");
        {
            String datasetId = "gbr4_bgc_simple_2017-08.nc";
            URI fileURI = netCDFFile_2017_08.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(definitionId, datasetId, fileURI, netCDFFile_2017_08, netCDFFile_2017_08.lastModified(), WITH_MD5SUM);

            MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
            metadataManager.save(metadata.toJSON());
        }
    }

    public void insertInputData_realData_noaa() throws Exception {
        File gribsFile_waveDir_2014_12 = new File("/home/glafond/Desktop/TMP_INPUT/netcdf/noaa/multi_1.glo_30m.dp.201412.grb2");
        {
            String definitionId = "downloads/noaa_wave-dir";
            String datasetId = "multi_1.glo_30m.dp.201412.grb2";
            URI fileURI = gribsFile_waveDir_2014_12.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(definitionId, datasetId, fileURI, gribsFile_waveDir_2014_12, gribsFile_waveDir_2014_12.lastModified(), WITH_MD5SUM);

            MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
            metadataManager.save(metadata.toJSON());
        }
        File gribsFile_waveDir_2018_11 = new File("/home/glafond/Desktop/TMP_INPUT/netcdf/noaa/multi_1.glo_30m.dp.201811.grb2");
        {
            String definitionId = "downloads/noaa_wave-dir";
            String datasetId = "multi_1.glo_30m.dp.201811.grb2";
            URI fileURI = gribsFile_waveDir_2018_11.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(definitionId, datasetId, fileURI, gribsFile_waveDir_2018_11, gribsFile_waveDir_2018_11.lastModified(), WITH_MD5SUM);

            MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
            metadataManager.save(metadata.toJSON());
        }

        File gribsFile_wavePeriod_2014_12 = new File("/home/glafond/Desktop/TMP_INPUT/netcdf/noaa/multi_1.glo_30m.tp.201412.grb2");
        {
            String definitionId = "downloads/noaa_wave-period";
            String datasetId = "multi_1.glo_30m.tp.201412.grb2";
            URI fileURI = gribsFile_wavePeriod_2014_12.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(definitionId, datasetId, fileURI, gribsFile_wavePeriod_2014_12, gribsFile_wavePeriod_2014_12.lastModified(), WITH_MD5SUM);

            MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
            metadataManager.save(metadata.toJSON());
        }
        File gribsFile_wavePeriod_2018_11 = new File("/home/glafond/Desktop/TMP_INPUT/netcdf/noaa/multi_1.glo_30m.tp.201811.grb2");
        {
            String definitionId = "downloads/noaa_wave-period";
            String datasetId = "multi_1.glo_30m.tp.201811.grb2";
            URI fileURI = gribsFile_wavePeriod_2018_11.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(definitionId, datasetId, fileURI, gribsFile_wavePeriod_2018_11, gribsFile_wavePeriod_2018_11.lastModified(), WITH_MD5SUM);

            MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
            metadataManager.save(metadata.toJSON());
        }

        File gribsFile_waveHeight_2014_12 = new File("/home/glafond/Desktop/TMP_INPUT/netcdf/noaa/multi_1.glo_30m.hs.201412.grb2");
        {
            String definitionId = "downloads/noaa_wave-height";
            String datasetId = "multi_1.glo_30m.hs.201412.grb2";
            URI fileURI = gribsFile_waveHeight_2014_12.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(definitionId, datasetId, fileURI, gribsFile_waveHeight_2014_12, gribsFile_waveHeight_2014_12.lastModified(), WITH_MD5SUM);

            MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
            metadataManager.save(metadata.toJSON());
        }
        File gribsFile_waveHeight_2018_11 = new File("/home/glafond/Desktop/TMP_INPUT/netcdf/noaa/multi_1.glo_30m.hs.201811.grb2");
        {
            String definitionId = "downloads/noaa_wave-height";
            String datasetId = "multi_1.glo_30m.hs.201811.grb2";
            URI fileURI = gribsFile_waveHeight_2018_11.toURI();

            NetCDFMetadataBean metadata = NetCDFMetadataBean.create(definitionId, datasetId, fileURI, gribsFile_waveHeight_2018_11, gribsFile_waveHeight_2018_11.lastModified(), WITH_MD5SUM);

            MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
            metadataManager.save(metadata.toJSON());
        }
    }
}
