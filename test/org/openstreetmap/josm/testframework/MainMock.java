package org.openstreetmap.josm.testframework;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JFrame;

import org.junit.BeforeClass;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.projection.Epsg4326;
import org.openstreetmap.josm.gui.PleaseWaitDialog;

public class MainMock {

	@BeforeClass public static void mockMain() throws Exception {
		Main.pref = new Preferences(){
			@Override protected void save() {}
			@Override public void load() throws IOException {}
			@Override public Collection<Bookmark> loadBookmarks() throws IOException {return Collections.emptyList();}
			@Override public void saveBookmarks(Collection<Bookmark> bookmarks) throws IOException {}
		};
		Main.parent = new JFrame();
		Main.proj = new Epsg4326();
		Main.pleaseWaitDlg = new PleaseWaitDialog(Main.parent);
		Main.main = new Main(){};
	}
}
