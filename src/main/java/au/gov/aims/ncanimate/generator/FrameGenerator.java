/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate.generator;

import au.gov.aims.ereefs.Utils;
import au.gov.aims.ereefs.bean.ncanimate.NcAnimateConfigBean;
import au.gov.aims.ereefs.bean.ncanimate.render.NcAnimateRenderBean;
import au.gov.aims.ncanimate.SystemCallThread;
import au.gov.aims.ncanimate.commons.NcAnimateUtils;
import au.gov.aims.ncanimate.commons.generator.context.GeneratorContext;
import au.gov.aims.ncanimate.commons.timetable.DateTimeRange;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;

public class FrameGenerator {
    private static final Logger LOGGER = Logger.getLogger(FrameGenerator.class);

    private static final String NCANIMATE_REGION_ENV_VARIABLE = "NCANIMATE_REGION";
    private static final String NCANIMATE_DATABASE_SERVER_ADDRESS_ENV_VARIABLE = "DATABASE_SERVER_ADDRESS";
    private static final String NCANIMATE_DATABASE_SERVER_PORT_ENV_VARIABLE = "DATABASE_SERVER_PORT";
    private static final String NCANIMATE_DATABASE_NAME_ENV_VARIABLE = "DATABASE_NAME";

    protected static final Pattern NCANIMATE_FRAME_JAR_PATTERN = Pattern.compile("ereefs-ncanimate2-frame.*-jar-with-dependencies\\.jar");

    private String customDatabaseServerAddress, customDatabaseName;
    private int customDatabaseServerPort;

    private String regionId;

    public FrameGenerator() {
        this.customDatabaseServerAddress = null;
        this.customDatabaseServerPort = -1;
        this.customDatabaseName = null;
        this.regionId = null;
    }

    /**
     * Set a custom service end point, for MongoDB.
     * Used with unit tests.
     */
    public void setCustomDatabaseServerAddress(String customDatabaseServerAddress, int customDatabaseServerPort) {
        this.customDatabaseServerAddress = customDatabaseServerAddress;
        this.customDatabaseServerPort = customDatabaseServerPort;
    }

