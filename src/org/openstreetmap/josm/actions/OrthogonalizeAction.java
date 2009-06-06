// License: GPL. See LICENSE file for details.
//
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.DontShowAgainInfo;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Align edges of a way so all angles are right angles.
 *
 * 1. Find orientation of all edges
 * 2. Compute main orientation, weighted by length of edge, normalized to angles between 0 and pi/2
 * 3. Rotate every edge around its center to align with main orientation or perpendicular to it
 * 4. Compute new intersection points of two adjascent edges
 * 5. Move nodes to these points
 */
public final class OrthogonalizeAction extends JosmAction {

    public OrthogonalizeAction() {
        super(tr("Orthogonalize Shape"),
            "ortho",
            tr("Move nodes so all angles are 90 or 270 degree"),
            Shortcut.registerShortcut("tools:orthogonalize", tr("Tool: {0}", tr("Orthogonalize Shape")),
            KeyEvent.VK_Q,
            Shortcut.GROUP_EDIT), true);
    }

    public void actionPerformed(ActionEvent e) {

        Collection<OsmPrimitive> sel = Main.ds.getSelected();

        ArrayList<Node> dirnodes = new ArrayList<Node>();

        // Check the selection if it is suitible for the orthogonalization
        for (OsmPrimitive osm : sel) {
            // Check if not more than two nodes in the selection
            if(osm instanceof Node) {
                if(dirnodes.size() == 2) {
                    JOptionPane.showMessageDialog(Main.parent, tr("Only two nodes allowed"));
                    return;
                }
                dirnodes.add((Node) osm);
                continue;
            }
            // Check if selection consists now only of ways
            if (!(osm instanceof Way)) {
                JOptionPane.showMessageDialog(Main.parent, tr("Selection must consist only of ways."));
                return;
            }

            // Check if every way is made of at least four segments and closed
            Way way = (Way)osm;
            if ((way.nodes.size() < 5) || (!way.nodes.get(0).equals(way.nodes.get(way.nodes.size() - 1)))) {
                JOptionPane.showMessageDialog(Main.parent, tr("Please select one or more closed ways of at least four nodes."));
                return;
            }

            // Check if every edge in the way is a definite edge of at least 45 degrees of direction change
            // Otherwise, two segments could be turned into same direction and intersection would fail.
            // Or changes of shape would be too serious.
            for (int i1=0; i1 < way.nodes.size()-1; i1++) {
                int i2 = (i1+1) % (way.nodes.size()-1);
                int i3 = (i1+2) % (way.nodes.size()-1);
                double angle1  =Math.abs(way.nodes.get(i1).getEastNorth().heading(way.nodes.get(i2).getEastNorth()));
                double angle2 = Math.abs(way.nodes.get(i2).getEastNorth().heading(way.nodes.get(i3).getEastNorth()));
                double delta = Math.abs(angle2 - angle1);
                while(delta > Math.PI) delta -= Math.PI;
                if(delta < Math.PI/4) {
                    JOptionPane.showMessageDialog(Main.parent, tr("Please select ways with almost right angles to orthogonalize."));
                    return;
                }
            }
        }

        if ("EPSG:4326".equals(Main.proj.toString())) {
            String msg = tr("<html>You are using the EPSG:4326 projection which might lead<br>" +
                    "to undesirable results when doing rectangular alignments.<br>" +
                    "Change your projection to get rid of this warning.<br>" +
                    "Do you want to continue?");

            if (!DontShowAgainInfo.show("align_rectangular_4326", msg, false)) {
                return;
            }
        }
        // Check, if selection held neither none nor two nodes
        if(dirnodes.size() == 1) {
            JOptionPane.showMessageDialog(Main.parent, tr("Only one node selected"));
            return;
        }

        // Now all checks are done and we can now do the neccessary computations
        // From here it is assumed that the above checks hold
        Collection<Command> cmds = new LinkedList<Command>();
        double align_to_heading = 0.0;
        boolean use_dirnodes = false;

        if (dirnodes.size() == 2) {
            // When selection contains two nodes, use the nodes to compute a direction
            // to align all ways to
            align_to_heading = normalize_angle(dirnodes.get(0).getEastNorth().heading(dirnodes.get(1).getEastNorth()));
            use_dirnodes = true;
        }

        for (OsmPrimitive osm : sel) {
            if(!(osm instanceof Way))
                continue;

            Way way = (Way)osm;
            int nodes = way.nodes.size();
            int sides = nodes - 1;
            // Copy necessary data into a more suitable data structure
            EastNorth en[] = new EastNorth[sides];
            for (int i=0; i < sides; i++) {
                en[i] = new EastNorth(way.nodes.get(i).getEastNorth().east(), way.nodes.get(i).getEastNorth().north());
            }

            if (! use_dirnodes) {
                // To find orientation of all segments, compute weighted average of all segment's headings
                // all headings are mapped into [-PI/4, PI/4] by PI/2 rotations so both main orientations are mapped into one
                // the headings are weighted by the length of the segment establishing it, so a longer segment, that is more
                // likely to have the correct orientation, has more influence in the computing than a short segment, that is easier to misalign.
                double headings[] = new double[sides];
                double weights[] = new double[sides];
                for (int i=0; i < sides; i++) {
                    headings[i] = normalize_angle(way.nodes.get(i).getEastNorth().heading(way.nodes.get(i+1).getEastNorth()));
                    weights[i] = way.nodes.get(i).getEastNorth().distance(way.nodes.get(i+1).getEastNorth());
                }

                // CAVEAT: for orientations near -PI/4 or PI/4 the mapping into ONE orientation fails
                //         resulting in a heading-difference between adjacent sides of almost PI/2
                //         and a totally wrong average
                // check for this (use PI/3 as arbitray limit) and rotate into ONE orientation
                double angle_diff_max = 0.0;
                for (int i=0; i < sides; i++) {
                    double diff = 0.0;
                    if (i == 0) {
                        diff = heading_diff(headings[i], headings[sides - 1]);
                    } else {
                        diff = heading_diff(headings[i], headings[i - 1]);
                    }
                    if (diff > angle_diff_max) angle_diff_max = diff;
                }

                if (angle_diff_max > Math.PI/3) {
                    // rearrange headings: everything < 0 gets PI/2-rotated
                    for (int i=0; i < sides; i++) {
                        if (headings[i] < 0)
                            headings[i] += Math.PI/2;
                    }
                }

                // TODO:
                // use angle_diff_max as an indicator that the way is already orthogonal
                // e.g. if angle_diff_max is less then Math.toRadians(0.5)
                // and do nothing in that case (?)

                // Compute the weighted average of the headings of all segments
                double sum_weighted_headings = 0.0;
                double sum_weights = 0.0;
                for (int i=0; i < sides; i++) {
                    sum_weighted_headings += headings[i] * weights[i];
                    sum_weights += weights[i];
                }
                align_to_heading = normalize_angle(sum_weighted_headings/sum_weights);
            }


            for (int i=0; i < sides; i++) {
                // Compute handy indices of three nodes to be used in one loop iteration.
                // We use segments (i1,i2) and (i2,i3), align them and compute the new
                // position of the i2-node as the intersection of the realigned (i1,i2), (i2,i3) segments
                // Not the most efficient algorithm, but we don't handle millions of nodes...
                int i1 = i;
                int i2 = (i+1)%sides;
                int i3 = (i+2)%sides;
                double heading1, heading2;
                double delta1, delta2;
                // Compute neccessary rotation of first segment to align it with main orientation
                heading1 = normalize_angle(en[i1].heading(en[i2]), align_to_heading);
                delta1 = align_to_heading - heading1;
                // Compute neccessary rotation of second segment to align it with main orientation
                heading2 = normalize_angle(en[i2].heading(en[i3]), align_to_heading);
                delta2 = align_to_heading - heading2;
                // To align a segment, rotate around its center
                EastNorth pivot1 = new EastNorth((en[i1].east()+en[i2].east())/2, (en[i1].north()+en[i2].north())/2);
                EastNorth A=en[i1].rotate(pivot1, delta1);
                EastNorth B=en[i2].rotate(pivot1, delta1);
                EastNorth pivot2 = new EastNorth((en[i2].east()+en[i3].east())/2, (en[i2].north()+en[i3].north())/2);
                EastNorth C=en[i2].rotate(pivot2, delta2);
                EastNorth D=en[i3].rotate(pivot2, delta2);

                // compute intersection of segments
                double u=det(B.east() - A.east(), B.north() - A.north(),
                        C.east() - D.east(), C.north() - D.north());

                // Check for parallel segments and do nothing if they are
                // In practice this will probably only happen when a way has
                // been duplicated

                if (u == 0) continue;

                // q is a number between 0 and 1
                // It is the point in the segment where the intersection occurs
                // if the segment is scaled to length 1

                double q = det(B.north() - C.north(), B.east() - C.east(),
                        D.north() - C.north(), D.east() - C.east()) / u;
                EastNorth intersection = new EastNorth(
                        B.east() + q * (A.east() - B.east()),
                        B.north() + q * (A.north() - B.north()));

                Node n = way.nodes.get(i2);

                LatLon ill = Main.proj.eastNorth2latlon(intersection);
                if (!ill.equalsEpsilon(n.getCoor())) {
                    double dx = intersection.east()-n.getEastNorth().east();
                    double dy = intersection.north()-n.getEastNorth().north();
                    cmds.add(new MoveCommand(n, dx, dy));
                }
            }
        }

        if (cmds.size() > 0) {
            Main.main.undoRedo.add(new SequenceCommand(tr("Orthogonalize"), cmds));
            Main.map.repaint();
        }
    }

    static double det(double a, double b, double c, double d)
    {
        return a * d - b * c;
    }

    static double normalize_angle(double h) {
        return normalize_angle(h, 0.0);
    }
    static double normalize_angle(double h, double align_to) {
        double llimit = -Math.PI/4;
        double ulimit = Math.PI/4;
        while (h - align_to > ulimit) h -= Math.PI/2;
        while (h - align_to < llimit) h += Math.PI/2;

        return h;
    }

    static double heading_diff(double h1, double h2) {
        double heading_delta = h1 > h2 ? h1 - h2 : h2 - h1;
        return heading_delta;
    }
}
