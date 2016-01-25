// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.plugins.PluginDownloadTask;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * Unit tests of {@link PluginPreference} class.
 */
public class PluginPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link PluginPreference#PluginPreference}.
     */
    @Test
    public void testPluginPreference()  {
        assertNotNull(new PluginPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link PluginPreference#buildDownloadSummary}.
     * @throws Exception if an error occurs
     */
    @Test
    public void testBuildDownloadSummary() throws Exception  {
        final PluginInformation dummy = new PluginInformation(
                new File(TestUtils.getTestDataRoot() + "plugin/dummy_plugin.jar"), "dummy_plugin");
        assertEquals("", PluginPreference.buildDownloadSummary(
                new PluginDownloadTask(NullProgressMonitor.INSTANCE, Collections.<PluginInformation>emptyList(), "")));
        assertEquals("", PluginPreference.buildDownloadSummary(
                new PluginDownloadTask(NullProgressMonitor.INSTANCE, Arrays.asList(dummy), "")));
        assertEquals("The following plugin has been downloaded <strong>successfully</strong>:<ul><li>dummy_plugin (31772)</li></ul>"+
                     "Downloading the following plugin has <strong>failed</strong>:<ul><li>dummy_plugin</li></ul>"+
                     "<br>Error message(untranslated): test",
                PluginPreference.buildDownloadSummary(
                        new PluginDownloadTask(NullProgressMonitor.INSTANCE, Arrays.asList(dummy), "") {
                    @Override
                    public Collection<PluginInformation> getFailedPlugins() {
                        return Collections.singleton(dummy);
                    }

                    @Override
                    public Collection<PluginInformation> getDownloadedPlugins() {
                        return Collections.singleton(dummy);
                    }

                    @Override
                    public Exception getLastException() {
                        return new Exception("test");
                    }
                }));
    }

    /**
     * Unit test of {@link PluginPreference#notifyDownloadResults}.
     */
    @Test
    public void testNotifyDownloadResults() {
        PluginDownloadTask task = new PluginDownloadTask(NullProgressMonitor.INSTANCE, Collections.<PluginInformation>emptyList(), "");
        PluginPreference.notifyDownloadResults(null, task, false);
        PluginPreference.notifyDownloadResults(null, task, true);
    }

    /**
     * Unit test of {@link PluginPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.testPreferenceSettingAddGui(new PluginPreference.Factory(), null);
    }
}
