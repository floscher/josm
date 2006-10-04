package org.openstreetmap.josm;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.actions.AlignInCircleAction;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.actions.ExitAction;
import org.openstreetmap.josm.actions.ExternalToolsAction;
import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.OpenAction;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.RedoAction;
import org.openstreetmap.josm.actions.ReverseSegmentAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.actions.UndoAction;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.projection.Epsg4326;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.annotation.AnnotationTester;
import org.openstreetmap.josm.gui.dialogs.SelectionListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.CommandQueueListener;
import org.openstreetmap.josm.plugins.Plugin;
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
	 * All installed and loaded plugins (resp. their main classes)
	 */
	public final Collection<Plugin> plugins = new LinkedList<Plugin>();

	/**
	 * Set or clear (if passed <code>null</code>) the map.
	 */
	public final void setMapFrame(final MapFrame map) {
		MapFrame old = Main.map;
		Main.map = map;
		panel.setVisible(false);
		panel.removeAll();
		if (map != null) {
			map.fillPanel(panel);
			panel.setVisible(true);
			map.mapView.addLayerChangeListener(new LayerChangeListener(){
				public void activeLayerChange(final Layer oldLayer, final Layer newLayer) {
					setLayerMenu(newLayer.getMenuEntries());
				}
				public void layerAdded(final Layer newLayer) {
					if (newLayer instanceof OsmDataLayer)
						Main.main.editLayer().listenerCommands.add(redoUndoListener);
				}
				public void layerRemoved(final Layer oldLayer) {
					if (oldLayer instanceof OsmDataLayer)
						Main.main.editLayer().listenerCommands.add(redoUndoListener);
					if (map.mapView.getAllLayers().isEmpty())
						setLayerMenu(null);
				}
			});
			if (map.mapView.editLayer != null)
				map.mapView.editLayer.listenerCommands.add(redoUndoListener);
		}
		redoUndoListener.commandChanged(0,0);

		for (Plugin plugin : plugins)
			plugin.mapFrameInitialized(old, map);
	}

	/**
	 * Set the layer menu (changed when active layer changes).
	 */
	public final void setLayerMenu(Component[] entries) {
		if (entries == null || entries.length == 0)
			layerMenu.setVisible(false);
		else {
			layerMenu.removeAll();
			for (Component c : entries)
				layerMenu.add(c);
			layerMenu.setVisible(true);
		}
	}

	/**
	 * Remove the specified layer from the map. If it is the last layer, remove the map as well.
	 */
	public final void removeLayer(final Layer layer) {
		map.mapView.removeLayer(layer);
		if (layer instanceof OsmDataLayer)
			ds = new DataSet();
		if (map.mapView.getAllLayers().isEmpty())
			setMapFrame(null);
	}


	public Main() {
		main = this;
		contentPane.add(panel, BorderLayout.CENTER);

		final Action annotationTesterAction = new AbstractAction(){
			public void actionPerformed(ActionEvent e) {
				String annotationSources = pref.get("annotation.sources");
				if (annotationSources.equals("")) {
					JOptionPane.showMessageDialog(Main.parent, tr("You have to specify annotation sources in the preferences first."));
					return;
				}
				String[] args = annotationSources.split(";");
				new AnnotationTester(args);
			}
		};
		annotationTesterAction.putValue(Action.NAME, tr("Annotation Preset Tester"));
		annotationTesterAction.putValue(Action.SMALL_ICON, ImageProvider.get("annotation-tester"));
		final Action reverseSegmentAction = new ReverseSegmentAction();
		final Action alignInCircleAction = new AlignInCircleAction();
		final Action uploadAction = new UploadAction();
		final Action saveAction = new SaveAction();
		final Action saveAsAction = new SaveAsAction();
		final Action gpxExportAction = new GpxExportAction(null);
		final Action exitAction = new ExitAction();
		final Action preferencesAction = new PreferencesAction();
		final Action aboutAction = new AboutAction();

		final JMenu fileMenu = new JMenu(tr("Files"));
		fileMenu.setMnemonic('F');
		fileMenu.add(openAction);
		fileMenu.add(saveAction);
		fileMenu.add(saveAsAction);
		fileMenu.add(gpxExportAction);
		fileMenu.addSeparator();
		fileMenu.add(exitAction);
		mainMenu.add(fileMenu);


		final JMenu connectionMenu = new JMenu(tr("Connection"));
		connectionMenu.setMnemonic('C');
		connectionMenu.add(downloadAction);
		//connectionMenu.add(new DownloadIncompleteAction());
		connectionMenu.add(uploadAction);
		mainMenu.add(connectionMenu);

		layerMenu = new JMenu(tr("Layer"));
		layerMenu.setMnemonic('L');
		mainMenu.add(layerMenu);
		layerMenu.setVisible(false);

		final JMenu editMenu = new JMenu(tr("Edit"));
		editMenu.setMnemonic('E');
		editMenu.add(undoAction);
		editMenu.add(redoAction);
		editMenu.addSeparator();
		editMenu.add(reverseSegmentAction);
		editMenu.add(alignInCircleAction);
		editMenu.addSeparator();
		editMenu.add(preferencesAction);
		mainMenu.add(editMenu);

		JMenu externalMenu = ExternalToolsAction.buildMenu();
		if (externalMenu != null)
			mainMenu.add(externalMenu);

		mainMenu.add(new JSeparator());
		final JMenu helpMenu = new JMenu(tr("Help"));
		helpMenu.setMnemonic('H');
		helpMenu.add(annotationTesterAction);
		helpMenu.addSeparator();
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
		
		// Plugins
		if (pref.hasKey("plugins")) {
			for (String pluginName : pref.get("plugins").split(",")) {
				try {
	                plugins.add((Plugin)Class.forName(pluginName).newInstance());
                } catch (Exception e) {
                	e.printStackTrace();
                	JOptionPane.showMessageDialog(parent, tr("Could not load plugin {0}.", pluginName));
                }
			}
		}
	}
	/**
	 * Add a new layer to the map. If no map exist, create one.
	 */
	public final void addLayer(final Layer layer) {
		if (map == null) {
			final MapFrame mapFrame = new MapFrame();
			setMapFrame(mapFrame);
			mapFrame.selectMapMode((MapMode)mapFrame.getDefaultButtonAction());
			mapFrame.setVisible(true);
			mapFrame.setVisibleDialogs();
		}
		map.mapView.addLayer(layer);
	}
	/**
	 * @return The edit osm layer. If none exist, it will be created.
	 */
	public final OsmDataLayer editLayer() {
		if (map == null || map.mapView.editLayer == null)
			addLayer(new OsmDataLayer(ds, tr("unnamed"), null));
		return map.mapView.editLayer;
	}




	/**
	 * Use this to register shortcuts to
	 */
	public static final JPanel contentPane = new JPanel(new BorderLayout());


	////////////////////////////////////////////////////////////////////////////////////////
	//  Implementation part
	////////////////////////////////////////////////////////////////////////////////////////


	private static JPanel panel = new JPanel(new BorderLayout());

	protected final JMenuBar mainMenu = new JMenuBar();
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
	private JMenu layerMenu;

	/**
	 * Should be called before the main constructor to setup some parameter stuff
	 * @param args The parsed argument list.
	 */
	public static void preConstructorInit(Map<String, Collection<String>> args) {
		try {
			Main.pref.upgrade(Integer.parseInt(AboutAction.version));
		} catch (NumberFormatException e1) {
		}

		try {
			Main.proj = (Projection)Class.forName(Main.pref.get("projection")).newInstance();
		} catch (final Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, tr("The projection could not be read from preferences. Using EPSG:4263."));
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
				JOptionPane.showMessageDialog(Main.parent, tr("Ignoring malformed url: \"{0}\"", s));
			else
				main.downloadAction.download(false, b.min.lat(), b.min.lon(), b.max.lat(), b.max.lon());
			return;
		}

		if (s.startsWith("file:")) {
			try {
				main.openAction.openFile(new File(new URI(s)));
			} catch (URISyntaxException e) {
				JOptionPane.showMessageDialog(Main.parent, tr("Ignoring malformed file url: \"{0}\"", s));
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
