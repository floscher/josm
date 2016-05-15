// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Unit tests for class {@link PostDownloadHandler}.
 */
public class PostDownloadHandlerTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    private static DownloadTask newTask(final List<Object> errorObjects) {
        return new DownloadTask() {
            @Override
            public Future<?> loadUrl(boolean newLayer, String url, ProgressMonitor progressMonitor) {
                return null;
            }

            @Override
            public String getTitle() {
                return null;
            }

            @Override
            public String[] getPatterns() {
                return new String[0];
            }

            @Override
            public List<Object> getErrorObjects() {
                return errorObjects;
            }

            @Override
            public String getConfirmationMessage(URL url) {
                return null;
            }

            @Override
            public Future<?> download(boolean newLayer, Bounds downloadArea, ProgressMonitor progressMonitor) {
                return null;
            }

            @Override
            public void cancel() {
            }

            @Override
            public boolean acceptsUrl(String url, boolean isRemotecontrol) {
                return false;
            }

            @Override
            public String acceptsDocumentationSummary() {
                return null;
            }
        };
    }

    private static Future<Object> newFuture(final String exceptionName) {
        return new Future<Object>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Object get() throws InterruptedException, ExecutionException {
                if (exceptionName != null) {
                    throw new ExecutionException(exceptionName, null);
                }
                return null;
            }

            @Override
            public Object get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                return null;
            }
        };
    }

    /**
     * Unit test of {@code PostDownloadHandler#run} - error case: future throws exception.
     */
    @Test
    public void testRunExceptionFuture() {
        Main.clearLastErrorAndWarnings();
        new PostDownloadHandler(null, newFuture("testRunExceptionFuture")).run();
        assertTrue(Main.getLastErrorAndWarnings().toString(),
                Main.getLastErrorAndWarnings().contains("E: java.util.concurrent.ExecutionException: testRunExceptionFuture"));
    }

    /**
     * Unit test of {@code PostDownloadHandler#run} - nominal case: no errors.
     */
    @Test
    public void testRunNoError() {
        Main.clearLastErrorAndWarnings();
        new PostDownloadHandler(newTask(Collections.emptyList()), newFuture(null)).run();
        assertTrue(Main.getLastErrorAndWarnings().toString(), Main.getLastErrorAndWarnings().isEmpty());
    }

    /**
     * Unit test of {@code PostDownloadHandler#run} - nominal case: only one error.
     */
    @Test
    public void testRunOneError() {
        Main.clearLastErrorAndWarnings();
        new PostDownloadHandler(newTask(Collections.singletonList(new Object())), newFuture(null)).run();
        assertTrue(Main.getLastErrorAndWarnings().toString(), Main.getLastErrorAndWarnings().isEmpty());
    }

    /**
     * Unit test of {@code PostDownloadHandler#run} - nominal case: multiple errors.
     */
    @Test
    public void testRunMultipleErrors() {
        Main.clearLastErrorAndWarnings();
        new PostDownloadHandler(newTask(Arrays.asList("foo", new Exception("bar"), new Object())), newFuture(null)).run();
        assertTrue(Main.getLastErrorAndWarnings().toString(),
                Main.getLastErrorAndWarnings().contains("E: java.lang.Exception: bar"));
    }
}
