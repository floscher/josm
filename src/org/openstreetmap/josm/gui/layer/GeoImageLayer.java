package org.openstreetmap.josm.gui.layer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.dialogs.LayerList;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.RawGpsLayer.GpsPoint;
import org.openstreetmap.josm.tools.DateParser;
import org.openstreetmap.josm.tools.ExifReader;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A layer which imports several photos from disk and read EXIF time information from them.
 *
 * @author Imi
 */
public class GeoImageLayer extends Layer {

	private static final class ImageEntry {
		File image;
		Date time;
		LatLon coor;
		EastNorth pos;
		Icon icon;
	}

	private static final class Loader extends PleaseWaitRunnable {
		boolean cancelled = false;
		private GeoImageLayer layer;
		private final Collection<File> files;
		private final RawGpsLayer gpsLayer;
		public Loader(Collection<File> files, RawGpsLayer gpsLayer) {
			super("Images");
			this.files = files;
			this.gpsLayer = gpsLayer;
		}
		@Override protected void realRun() throws IOException {
			currentAction.setText("Read GPS...");
			LinkedList<TimedPoint> gps = new LinkedList<TimedPoint>();

			// check the gps layer for time loops (and process it on the way)
			Date last = null;
			Pattern reg = Pattern.compile("(\\d\\d/\\d\\d/\\d{4}).(\\d\\d:\\d\\d:\\d\\d)");
			try {
				for (Collection<GpsPoint> c : gpsLayer.data) {
					for (GpsPoint p : c) {
						if (p.time == null)
							throw new IOException("No time for point "+p.latlon.lat()+","+p.latlon.lon());
						Matcher m = reg.matcher(p.time);
						if (!m.matches())
							throw new IOException("Cannot read time from point "+p.latlon.lat()+","+p.latlon.lon());
						Date d = DateParser.parse(m.group(1)+" "+m.group(2));
						gps.add(new TimedPoint(d, p.eastNorth));
						if (last != null && last.after(d))
							throw new IOException("Time loop in gps data.");
						last = d;
					}
				}
			} catch (ParseException e) {
				e.printStackTrace();
				throw new IOException("Incorrect date information");
			}

			if (gps.isEmpty()) {
				errorMessage = "No images with readable timestamps found.";
				return;
			}

			// read the image files
			ArrayList<ImageEntry> data = new ArrayList<ImageEntry>(files.size());
			int i = 0;
			progress.setMaximum(files.size());
			for (File f : files) {
				if (cancelled)
					break;
				currentAction.setText("Reading "+f.getName()+"...");
				progress.setValue(i++);

				ImageEntry e = new ImageEntry();
				e.time = ExifReader.readTime(f);
				if (e.time == null)
					continue;
				e.image = f;
				e.icon = loadScaledImage(f, 16);

				data.add(e);
			}
			layer = new GeoImageLayer(data, gps);
			layer.calculatePosition();
		}
		@Override protected void finish() {
			if (layer != null)
				Main.main.addLayer(layer);
		}
		@Override protected void cancel() {cancelled = true;}
	}

	public ArrayList<ImageEntry> data;
	private LinkedList<TimedPoint> gps = new LinkedList<TimedPoint>();

	/**
	 * The delta added to all timestamps in files from the camera 
	 * to match to the timestamp from the gps receivers tracklog.
	 */
	private long delta = Long.parseLong(Main.pref.get("tagimages.delta", "0"));
	private long gpstimezone = Long.parseLong(Main.pref.get("tagimages.gpstimezone", "0"))*60*60*1000;
	private boolean mousePressed = false;
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	private MouseAdapter mouseAdapter;

	public static final class GpsTimeIncorrect extends Exception {
		public GpsTimeIncorrect(String message, Throwable cause) {
			super(message, cause);
		}
		public GpsTimeIncorrect(String message) {
			super(message);
		}
	}

	private static final class TimedPoint {
		Date time;
		EastNorth pos;
		public TimedPoint(Date time, EastNorth pos) {
			this.time = time;
			this.pos = pos;
		}
	}

	public static void create(Collection<File> files, RawGpsLayer gpsLayer) {
		Loader loader = new Loader(files, gpsLayer);
		new Thread(loader).start();
		loader.pleaseWaitDlg.setVisible(true);
	}

