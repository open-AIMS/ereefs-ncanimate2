/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate;

import au.gov.aims.ereefs.Utils;
import au.gov.aims.ereefs.bean.metadata.netcdf.NetCDFMetadataBean;
import au.gov.aims.ereefs.database.CacheStrategy;
import au.gov.aims.ereefs.database.DatabaseClient;
import au.gov.aims.ereefs.database.manager.MetadataManager;
import au.gov.aims.ereefs.database.manager.ncanimate.ConfigManager;
import au.gov.aims.ereefs.database.manager.ncanimate.ConfigPartManager;
import au.gov.aims.ereefs.database.table.DatabaseTable;
import au.gov.aims.ereefs.helper.NcAnimateConfigHelper;
import au.gov.aims.ereefs.helper.TestHelper;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;

public class DatabaseTestBase {
    private static final Logger LOGGER = Logger.getLogger(DatabaseTestBase.class);
    private static final String DATABASE_NAME = "testdb";

    private static final boolean CHECKSUM = false;

    private MongoServer server;
    private DatabaseClient databaseClient;
    private NcAnimate ncanimate;

    protected long beforeSaveData;
    protected long afterSaveData;

    public DatabaseClient getDatabaseClient() {
        return this.databaseClient;
    }

    @Before
    public void init() throws Exception {
        File dbCacheDir = DatabaseTable.getDatabaseCacheDirectory();
        Utils.deleteDirectory(dbCacheDir);

        this.server = new MongoServer(new MemoryBackend());
        InetSocketAddress serverAddress = this.server.bind();

        this.databaseClient = new DatabaseClient(new ServerAddress(serverAddress), DATABASE_NAME);
        this.createTables();

        this.ncanimate = new NcAnimate(this.getDatabaseClient(), null);

        this.ncanimate.setCustomDatabaseServerAddress(serverAddress.getHostString(), serverAddress.getPort());
        this.ncanimate.setCustomDatabaseName(DATABASE_NAME);
    }

    @After
    public void shutdown() {
        NcAnimateConfigHelper.clearMetadataCache();
        if (this.server != null) {
            this.server.shutdown();
        }
    }

    private void createTables() throws Exception {
        TestHelper.createTables(this.databaseClient);
    }



    public NcAnimate getNcanimate() {
        return this.ncanimate;
    }

    public void insertData() throws Exception {
        this.beforeSaveData = new Date().getTime();

        Utils.deleteDirectory(new File("/tmp/ncanimateTests"));

        this.insertTestConfigParts();
        this.insertTestConfigs();

        this.copyLayerFiles();
        this.copyStyleFiles();
        this.copyPaletteFiles();

        this.afterSaveData = new Date().getTime();
    }

    public void insertFakePartialGBR4NetCDFFile() throws Exception {
        this.insertFakePartialGBR4NetCDFFile(CHECKSUM);
    }

    public void insertFakePartialGBR4NetCDFFile(boolean checksum) throws Exception {
        URL netCDFFileUrl = DatabaseTestBase.class.getClassLoader().getResource("netcdf/gbr4_v2_2010-09-01_00h00-02h00.nc");
        File netCDFFileOrig = new File(netCDFFileUrl.getFile());
        File netCDFFileCopy = new File("/tmp/ncanimateTests/netcdfFiles/gbr4_v2_2010-09-01_00h00-02h00.nc");
        netCDFFileCopy.getParentFile().mkdirs();

        Files.copy(netCDFFileOrig.toPath(), netCDFFileCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);

        String definitionId = "downloads/gbr4_v2";
        String datasetId = "gbr4_v2_2010-09-01_00h00-02h00.nc";
        URI fileURI = netCDFFileCopy.toURI();

        NetCDFMetadataBean metadata = NetCDFMetadataBean.create(definitionId, datasetId, fileURI, netCDFFileCopy, netCDFFileCopy.lastModified(), checksum);

        MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
        metadataManager.save(metadata.toJSON());
    }

    private void insertTestConfigParts() throws Exception {
        ConfigPartManager configPartManager = new ConfigPartManager(this.getDatabaseClient(), CacheStrategy.DISK);

        TestHelper.insertTestConfigs(configPartManager, "ncanimate/configParts", "NcAnimate config part", null, true);
    }

    private void insertTestConfigs() throws Exception {
        ConfigManager configManager = new ConfigManager(this.getDatabaseClient(), CacheStrategy.DISK);

        TestHelper.insertTestConfigs(configManager, "ncanimate", "NcAnimate configuration");
        TestHelper.insertTestConfigs(configManager, "ncanimate/current-multi-depth", "NcAnimate configuration");
    }

    private void copyLayerFiles() throws IOException {
        DatabaseTestBase.copyDir("layers", new File("/tmp/ncanimateTests/s3/layers"));
    }

    private void copyStyleFiles() throws IOException {
        DatabaseTestBase.copyDir("styles", new File("/tmp/ncanimateTests/s3/styles"));
    }

    private void copyPaletteFiles() throws IOException {
        DatabaseTestBase.copyDir("colourPalettes", new File("/tmp/ncanimateTests/s3/palettes"));
    }

    protected static void copyDir(String resourcesSourceStr, File destination) throws IOException {
        URL sourceUrl = DatabaseTestBase.class.getClassLoader().getResource(resourcesSourceStr);
        if (sourceUrl == null) {
            throw new IOException(String.format("Missing resource directory: %s", resourcesSourceStr));
        }

        Utils.prepareDirectory(destination);

        File resourcesSourceDir = new File(sourceUrl.getFile());
        FileUtils.copyDirectory(resourcesSourceDir, destination);
    }
}
