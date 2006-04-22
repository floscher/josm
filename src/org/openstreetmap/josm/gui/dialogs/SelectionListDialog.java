package org.openstreetmap.josm.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import org.jdom.JDOMException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmIdReader;
import org.openstreetmap.josm.io.ProgressReader;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.SearchCompiler;
import org.xml.sax.SAXException;

/**
 * A small tool dialog for displaying the current selection. The selection manager
 * respects clicks into the selection list. Ctrl-click will remove entries from
 * the list while single click will make the clicked entry the only selection.
 * 
 * @author imi
 */
public class SelectionListDialog extends ToggleDialog implements SelectionChangedListener {

	public static enum SearchMode {replace, add, remove}

	private static class SelectionWebsiteLoader extends PleaseWaitRunnable {
		public final URL url;
		public Collection<OsmPrimitive> sel;
		private final SearchMode mode;
		private OsmIdReader idReader = new OsmIdReader();
		public SelectionWebsiteLoader(String urlStr, SearchMode mode) {
	        super("Load Selection");
	        this.mode = mode;
	        URL u = null;
			try {u = new URL(urlStr);} catch (MalformedURLException e) {}
            this.url = u;
        }
		@Override protected void realRun() throws SAXException, JDOMException, IOException {
			currentAction.setText("Contact "+url.getHost()+"...");
			sel = mode != SearchMode.remove ? new LinkedList<OsmPrimitive>() : Main.ds.allNonDeletedPrimitives();
			try {
		        HttpURLConnection con = (HttpURLConnection)url.openConnection();
		        Reader in = new ProgressReader(con, progress);
				currentAction.setText("Downloading...");
				Map<Long, String> ids = idReader.parseIds(in);
		        for (OsmPrimitive osm : Main.ds.allNonDeletedPrimitives()) {
		        	if (ids.containsKey(osm.id) && osm.getClass().getName().toLowerCase().endsWith(ids.get(osm.id))) {
		        		if (mode == SearchMode.remove)
		        			sel.remove(osm);
		        		else
		        			sel.add(osm);
		        	}
				}
	        } catch (IOException e) {
		        e.printStackTrace();
		        JOptionPane.showMessageDialog(Main.main, "Could not read from url: '"+url+"'");
	        } catch (SAXException e) {
		        e.printStackTrace();
		        JOptionPane.showMessageDialog(Main.main, "Parsing error in url: '"+url+"'");
	        }
        }
		@Override protected void cancel() {
			sel = null;
			idReader.cancel();
		}
		@Override protected void finish() {
			if (sel != null)
				Main.ds.setSelected(sel);
        }
	}
	
	/**
	 * The selection's list data.
	 */
	private final DefaultListModel list = new DefaultListModel();
	/**
	 * The display list.
	 */
	private JList displaylist = new JList(list);
	
