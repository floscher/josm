// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main.InitStatusListener;
import org.openstreetmap.josm.Main.InitializationTask;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link Main} class.
 */
public class MainTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().https().devAPI().main().projection();

    /**
     * Unit tests on log messages.
     * @deprecated to remove end of 2017
     */
    @Test
    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    @Deprecated
    public void testLogs() {

        assertNull(Main.getErrorMessage(null));

        // Correct behaviour with errors
        Main.error(new Exception("exception_error"));
        Main.error("Error message on one line");
        Main.error("Error message with {0}", "param");
        Main.error("First line of error message on several lines\nline2\nline3\nline4");
        Collection<String> errors = Main.getLastErrorAndWarnings();
        assertTrue(errors.contains("E: java.lang.Exception: exception_error"));
        assertTrue(errors.contains("E: Error message with param"));
        assertTrue(errors.contains("E: Error message on one line"));
        assertTrue(errors.contains("E: First line of error message on several lines"));

        // Correct behaviour with warnings
        Main.warn(new Exception("exception_warn", new Exception("root_cause")));
        Main.warn(new Exception("exception_warn_bool"), true);
        Main.warn("Warning message on one line");
        Main.warn("First line of warning message on several lines\nline2\nline3\nline4");
        Collection<String> warnings = Main.getLastErrorAndWarnings();
        assertTrue(warnings.contains("W: java.lang.Exception: exception_warn. Cause: java.lang.Exception: root_cause"));
        assertTrue(warnings.contains("W: java.lang.Exception: exception_warn_bool"));
        assertTrue(warnings.contains("W: Warning message on one line"));
        assertTrue(warnings.contains("W: First line of warning message on several lines"));
    }

    /**
     * Unit test of {@link Main#preConstructorInit}.
     */
    @Test
    public void testPreConstructorInit() {
        Main.preConstructorInit();
        assertNotNull(Main.getProjection());
    }

    /**
     * Unit test of {@link Main#getBaseUserUrl}.
     */
    @Test
    public void testGetBaseUserUrl() {
        assertEquals("http://api06.dev.openstreetmap.org/user", Main.getBaseUserUrl());
    }

    /**
     * Unit test of {@link Main#addNetworkError}, {@link Main#getNetworkErrors} and {@link Main#clearNetworkErrors}.
     * @throws MalformedURLException if any error occurs
     */
    @Test
    public void testNetworkErrors() throws MalformedURLException {
        Main.clearNetworkErrors();
        assertTrue(Main.getNetworkErrors().isEmpty());
        Main.addNetworkError("http://url1", new Exception("exception_1"));
        Main.addNetworkError(new URL("http://url2"), new Exception("exception_2"));
        Map<String, Throwable> errors = Main.getNetworkErrors();
        assertEquals(2, errors.size());
        assertEquals("exception_1", errors.get("http://url1").getMessage());
        assertEquals("exception_2", errors.get("http://url2").getMessage());
        Main.clearNetworkErrors();
        assertTrue(Main.getNetworkErrors().isEmpty());
    }

    /**
     * Unit test of {@link Main#setOffline} and {@link Main#getOfflineResources}.
     */
    @Test
    public void testOfflineRessources() {
        Main.setOnline(OnlineResource.ALL);
        assertTrue(Main.getOfflineResources().isEmpty());
        assertFalse(Main.isOffline(OnlineResource.JOSM_WEBSITE));
        Main.setOffline(OnlineResource.JOSM_WEBSITE);
        assertTrue(Main.isOffline(OnlineResource.JOSM_WEBSITE));
        Main.setOnline(OnlineResource.JOSM_WEBSITE);
        assertFalse(Main.isOffline(OnlineResource.JOSM_WEBSITE));
        Main.setOffline(OnlineResource.ALL);
        assertTrue(Main.isOffline(OnlineResource.JOSM_WEBSITE));
        assertTrue(Main.isOffline(OnlineResource.OSM_API));
        Main.setOnline(OnlineResource.ALL);
    }

    private static class InitStatusListenerStub implements InitStatusListener {

        boolean updated;
        boolean finished;

        @Override
        public Object updateStatus(String event) {
            updated = true;
            return null;
        }

        @Override
        public void finish(Object status) {
            finished = true;
        }
    }

    /**
     * Unit test of {@link Main#setInitStatusListener}.
     */
    @Test
    public void testSetInitStatusListener() {
        InitStatusListenerStub listener = new InitStatusListenerStub();
        Main.setInitStatusListener(listener);
        assertFalse(listener.updated);
        assertFalse(listener.finished);
        new InitializationTask("", () -> { }).call();
        assertTrue(listener.updated);
        assertTrue(listener.finished);
    }
}
