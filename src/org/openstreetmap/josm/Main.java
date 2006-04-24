package org.openstreetmap.josm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.actions.ExitAction;
import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.OpenAction;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.RedoAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.UndoAction;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.projection.Epsg4326;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.dialogs.SelectionListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.CommandQueueListener;
import org.openstreetmap.josm.tools.ImageProvider;

abstract public class Main {
	/**
	 * Global parent component for all dialogs and message boxes
	 */
	public static Component parent;
	/**
	 * Global application window. Use this as JOPtionPane-parent to center on application.
	 */
	public static Main main;
	/**
	 * The worker thread slave. This is for executing all long and intensive
	 * calculations. The executed runnables are guaranteed to be executed seperatly
	 * and sequenciel.
	 */
	public final static Executor worker = Executors.newSingleThreadExecutor();
	/**
	 * Global application preferences
	 */
	public static Preferences pref = new Preferences();
	/**
	 * The global dataset.
	 */
	public static DataSet ds = new DataSet();
	/**
	 * The projection method used.
	 */
	public static Projection proj;
	/**
	 * The MapFrame. Use setMapFrame to set or clear it.
	 */
	public static MapFrame map;

	/**
	 * Set or clear (if passed <code>null</code>) the map.
	 */
	public final void setMapFrame(final MapFrame map) {
		Main.map = map;
		panel.setVisible(false);
		panel.removeAll();
		if (map != null) {
			map.fillPanel(panel);
			panel.setVisible(true);
			map.mapView.addLayerChangeListener(new LayerChangeListener(){
				public void activeLayerChange(final Layer oldLayer, final Layer newLayer) {}
				public void layerAdded(final Layer newLayer) {
					if (newLayer instanceof OsmDataLayer)
						Main.main.editLayer().listenerCommands.add(redoUndoListener);
				}
				public void layerRemoved(final Layer oldLayer) {}
			});
		}
		redoUndoListener.commandChanged(0,0);
	}

	/**
	 * Remove the specified layer from the map. If it is the last layer, remove the map as well.
	 */
	public final void removeLayer(final Layer layer) {
		final Collection<Layer> allLayers = map.mapView.getAllLayers();
		if (allLayers.size() == 1 && allLayers.iterator().next() == layer) {
			Main.map.setVisible(false);
			setMapFrame(null);
			ds = new DataSet();
		} else {
			map.mapView.removeLayer(layer);
			if (layer instanceof OsmDataLayer)
				ds = new DataSet();
		}
	}
	public Main() {
		main = this;
		contentPane.add(panel, BorderLayout.CENTER);

		final Action uploadAction = new UploadAction();
		final Action saveAction = new SaveAction();
		final Action gpxExportAction = new GpxExportAction(null);
		final Action exitAction = new ExitAction();
		final Action preferencesAction = new PreferencesAction();
		final Action aboutAction = new AboutAction();

		final JMenu fileMenu = new JMenu("Files");
		fileMenu.setMnemonic('F');
		fileMenu.add(openAction);
		fileMenu.add(saveAction);
		fileMenu.add(gpxExportAction);
		fileMenu.addSeparator();
		fileMenu.add(exitAction);
		mainMenu.add(fileMenu);


		final JMenu layerMenu = new JMenu("Layer");
		layerMenu.setMnemonic('L');
		layerMenu.add(downloadAction);
		layerMenu.add(uploadAction);
		layerMenu.addSeparator();
		mainMenu.add(layerMenu);

		final JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('E');
		editMenu.add(undoAction);
		editMenu.add(redoAction);
		editMenu.addSeparator();
		editMenu.add(preferencesAction);
		mainMenu.add(editMenu);

		mainMenu.add(new JSeparator());
		final JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		helpMenu.add(aboutAction);
		mainMenu.add(helpMenu);

		// creating toolbar
		final JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.add(downloadAction);
		toolBar.add(uploadAction);
		toolBar.addSeparator();
		toolBar.add(openAction);
		toolBar.add(saveAction);
		toolBar.add(gpxExportAction);
		toolBar.addSeparator();
		toolBar.add(undoAction);
		toolBar.add(redoAction);
		toolBar.addSeparator();
		toolBar.add(preferencesAction);
		contentPane.add(toolBar, BorderLayout.NORTH);

		contentPane.updateUI();
	}
	/**
	 * Add a new layer to the map. If no map exist, create one.
	 */
	public final void addLayer(final Layer layer) {
		if (map == null) {
			final MapFrame mapFrame = new MapFrame(layer);
			setMapFrame(mapFrame);
			mapFrame.setVisible(true);
			mapFrame.setVisibleDialogs();
		} else
			map.mapView.addLayer(layer);
	}
	/**
	 * @return The edit osm layer. If none exist, it will be created.
	 */
	public final OsmDataLayer editLayer() {
		if (map == null || map.mapView.editLayer == null)
			addLayer(new OsmDataLayer(ds, "unnamed", false));
		return map.mapView.editLayer;
	}