	/**
	 * Create a SelectionList dialog.
	 * @param mapView The mapView to get the dataset from.
	 */
	public SelectionListDialog(MapFrame mapFrame) {
		super("Current Selection", "Selection List", "selectionlist", "Open a selection list window.", "E", KeyEvent.VK_E, "selectionlist");
		setPreferredSize(new Dimension(320,150));
		displaylist.setCellRenderer(new OsmPrimitivRenderer());
		displaylist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		displaylist.addMouseListener(new MouseAdapter(){
			@Override public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() < 2)
					return;
				updateMap();
			}
		});

		add(new JScrollPane(displaylist), BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new GridLayout(1,2));
		
		JButton button = new JButton("Select", ImageProvider.get("mapmode", "selection"));
		button.setToolTipText("Set the selected elements on the map to the selected items in the list above.");
		button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				updateMap();
			}
		});
		buttonPanel.add(button);

		button = new JButton("Search", ImageProvider.get("dialogs", "search"));
		button.setToolTipText("Search for objects.");
		button.addActionListener(new ActionListener(){
			private String lastSearch = "";
			public void actionPerformed(ActionEvent e) {
				JLabel label = new JLabel("Please enter a search string.");
				final JTextField input = new JTextField(lastSearch);
				input.setToolTipText("<html>Fulltext search.<ul>" +
						"<li><code>Baker Street</code>  - 'Baker' and 'Street' in any key or name.</li>" +
						"<li><code>\"Baker Street\"</code>  - 'Baker Street' in any key or name.</li>" +
						"<li><code>name:Bak</code>  - 'Bak' anywhere in the name.</li>" +
						"<li><code>-name:Bak</code>  - not 'Bak' in the name.</li>" +
						"<li><code>foot:</code>  - key=foot set to any value." +
						"</ul></html>");

				JRadioButton replace = new JRadioButton("replace selection", true);
				JRadioButton add = new JRadioButton("add to selection", false);
				JRadioButton remove = new JRadioButton("remove from selection", false);
				ButtonGroup bg = new ButtonGroup();
				bg.add(replace);
				bg.add(add);
				bg.add(remove);

				JPanel p = new JPanel(new GridBagLayout());
				p.add(label, GBC.eop());
				p.add(input, GBC.eop().fill(GBC.HORIZONTAL));
				p.add(replace, GBC.eol());
				p.add(add, GBC.eol());
				p.add(remove, GBC.eol());
				JOptionPane pane = new JOptionPane(p, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null){
					@Override public void selectInitialValue() {
						input.requestFocusInWindow();
                    }
				};
				pane.createDialog(Main.main, "Search").setVisible(true);
				if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue()))
					return;
				lastSearch = input.getText();
				SearchMode mode = replace.isSelected() ? SearchMode.replace : (add.isSelected() ? SearchMode.add : SearchMode.remove);
				search(lastSearch, mode);
			}
		});
		buttonPanel.add(button);
		
		add(buttonPanel, BorderLayout.SOUTH);
		selectionChanged(Main.ds.getSelected());
	}

	@Override public void setVisible(boolean b) {
		if (b) {
			Main.ds.addSelectionChangedListener(this);
			selectionChanged(Main.ds.getSelected());
		} else {
			Main.ds.removeSelectionChangedListener(this);
		}
		super.setVisible(b);
	}



	/**
	 * Called when the selection in the dataset changed.
	 * @param newSelection The new selection array.
	 */
	public void selectionChanged(Collection<OsmPrimitive> newSelection) {
		if (list == null)
			return; // selection changed may be received in base class constructor before init
		list.removeAllElements();
		list.setSize(newSelection.size());
		int i = 0;
		for (OsmPrimitive osm : newSelection)
			list.setElementAt(osm, i++);
	}

	/**
	 * Sets the selection of the map to the current selected items.
	 */
	public void updateMap() {
		Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
		for (int i = 0; i < list.getSize(); ++i)
			if (displaylist.isSelectedIndex(i))
				sel.add((OsmPrimitive)list.get(i));
		Main.ds.setSelected(sel);
	}

	public static void search(String search, SearchMode mode) {
	    if (search.startsWith("http://")) {
	    	SelectionWebsiteLoader loader = new SelectionWebsiteLoader(search, mode);
	    	if (loader.url != null) {
	    		Main.worker.execute(loader);
	    		loader.pleaseWaitDlg.setVisible(true);
	    		return;
	    	}
	    }
	    Collection<OsmPrimitive> sel = Main.ds.getSelected();
    	SearchCompiler.Match matcher = SearchCompiler.compile(search);
    	for (OsmPrimitive osm : Main.ds.allNonDeletedPrimitives()) {
    		if (mode == SearchMode.replace) {
    			if (matcher.match(osm))
    				sel.add(osm);
    			else
    				sel.remove(osm);
    		} else if (mode == SearchMode.add && !osm.selected && matcher.match(osm))
    			sel.add(osm);
    		else if (mode == SearchMode.remove && osm.selected && matcher.match(osm))
    			sel.remove(osm);
    	}
	    Main.ds.setSelected(sel);
    }
}
