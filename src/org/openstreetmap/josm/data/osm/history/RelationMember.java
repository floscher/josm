// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;

/**
 * Represents a relation member in the the context of a historical view on
 * OSM data.
 *
 */
public class RelationMember {

    private String role;
    private OsmPrimitiveType primitiveType;
    private long primitiveId;

    /**
     * 
     * @param role  the role
     * @param primitiveType  the type (must not be null)
     * @param primitiveId the id (>0 required)
     * 
     * @exception IllegalArgumentException thrown, if primitiveType is null
     * @exception IllegalArgumentException thrown, if primitiveId <= 0
     */
    public RelationMember(String role, OsmPrimitiveType primitiveType, long primitiveId) {
        this.role = (role == null ? "" : role);
        if (primitiveType == null)
            throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "primitiveType"));
        this.primitiveType = primitiveType;
        if (primitiveId <=0)
            throw new IllegalArgumentException(tr("parameter ''{0}'' > 0 expected, got ''{1}''", "primitiveId", primitiveId));
        this.primitiveId = primitiveId;
    }

    /**
     * replies the member role
     * @return the member role
     */
    public String getRole() {
        return role;
    }

    /**
     * replies the type of the referenced OSM primitive
     * 
     * @return the type of the referenced OSM primitive
     */
    public OsmPrimitiveType getPrimitiveType() {
        return primitiveType;
    }

    /**
     * replies the id of the referenced OSM primitive
     * 
     * @return the id of the referenced OSM primitive
     */
    public long getPrimitiveId() {
        return primitiveId;
    }
}
