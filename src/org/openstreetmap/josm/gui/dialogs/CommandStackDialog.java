package org.openstreetmap.josm.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.CommandQueueListener;

public class CommandStackDialog extends ToggleDialog implements CommandQueueListener {

	private DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    private JTree tree = new JTree(treeModel);
	private final MapFrame mapFrame;

	public CommandStackDialog(MapFrame mapFrame) {
		super("Command Stack", "commandstack", "Open a list of all commands (undo buffer).", KeyEvent.VK_C);
		this.mapFrame = mapFrame;
		setPreferredSize(new Dimension(320,100));
		mapFrame.mapView.editLayer().listenerCommands.add(this);
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.expandRow(0);
		tree.setCellRenderer(new DefaultTreeCellRenderer(){
			@Override public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
				DefaultMutableTreeNode v = (DefaultMutableTreeNode)value;
				if (v.getUserObject() instanceof JLabel) {
					JLabel l = (JLabel)v.getUserObject();
					setIcon(l.getIcon());
					setText(l.getText());
				}
				return this;
			}
		});
		tree.setVisibleRowCount(8);
		add(new JScrollPane(tree), BorderLayout.CENTER);
	}

	@Override public void setVisible(boolean v) {
		if (v)
			buildList();
		else if (tree != null)
			treeModel.setRoot(new DefaultMutableTreeNode());
		super.setVisible(v);
	}

	private void buildList() {
		Collection<Command> commands = mapFrame.mapView.editLayer().commands;
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		for (Command c : commands)
			root.add(c.description());
		treeModel.setRoot(root);
	}

	public void commandChanged() {
		if (!isVisible())
			return;
        treeModel.setRoot(new DefaultMutableTreeNode());
		buildList();
    }
}
