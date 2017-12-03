// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.auxiliary.disk.block.BlockDiskCacheAttributes;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests for class {@link JCSCacheManager}.
 */
public class JCSCacheManagerTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().timeout(20000);

    /**
     * Tests that {@code JCSCacheManager} satisfies utility class criterias.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(JCSCacheManager.class);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/12054">Bug #12054</a>.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testLoggingAdaptor12054() throws IOException {
        JCSCacheManager.getCache("foobar", 1, 0, "foobar"); // cause logging adaptor to be initialized
        Logger.getLogger("org.apache.commons.jcs").warning("{switch:0}");
    }

    @Test
    public void testUseBigDiskFile() throws IOException {
        if (JCSCacheManager.USE_BLOCK_CACHE.get()) {
            // test only when using block cache
            File cacheFile = new File("foobar/testUseBigDiskFile_BLOCK_v2.data");
            if (!cacheFile.exists()) {
                if (!cacheFile.createNewFile()) {
                    System.err.println("Unable to create " + cacheFile.getAbsolutePath());
                }
            }
            try (FileOutputStream fileOutputStream = new FileOutputStream(cacheFile, false)) {
                fileOutputStream.getChannel().truncate(0);
                fileOutputStream.write(new byte[1024*1024*10]); // create 10MB empty file
            }

            CacheAccess<Object, Object> cache = JCSCacheManager.getCache("testUseBigDiskFile", 1, 100, "foobar");
            assertEquals("BlockDiskCache use file size to calculate its size", 10*1024,
                    ((BlockDiskCacheAttributes) cache.getCacheControl().getAuxCaches()[0].getAuxiliaryCacheAttributes()).getMaxKeySize());
        }
    }
}
