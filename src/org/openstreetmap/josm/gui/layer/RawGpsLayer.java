package org.openstreetmap.josm.gui.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A layer holding data from a gps source.
 * The data is read only.
 * 
 * @author imi
 */
public class RawGpsLayer extends Layer implements PreferenceChangedListener {

	public class ConvertToOsmAction extends AbstractAction {
		public ConvertToOsmAction() {
			super("Convert layer to OSM");
        }
		public void actionPerformed(ActionEvent e) {
			DataSet ds = new DataSet();
			for (Collection<GpsPoint> c : data) {
				Way w = new Way();
				Node start = null;
				for (GpsPoint p : c) {
					Node end = new Node(p.latlon);
					ds.nodes.add(end);
					if (start != null) {
						Segment segment = new Segment(start,end);
						w.segments.add(segment);
						ds.segments.add(segment);
					}
					start = end;
				}
				ds.ways.add(w);
			}
			Main.main.addLayer(new OsmDataLayer(ds, "Data Layer", true));
			Main.main.removeLayer(RawGpsLayer.this);
        }
    }

	public static class GpsPoint {
		public final LatLon latlon;
		public final EastNorth eastNorth;
		public final String time;
		public GpsPoint(LatLon ll, String t) {
			latlon = ll; 
			eastNorth = Main.proj.latlon2eastNorth(ll); 
			time = t;
		}
	}
	
	/**
	 * A list of ways which containing a list of points.
	 */
	public final Collection<Collection<GpsPoint>> data;

	public RawGpsLayer(Collection<Collection<GpsPoint>> data, String name) {
		super(name);
		this.data = data;
		Main.pref.listener.add(this);
	}

	/**
	 * Return a static icon.
	 */
	@Override public Icon getIcon() {
		return ImageProvider.get("layer", "rawgps");
	}

	@Override public void paint(Graphics g, MapView mv) {
		String gpsCol = Main.pref.get("color.gps point");
		String gpsColSpecial = Main.pref.get("color.layer "+name);
		if (!gpsColSpecial.equals(""))
			g.setColor(ColorHelper.html2color(gpsColSpecial));
		else if (!gpsCol.equals(""))
			g.setColor(ColorHelper.html2color(gpsCol));
		else
			g.setColor(Color.GRAY);
		Point old = null;
		for (Collection<GpsPoint> c : data) {
			if (!Main.pref.getBoolean("draw.rawgps.lines.force"))
				old = null;
			for (GpsPoint p : c) {
				Point screen = mv.getPoint(p.eastNorth);
				if (Main.pref.getBoolean("draw.rawgps.lines") && old != null)
					g.drawLine(old.x, old.y, screen.x, screen.y);
				else
					g.drawRect(screen.x, screen.y, 0, 0);
				old = screen;
			}
		}
	}

	@Override public String getToolTipText() {
		int points = 0;
		for (Collection<GpsPoint> c : data)
			points += c.size();
		return data.size()+" tracks, "+points+" points.";
	}

	@Override public void mergeFrom(Layer from) {
		RawGpsLayer layer = (RawGpsLayer)from;
		data.addAll(layer.data);
	}

	@Override public boolean isMergable(Layer other) {
		return other instanceof RawGpsLayer;
	}

	@Override public void visitBoundingBox(BoundingXYVisitor v) {
		for (Collection<GpsPoint> c : data)
			for (GpsPoint p : c)
				v.visit(p.eastNorth);
	}

	@Override public Object getInfoComponent() {
		StringBuilder b = new StringBuilder();
		int points = 0;
		for (Collection<GpsPoint> c : data) {
			b.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;a track with "+c.size()+" points<br>");
			points += c.size();
		}
		b.append("</html>");
		return "<html>"+name+" consists of "+data.size()+" tracks ("+points+" points)<br>"+b.toString();
	}

	@Override public void addMenuEntries(JPopupMenu menu) {
		menu.add(new JMenuItem(new GpxExportAction(this)));
		
		JMenuItem color = new JMenuItem("Customize Color", ImageProvider.get("colorchooser"));
		color.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				String col = Main.pref.get("color.layer "+name, Main.pref.get("color.gps point", ColorHelper.color2html(Color.gray)));
				JColorChooser c = new JColorChooser(ColorHelper.html2color(col));
				Object[] options = new Object[]{"OK", "Cancel", "Default"};
				int answer = JOptionPane.showOptionDialog(Main.parent, c, "Choose a color", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
				switch (answer) {
				case 0:
					Main.pref.put("color.layer "+name, ColorHelper.color2html(c.getColor()));
					break;
				case 1:
					return;
				case 2:
					Main.pref.put("color.layer "+name, null);
					break;
				}
				Main.map.repaint();
			}
		});
		menu.add(color);
		
		JMenuItem tagimage = new JMenuItem("Import images", ImageProvider.get("tagimages"));
		tagimage.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser(Main.pref.get("tagimages.lastdirectory"));
				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				fc.setMultiSelectionEnabled(true);
				fc.setAcceptAllFileFilterUsed(false);
				fc.setFileFilter(new FileFilter(){
					@Override public boolean accept(File f) {
						return f.isDirectory() || f.getName().toLowerCase().endsWith(".jpg");
					}
					@Override public String getDescription() {
						return "JPEG images (*.jpg)";
					}
				});
				fc.showOpenDialog(Main.parent);
				File[] sel = fc.getSelectedFiles();
				if (sel == null || sel.length == 0)
					return;
				LinkedList<File> files = new LinkedList<File>();
				addRecursiveFiles(files, sel);
				Main.pref.put("tagimages.lastdirectory", fc.getCurrentDirectory().getPath());
				GeoImageLayer.create(files, RawGpsLayer.this);
            }

			private void addRecursiveFiles(LinkedList<File> files, File[] sel) {
				for (File f : sel) {
					if (f.isDirectory())
						addRecursiveFiles(files, f.listFiles());
					else if (f.getName().toLowerCase().endsWith(".jpg"))
						files.add(f);
				}
            }
		});
		menu.add(tagimage);
		
		menu.add(new JMenuItem(new ConvertToOsmAction()));
		
		menu.addSeparator();
		menu.add(new LayerListPopup.InfoAction(this));
    }

	public void preferenceChanged(String key, String newValue) {
		if (Main.map != null && (key.equals("draw.rawgps.lines") || key.equals("draw.rawgps.lines.force")))
			Main.map.repaint();
	}

	@Override public void layerRemoved() {
		Main.pref.listener.remove(this);
    }
}
