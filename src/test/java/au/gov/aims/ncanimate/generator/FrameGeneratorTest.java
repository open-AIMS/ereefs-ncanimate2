/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate.generator;

import org.junit.Assert;
import org.junit.Test;

public class FrameGeneratorTest {

    @Test
    public void testNcAnimateFrameJarPattern() {
        // Test valid filenames

        // Example of a valid jar filename
        String validFilename1 = "ereefs-ncanimate2-frame-0.1-jar-with-dependencies.jar";
        // Still a valid filename, just a different version
        String validFilename2 = "ereefs-ncanimate2-frame-12.42-jar-with-dependencies.jar";

        Assert.assertTrue(String.format("NCANIMATE_FRAME_JAR_PATTERN didn't accept valid filename: %s", validFilename1),
                FrameGenerator.NCANIMATE_FRAME_JAR_PATTERN.matcher(validFilename1).matches());

        Assert.assertTrue(String.format("NCANIMATE_FRAME_JAR_PATTERN didn't accept valid filename: %s", validFilename2),
                FrameGenerator.NCANIMATE_FRAME_JAR_PATTERN.matcher(validFilename2).matches());


        // Test invalid jar filenames

        // We don't want that jar to be selected, it doesn't have bundled dependencies.
        String invalidFilename1 = "ereefs-ncanimate2-frame-0.1.jar";

        // Just to be sure the "." is considered as a character, not a metacharacter (allow any character)
        String invalidFilename2 = "ereefs-ncanimate2-frame-0.1-jar-with-dependenciesXjar";

        Assert.assertFalse(String.format("NCANIMATE_FRAME_JAR_PATTERN accepted invalid filename: %s", invalidFilename1),
                FrameGenerator.NCANIMATE_FRAME_JAR_PATTERN.matcher(invalidFilename1).matches());

        Assert.assertFalse(String.format("NCANIMATE_FRAME_JAR_PATTERN accepted invalid filename: %s", invalidFilename2),
                FrameGenerator.NCANIMATE_FRAME_JAR_PATTERN.matcher(invalidFilename2).matches());
    }
}
