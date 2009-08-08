package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.tools.CopyList;

/**
 * An relation, having a set of tags and any number (0...n) of members.
 *
 * @author Frederik Ramm <frederik@remote.org>
 */
public final class Relation extends OsmPrimitive {

    /**
     * All members of this relation. Note that after changing this,
     * makeBackReferences and/or removeBackReferences should be called.
     */
    public final List<RelationMember> members = new ArrayList<RelationMember>();

    /**
     * @return Members of the relation. Changes made in returned list are not mapped
     * back to the primitive, use setMembers() to modify the members
     * @since 1925
     */
    public List<RelationMember> getMembers() {
        return new CopyList<RelationMember>(members.toArray(new RelationMember[members.size()]));
    }

    /**
     *
     * @param members
     * @since 1925
     */
    public void setMembers(List<RelationMember> members) {
        this.members.clear();
        this.members.addAll(members);
    }

    final static String[] defnames = {"name", "ref", "restriction", "note"};
    static Collection<String> names = null;

    @Override public void visit(Visitor visitor) {
        visitor.visit(this);
    }

    /**
     * Create an identical clone of the argument (including the id)
     */
    public Relation(Relation clone) {
        cloneFrom(clone);
    }

    /**
     * Create an incomplete Relation.
     */
    public Relation(long id) {
        this.id = id;
        incomplete = true;
    }

    /**
     * Create an empty Relation. Use this only if you set meaningful values
     * afterwards.
     */
    public Relation() {
    }

    @Override public void cloneFrom(OsmPrimitive osm) {
        super.cloneFrom(osm);
        members.clear();
        // we must not add the members themselves, but instead
        // add clones of the members
        for (RelationMember em : ((Relation)osm).getMembers()) {
            members.add(new RelationMember(em));
        }
    }

    @Override public String toString() {
        // return "{Relation id="+id+" version="+version+" members="+Arrays.toString(members.toArray())+"}";
        // adding members in string increases memory usage a lot and overflows for looped relations
        return "{Relation id="+id+" version="+version+"}";
    }

    @Override
    public boolean hasEqualSemanticAttributes(OsmPrimitive other) {
        if (other == null || ! (other instanceof Relation) )
            return false;
        if (! super.hasEqualSemanticAttributes(other))
            return false;
        Relation r = (Relation)other;
        return members.equals(r.members);
    }

    public int compareTo(OsmPrimitive o) {
        return o instanceof Relation ? Long.valueOf(id).compareTo(o.id) : -1;
    }

    @Override
    public String getName() {
        String name;
        if (incomplete) {
            name = tr("incomplete");
        } else {
            name = get("type");
            if (name == null) {
                name = tr("relation");
            }

            name += " (";
            if(names == null) {
                names = Main.pref.getCollection("relation.nameOrder", Arrays.asList(defnames));
            }
            String nameTag = null;
            for (String n : names) {
                nameTag = get(n);
                if (nameTag != null) {
                    break;
                }
            }
            if (nameTag != null) {
                name += "\"" + nameTag + "\", ";
            }

            int mbno = members.size();
            name += trn("{0} member", "{0} members", mbno, mbno) + ")";
            if(errors != null) {
                name = "*"+name;
            }
        }
        return name;
    }

    // seems to be different from member "incomplete" - FIXME
    public boolean isIncomplete() {
        for (RelationMember m : members)
            if (m.member == null)
                return true;
        return false;
    }

    public RelationMember firstMember() {
        if (incomplete) return null;
        return (members.size() == 0) ? null : members.get(0);
    }
    public RelationMember lastMember() {
        if (incomplete) return null;
        return (members.size() == 0) ? null : members.get(members.size() -1);
    }

    /**
     * removes all members with member.member == primitive
     *
     * @param primitive the primitive to check for
     */
    public void removeMembersFor(OsmPrimitive primitive) {
        if (primitive == null)
            return;

        ArrayList<RelationMember> todelete = new ArrayList<RelationMember>();
        for (RelationMember member: members) {
            if (member.member == primitive) {
                todelete.add(member);
            }
        }
        members.removeAll(todelete);
    }
}