	private GeoImageLayer(final ArrayList<ImageEntry> data, LinkedList<TimedPoint> gps) {
		super("Geotagged Images");
		this.data = data;
		this.gps = gps;
		mouseAdapter = new MouseAdapter(){
			@Override public void mousePressed(MouseEvent e) {
				if (e.getButton() != MouseEvent.BUTTON1)
					return;
				mousePressed  = true;
				if (visible)
					Main.map.mapView.repaint();
			}
			@Override public void mouseReleased(MouseEvent ev) {
				if (ev.getButton() != MouseEvent.BUTTON1)
					return;
				mousePressed = false;
				if (!visible)
					return;
				for (int i = data.size(); i > 0; --i) {
					ImageEntry e = data.get(i-1);
					if (e.pos == null)
						continue;
					Point p = Main.map.mapView.getPoint(e.pos);
					Rectangle r = new Rectangle(p.x-e.icon.getIconWidth()/2, p.y-e.icon.getIconHeight()/2, e.icon.getIconWidth(), e.icon.getIconHeight());
					if (r.contains(ev.getPoint())) {
						showImage(e);
						break;
					}
				}
				Main.map.mapView.repaint();
			}
		};
		Main.map.mapView.addMouseListener(mouseAdapter);
	}

	private void showImage(final ImageEntry e) {
		final JPanel p = new JPanel(new BorderLayout());
		final JScrollPane scroll = new JScrollPane(new JLabel(loadScaledImage(e.image, 580)));
		//scroll.setPreferredSize(new Dimension(800,600));
		final JViewport vp = scroll.getViewport();
		p.add(scroll, BorderLayout.CENTER);

		final JToggleButton scale = new JToggleButton(ImageProvider.get("misc", "rectangle"));
		JPanel p2 = new JPanel();
		p2.add(scale);
		p.add(p2, BorderLayout.SOUTH);
		scale.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ev) {
				p.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				if (scale.getModel().isSelected())
					((JLabel)vp.getView()).setIcon(loadScaledImage(e.image, Math.max(vp.getWidth(), vp.getHeight())));
				else
					((JLabel)vp.getView()).setIcon(new ImageIcon(e.image.getPath()));
				p.setCursor(Cursor.getDefaultCursor());
			}
		});
		scale.setSelected(true);
		JOptionPane.showMessageDialog(Main.parent, p, e.image+" ("+e.coor.lat()+","+e.coor.lon()+")", JOptionPane.PLAIN_MESSAGE);
	}

	@Override public Icon getIcon() {
		return ImageProvider.get("layer", "tagimages");
	}

	@Override public Object getInfoComponent() {
		JPanel p = new JPanel(new GridBagLayout());
		p.add(new JLabel(getToolTipText()), GBC.eop());

		p.add(new JLabel("GPS start: "+dateFormat.format(gps.getFirst().time)), GBC.eol());
		p.add(new JLabel("GPS end: "+dateFormat.format(gps.getLast().time)), GBC.eop());

		p.add(new JLabel("current delta: "+(delta/1000.0)+"s"), GBC.eol());
		p.add(new JLabel("timezone difference: "+(gpstimezone>0?"+":"")+(gpstimezone/1000/60/60)), GBC.eop());

		JList img = new JList(data.toArray());
		img.setCellRenderer(new DefaultListCellRenderer(){
			@Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				ImageEntry e = (ImageEntry)value;
				setIcon(e.icon);
				setText(e.image.getName()+" ("+dateFormat.format(new Date(e.time.getTime()+(delta+gpstimezone)))+")");
				if (e.pos == null)
					setForeground(Color.red);
				return this;
			}
		});
		img.setVisibleRowCount(5);
		p.add(new JScrollPane(img), GBC.eop().fill(GBC.BOTH));
		return p;
	}

	@Override public String getToolTipText() {
		int i = 0;
		for (ImageEntry e : data)
			if (e.pos != null)
				i++;
		return data.size()+" images, "+i+" within the track.";
	}

	@Override public boolean isMergable(Layer other) {
		return other instanceof GeoImageLayer;
	}

	@Override public void mergeFrom(Layer from) {
		GeoImageLayer l = (GeoImageLayer)from;
		data.addAll(l.data);
	}

	@Override public void paint(Graphics g, MapView mv) {
		boolean clickedFound = false;
		for (ImageEntry e : data) {
			if (e.pos != null) {
				Point p = mv.getPoint(e.pos);
				Rectangle r = new Rectangle(p.x-e.icon.getIconWidth()/2, p.y-e.icon.getIconHeight()/2, e.icon.getIconWidth(), e.icon.getIconHeight());
				e.icon.paintIcon(mv, g, r.x, r.y);
				Border b = null;
				if (!clickedFound && mousePressed && r.contains(mv.getMousePosition())) {
					b = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
					clickedFound = true;
				} else
					b = BorderFactory.createBevelBorder(BevelBorder.RAISED);
				Insets inset = b.getBorderInsets(mv);
				r.grow((inset.top+inset.bottom)/2, (inset.left+inset.right)/2);
				b.paintBorder(mv, g, r.x, r.y, r.width, r.height);
			}
		}
	}

	@Override public void visitBoundingBox(BoundingXYVisitor v) {
		for (ImageEntry e : data)
			v.visit(e.pos);
	}

	@Override public Component[] getMenuEntries() {
		JMenuItem sync = new JMenuItem("Sync clock", ImageProvider.get("clock"));
		sync.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser(Main.pref.get("tagimages.lastdirectory"));
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
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
				File sel = fc.getSelectedFile();
				if (sel == null)
					return;
				Main.pref.put("tagimages.lastdirectory", sel.getPath());
				sync(sel);
				Main.map.repaint();
			}
		});
		return new Component[]{
				new JMenuItem(new LayerList.ShowHideLayerAction(this)),
				new JMenuItem(new LayerList.DeleteLayerAction(this)),
				new JSeparator(),
				sync,
				new JSeparator(), 
				new JMenuItem(new LayerListPopup.InfoAction(this))};
	}

	private void calculatePosition() {
		for (ImageEntry e : data) {
			TimedPoint lastTP = null;
			for (TimedPoint tp : gps) {
				Date time = new Date(tp.time.getTime() - (delta+gpstimezone));
				if (time.after(e.time) && lastTP != null) {
					double x = (lastTP.pos.east()+tp.pos.east())/2;
					double y = (lastTP.pos.north()+tp.pos.north())/2;
					e.pos = new EastNorth(x,y);
					break;
				}
				lastTP = tp;
			}
			if (e.pos != null)
				e.coor = Main.proj.eastNorth2latlon(e.pos);
		}
	}

	private void sync(File f) {
		Date exifDate = ExifReader.readTime(f);
		JPanel p = new JPanel(new GridBagLayout());
		p.add(new JLabel("Image"), GBC.eol());
		p.add(new JLabel(loadScaledImage(f, 300)), GBC.eop());
		p.add(new JLabel("Enter shown date (mm/dd/yyyy HH:MM:SS)"), GBC.eol());
		JTextField gpsText = new JTextField(dateFormat.format(new Date(exifDate.getTime()+delta)));
		p.add(gpsText, GBC.eol().fill(GBC.HORIZONTAL));
		p.add(new JLabel("GPS unit timezome (difference to photo)"), GBC.eol());
		String t = Main.pref.get("tagimages.gpstimezone", "0");
		if (t.charAt(0) != '-')
			t = "+"+t;
		JTextField gpsTimezone = new JTextField(t);
		p.add(gpsTimezone, GBC.eol().fill(GBC.HORIZONTAL));

		while (true) {
			int answer = JOptionPane.showConfirmDialog(Main.parent, p, "Syncronize Time with GPS Unit", JOptionPane.OK_CANCEL_OPTION);
			if (answer != JOptionPane.OK_OPTION || gpsText.getText().equals(""))
				return;
			try {
				delta = DateParser.parse(gpsText.getText()).getTime() - exifDate.getTime();
				Main.pref.put("tagimages.delta", ""+delta);
				String time = gpsTimezone.getText();
				if (!time.equals("") && time.charAt(0) == '+')
					time = time.substring(1);
				if (time.equals(""))
					time = "0";
				Main.pref.put("tagimages.gpstimezone", time);
				gpstimezone = Long.valueOf(time)*60*60*1000;
				calculatePosition();
				return;
			} catch (ParseException x) {
				JOptionPane.showMessageDialog(Main.parent, "Time entered could not be parsed.");
			}
		}
	}

	private static Icon loadScaledImage(File f, int maxSize) {
		Image img = new ImageIcon(f.getPath()).getImage();
		int w = img.getWidth(null);
		int h = img.getHeight(null);
		if (w>h) {
			h = Math.round(maxSize*((float)h/w));
			w = maxSize;
		} else {
			w = Math.round(maxSize*((float)w/h));
			h = maxSize;
		}
		return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
	}

	@Override public void layerRemoved() {
		Main.map.mapView.removeMouseListener(mouseAdapter);
    }
}
