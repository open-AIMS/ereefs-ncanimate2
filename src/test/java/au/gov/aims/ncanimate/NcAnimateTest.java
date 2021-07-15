/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate;

import au.gov.aims.ereefs.bean.metadata.netcdf.NetCDFMetadataBean;
import au.gov.aims.ereefs.database.CacheStrategy;
import au.gov.aims.ereefs.database.manager.MetadataManager;
import au.gov.aims.ereefs.database.table.JSONObjectIterable;
import au.gov.aims.ncanimate.generator.MetadataGenerator;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class NcAnimateTest extends DatabaseTestBase {
    private static final Logger LOGGER = Logger.getLogger(NcAnimateTest.class);

    @Test
    public void testGenerateTinyGBR4v2Videos() throws Exception {
        super.insertData();
        super.insertFakePartialGBR4NetCDFFile();

        String productId = "gbr4_v2_temp-wind-salt-current";

        File[] expectedGeneratedFiles = new File[] {
            // Maps for 2010-09-01 00h00 to 01h00
            //     Depths:  [-1.5, -49]
            //     Regions: [brisbane, qld, torres-strait]
            //     Formats: [gif, png, svg]
            //     = 2 depths * 3 regions * 3 formats = 18 files
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_brisbane_-1.5.gif"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_brisbane_-1.5.png"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_brisbane_-1.5.svg"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_brisbane_-49.0.gif"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_brisbane_-49.0.png"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_brisbane_-49.0.svg"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_qld_-1.5.gif"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_qld_-1.5.png"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_qld_-1.5.svg"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_qld_-49.0.gif"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_qld_-49.0.png"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_qld_-49.0.svg"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_torres-strait_-1.5.gif"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_torres-strait_-1.5.png"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_torres-strait_-1.5.svg"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_torres-strait_-49.0.gif"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_torres-strait_-49.0.png"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_torres-strait_-49.0.svg"),

            // Maps for 2010-09-01 01h00 to 02h00
            //     Depths:  [-1.5, -49]
            //     Regions: [brisbane, qld, torres-strait]
            //     Formats: [gif, png, svg]
            //     = 2 depths * 3 regions * 3 formats = 18 files
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_brisbane_-1.5.gif"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_brisbane_-1.5.png"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_brisbane_-1.5.svg"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_brisbane_-49.0.gif"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_brisbane_-49.0.png"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_brisbane_-49.0.svg"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_qld_-1.5.gif"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_qld_-1.5.png"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_qld_-1.5.svg"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_qld_-49.0.gif"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_qld_-49.0.png"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_qld_-49.0.svg"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_torres-strait_-1.5.gif"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_torres-strait_-1.5.png"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_torres-strait_-1.5.svg"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_torres-strait_-49.0.gif"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_torres-strait_-49.0.png"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_torres-strait_-49.0.svg"),

            // Videos for 2010 to 2011
            //     Depths:  [-1.5, -49]
            //     Regions: [brisbane, qld, torres-strait]
            //     Formats: [mp4, wmv]
            //     = 2 depths * 3 regions * 2 formats = 12 files
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_brisbane_-1.5.mp4"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_brisbane_-1.5.wmv"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_brisbane_-49.0.mp4"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_brisbane_-49.0.wmv"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_qld_-1.5.mp4"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_qld_-1.5.wmv"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_qld_-49.0.mp4"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_qld_-49.0.wmv"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_torres-strait_-1.5.mp4"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_torres-strait_-1.5.wmv"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_torres-strait_-49.0.mp4"),
            new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_torres-strait_-49.0.wmv")
        };


        // Ensure files doesn't exists before the generation (no cheating!)
        for (File expectedGeneratedFile : expectedGeneratedFiles) {
            Assert.assertFalse(String.format("The file exists before running the test: %s", expectedGeneratedFile), expectedGeneratedFile.exists());
        }

        NcAnimate ncAnimate = this.getNcanimate();

        LOGGER.info("################################ 1st generation ################################");
        ncAnimate.generateFromProductId(productId);

        // Verify if files have been generated
        for (File expectedGeneratedFile : expectedGeneratedFiles) {
            Assert.assertTrue(String.format("The file was not be generated: %s", expectedGeneratedFile), expectedGeneratedFile.exists());
        }


        // Validate metadata
        MetadataManager metadataManager = new MetadataManager(this.getDatabaseClient(), CacheStrategy.DISK);
        String definitionId = productId;
        JSONObjectIterable metadatas =
                metadataManager.selectByDefinitionId(MetadataManager.MetadataType.NCANIMATE_PRODUCT, definitionId);

        Assert.assertNotNull("No metadata found in the database", metadatas);

        int count = 0;
        for (JSONObject jsonMetadata : metadatas) {
            Assert.assertNotNull("The database returned null for a JSON record.", jsonMetadata);

            String metadataId = jsonMetadata.optString("_id", null);
            Assert.assertNotNull(String.format("The database returned for a metadata without an ID:%n%s", jsonMetadata.toString(4)), metadataId);

            switch (metadataId) {
                // Validate maps
                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_brisbane_-1_5":
                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_brisbane_-49_0":
                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_brisbane_-1_5":
                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_brisbane_-49_0":
                    this.validateMapMetadata(metadataId, "brisbane", jsonMetadata);
                    break;

                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_qld_-1_5":
                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_qld_-49_0":
                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_qld_-1_5":
                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_qld_-49_0":
                    this.validateMapMetadata(metadataId, "qld", jsonMetadata);
                    break;

                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_torres-strait_-1_5":
                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_00h00_torres-strait_-49_0":
                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_torres-strait_-1_5":
                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_map_hourly_2010-09-01_01h00_torres-strait_-49_0":
                    this.validateMapMetadata(metadataId, "torres-strait", jsonMetadata);
                    break;

                // Validate videos
                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_brisbane_-1_5":
                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_brisbane_-49_0":
                    this.validateVideoMetadata(metadataId, "brisbane", jsonMetadata);
                    break;

                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_qld_-1_5":
                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_qld_-49_0":
                    this.validateVideoMetadata(metadataId, "qld", jsonMetadata);
                    break;

                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_torres-strait_-1_5":
                case "gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_torres-strait_-49_0":
                    this.validateVideoMetadata(metadataId, "torres-strait", jsonMetadata);
                    break;

                default:
                    Assert.fail(String.format("Unexpected metadata ID returned by the database: %s", metadataId));
            }

            count++;
        }

        Assert.assertEquals("Wrong number of metadata found in the database", 18, count);


        // Regenerate. this should do nothing since the product is already up to date
        LOGGER.info("################################ 2nd generation ################################");

        // Wait 1s to make an obvious difference in file timestamp (if they are re-generated)
        Thread.sleep(1000);
        long beforeSecondGenerationTimestamp = System.currentTimeMillis();
        ncAnimate.generateFromProductId(productId);

        // Check that nothing was re-generated
        for (File expectedGeneratedFile : expectedGeneratedFiles) {
            Assert.assertTrue(String.format("A generated file could not be found after the 2nd generation: %s", expectedGeneratedFile), expectedGeneratedFile.exists());
            Assert.assertTrue(String.format("An up-to-date file was re-generated: %s", expectedGeneratedFile), expectedGeneratedFile.lastModified() < beforeSecondGenerationTimestamp);
        }


        // Regenerate after deleting a video. this should only regenerate the affected products
        LOGGER.info("################################ 3rd generation ################################");
        // Delete generated video
        File generatedVideo = new File("/tmp/ncanimateTests/s3/ncanimate/products/gbr4_v2_temp-wind-salt-current/gbr4_v2_temp-wind-salt-current_video_yearly_2010_qld_-1.5.mp4");
        Assert.assertTrue(String.format("The generated file %s doesn't exists", generatedVideo), generatedVideo.exists());
        Assert.assertTrue(String.format("Could not delete the generated file %s", generatedVideo), generatedVideo.delete());

        ncAnimate.generateFromProductId(productId);

        Assert.assertTrue(String.format("The generated file %s was not re-generated", generatedVideo), generatedVideo.exists());
    }

    private void validateMapMetadata(String metadataId, String regionId, JSONObject jsonMetadata) {
        this.validateCommonMetadata(metadataId, jsonMetadata);

        JSONObject jsonOutputFiles = jsonMetadata.optJSONObject("outputFiles");
        Assert.assertEquals(String.format("Metadata outputFiles count is wrong for map metadata ID %s", metadataId), 3, jsonOutputFiles.length());

        for (String outputFileId: jsonOutputFiles.keySet()) {
            JSONObject jsonOutputFile = jsonOutputFiles.optJSONObject(outputFileId);
            Assert.assertNotNull(String.format("JSON output file is null for output file ID %s for map metadata ID: %s", outputFileId, metadataId), jsonOutputFile);

            Assert.assertTrue(String.format("JSON output file ID %s has no width attribute for map metadata ID: %s", outputFileId, metadataId), jsonOutputFile.has("width"));
            Assert.assertTrue(String.format("JSON output file ID %s has no height attribute for map metadata ID: %s", outputFileId, metadataId), jsonOutputFile.has("height"));

            int width = jsonOutputFile.optInt("width", -1);
            int height = jsonOutputFile.optInt("height", -1);
            Assert.assertTrue(String.format("JSON output file ID %s width is out of bound for map metadata ID: %s", outputFileId, metadataId), width > 0 && width < 1000);
            Assert.assertTrue(String.format("JSON output file ID %s height is out of bound for map metadata ID: %s", outputFileId, metadataId), height > 0 && height < 1000);

            String fileURIStr = jsonOutputFile.optString("fileURI", null);
            Assert.assertNotNull(String.format("JSON output file ID %s has no fileURI for map metadata ID: %s", outputFileId, metadataId), fileURIStr);
            Assert.assertFalse(String.format("JSON output file ID %s has an empty fileURI for map metadata ID: %s", outputFileId, metadataId), fileURIStr.isEmpty());

            String type = jsonOutputFile.optString("type", null);
            Assert.assertNotNull(String.format("JSON output file ID %s has no type for map metadata ID: %s", outputFileId, metadataId), type);
            Assert.assertEquals(String.format("JSON output file ID %s has an unexpected type for map metadata ID: %s", outputFileId, metadataId), MetadataGenerator.OUTPUT_FILE_MAP_TYPE, type);

            int expectedHeight;
            switch (outputFileId) {
                case "pngMap":
                    if (regionId.equals("qld")) {
                        expectedHeight = 113;
                    } else if (regionId.equals("torres-strait")) {
                        expectedHeight = 115;
                    } else {
                        expectedHeight = 113;
                    }

                    Assert.assertEquals(String.format("JSON output file ID %s wrong width for PNG map metadata ID: %s", outputFileId, metadataId), 250, width);
                    Assert.assertEquals(String.format("JSON output file ID %s wrong height for PNG map metadata ID: %s", outputFileId, metadataId), expectedHeight, height);
                    Assert.assertTrue(String.format("JSON output file ID %s fileURI doesn't end with .png for PNG map metadata ID: %s", outputFileId, metadataId), fileURIStr.endsWith(".png"));
                    break;

                case "gifMap":
                    if (regionId.equals("qld")) {
                        expectedHeight = 427;
                    } else if (regionId.equals("torres-strait")) {
                        expectedHeight = 435;
                    } else {
                        expectedHeight = 427;
                    }

                    Assert.assertEquals(String.format("JSON output file ID %s wrong width for GIF map metadata ID: %s", outputFileId, metadataId), 944, width);
                    Assert.assertEquals(String.format("JSON output file ID %s wrong height for GIF map metadata ID: %s", outputFileId, metadataId), expectedHeight, height);
                    Assert.assertTrue(String.format("JSON output file ID %s fileURI doesn't end with .gif for GIF map metadata ID: %s", outputFileId, metadataId), fileURIStr.endsWith(".gif"));
                    break;

                case "svgMap":
                    if (regionId.equals("qld")) {
                        expectedHeight = 427;
                    } else if (regionId.equals("torres-strait")) {
                        expectedHeight = 435;
                    } else {
                        expectedHeight = 427;
                    }

                    Assert.assertEquals(String.format("JSON output file ID %s wrong width for SVG map metadata ID: %s", outputFileId, metadataId), 944, width);
                    Assert.assertEquals(String.format("JSON output file ID %s wrong height for SVG map metadata ID: %s", outputFileId, metadataId), expectedHeight, height);
                    Assert.assertTrue(String.format("JSON output file ID %s fileURI doesn't end with .svg for SVG map metadata ID: %s", outputFileId, metadataId), fileURIStr.endsWith(".svg"));
                    break;

                default:
                    Assert.fail(String.format("Unexpected output file ID %s found for map metadata ID: %s", outputFileId, metadataId));
            }
        }
    }

    private void validateVideoMetadata(String metadataId, String regionId, JSONObject jsonMetadata) {
        this.validateCommonMetadata(metadataId, jsonMetadata);

        String preview = jsonMetadata.optString("preview");
        Assert.assertNotNull(String.format("Metadata preview is null for video metadata ID %s", metadataId), preview);
        Assert.assertFalse(String.format("Metadata preview is empty for video metadata ID %s", metadataId), preview.isEmpty());

        JSONObject jsonOutputFiles = jsonMetadata.optJSONObject("outputFiles");
        Assert.assertEquals(String.format("Metadata outputFiles count is wrong for video metadata ID %s", metadataId), 3, jsonOutputFiles.length());

        for (String outputFileId: jsonOutputFiles.keySet()) {
            JSONObject jsonOutputFile = jsonOutputFiles.optJSONObject(outputFileId);
            Assert.assertNotNull(String.format("JSON output file is null for output file ID %s for video metadata ID: %s", outputFileId, metadataId), jsonOutputFile);

            Assert.assertTrue(String.format("JSON output file ID %s has no width attribute for video metadata ID: %s", outputFileId, metadataId), jsonOutputFile.has("width"));
            Assert.assertTrue(String.format("JSON output file ID %s has no height attribute for video metadata ID: %s", outputFileId, metadataId), jsonOutputFile.has("height"));

            int width = jsonOutputFile.optInt("width", -1);
            int height = jsonOutputFile.optInt("height", -1);
            Assert.assertTrue(String.format("JSON output file ID %s width is out of bound for video metadata ID: %s", outputFileId, metadataId), width > 0 && width < 1000);
            Assert.assertTrue(String.format("JSON output file ID %s height is out of bound for video metadata ID: %s", outputFileId, metadataId), height > 0 && height < 1000);

            String fileURIStr = jsonOutputFile.optString("fileURI", null);
            Assert.assertNotNull(String.format("JSON output file ID %s has no fileURI for video metadata ID: %s", outputFileId, metadataId), fileURIStr);
            Assert.assertFalse(String.format("JSON output file ID %s has an empty fileURI for video metadata ID: %s", outputFileId, metadataId), fileURIStr.isEmpty());

            String type = jsonOutputFile.optString("type", null);
            Assert.assertNotNull(String.format("JSON output file ID %s has no type for video metadata ID: %s", outputFileId, metadataId), type);
            Assert.assertEquals(String.format("JSON output file ID %s has an unexpected type for video metadata ID: %s", outputFileId, metadataId), MetadataGenerator.OUTPUT_FILE_VIDEO_TYPE, type);

            Integer fps = jsonOutputFile.has("fps") ? jsonOutputFile.optInt("fps", -1) : null;
            String filetype = jsonOutputFile.optString("filetype", null);

            int expectedHeight;
            switch (outputFileId) {
                case "mp4Video":
                    if (regionId.equals("qld")) {
                        expectedHeight = 432;
                    } else if (regionId.equals("torres-strait")) {
                        expectedHeight = 448;
                    } else {
                        expectedHeight = 432;
                    }

                    Assert.assertEquals(String.format("JSON output file ID %s wrong filetype for MP4 video metadata ID: %s", outputFileId, metadataId), "MP4", filetype);
                    Assert.assertEquals(String.format("JSON output file ID %s wrong width for MP4 video metadata ID: %s", outputFileId, metadataId), 944, width);
                    Assert.assertEquals(String.format("JSON output file ID %s wrong height for MP4 video metadata ID: %s", outputFileId, metadataId), expectedHeight, height);
                    Assert.assertNotNull(String.format("JSON output file ID %s has no fps attribute for video metadata ID: %s", outputFileId, metadataId), fps);
                    Assert.assertEquals(String.format("JSON output file ID %s wrong fps for MP4 video metadata ID: %s", outputFileId, metadataId), 12, fps.intValue());
                    Assert.assertTrue(String.format("JSON output file ID %s fileURI doesn't end with .mp4 for MP4 video metadata ID: %s", outputFileId, metadataId), fileURIStr.endsWith(".mp4"));
                    break;

                case "wmvVideo":
                    if (regionId.equals("qld")) {
                        expectedHeight = 290;
                    } else if (regionId.equals("torres-strait")) {
                        expectedHeight = 295;
                    } else {
                        expectedHeight = 289;
                    }

                    Assert.assertEquals(String.format("JSON output file ID %s wrong filetype for WMV video metadata ID: %s", outputFileId, metadataId), "WMV", filetype);
                    Assert.assertEquals(String.format("JSON output file ID %s wrong width for WMV video metadata ID: %s", outputFileId, metadataId), 640, width);
                    Assert.assertEquals(String.format("JSON output file ID %s wrong height for WMV video metadata ID: %s", outputFileId, metadataId), expectedHeight, height);
                    Assert.assertNotNull(String.format("JSON output file ID %s has no fps attribute for video metadata ID: %s", outputFileId, metadataId), fps);
                    Assert.assertEquals(String.format("JSON output file ID %s wrong fps for WMV video metadata ID: %s", outputFileId, metadataId), 10, fps.intValue());
                    Assert.assertTrue(String.format("JSON output file ID %s fileURI doesn't end with .wmv for WMV video metadata ID: %s", outputFileId, metadataId), fileURIStr.endsWith(".wmv"));
                    break;

                case "zipArchive":
                    if (regionId.equals("qld")) {
                        expectedHeight = 427;
                    } else if (regionId.equals("torres-strait")) {
                        expectedHeight = 435;
                    } else {
                        expectedHeight = 427;
                    }

                    Assert.assertEquals(String.format("JSON output file ID %s wrong filetype for ZIP video metadata ID: %s", outputFileId, metadataId), "ZIP", filetype);
                    Assert.assertEquals(String.format("JSON output file ID %s wrong width for ZIP archive metadata ID: %s", outputFileId, metadataId), 944, width);
                    Assert.assertEquals(String.format("JSON output file ID %s wrong height for ZIP archive metadata ID: %s", outputFileId, metadataId), expectedHeight, height);
                    Assert.assertNull(String.format("JSON output file ID %s has fps attribute for video metadata ID: %s", outputFileId, metadataId), fps);
                    Assert.assertTrue(String.format("JSON output file ID %s fileURI doesn't end with .zip for ZIP archive metadata ID: %s", outputFileId, metadataId), fileURIStr.endsWith(".zip"));
                    break;

                default:
                    Assert.fail(String.format("Unexpected output file ID %s found for video metadata ID: %s", outputFileId, metadataId));
            }
        }
    }

    private void validateCommonMetadata(String metadataId, JSONObject jsonMetadata) {
        // Check mandatory database elements
        String definitionId = jsonMetadata.optString("definitionId", null);
        Assert.assertNotNull(String.format("Metadata definitionId is null for metadata ID %s", metadataId), definitionId);
        Assert.assertEquals(String.format("Metadata definitionId is wrong for metadata ID %s", metadataId), "gbr4_v2_temp-wind-salt-current", definitionId);

        String datasetId = jsonMetadata.optString("datasetId", null);
        Assert.assertNotNull(String.format("Metadata datasetId is null for metadata ID %s", metadataId), datasetId);
        Assert.assertFalse(String.format("Metadata datasetId is empty for metadata ID %s", metadataId), datasetId.isEmpty());

        String id = jsonMetadata.optString("_id", null);
        Assert.assertNotNull(String.format("Metadata ID found in JSON object is null for metadata ID %s", metadataId), id);
        Assert.assertEquals(String.format("Metadata ID is different from the metadata ID used as key for the HashMap. for metadata ID %s", metadataId), metadataId, id);
        Assert.assertEquals(String.format("Metadata ID is different from the ID returned by NetCDFMetadataBean.getUniqueDatasetId() for metadata ID %s", metadataId),
                NetCDFMetadataBean.getUniqueDatasetId(definitionId, datasetId), id);

        String type = jsonMetadata.optString("type", null);
        Assert.assertNotNull(String.format("Metadata type is null for metadata ID %s", metadataId), type);
        Assert.assertEquals(String.format("Metadata type is wrong for metadata ID %s", metadataId), MetadataManager.MetadataType.NCANIMATE_PRODUCT.name(), type);


        // Check info about output files
        JSONObject jsonOutputFiles = jsonMetadata.optJSONObject("outputFiles");
        Assert.assertNotNull(String.format("Metadata outputFiles is null for metadata ID %s", metadataId), jsonOutputFiles);
        Assert.assertFalse(String.format("Metadata outputFiles is empty for metadata ID %s", metadataId), jsonOutputFiles.isEmpty());


        JSONObject jsonDateRange = jsonMetadata.optJSONObject("dateRange");
        Assert.assertNotNull(String.format("Metadata dateRange is null for metadata ID %s", metadataId), jsonDateRange);

        String startDate = jsonDateRange.optString("startDate", null);
        Assert.assertNotNull(String.format("Metadata dateRange.startDate is null for metadata ID %s", metadataId), startDate);
        Assert.assertFalse(String.format("Metadata dateRange.startDate is empty for metadata ID %s", metadataId), startDate.isEmpty());

        String endDate = jsonDateRange.optString("endDate", null);
        Assert.assertNotNull(String.format("Metadata dateRange.endDate is null for metadata ID %s", metadataId), endDate);
        Assert.assertFalse(String.format("Metadata dateRange.endDate is empty for metadata ID %s", metadataId), endDate.isEmpty());


        JSONObject jsonRegion = jsonMetadata.optJSONObject("region");
        Assert.assertNotNull(String.format("Metadata region is null for metadata ID %s", metadataId), jsonRegion);

        String regionId = jsonRegion.optString("id", null);
        Assert.assertNotNull(String.format("Metadata region.id is null for metadata ID %s", metadataId), regionId);
        Assert.assertFalse(String.format("Metadata region.id is empty for metadata ID %s", metadataId), regionId.isEmpty());

        String regionLabel = jsonRegion.optString("label", null);
        Assert.assertNotNull(String.format("Metadata region.label is null for metadata ID %s", metadataId), regionLabel);
        Assert.assertFalse(String.format("Metadata region.label is empty for metadata ID %s", metadataId), regionLabel.isEmpty());

        JSONObject jsonRegionBbox = jsonRegion.optJSONObject("bbox");
        Assert.assertNotNull(String.format("Metadata region.bbox is null for metadata ID %s", metadataId), jsonRegionBbox);
        Assert.assertFalse(String.format("Metadata region.bbox is empty for metadata ID %s", metadataId), jsonRegionBbox.isEmpty());

        Assert.assertNotEquals(String.format("Metadata region.bbox.north is null for metadata ID %s", metadataId), jsonRegionBbox.has("north"));
        Assert.assertNotEquals(String.format("Metadata region.bbox.south is null for metadata ID %s", metadataId), jsonRegionBbox.has("south"));
        Assert.assertNotEquals(String.format("Metadata region.bbox.east is null for metadata ID %s", metadataId), jsonRegionBbox.has("east"));
        Assert.assertNotEquals(String.format("Metadata region.bbox.west is null for metadata ID %s", metadataId), jsonRegionBbox.has("west"));

        double jsonRegionBboxNorth = jsonRegionBbox.optDouble("north", -1000);
        double jsonRegionBboxSouth = jsonRegionBbox.optDouble("south", -1000);
        double jsonRegionBboxEast = jsonRegionBbox.optDouble("east", -1000);
        double jsonRegionBboxWest = jsonRegionBbox.optDouble("west", -1000);
        Assert.assertEquals(String.format("Metadata region.bbox.north is out of bound for metadata ID %s", metadataId), 0, jsonRegionBboxNorth, 90);
        Assert.assertEquals(String.format("Metadata region.bbox.south is out of bound for metadata ID %s", metadataId), 0, jsonRegionBboxSouth, 90);
        Assert.assertEquals(String.format("Metadata region.bbox.east is out of bound for metadata ID %s", metadataId), 0, jsonRegionBboxEast, 180);
        Assert.assertEquals(String.format("Metadata region.bbox.west is out of bound for metadata ID %s", metadataId), 0, jsonRegionBboxWest, 180);


        // Check properties used with the user interface (Drupal module)
        JSONObject jsonProperties = jsonMetadata.optJSONObject("properties");
        Assert.assertNotNull(String.format("Metadata properties is null for metadata ID %s", metadataId), jsonProperties);
        Assert.assertFalse(String.format("Metadata properties is empty for metadata ID %s", metadataId), jsonProperties.isEmpty());
    }
}