    /**
     * Set custom database name, for MongoDB.
     * Used with unit tests.
     */
    public void setCustomDatabaseName(String customDatabaseName) {
        this.customDatabaseName = customDatabaseName;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    /**
     * Run ncanimate-frame in a loop
     * until it manage to go through without crashing
     * or it crashes without generating any new frame.
     */
    public void generateFrames(
            NcAnimateConfigBean ncanimateConfig,
            DateTimeRange dateRange) throws Exception {

        GeneratorContext context = new GeneratorContext(ncanimateConfig);
        File frameDirectory = context.getFrameDirectory();

        int currentNbFrame = FrameGenerator.countFiles(frameDirectory),
            lastNbFrame, nbNewFrame;
        Exception lastEx;
        do {
            lastNbFrame = currentNbFrame;

            lastEx = null;
            try {
                this.callNcAnimateFrame(ncanimateConfig, dateRange);
            } catch(Exception ex) {
                lastEx = ex;
            }
            currentNbFrame = FrameGenerator.countFiles(frameDirectory);
            nbNewFrame = currentNbFrame - lastNbFrame;

            if (lastEx != null) {
                LOGGER.warn(String.format("Exception occurred while generating frame. %d frames has been generated (%d new frame)",
                        currentNbFrame,
                        nbNewFrame), lastEx);
            }
        } while (lastEx != null && nbNewFrame > 0);

        // ncanimate-frame crashes without generating any new frame.
        // There is no point trying anymore than this.
        if (lastEx != null) {
            throw lastEx;
        }
    }

    private static int countFiles(File dir) {
        if (dir == null) {
            return 0;
        }
        File[] fileList = dir.listFiles();
        if (fileList == null || fileList.length == 0) {
            return 0;
        }

        int count = 0;
        for (File file : fileList) {
            if (file.isDirectory()) {
                count += FrameGenerator.countFiles(file);
            } else {
                count++;
            }
        }

        return count;
    }

    /**
     * Call the external ereefs-ncanimate2-frame jar to generate the required frame files
     * @param ncanimateConfig
     * @param dateRange
     * @throws InterruptedException
     */
    private void callNcAnimateFrame(NcAnimateConfigBean ncanimateConfig, DateTimeRange dateRange) throws Exception {
        String productId = ncanimateConfig.getId().getValue();
        LOGGER.debug(String.format("Calling ereefs-ncanimate-frame %s %s %s", productId, dateRange.getStartDate(), dateRange.getEndDate()));

        // Get the path to the NcAnimate frame jar file (ereefs-ncanimate2-frame-X.X-jar-with-dependencies.jar)
        File ncanimateFrameJarFile = this.getNcanimateFrameJarFile(ncanimateConfig);
        if (ncanimateFrameJarFile == null || !ncanimateFrameJarFile.exists()) {
            throw new IllegalArgumentException(String.format("NcAnimate frame jar file could not be found. " +
                "Add the property \"ncanimateFrameJar\" to the render section of the NcAnimate configuration: %s",
                productId));
        }

        String commandLine = String.format("java -XX:MaxRAMPercentage=80.0 -jar \"%s\" \"%s\" \"%s\" \"%s\"",
                ncanimateFrameJarFile.getAbsolutePath(),
                productId, dateRange.getStartDate(), dateRange.getEndDate());

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


        if (this.customDatabaseServerAddress != null) {
            systemCall.setEnvironmentVariable(NCANIMATE_DATABASE_SERVER_ADDRESS_ENV_VARIABLE, this.customDatabaseServerAddress);
        }
        if (this.customDatabaseServerPort > 0) {
            systemCall.setEnvironmentVariable(NCANIMATE_DATABASE_SERVER_PORT_ENV_VARIABLE, "" + this.customDatabaseServerPort);
        }
        if (this.customDatabaseName != null) {
            systemCall.setEnvironmentVariable(NCANIMATE_DATABASE_NAME_ENV_VARIABLE, this.customDatabaseName);
        }
        if (this.regionId != null) {
            systemCall.setEnvironmentVariable(NCANIMATE_REGION_ENV_VARIABLE, this.regionId);
        }

        systemCall.start();
        systemCall.join();

        Integer exitCode = systemCall.getExitCode();
        if (exitCode != null && exitCode != 0) {
            throw new IOException(String.format("Exception occurred while generating frames. Exit code: %d", exitCode));
        }
    }

    /**
     * Get the path to the NcAnimate frame jar file (ereefs-ncanimate2-frame-X.X-jar-with-dependencies.jar)
     * If the attribute is missing or represent a non existent file, NcAnimate will look in the same directory as NcAnimate jar file.
     * If the path represent a directory, NcAnimate will try to find the most appropriate file in the directory (ereefs-ncanimate2-frame*-jar-with-dependencies.jar).
     * @param ncAnimateConfig
     * @return
     */
    public File getNcanimateFrameJarFile(NcAnimateConfigBean ncAnimateConfig) {
        if (ncAnimateConfig != null) {
            NcAnimateRenderBean render = ncAnimateConfig.getRender();
            if (render != null) {
                String ncanimateFrameJar = render.getNcanimateFrameJar();
                if (ncanimateFrameJar != null && !ncanimateFrameJar.isEmpty()) {
                    GeneratorContext context = new GeneratorContext(ncAnimateConfig);
                    String parsedNcanimateFrameJar = NcAnimateUtils.parseString(ncanimateFrameJar, context);
                    File ncanimateFrameJarFile = new File(parsedNcanimateFrameJar).getAbsoluteFile();
                    if (ncanimateFrameJarFile.exists()) {
                        if (ncanimateFrameJarFile.isDirectory()) {
                            return this.findNcanimateFrameJarFile(ncanimateFrameJarFile);
                        } else {
                            return ncanimateFrameJarFile;
                        }
                    } else {
                        LOGGER.warn(String.format("The ncanimateFrameJar doesn't exist: %s", ncanimateFrameJarFile));
                    }
                }
            }
        }

        // Property ncanimateFrameJar is not defined or the file doesn't exist

        // Find the directory where the ncanimate jar is
        File ncanimateDirectory = Utils.getJarDirectory();
        return findNcanimateFrameJarFile(ncanimateDirectory);
    }

    private File findNcanimateFrameJarFile(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return null;
        }

        LOGGER.info(String.format("Looking for NcAnimate frame jar in directory: %s", directory));

        File[] foundJarFiles = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return NCANIMATE_FRAME_JAR_PATTERN.matcher(filename).matches();
            }
        });

        if (foundJarFiles != null && foundJarFiles.length > 0) {
            return foundJarFiles[0];
        }

        return null;
    }

}
