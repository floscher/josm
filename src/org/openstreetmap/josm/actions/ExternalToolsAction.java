package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AddVisitor;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.data.osm.visitor.CollectBackReferencesVisitor;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.RawGpsLayer;
import org.openstreetmap.josm.gui.layer.RawGpsLayer.GpsPoint;
import org.openstreetmap.josm.io.GpxWriter;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.io.OsmWriter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import uk.co.wilson.xml.MinML2;

/**
 * Launches external tools configured in the preferences.
 *
 * @author Imi
 */
public class ExternalToolsAction extends AbstractAction {

	private final Collection<String> flags;
	private final Collection<String> input;
	private final String output;
	private final String[] exec;

	private final class ExecuteToolRunner extends PleaseWaitRunnable {
		private final Process p;
		private DataSet dataSet;
		private DataSet fromDataSet;
		private ExecuteToolRunner(String msg, Process p) {
			super(msg);
			this.p = p;
		}

		@Override protected void realRun() throws SAXException, IOException {
			Main.pleaseWaitDlg.currentAction.setText(tr("Executing {0}",getValue(NAME)));
			if (!input.isEmpty()) {
				fromDataSet = new DataSet();
				final Collection<GpsPoint> gpxPoints = new LinkedList<GpsPoint>();
				final boolean addOsm = !flags.contains("noosm");
				final boolean addGpx = flags.contains("gpx");

				AddVisitor adder = new AddVisitor(fromDataSet);
				if (flags.contains("include_references")) {
					adder = new AddVisitor(fromDataSet){
						@Override public void visit(Node n) {
							if (!ds.nodes.contains(n))
								super.visit(n);
                        }
						@Override public void visit(Segment s) {
	                        super.visit(s);
	                		if (!s.incomplete) {
	                			if (!ds.nodes.contains(s.from))
	                				s.from.visit(this);
	                			if (!ds.nodes.contains(s.to))
	                				s.to.visit(this);
	                		}
                        }
						@Override public void visit(Way w) {
	                        super.visit(w);
	            			for (Segment s : w.segments)
	            				if (!ds.segments.contains(s))
	            					s.visit(this);
                        }
					};
				}
				if (input.contains("selection")) {
					Collection<OsmPrimitive> sel = Main.ds.getSelected();
					if (addOsm) {
						for (OsmPrimitive osm : sel)
							osm.visit(adder);
						if (flags.contains("include_back_references")) {
							CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.ds);
							for (OsmPrimitive osm : sel)
								osm.visit(v);
							AddVisitor a = new AddVisitor(fromDataSet);
							for (OsmPrimitive osm : v.data)
								osm.visit(a);
						}
					}
					if (addGpx) {
						AllNodesVisitor v = new AllNodesVisitor();
						for (OsmPrimitive osm : sel)
							osm.visit(v);
						Bounds b = new Bounds();
						for (Node n : v.nodes)
							b.extend(n.coor);
						for (Layer l : Main.map.mapView.getAllLayers()) {
							if (!(l instanceof RawGpsLayer))
								continue;
							RawGpsLayer layer = (RawGpsLayer)l;
							for (Collection<GpsPoint> c : layer.data)
								for (GpsPoint p : c)
									if (p.latlon.isWithin(b))
										gpxPoints.add(p);
						}
					}
				}
				if (input.contains("all")) {
					if (addOsm)
						for (OsmPrimitive osm : Main.ds.allPrimitives())
							osm.visit(adder);
					for (Layer l : Main.map.mapView.getAllLayers())
						if (l instanceof RawGpsLayer)
							for (Collection<GpsPoint> c : ((RawGpsLayer)l).data)
								for (GpsPoint p : c)
									gpxPoints.add(p);
				}
				if (input.contains("screen")) {
					if (Main.map == null) {
						errorMessage = tr("The Tool requires some data to be loaded.");
						cancel();
						return;
					}
					LatLon bottomLeft = Main.map.mapView.getLatLon(0,Main.map.mapView.getHeight());
					LatLon topRight = Main.map.mapView.getLatLon(Main.map.mapView.getWidth(), 0);
					Bounds b = new Bounds(bottomLeft, topRight);
					if (addOsm) {
						Collection<Node> nodes = new HashSet<Node>();
						for (Node n : Main.ds.nodes) {
							if (n.coor.isWithin(b)) {
								n.visit(adder);
								nodes.add(n);
							}
						}
						Collection<Segment> segments = new HashSet<Segment>();
						for (Segment s : Main.ds.segments) {
							if (nodes.contains(s.from) || nodes.contains(s.to)) {
								s.visit(adder);
								segments.add(s);
							}
						}
						for (Way w : Main.ds.ways) {
							for (Segment s : w.segments) {
								if (segments.contains(s)) {
									w.visit(adder);
									break;
								}
							}
						}
					}
					if (addGpx) {
						for (Layer l : Main.map.mapView.getAllLayers())
							if (l instanceof RawGpsLayer)
								for (Collection<GpsPoint> c : ((RawGpsLayer)l).data)
									for (GpsPoint p : c)
										if (p.latlon.isWithin(b))
											gpxPoints.add(p);
					}
				}
				OsmWriter.output(p.getOutputStream(), new OsmWriter.Osm(){
					public void write(PrintWriter out) {
						if (addOsm)
							new OsmWriter.All(fromDataSet, false).write(out);
						if (addGpx) {
							Collection<Collection<GpsPoint>> c = new LinkedList<Collection<GpsPoint>>();
							c.add(gpxPoints);
							GpxWriter.Trk writer = new GpxWriter.Trk(c);
							writer.header(out);
							if (!gpxPoints.isEmpty())
								writer.write(out);
							writer.footer(out);
						}
					}
				});
			}
			if (output != null)
				dataSet = OsmReader.parseDataSet(p.getInputStream(), Main.ds, Main.pleaseWaitDlg);
		}

