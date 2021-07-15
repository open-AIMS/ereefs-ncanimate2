/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate;

import au.gov.aims.ereefs.Utils;
import au.gov.aims.ereefs.database.CacheStrategy;
import au.gov.aims.ereefs.database.manager.ncanimate.ConfigManager;
import au.gov.aims.ereefs.database.manager.ncanimate.ConfigPartManager;
import au.gov.aims.ereefs.helper.TestHelper;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * - NcAnimate manual tests -
 * Those tests are run manually to check NcAnimate behaviour
 * or to see the content of a given NetCDF file.
 */
public class NcAnimateTestLive extends DatabaseTestBase {
    private static final Logger LOGGER = Logger.getLogger(DatabaseTestBase.class);

    @Test
    @Ignore
    public void testGenerate_gbr4_hydro() throws Exception {
        this.insertLiveData();

        this.insertInputData_liveData_hydro_gbr4();

        NcAnimate ncAnimate = this.getNcanimate();
        ncAnimate.setRegionId("queensland-1");
        ncAnimate.generateFromProductId("products__ncanimate__ereefs__gbr4_v2__temp-wind-salt-current_hourly");
    }

    @Test
    @Ignore
    public void testGenerate_mixed_imosVsEreefsTemperature() throws Exception {
        this.insertLiveData();

        this.insertInputData_liveData_hydro_gbr4_daily_aggregate();
        this.insertInputData_liveData_imos_sst();
        this.insertInputData_liveData_imos_oceanCurrent();

        NcAnimate ncAnimate = this.getNcanimate();
        ncAnimate.generateFromProductId("products__ncanimate__mixed__imos-vs-ereefs-temperature");
    }

    @Test
    @Ignore
    public void testGenerate_gbr1_heatstress_dhw_daily() throws Exception {
        this.insertLiveData();

        this.insertInputData_liveData_historic_heatstress_gbr1_ncAggregate_daily();
        this.insertInputData_liveData_nrt_heatstress_gbr1_ncAggregate_daily();

        this.insertInputData_liveData_hydro_gbr1_daily();

        NcAnimate ncAnimate = this.getNcanimate();
        ncAnimate.setRegionId("queensland-1");
        ncAnimate.generateFromProductId("products__ncanimate__ereefs__gbr1_2-0__dhw_heatstress_daily");
    }



    public void insertLiveData() throws Exception {
        String protocol = "file://";
        File tmpDir = new File("/tmp/ncanimate");
        File privateBucket = new File(tmpDir, "private");
        File publicBucket = new File(tmpDir, "public");

        File layerDir = new File(privateBucket, "ncanimate/resources/layers");
        File styleDir = new File(privateBucket, "ncanimate/resources/styles");
        File paletteDir = new File(privateBucket, "ncanimate/resources/palettes");

        Utils.deleteDirectory(tmpDir);

        Map<String, String> substitutions = new HashMap<String, String>();
        substitutions.put("${STORAGE_PROTOCOL}", protocol);
        substitutions.put("${PRIVATE_BUCKET_NAME}", privateBucket.getAbsolutePath());
        substitutions.put("${PUBLIC_BUCKET_NAME}", publicBucket.getAbsolutePath());

        this.insertLiveConfigParts(substitutions);
        this.insertLiveConfigs(substitutions);

        this.copyLiveLayerFiles(layerDir);
        this.copyLiveStyleFiles(styleDir);
        this.copyLivePaletteFiles(paletteDir);
    }

    private void insertLiveConfigParts(Map<String, String> substitutions) throws Exception {
        ConfigPartManager configPartManager = new ConfigPartManager(this.getDatabaseClient(), CacheStrategy.DISK);
        TestHelper.insertTestConfigs(configPartManager, "liveConfig/data/definitions/ncanimateConfigParts", "NcAnimate config part", substitutions, true);
    }

    private void insertLiveConfigs(Map<String, String> substitutions) throws Exception {
        ConfigManager configManager = new ConfigManager(this.getDatabaseClient(), CacheStrategy.DISK);
        TestHelper.insertTestConfigs(configManager, "liveConfig/data/definitions/ncanimateConfigs", "NcAnimate configuration", substitutions, true);
    }

    private void copyLiveLayerFiles(File layerDir) throws IOException {
        DatabaseTestBase.copyDir("liveConfig/resources/ncanimate/layers", layerDir);
    }

    private void copyLiveStyleFiles(File styleDir) throws IOException {
        DatabaseTestBase.copyDir("liveConfig/resources/ncanimate/styles", styleDir);
    }

    private void copyLivePaletteFiles(File paletteDir) throws IOException {
        DatabaseTestBase.copyDir("liveConfig/resources/ncanimate/palettes", paletteDir);
    }



    public void insertInputData_liveData_hydro_gbr4() throws Exception {
        NcAnimateTestUtils.insertInputDataFromDirectory(
            this.getDatabaseClient(),
            "products__ncaggregate__ereefs__gbr4_v2__raw",
            new File("/home/glafond/Desktop/TMP_INPUT/netcdf/ereefs/gbr4_v2/hydro/hourly"));
    }

    public void insertInputData_liveData_hydro_gbr4_daily_aggregate() throws Exception {
        NcAnimateTestUtils.insertInputDataFromDirectory(
            this.getDatabaseClient(),
            "products__ncaggregate__ereefs__gbr4_v2__daily-monthly",
            new File("/home/glafond/Desktop/TMP_INPUT/netcdf/ereefs/gbr4_v2/hydro/daily"));
    }

    public void insertInputData_liveData_hydro_gbr1_daily() throws Exception {
        NcAnimateTestUtils.insertInputDataFromDirectory(
                this.getDatabaseClient(),
                "products__ncaggregate__ereefs__gbr1_2-0__daily-daily",
                new File("/home/glafond/Desktop/TMP_INPUT/netcdf/ereefs/gbr1/hydro/daily"));
    }

    public void insertInputData_liveData_imos_sst() throws Exception {
        NcAnimateTestUtils.insertInputDataFromDirectory(
            this.getDatabaseClient(),
            "downloads__imos__SRS_SST_ghrsst_L3S_6d_ngt",
            new File("/home/glafond/Desktop/TMP_INPUT/netcdf/imos/SRS_SST_ghrsst_L3S_6d_ngt"));
    }

    public void insertInputData_liveData_imos_oceanCurrent() throws Exception {
        NcAnimateTestUtils.insertInputDataFromDirectory(
            this.getDatabaseClient(),
            "downloads__imos__OceanCurrent_GSLA_NRT00",
            new File("/home/glafond/Desktop/TMP_INPUT/netcdf/imos/OceanCurrent_GSLA_NRT00"));
    }

    public void insertInputData_liveData_historic_heatstress_gbr1_ncAggregate_daily() throws Exception {
        NcAnimateTestUtils.insertInputDataFromDirectory(
                this.getDatabaseClient(),
                "products__ncaggregate__ereefs__gbr1_2-0-historic_heat_stress-daily-monthly",
                new File("/home/glafond/Desktop/TMP_INPUT/netcdf/ereefs/gbr1/heat-stress/historic/daily"));
    }
    public void insertInputData_liveData_nrt_heatstress_gbr1_ncAggregate_daily() throws Exception {
        NcAnimateTestUtils.insertInputDataFromDirectory(
                this.getDatabaseClient(),
                "products__ncaggregate__ereefs__gbr1_2-0-nrt_heat_stress-daily-daily",
                new File("/home/glafond/Desktop/TMP_INPUT/netcdf/ereefs/gbr1/heat-stress/nrt/daily"));
    }

}
