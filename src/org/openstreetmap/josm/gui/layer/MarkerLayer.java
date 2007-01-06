package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A layer holding markers.
 * 
 * Markers are GPS points with a name and, optionally, a symbol code attached;
 * marker layers can be created from waypoints when importing raw GPS data,
 * but they may also come from other sources.
 * 
 * The symbol code is for future use.
 * 
 * The data is read only.
 */
public class MarkerLayer extends Layer {

	public static class Marker {
		public final EastNorth eastNorth;
		public final String text;
		public final Icon symbol;
		public Marker(LatLon ll, String t, String s) {
			eastNorth = Main.proj.latlon2eastNorth(ll); 
			text = t;
			Icon symbol = null;
			try {
                symbol = ImageProvider.get("symbols",s);
            } catch (RuntimeException e) {
    			try {
                    symbol = ImageProvider.get("nodes",s);
                } catch (RuntimeException e2) {
                }
            }
            this.symbol = symbol;
		}
	}

	/**
	 * A list of markers.
	 */
	public final Collection<Marker> data;

	public MarkerLayer(Collection<Marker> data, String name, File associatedFile) {
		super(name);
		this.associatedFile = associatedFile;
		this.data = data;
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				Main.map.mapView.addLayerChangeListener(new LayerChangeListener(){
					public void activeLayerChange(Layer oldLayer, Layer newLayer) {}
					public void layerAdded(Layer newLayer) {}
					public void layerRemoved(Layer oldLayer) {
						Main.pref.listener.remove(MarkerLayer.this);
					}
				});
			}
		});
	}

	/**
	 * Return a static icon.
	 */
	@Override public Icon getIcon() {
		return ImageProvider.get("layer", "marker");
	}

	@Override public void paint(Graphics g, MapView mv) {
		String mkrCol = Main.pref.get("color.gps marker");
		String mkrColSpecial = Main.pref.get("color.layer "+name);
		if (!mkrColSpecial.equals(""))
			g.setColor(ColorHelper.html2color(mkrColSpecial));
		else if (!mkrCol.equals(""))
			g.setColor(ColorHelper.html2color(mkrCol));
		else
			g.setColor(Color.GRAY);

		for (Marker mkr : data) {
			Point screen = mv.getPoint(mkr.eastNorth);
			if (mkr.symbol != null)
				mkr.symbol.paintIcon(Main.map.mapView, g, screen.x-mkr.symbol.getIconWidth()/2, screen.y-mkr.symbol.getIconHeight()/2);
			else {
				g.drawLine(screen.x-2, screen.y-2, screen.x+2, screen.y+2);
				g.drawLine(screen.x+2, screen.y-2, screen.x-2, screen.y+2);
			}
			g.drawString(mkr.text, screen.x+4, screen.y+2);
		}
	}

	@Override public String getToolTipText() {
		return data.size()+" "+trn("marker", "markers", data.size());
	}

	@Override public void mergeFrom(Layer from) {
		MarkerLayer layer = (MarkerLayer)from;
		data.addAll(layer.data);
	}

	@Override public boolean isMergable(Layer other) {
		return other instanceof MarkerLayer;
	}

	@Override public void visitBoundingBox(BoundingXYVisitor v) {
		for (Marker mkr : data)
			v.visit(mkr.eastNorth);
	}

	@Override public Object getInfoComponent() {
		return "<html>"+trn("{0} consists of {1} marker", "{0} consists of {1} markers", data.size(), name, data.size()) + "</html>";
	}

	@Override public Component[] getMenuEntries() {
		JMenuItem color = new JMenuItem(tr("Customize Color"), ImageProvider.get("colorchooser"));
		color.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				String col = Main.pref.get("color.layer "+name, Main.pref.get("color.gps marker", ColorHelper.color2html(Color.gray)));
				JColorChooser c = new JColorChooser(ColorHelper.html2color(col));
				Object[] options = new Object[]{tr("OK"), tr("Cancel"), tr("Default")};
				int answer = JOptionPane.showOptionDialog(Main.parent, c, tr("Choose a color"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
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

		return new Component[] {
			new JMenuItem(new LayerListDialog.ShowHideLayerAction(this)),
			new JMenuItem(new LayerListDialog.DeleteLayerAction(this)),
			new JSeparator(),
			color,
			new JMenuItem(new RenameLayerAction(associatedFile, this)),
			new JSeparator(),
			new JMenuItem(new LayerListPopup.InfoAction(this))
		};
	}
}
