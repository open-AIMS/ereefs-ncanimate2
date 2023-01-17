/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate;

import au.gov.aims.ereefs.bean.AbstractBean;
import au.gov.aims.ereefs.database.CacheStrategy;
import au.gov.aims.ereefs.database.DatabaseClient;
import au.gov.aims.ereefs.database.manager.MetadataManager;
import au.gov.aims.ereefs.database.table.JSONObjectIterable;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * This class was intended to be used once only, to fix
 * erroneous metadata IDs previously saved in the Database.
 * It's very unlikely to be needed again, but I decided to
 * keep it as an example of what can be done to fix
 * complex issues with the database.
 */
public class NcAnimateMetadataIdFixer {
    private static final Logger LOGGER = Logger.getLogger(NcAnimateMetadataIdFixer.class);

    private static final boolean DRY_RUN = false;

    public static void fixMetadataIds(DatabaseClient dbClient, CacheStrategy cacheStrategy) throws Exception {
        // List all IDs
        MetadataManager metadataManager = new MetadataManager(dbClient, cacheStrategy);

        JSONObjectIterable metadatas = metadataManager.selectAll(MetadataManager.MetadataType.NCANIMATE_PRODUCT);

        for (JSONObject metadata : metadatas) {
            String origId = metadata.optString("_id", null);
            String fixedId = AbstractBean.safeIdValue(origId);

            if (!origId.equals(fixedId)) {
                boolean exists = metadataManager.exists(fixedId);
                if (!exists) {
                    // The metadata ID need fixing
                    metadata.put("_id", fixedId);
                    LOGGER.info(String.format("Updating metadata ID %s with new ID %s", origId, fixedId));
                    if (!DRY_RUN) {
                        metadataManager.save(metadata);
                    }
                } else {
                    LOGGER.info(String.format("Metadata ID %s already exists", fixedId));
                }
                if (!DRY_RUN) {
                    metadataManager.delete(origId);
                }
            }
        }
    }

    public static void fixDownloadMetadataIds(DatabaseClient dbClient, CacheStrategy cacheStrategy) throws Exception {
        // List all IDs
        MetadataManager metadataManager = new MetadataManager(dbClient, cacheStrategy);

        JSONObjectIterable metadatas = metadataManager.selectAll(MetadataManager.MetadataType.NETCDF);

        for (JSONObject metadata : metadatas) {
            String definitionId = metadata.optString("definitionId", null);

            // Only process metadata with definition ID starting with "downloads__".
            boolean isDownloadsMetadata = (definitionId != null && definitionId.startsWith("downloads__"));
            if (!isDownloadsMetadata) {
                continue;
            }

            String origId = metadata.optString("_id", null);
            String fixedId = AbstractBean.safeIdValue(origId);

            if (!origId.equals(fixedId)) {
                boolean exists = metadataManager.exists(fixedId);
                if (!exists) {
                    // The metadata ID need fixing
                    metadata.put("_id", fixedId);
                    LOGGER.info(String.format("Updating metadata ID %s with new ID %s", origId, fixedId));
                    if (!DRY_RUN) {
                        metadataManager.save(metadata);
                    }
                } else {
                    LOGGER.info(String.format("Metadata ID %s already exists", fixedId));
                }
                if (!DRY_RUN) {
                    metadataManager.delete(origId);
                }
            }
        }
    }

    public static void fixDuplicatedMetadataIds(DatabaseClient dbClient, CacheStrategy cacheStrategy) throws Exception {
        // List all IDs
        MetadataManager metadataManager = new MetadataManager(dbClient, cacheStrategy);

        JSONObjectIterable metadatas = metadataManager.selectAll(MetadataManager.MetadataType.NETCDF);

        for (JSONObject metadata : metadatas) {
            String origId = metadata.optString("_id", null);
            String fixedId = AbstractBean.safeIdValue(origId);

            if (!origId.equals(fixedId)) {
                boolean exists = metadataManager.exists(fixedId);
                if (exists) {
                    LOGGER.info(String.format("Deleting duplicated metadata ID: %s", origId));
                    if (!DRY_RUN) {
                        metadataManager.delete(origId);
                    }
                }
            }
        }
    }

    public static void lockOldMetadataIds(DatabaseClient dbClient, CacheStrategy cacheStrategy) throws Exception {
        // List all IDs
        MetadataManager metadataManager = new MetadataManager(dbClient, cacheStrategy);

        JSONObjectIterable metadatas = metadataManager.selectAll(MetadataManager.MetadataType.NETCDF);

        Map<String, Integer> duplicatedIdCountMap = new HashMap<>();
        Map<String, Integer> lockedMetadataCountMap = new HashMap<>();
        Map<String, Integer> skippedMetadataCountMap = new HashMap<>();

        for (JSONObject metadata : metadatas) {
            if (metadata != null) {
                String origId = metadata.optString("_id", null);
                if (origId != null) {
                    String idPrefix = origId.split("/")[0];
                    String fixedId = AbstractBean.safeIdValue(origId);
                    if (!origId.equals(fixedId)) {
                        if (origId.startsWith("downloads__ereefs__")) {
                            LOGGER.info(String.format("Lock down old metadata ID: %s", origId));
                            incrementCount(lockedMetadataCountMap, idPrefix);
                            metadata.put("lastModified", "3000-01-01T00:00:00.000Z");
                            if (!DRY_RUN) {
                                metadataManager.save(metadata, false);
                            }

                            boolean exists = metadataManager.exists(fixedId);
                            if (exists) {
                                LOGGER.info(String.format("Duplicate metadata ID found: %s %s", origId, fixedId));
                                incrementCount(duplicatedIdCountMap, idPrefix);

                                LOGGER.info(String.format("Deleting NEW duplicated metadata ID: %s", fixedId));
                                if (!DRY_RUN) {
                                    // Delete
                                    metadataManager.delete(fixedId);
                                }
                            }
                        } else {
                            LOGGER.info(String.format("NOT A EREEFS DOWNLOAD - SKIPPING: %s", origId));
                            incrementCount(skippedMetadataCountMap, idPrefix);
                        }
                    }
                }
            }
        }

        LOGGER.info("--- Summary ---");
        LOGGER.info("Duplicate metadata ID fixed");
        LOGGER.info(printCountMap(duplicatedIdCountMap));

        LOGGER.info("Locked metadata");
        LOGGER.info(printCountMap(lockedMetadataCountMap));

        LOGGER.info("Skipped metadata (not eReefs download)");
        LOGGER.info(printCountMap(skippedMetadataCountMap));
    }

    private static void incrementCount(Map<String, Integer> map, String key) {
        Integer count = map.get(key);
        if (count == null) {
            count = 0;
        }
        count++;
        map.put(key, count);
    }
    private static String printCountMap(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        return sb.toString();
    }
}
