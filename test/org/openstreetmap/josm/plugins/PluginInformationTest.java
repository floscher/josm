package org.openstreetmap.josm.plugins;

import java.io.File;

import junit.framework.TestCase;

public class PluginInformationTest extends TestCase {

	public void testConstructorExtractsAttributesFromManifest() throws Exception {
		PluginInformation info = new PluginInformation(new File(getClass().getResource("simple.jar").getFile()));
		String s = getClass().getResource(".").getFile();
		
		assertEquals(4, info.libraries.size());
		assertEquals(s+"foo", info.libraries.get(1).getFile());
		assertEquals(s+"bar", info.libraries.get(2).getFile());
		assertEquals(s+"C:/Foo%20and%20Bar", info.libraries.get(3).getFile());
		
		assertEquals("imi", info.author);
		assertEquals("Simple", info.className);
		assertEquals("Simpler", info.description);
		assertEquals(true, info.early);
    }
}