		@Override protected void cancel() {
			p.destroy();
		}

		@Override protected void finish() {
			if (dataSet == null || output == null || output.equals("discard"))
				return; // user cancelled or no stdout to process
			Collection<OsmPrimitive> allNew = dataSet.allPrimitives();
			Collection<OsmPrimitive> allOld = fromDataSet.allPrimitives();
			if (output.equals("replace")) {
				Command cmd = createCommand(allOld, allNew);
				if (cmd != null) {
					Main.main.editLayer().add(cmd);
					Main.ds.clearSelection();
				}
			} else if (output.equals("selection")) {
				Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
				for (OsmPrimitive osm : Main.ds.allPrimitives())
					if (allNew.contains(osm))
						sel.add(osm);
				Main.ds.setSelected(sel);
			}
		}

		/**
		 * Create a command that replaces all objects in from with those in to. The lists will be
		 * changed by createCommand.
		 */
		private Command createCommand(Collection<OsmPrimitive> from, Collection<OsmPrimitive> to) {
			// remove all objects in from/to, that are present in both lists.
			for (Iterator<OsmPrimitive> toIt = to.iterator(); toIt.hasNext();) {
				OsmPrimitive osm = toIt.next();
				for (Iterator<OsmPrimitive> fromIt = from.iterator(); fromIt.hasNext();) {
					if (fromIt.next().realEqual(osm)) {
						toIt.remove();
						fromIt.remove();
						break;
					}
				}
			}

			Collection<Command> cmd = new LinkedList<Command>();

			// extract all objects that have changed
			for (Iterator<OsmPrimitive> toIt = to.iterator(); toIt.hasNext();) {
				OsmPrimitive toOsm = toIt.next();
				for (Iterator<OsmPrimitive> fromIt = from.iterator(); fromIt.hasNext();) {
					OsmPrimitive fromOsm = fromIt.next();
					if (fromOsm.equals(toOsm)) {
						toIt.remove();
						fromIt.remove();
						cmd.add(new ChangeCommand(fromOsm, toOsm));
						break;
					}
				}
			}
			for (OsmPrimitive fromOsm : Main.ds.allPrimitives()) {
				for (Iterator<OsmPrimitive> it = to.iterator(); it.hasNext();) {
					OsmPrimitive toOsm = it.next();
					if (fromOsm.equals(toOsm)) {
						it.remove();
						cmd.add(new ChangeCommand(fromOsm, toOsm));
						break;
					}
				}
			}

			// extract all added objects
			for (OsmPrimitive osm : to)
				cmd.add(new AddCommand(osm));

			// extract all deleted objects. Delete references as well.
			CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.ds);
			for (OsmPrimitive osm : from)
				osm.visit(v);
			v.data.addAll(from);
			if (!v.data.isEmpty())
				cmd.add(new DeleteCommand(v.data));

			if (cmd.isEmpty())
				return null;
			return new SequenceCommand(tr("Executing {0}",getValue(NAME)), cmd);
		}
	}

	public ExternalToolsAction(String name, String[] exec, String[] flags, String[] input, String output) {
		super(name);
		this.exec = exec;
		this.flags = Arrays.asList(flags);
		this.input = Arrays.asList(input);
		this.output = output;
	}

	public void actionPerformed(ActionEvent e) {
		try {
			final Process p = new ProcessBuilder(exec).start();
			PleaseWaitRunnable runner = new ExecuteToolRunner(tr("Executing {0}",getValue(NAME)), p);
			Main.worker.execute(runner);
		} catch (IOException e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(Main.parent, tr("Could not execute command: {0}", exec[0]));
		}
	}

	/**
	 * @return All external tools configured so far as array.
	 */
	public static JMenu buildMenu() {
		if (!new File(Main.pref.getPreferencesDir()+"external_tools").exists())
			return null;

		final JMenu main = new JMenu(tr("Tools"));
		main.setMnemonic('T');
		MinML2 parser = new MinML2() {
			Stack<JMenu> current = new Stack<JMenu>();
			@Override public void startDocument() {
				current.push(main);
			}
			@Override public void startElement(String ns, String lname, String qname, Attributes a) throws SAXException {
				if (qname.equals("group")) {
					JMenu m = current.peek();
					current.push(new JMenu(a.getValue(("name"))));
					m.add(current.peek());
				}
				if (!qname.equals("tool"))
					return;
				String flagValue = a.getValue("flags");
				String[] flags = flagValue==null ? new String[]{} : flagValue.split(",");
				String output = a.getValue("out");
				String[] exec = a.getValue("exec").split(" ");
				if (exec.length < 1)
					throw new SAXException("Execute attribute must not be empty");
				String inValue = a.getValue("in");
				String[] input = inValue==null ? new String[]{} : inValue.split(",");

				current.peek().add(new ExternalToolsAction(a.getValue("name"), exec, flags, input, output));
			}
			@Override public void endElement(String ns, String lname, String qname) {
				if (qname.equals("group"))
					current.pop();
			}
		};
		try {
			parser.parse(new FileReader(Main.pref.getPreferencesDir()+"external_tools"));
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(Main.parent, tr("Could not read external tool configuration."));
		}
		return main;
	}
}
