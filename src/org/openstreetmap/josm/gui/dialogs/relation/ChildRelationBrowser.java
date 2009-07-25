// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.PrimitiveNameFormatter;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.ImageProvider;
import org.xml.sax.SAXException;

/**
 * ChildRelationBrowser is a UI component which provides a tree-like view on the hierarchical
 * structure of relations
 * 
 *
 */
public class ChildRelationBrowser extends JPanel {
    static private final Logger logger = Logger.getLogger(ChildRelationBrowser.class.getName());

    /** the tree with relation children */
    private RelationTree childTree;
    /**  the tree model */
    private RelationTreeModel model;

    /** the osm data layer this browser is related to */
    private OsmDataLayer layer;

    /**
     * Replies the {@see OsmDataLayer} this editor is related to
     * 
     * @return the osm data layer
     */
    protected OsmDataLayer getLayer() {
        return layer;
    }

    /**
     * builds the UI
     */
    protected void build() {
        setLayout(new BorderLayout());
        childTree = new RelationTree(model);
        JScrollPane pane = new JScrollPane(childTree);
        add(pane, BorderLayout.CENTER);

        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    /**
     * builds the panel with the command buttons
     * 
     * @return the button panel
     */
    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.LEFT));

        // ---
        DownloadAllChildRelationsAction downloadAction= new DownloadAllChildRelationsAction();
        pnl.add(new JButton(downloadAction));

        // ---
        DownloadSelectedAction downloadSelectedAction= new DownloadSelectedAction();
        childTree.addTreeSelectionListener(downloadSelectedAction);
        pnl.add(new JButton(downloadSelectedAction));

        // ---
        EditAction editAction = new EditAction();
        childTree.addTreeSelectionListener(editAction);
        pnl.add(new JButton(editAction));

        return pnl;
    }

    /**
     * constructor
     * 
     * @param layer the {@see OsmDataLayer} this browser is related to. Must not be null.
     * @exception IllegalArgumentException thrown, if layer is null
     */
    public ChildRelationBrowser(OsmDataLayer layer) throws IllegalArgumentException {
        if (layer == null)
            throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "layer"));
        this.layer = layer;
        model = new RelationTreeModel();
        build();
    }

    /**
     * constructor
     * 
     * @param layer the {@see OsmDataLayer} this browser is related to. Must not be null.
     * @param root the root relation
     * @exception IllegalArgumentException thrown, if layer is null
     */
    public ChildRelationBrowser(OsmDataLayer layer, Relation root) throws IllegalArgumentException {
        this(layer);
        populate(root);
    }

    /**
     * populates the browser with a relation
     * 
     * @param r the relation
     */
    public void populate(Relation r) {
        model.populate(r);
    }

    /**
     * populates the browser with a list of relation members
     * 
     * @param members the list of relation members
     */

    public void populate(List<RelationMember> members) {
        model.populate(members);
    }

    /**
     * replies the parent dialog this browser is embedded in
     * 
     * @return the parent dialog; null, if there is no {@see Dialog} as parent dialog
     */
    protected Dialog getParentDialog() {
        Component c  = this;
        while(c != null && ! (c instanceof Dialog)) {
            c = c.getParent();
        }
        return (Dialog)c;
    }

    /**
     * Action for editing the currently selected relation
     * 
     * 
     */
    class EditAction extends AbstractAction implements TreeSelectionListener {
        public EditAction() {
            putValue(SHORT_DESCRIPTION, tr("Edit the relation the currently selected relation member refers to"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
            putValue(NAME, tr("Edit"));
            refreshEnabled();
        }

        protected void refreshEnabled() {
            TreePath[] selection = childTree.getSelectionPaths();
            setEnabled(selection != null && selection.length > 0);
        }

        public void run() {
            TreePath [] selection = childTree.getSelectionPaths();
            if (selection == null || selection.length == 0) return;
            // do not launch more than 10 relation editors in parallel
            //
            for (int i=0; i < Math.min(selection.length,10);i++) {
                Relation r = (Relation)selection[i].getLastPathComponent();
                if (r.incomplete) {
                    continue;
                }
                RelationEditor editor = RelationEditor.getEditor(getLayer(), r, null);
                editor.setVisible(true);
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            run();
        }

        public void valueChanged(TreeSelectionEvent e) {
            refreshEnabled();
        }
    }

    /**
     * Action for downloading all child relations for a given parent relation.
     * Recursively.
     */
    class DownloadAllChildRelationsAction extends AbstractAction{
        public DownloadAllChildRelationsAction() {
            putValue(SHORT_DESCRIPTION, tr("Download all child relations (recursively)"));
            putValue(SMALL_ICON, ImageProvider.get("download"));
            putValue(NAME, tr("Download All Children"));
        }

        public void run() {
            Main.worker.submit(new DownloadAllChildrenTask(getParentDialog(), (Relation)model.getRoot()));
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            run();
        }
    }

    /**
     * Action for downloading all selected relations
     */
    class DownloadSelectedAction extends AbstractAction implements TreeSelectionListener {
        public DownloadSelectedAction() {
            putValue(SHORT_DESCRIPTION, tr("Download selected relations"));
            // FIXME: replace with better icon
            //
            putValue(SMALL_ICON, ImageProvider.get("download"));
            putValue(NAME, tr("Download Selected Children"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            TreePath [] selection = childTree.getSelectionPaths();
            setEnabled(selection != null && selection.length > 0);
        }

        public void run() {
            TreePath [] selection = childTree.getSelectionPaths();
            if (selection == null || selection.length == 0)
                return;
            HashSet<Relation> relations = new HashSet<Relation>();
            for (int i=0; i < selection.length;i++) {
                relations.add((Relation)selection[i].getLastPathComponent());
            }
            Main.worker.submit(new DownloadRelationSetTask(getParentDialog(),relations));
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            run();
        }

        public void valueChanged(TreeSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * The asynchronous task for downloading relation members.
     * 
     * 
     */
    class DownloadAllChildrenTask extends PleaseWaitRunnable {
        private boolean cancelled;
        private int conflictsCount;
        private Exception lastException;
        private Relation relation;
        private Stack<Relation> relationsToDownload;
        private Set<Long> downloadedRelationIds;

        public DownloadAllChildrenTask(Dialog parent, Relation r) {
            super(tr("Download relation members"), new PleaseWaitProgressMonitor(parent), false /*
             * don't
             * ignore
             * exception
             */);
            this.relation = r;
            relationsToDownload = new Stack<Relation>();
            downloadedRelationIds = new HashSet<Long>();
            relationsToDownload.push(this.relation);
        }

        @Override
        protected void cancel() {
            cancelled = true;
            OsmApi.getOsmApi().cancel();
        }

        protected void showLastException() {
            String msg = lastException.getMessage();
            if (msg == null) {
                msg = lastException.toString();
            }
            JOptionPane.showMessageDialog(null, msg, tr("Error"), JOptionPane.ERROR_MESSAGE);
        }

        protected void refreshView(Relation relation){
            for (int i=0; i < childTree.getRowCount(); i++) {
                Relation reference = (Relation)childTree.getPathForRow(i).getLastPathComponent();
                if (reference == relation) {
                    model.refreshNode(childTree.getPathForRow(i));
                }
            }
        }

        @Override
        protected void finish() {
            if (cancelled)
                return;
            if (lastException != null) {
                showLastException();
                return;
            }

            if (conflictsCount > 0) {
                JOptionPane op = new JOptionPane(
                        tr("There were {0} conflicts during import.", conflictsCount),
                        JOptionPane.WARNING_MESSAGE
                );
                JDialog dialog = op.createDialog(ChildRelationBrowser.this, tr("Conflicts in data"));
                dialog.setAlwaysOnTop(true);
                dialog.setModal(true);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setVisible(true);
            }
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            try {
                PrimitiveNameFormatter nameFormatter = new PrimitiveNameFormatter();
                while(! relationsToDownload.isEmpty() && !cancelled) {
                    Relation r = relationsToDownload.pop();
                    downloadedRelationIds.add(r.id);
                    for (RelationMember member: r.members) {
                        if (member.member instanceof Relation) {
                            Relation child = (Relation)member.member;
                            if (!downloadedRelationIds.contains(child)) {
                                relationsToDownload.push(child);
                            }
                        }
                    }
                    progressMonitor.setCustomText(tr("Downloading relation {0}", nameFormatter.getName(r)));
                    OsmServerObjectReader reader = new OsmServerObjectReader(r.id, OsmPrimitiveType.RELATION,
                            true);
                    DataSet dataSet = reader.parseOsm(progressMonitor
                            .createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
                    if (dataSet != null) {
                        final MergeVisitor visitor = new MergeVisitor(getLayer().data, dataSet);
                        visitor.merge();
                        // FIXME: this is necessary because there are dialogs listening
                        // for DataChangeEvents which manipulate Swing components on this
                        // thread.
                        //
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                getLayer().fireDataChange();
                            }
                        });
                        if (!visitor.getConflicts().isEmpty()) {
                            getLayer().getConflicts().add(visitor.getConflicts());
                            conflictsCount +=  visitor.getConflicts().size();
                        }
                    }
                    refreshView(r);
                }
            } catch (Exception e) {
                if (cancelled) {
                    System.out.println(tr("Warning: ignoring exception because task is cancelled. Exception: {0}", e
                            .toString()));
                    return;
                }
                lastException = e;
            }
        }
    }


    /**
     * The asynchronous task for downloading a set of relations
     */
    class DownloadRelationSetTask extends PleaseWaitRunnable {
        private boolean cancelled;
        private int conflictsCount;
        private Exception lastException;
        private Set<Relation> relations;

        public DownloadRelationSetTask(Dialog parent, Set<Relation> relations) {
            super(tr("Download relation members"), new PleaseWaitProgressMonitor(parent), false /*
             * don't
             * ignore
             * exception
             */);
            this.relations = relations;
        }

        @Override
        protected void cancel() {
            cancelled = true;
            OsmApi.getOsmApi().cancel();
        }

        protected void showLastException() {
            String msg = lastException.getMessage();
            if (msg == null) {
                msg = lastException.toString();
            }
            JOptionPane.showMessageDialog(null, msg, tr("Error"), JOptionPane.ERROR_MESSAGE);
        }

        protected void refreshView(Relation relation){
            for (int i=0; i < childTree.getRowCount(); i++) {
                Relation reference = (Relation)childTree.getPathForRow(i).getLastPathComponent();
                if (reference == relation) {
                    model.refreshNode(childTree.getPathForRow(i));
                }
            }
        }

        @Override
        protected void finish() {
            if (cancelled)
                return;
            if (lastException != null) {
                showLastException();
                return;
            }

            if (conflictsCount > 0) {
                JOptionPane op = new JOptionPane(
                        tr("There were {0} conflicts during import.", conflictsCount),
                        JOptionPane.WARNING_MESSAGE
                );
                JDialog dialog = op.createDialog(ChildRelationBrowser.this, tr("Conflicts in data"));
                dialog.setAlwaysOnTop(true);
                dialog.setModal(true);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setVisible(true);
            }
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            try {
                PrimitiveNameFormatter nameFormatter = new PrimitiveNameFormatter();
                Iterator<Relation> it = relations.iterator();
                while(it.hasNext() && !cancelled) {
                    Relation r = it.next();
                    progressMonitor.setCustomText(tr("Downloading relation {0}", nameFormatter.getName(r)));
                    OsmServerObjectReader reader = new OsmServerObjectReader(r.id, OsmPrimitiveType.RELATION,
                            true);
                    DataSet dataSet = reader.parseOsm(progressMonitor
                            .createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
                    if (dataSet != null) {
                        final MergeVisitor visitor = new MergeVisitor(getLayer().data, dataSet);
                        visitor.merge();
                        // FIXME: this is necessary because there are dialogs listening
                        // for DataChangeEvents which manipulate Swing components on this
                        // thread.
                        //
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                getLayer().fireDataChange();
                            }
                        });
                        if (!visitor.getConflicts().isEmpty()) {
                            getLayer().getConflicts().add(visitor.getConflicts());
                            conflictsCount +=  visitor.getConflicts().size();
                        }
                    }
                    refreshView(r);
                }
            } catch (Exception e) {
                if (cancelled) {
                    System.out.println(tr("Warning: ignoring exception because task is cancelled. Exception: {0}", e
                            .toString()));
                    return;
                }
                lastException = e;
            }
        }
    }
}