	/**
	 * Use this to register shortcuts to
	 */
	public static JPanel panel = new JPanel(new BorderLayout());


	////////////////////////////////////////////////////////////////////////////////////////
	//  Implementation part
	////////////////////////////////////////////////////////////////////////////////////////


	protected final JMenuBar mainMenu = new JMenuBar();
	protected static final JPanel contentPane = new JPanel(new BorderLayout());
	protected static Rectangle bounds;

	private final UndoAction undoAction = new UndoAction();
	private final RedoAction redoAction = new RedoAction();
	private final OpenAction openAction = new OpenAction();
	private final DownloadAction downloadAction = new DownloadAction();

	private final CommandQueueListener redoUndoListener = new CommandQueueListener(){
		public void commandChanged(final int queueSize, final int redoSize) {
			undoAction.setEnabled(queueSize > 0);
			redoAction.setEnabled(redoSize > 0);
		}
	};

	/**
	 * Should be called before the main constructor to setup some parameter stuff
	 * @param args The parsed argument list.
	 */
	public static void preConstructorInit(Map<String, Collection<String>> args) {
		// load preferences
		String errMsg = null;
		try {
			if (args.containsKey("reset-preferences")) {
				Main.pref.resetToDefault();
			} else
				Main.pref.load();
		} catch (final IOException e1) {
			e1.printStackTrace();
			errMsg = "Preferences could not be loaded. Write default preference file to '"+pref.getPreferencesDir()+"preferences'.";
			Main.pref.resetToDefault();
		}
		if (errMsg != null)
			JOptionPane.showMessageDialog(null, errMsg);

		try {
			Main.proj = (Projection)Class.forName(Main.pref.get("projection")).newInstance();
		} catch (final Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "The projection could not be read from preferences. Using EPSG:4263.");
			Main.proj = new Epsg4326();
		}

		try {
			UIManager.setLookAndFeel(Main.pref.get("laf"));
			contentPane.updateUI();
			panel.updateUI();
		} catch (final Exception e) {
			e.printStackTrace();
		}
		UIManager.put("OptionPane.okIcon", ImageProvider.get("ok"));
		UIManager.put("OptionPane.yesIcon", UIManager.get("OptionPane.okIcon"));
		UIManager.put("OptionPane.cancelIcon", ImageProvider.get("cancel"));
		UIManager.put("OptionPane.noIcon", UIManager.get("OptionPane.cancelIcon"));

		Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
		if (args.containsKey("geometry")) {
			String geometry = args.get("geometry").iterator().next();
			final Matcher m = Pattern.compile("(\\d+)x(\\d+)(([+-])(\\d+)([+-])(\\d+))?").matcher(geometry);
			if (m.matches()) {
				int w = Integer.valueOf(m.group(1));
				int h = Integer.valueOf(m.group(2));
				int x = 0, y = 0;
				if (m.group(3) != null) {
					x = Integer.valueOf(m.group(5));
					y = Integer.valueOf(m.group(7));
					if (m.group(4).equals("-"))
						x = screenDimension.width - x - w;
					if (m.group(6).equals("-"))
						y = screenDimension.height - y - h;
				}
				bounds = new Rectangle(x,y,w,h);
			} else
				System.out.println("Ignoring malformed geometry: "+geometry);
		}
		if (bounds == null)
			bounds = !args.containsKey("no-fullscreen") ? new Rectangle(0,0,screenDimension.width,screenDimension.height) : new Rectangle(1000,740);
	}

	public void postConstructorProcessCmdLine(Map<String, Collection<String>> args) {
		if (args.containsKey("download"))
			for (String s : args.get("download"))
				downloadFromParamString(false, s);
		if (args.containsKey("downloadgps"))
			for (String s : args.get("downloadgps"))
				downloadFromParamString(true, s);
		if (args.containsKey("selection"))
			for (String s : args.get("selection"))
				SelectionListDialog.search(s, SelectionListDialog.SearchMode.add);
	}

	private static void downloadFromParamString(final boolean rawGps, String s) {
		if (s.startsWith("http:")) {
			final Bounds b = DownloadAction.osmurl2bounds(s);
			if (b == null)
				JOptionPane.showMessageDialog(Main.parent, "Ignoring malformed url: '"+s+"'");
			else
				main.downloadAction.download(false, b.min.lat(), b.min.lon(), b.max.lat(), b.max.lon());
			return;
		}

		if (s.startsWith("file:")) {
			try {
				main.openAction.openFile(new File(new URI(s)));
			} catch (URISyntaxException e) {
				JOptionPane.showMessageDialog(Main.parent, "Ignoring malformed file url: '"+s+"'");
			}
			return;
		}

		final StringTokenizer st = new StringTokenizer(s, ",");
		if (st.countTokens() == 4) {
			try {
				main.downloadAction.download(rawGps, Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()));
				return;
			} catch (final NumberFormatException e) {
			}
		}

		main.openAction.openFile(new File(s));
	}
}
