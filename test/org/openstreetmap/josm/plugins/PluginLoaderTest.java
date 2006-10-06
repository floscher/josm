package org.openstreetmap.josm.plugins;

import java.io.File;

import junit.framework.TestCase;

public class PluginLoaderTest extends TestCase {

	public static class TestPlugin extends Plugin {
		public TestPlugin() {
	        super();
        }
	}

	private PluginLoader loader;

	@Override protected void setUp() throws Exception {
		super.setUp();
		loader = new PluginLoader();
	}

	public void testLoadPluginCallsStandardConstructor() throws Exception {
		PluginProxy plugin = loader.loadPlugin(getClass().getName()+"$TestPlugin", new File("foo.jar"));
		assertTrue(plugin.plugin instanceof TestPlugin);
	}
	
	public void testLoadPluginLoadsAllClasses() throws Exception {
		File classFile = new File(getClass().getResource("simple.jar").toURI());
		
		PluginProxy plugin = loader.loadPlugin("Simple", classFile);

		assertNotNull(plugin.plugin);
		assertEquals("simple", plugin.name);
	}
}
