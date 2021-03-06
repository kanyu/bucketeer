
package edu.ucla.library.bucketeer.converters;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.bucketeer.Constants;
import edu.ucla.library.bucketeer.MessageCodes;

/**
 * Our Kakadu converter test should be running in a single thread since it sets static variables.
 */
@net.jcip.annotations.NotThreadSafe
public class KakaduConverterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(KakaduConverterTest.class, Constants.MESSAGES);

    private static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    private static final String EXEC_LOCATION = "/usr/local/bin/";

    private static final String EXEC = "kdu_compress";

    private File myTIFF;

    private String myUUID;

    private String myKakaduHome;

    /**
     * Sets up a Kakadu converter test.
     *
     * @throws Exception If there is a problem with the set up
     */
    @Before
    public void setUp() throws Exception {
        myTIFF = new File("src/test/resources/images/test.tif");
        myUUID = UUID.randomUUID().toString();

        // Remember our KAKADU_HOME location, if any, so we can set it again after the test
        myKakaduHome = System.getProperty(KakaduConverter.KAKADU_HOME);

        // This sets a flag for whether or not we have kakadu installed
        ConverterFactory.checkSystemKakadu();
    }

    /**
     * Tears down a Kakadu converter test.
     *
     * @throws Exception If there is a problem with the tear down
     */
    @After
    public void tearDown() throws Exception {
        // Reset our native KAKADU_HOME value
        if (myKakaduHome != null) {
            System.setProperty(KakaduConverter.KAKADU_HOME, myKakaduHome);
        }
    }

    /**
     * Tests being able to get the executable.
     */
    @Test
    public final void testGetExecutable() {
        // We can test this without having Kakadu actually installed
        final KakaduConverter converter = new KakaduConverter();

        System.clearProperty(KakaduConverter.KAKADU_HOME);
        assertEquals(EXEC, converter.getExecutable());

        System.setProperty(KakaduConverter.KAKADU_HOME, EXEC_LOCATION);
        assertEquals(EXEC_LOCATION + EXEC, converter.getExecutable());
    }

    /**
     * Tests being able to convert to JP2. This depends on Kakadu being installed, so the test is skipped if that isn't
     * the case.
     *
     * @throws IOException If there is trouble reading the image
     * @throws InterruptedException If the process gets interrupted
     */
    @Test
    public final void testConvert() throws IOException, InterruptedException {
        if (ConverterFactory.hasSystemKakadu()) {
            final KakaduConverter converter = (KakaduConverter) ConverterFactory.getConverter();
            final File jpx = new File(TMP_DIR, myUUID + ".jpx");

            try {
                converter.convert(myUUID, myTIFF, Conversion.LOSSLESS);

                // Check that the JP2 exists and that it's not an empty file
                assertTrue(jpx.exists());
                assertTrue(jpx.length() > 30000);
            } finally {
                jpx.delete();
            }
        } else {
            LOGGER.warn(MessageCodes.BUCKETEER_003);
            assumeTrue(false); // Ignore this test
        }
    }

    /**
     * Gets the converter's getPath method.
     *
     * @throws IOException If there is trouble with the supplied file
     */
    @Test
    public final void testGetPath() throws IOException {
        final File file = new File(TMP_DIR, myUUID);
        assertEquals(file.getAbsolutePath(), new KakaduConverter().getPath(file));
    }

    /**
     * Sets whether hasKakaduHome method works as it should.
     */
    @Test
    public final void testHasKakaduHome() {
        System.clearProperty(KakaduConverter.KAKADU_HOME);
        Assert.assertFalse(KakaduConverter.hasKakaduHome());

        System.setProperty(KakaduConverter.KAKADU_HOME, "/usr/local/bin");
        Assert.assertTrue(KakaduConverter.hasKakaduHome());
    }

}
